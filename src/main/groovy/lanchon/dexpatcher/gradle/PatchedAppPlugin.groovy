package lanchon.dexpatcher.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.MergeAssets
import com.android.build.gradle.tasks.PreDex
import com.android.ide.common.res2.AssetSet
import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.extensions.AbstractPatcherExtension
import lanchon.dexpatcher.gradle.extensions.PatchedAppExtension
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Sync

@CompileStatic
class PatchedAppPlugin extends AbstractPatcherPlugin {

    protected PatchedAppExtension patchedAppExtension
    protected AppExtension appExtension
    protected DefaultDomainObjectSet<ApplicationVariant> appVariants

    @Override protected AbstractPatcherExtension getPatcherExtension() { patchedAppExtension }
    @Override protected BaseExtension getAndroidExtension() { appExtension }
    @Override protected DomainObjectSet<? extends BaseVariant> getAndroidVariants() { appVariants }

    @Override
    void apply(Project project) {

        super.apply(project)

        def subextensions = (dexpatcherConfig as ExtensionAware).extensions
        patchedAppExtension = (PatchedAppExtension) subextensions.create(PatchedAppExtension.EXTENSION_NAME, PatchedAppExtension)

        project.plugins.apply(AppPlugin)
        appExtension = project.extensions.getByType(AppExtension)
        appVariants = appExtension.applicationVariants

        applyAndroid()

    }

    @Override
    protected void applyAndroid() {

        super.applyAndroid()

        def libClasspath = project.dependencies.create(dexpatcherConfig.getLibClasspath())
        project.configurations.getByName('compile').dependencies.add(libClasspath)

        project.afterEvaluate {
            appVariants.all { ApplicationVariant variant ->
                processJavaResources(variant)
            }
        }

    }

    // Code

    @Override
    protected void processCode(BaseVariant variant) {
        super.processCode(variant)
        def appVariant = (ApplicationVariant) variant
        afterPrepareDependencies(appVariant) { File apkDir ->
            def excludeFromDex = project.fileTree(new File(apkDir, 'jars'))
            def dex = appVariant.dex
            if (dex) {
                if (dex.inputFiles) dex.inputFiles = (dex.inputFiles as List) - excludeFromDex
                def preDex = (PreDex) project.tasks.findByName("preDex${appVariant.name.capitalize()}")
                if (preDex && preDex.inputFiles) preDex.inputFiles = (preDex.inputFiles as List) - excludeFromDex
            } else {
                // TODO: Support Jack compiler.
                throw new RuntimeException('The Jack compiler is not supported')
            }
        }
        createPatchDexTask(appVariant)
    }

    @Override
    protected void bypassCode(BaseVariant variant) {
        super.processCode(variant)
        def appVariant = (ApplicationVariant) variant
        project.tasks.findByName("preDex${appVariant.name.capitalize()}")?.setEnabled(false)
        appVariant.dex?.setEnabled(false)
        createImportDexTask(appVariant)
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

    // Java Resources

    private void processJavaResources(ApplicationVariant variant) {
        def mergeJavaRes = createMergeJavaResTask(variant)
        variant.outputs.each {
            if (it instanceof ApkVariantOutput) {
                it.packageApplication.javaResourceDir = mergeJavaRes.outputDir
            }
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

}
