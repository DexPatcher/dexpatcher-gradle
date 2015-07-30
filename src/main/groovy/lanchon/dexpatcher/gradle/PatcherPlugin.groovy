package lanchon.dexpatcher.gradle

import com.android.build.gradle.*
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.PrepareLibraryTask
import com.android.build.gradle.tasks.MergeAssets
import com.android.build.gradle.tasks.MergeResources
import com.android.build.gradle.tasks.PreDex
import com.android.ide.common.res2.AssetSet
import com.android.ide.common.res2.ResourceSet
import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Sync

// TODO: Add plugin version to apk libs.
// TODO: Warn on outdated apk lib.
// TODO: Automatically set dexpatcher api level.
// TODO: Warn on incorrect defaultConfig info.

// Parsing of 'apktool.yml' (contains defaultConfig info):
// http://yaml.org/
// https://bitbucket.org/asomov/snakeyaml
// https://github.com/iBotPeaches/Apktool/blob/master/brut.apktool/apktool-lib/src/main/java/brut/androlib/Androlib.java#L243

// Pending plugins:

// TODO: patch-library
// TODO: apktool-application (may bring in dexptacher if java plugin is applied too)
// TODO: maybe apktool-dexpatcher-application
// TODO: maybe apktool-smali-application

@CompileStatic
class PatcherPlugin extends AbstractPlugin {

    protected BaseExtension android
    protected DomainObjectSet<? extends BaseVariant> variants

    void apply(Project project) {

        super.apply(project)

        project.plugins.withType(AppPlugin) {
            def androidApp = project.extensions.getByType(AppExtension)
            android = androidApp
            variants = androidApp.applicationVariants
            applyAndroid()
        }
        project.plugins.withType(LibraryPlugin) {
            def androidLib = project.extensions.getByType(LibraryExtension)
            android = androidLib
            variants = androidLib.libraryVariants
            applyAndroid()
        }

    }

    private void applyAndroid() {

        def libClasspath = project.dependencies.create(dexpatcherConfig.getLibClasspath())
        project.configurations.getByName('compile').dependencies.add(libClasspath)

        project.afterEvaluate {

            // TODO: Conditionally disable workarounds once issues are fixed.
            applyWorkaroundForJarsInExplodedAarBug()
            if (/* dexpatcher.patchResources */ true) applyWorkaroundForPublicXmlMergeBug()

            variants.all { BaseVariant variant ->
                setupExplodedApkLibraryDir(variant)
                processJavaResources(variant)
                //if (!dexpatcher.patchManifest) bypassManifest(variant)            // TODO
                //if (!dexpatcher.patchResources) bypassAndroidResources(variant)   // TODO
                if (dexpatcherConfig.patchCode) processCode(variant)
                else bypassCode(variant)
            }

        }

    }

    // APK Library

    private void afterPrepareDependencies(BaseVariant variant, Closure closure) {
        project.tasks.getByName("prepare${variant.name.capitalize()}Dependencies").doLast {
            closure.maximumNumberOfParameters ? closure(getExplodedApkLibraryDir(variant)) : closure()
        }
    }

    private File getExplodedApkLibraryDir(BaseVariant variant) {
        (File) variant.assemble.extensions.getByName('explodedApkLibraryDir')
    }

    private void setupExplodedApkLibraryDir(BaseVariant variant) {
        afterPrepareDependencies(variant) { ->
            def apkLibTask = getPrepareApkLibraryTask(variant)
            variant.assemble.extensions.add('explodedApkLibraryDir', apkLibTask.explodedDir as File)
        }
    }

    private PrepareLibraryTask getPrepareApkLibraryTask(BaseVariant variant) {
        def apkLibTasks = project.tasks.findAll { task ->
            return task instanceof PrepareLibraryTask &&
                    task.dependsOn.contains(variant.preBuild) &&
                    new File(task.explodedDir, 'dexpatcher').exists()
        }
        if (apkLibTasks.isEmpty()) throw new RuntimeException("No apk library found in variant '${variant.name}'")
        if (apkLibTasks.size() > 1) throw new RuntimeException("Multiple apk libraries found in variant '${variant.name}'")
        return (PrepareLibraryTask) apkLibTasks[0]
    }

    // Java Resources

    private void processJavaResources(BaseVariant variant) {
        def mergeJavaRes = createMergeJavaResTask(variant)
        variant.outputs.each {
            if (it instanceof ApkVariantOutput) {
                it.packageApplication.javaResourceDir = mergeJavaRes.outputDir
            }
            // TODO: Support patch library output.
        }
    }

    private MergeAssets createMergeJavaResTask(BaseVariant variant) {
        def mergeAssets = variant.mergeAssets
        def processJavaResources = variant.processJavaResources
        def mergeJavaRes = project.tasks.create("merge${variant.name.capitalize()}JavaRes", MergeAssets)
        mergeJavaRes.with {
            description = "Merge the java resource tree that will be packaged into the root of the apk."
            group = DexpatcherBasePlugin.TASK_GROUP
            dependsOn = mergeAssets.dependsOn
            dependsOn processJavaResources
            androidBuilder = mergeAssets.androidBuilder
            incrementalFolder = new File(project.buildDir, "intermediates/incremental/mergeJavaRes/${variant.dirName}")
            outputDir = new File(project.buildDir, "intermediates/merged-java-resources/${variant.dirName}")
            onlyIf {
                // WARNING: Abusing onlyIf to configure task.
                def explodedAarPath = new File(project.buildDir, 'intermediates/exploded-aar').canonicalPath
                def javaResSets = new ArrayList<AssetSet>()
                mergeAssets.inputAssetSets.each { assetSet ->
                    def javaResSet = new AssetSet(assetSet.configName)
                    assetSet.sourceFiles.each {
                        def assetDir = (File) it
                        if (assetDir.canonicalPath.startsWith(explodedAarPath)) {
                            if (assetDir.name == 'assets') javaResSet.addSource new File(assetDir.parentFile, 'resources')
                            else logger.log LogLevel.WARN, "Could not derive library resource directory from library asset directory '$assetDir'"
                        }
                    }
                    if (!javaResSet.sourceFiles.empty) javaResSets.add(javaResSet)
                }
                def javaResSet = new AssetSet(processJavaResources.name)
                javaResSet.addSource processJavaResources.destinationDir
                javaResSets.add(javaResSet)
                mergeJavaRes.inputAssetSets = javaResSets
                return true
            }
        }
        mergeAssets.dependsOn mergeJavaRes
        variant.assemble.extensions.add 'mergeJavaResources', mergeJavaRes
        return mergeJavaRes
    }

    // Android Resources

    private void bypassAndroidResources(BaseVariant variant) {
        // TODO: Fix 9Patch issues and enable functionality.
        def mergeResources = variant.mergeResources
        project.tasks.getByName("generate${variant.name.capitalize()}ResValues").enabled = false
        mergeResources.enabled = false
        def importResources = project.tasks.create("import${variant.name.capitalize()}Resources", Sync)
        importResources.with {
            description = "Imports the resources from an apk library."
            group = DexpatcherBasePlugin.TASK_GROUP
            dependsOn = mergeResources.dependsOn
            into mergeResources.outputDir
        }
        afterPrepareDependencies(variant) { File apkDir ->
            importResources.from new File(apkDir, 'res')
        }
        mergeResources.dependsOn importResources
        variant.assemble.extensions.add 'importResources', importResources
    }

    // Code

    private void processCode(BaseVariant variant) {
        if (variant instanceof ApplicationVariant) {
            afterPrepareDependencies(variant) { File apkDir ->
                def excludeFromDex = project.fileTree(new File(apkDir, 'jars'))
                def dex = ((ApplicationVariant) variant).dex
                if (dex) {
                    if (dex.inputFiles) dex.inputFiles = (dex.inputFiles as List) - excludeFromDex
                    def preDex = (PreDex) project.tasks.findByName("preDex${variant.name.capitalize()}")
                    if (preDex && preDex.inputFiles) preDex.inputFiles = (preDex.inputFiles as List) - excludeFromDex
                } else {
                    // TODO: Support Jack compiler.
                    throw new RuntimeException("The Jack compiler is not supported")
                }
            }
            createPatchDexTask(variant)
        }
    }

    private DexpatcherTask createPatchDexTask(ApplicationVariant variant) {
        Task dexCreator = variant.dex ?: variant.javaCompiler
        File dexDir = variant.dex ? variant.dex.outputFolder : variant.javaCompiler.destinationDir
        File patchDexDir = new File(project.buildDir, "intermediates/patch-dex/${variant.dirName}")
        variant.dex ? (variant.dex.outputFolder = patchDexDir) : (variant.javaCompiler.destinationDir = patchDexDir)
        def patchDex = project.tasks.create("patch${variant.name.capitalize()}Dex", DexpatcherTask)
        patchDex.with {
            description = "Patches the source dex from an apk library using the just-built patch dex."
            group = DexpatcherBasePlugin.TASK_GROUP
            dependsOn dexCreator
            sourceFile = new File(patchDexDir, 'nonexistent-file')      // avoid null exception
            patchFiles = {
                def patchDexFiles = project.fileTree(patchDexDir)
                switch (patchDexFiles.files.size()) {
                    case 0: //throw new RuntimeException('No patch dex file found')     // avoid null exception
                    case 1: break
                    default: throw new RuntimeException('MultiDex patches are not supported')
                }
                return patchDexFiles as List<File>
            }
            outputFile = new File(dexDir, 'classes.dex')
        }
        afterPrepareDependencies(variant) { File apkDir ->
            def apkDexFiles = project.fileTree(new File(apkDir, 'dexpatcher/dex'))
            switch (apkDexFiles.files.size()) {
                case 0: throw new RuntimeException('No dex file found in apk library')
                case 1: break
                default: throw new RuntimeException('Cannot patch the code of MultiDex applications')
            }
            patchDex.sourceFile = apkDexFiles.singleFile
        }
        variant.outputs.each {
            if (it instanceof ApkVariantOutput) {
                it.packageApplication.with {
                    dependsOn patchDex
                    dexFolder = dexDir
                }
            }
        }
        variant.assemble.extensions.add 'patchDex', patchDex
        return patchDex
    }

    private void bypassCode(BaseVariant variant) {
        variant.generateBuildConfig.enabled = false
        variant.aidlCompile.enabled = false
        variant.javaCompiler.enabled = false
        if (variant instanceof ApplicationVariant) {
            project.tasks.findByName("preDex${variant.name.capitalize()}")?.setEnabled(false)
            variant.dex?.setEnabled(false)
            createImportDexTask(variant)
        }
    }

    private Sync createImportDexTask(ApplicationVariant variant) {
        Task dexCreator = variant.dex ?: variant.javaCompiler
        File dexDir = variant.dex ? variant.dex.outputFolder : variant.javaCompiler.destinationDir
        def importDex = project.tasks.create("import${variant.name.capitalize()}Dex", Sync)
        importDex.with {
            description = "Imports the dex file(s) from an apk library."
            group = DexpatcherBasePlugin.TASK_GROUP
            dependsOn dexCreator
            into dexDir
        }
        afterPrepareDependencies(variant) { File apkDir ->
            importDex.from new File(apkDir, 'dexpatcher/dex')
        }
        variant.outputs.each {
            if (it instanceof ApkVariantOutput) {
                it.packageApplication.dependsOn importDex
            }
        }
        variant.assemble.extensions.add 'importDex', importDex
        return importDex
    }

    // Workarounds

    private void applyWorkaroundForJarsInExplodedAarBug() {
        project.tasks.withType(PrepareLibraryTask) { PrepareLibraryTask task ->
            task << {
                project.logger.lifecycle 'Applying workaround for issue: https://code.google.com/p/android/issues/detail?id=172048'
                def base = task.explodedDir
                def fromBase = new File(base, 'jars')
                ['assets', 'dexpatcher', 'jni', 'resources'].each {
                    def fromDir = new File(fromBase, it)
                    if (fromDir.exists()) project.ant.invokeMethod('move', [file: fromDir, toDir: base])
                }

            }
        }
    }

    private void applyWorkaroundForPublicXmlMergeBug() {
        project.tasks.withType(PrepareLibraryTask) { PrepareLibraryTask task ->
            task << {
                def base = task.explodedDir
                def toDir = new File(base, 'dexpatcher')
                if (toDir.exists()) {
                    def fromFile = new File(base, 'res/values/public.xml')
                    if (fromFile.exists()) {
                        project.logger.lifecycle 'Applying workaround #1 of 2 for issue: https://code.google.com/p/android/issues/detail?id=170153'
                        project.ant.invokeMethod('move', [file: fromFile, toDir: toDir])
                    }
                }
            }
        }
        project.tasks.withType(MergeResources) { MergeResources task ->
            task << {
                def found = false
                task.inputResourceSets.each { ResourceSet resourceSet ->
                    resourceSet.sourceFiles.each { sourceDir ->
                        def fromFile = new File((File) sourceDir, '../dexpatcher/public.xml')
                        if (fromFile.exists()) {
                            if (found) throw new RuntimeException("Multiple 'public.xml' files found in resource merge set")
                            found = true
                            project.logger.lifecycle 'Applying workaround #2 of 2 for issue: https://code.google.com/p/android/issues/detail?id=170153'
                            def toFile = new File(task.outputDir, 'values/dexpatcher-public.xml')
                            project.ant.invokeMethod('copy', [file: fromFile, toFile: toFile])
                        }
                    }
                }
            }
        }
    }

}
