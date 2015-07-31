package lanchon.dexpatcher.gradle

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.PrepareLibraryTask
import com.android.build.gradle.tasks.MergeResources
import com.android.ide.common.res2.ResourceSet
import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.extensions.AbstractPatcherExtension
import org.gradle.api.DomainObjectSet
import org.gradle.api.tasks.Sync

// TODO: Warn on outdated apk lib based on plugin version that created it.
// TODO: Automatically set dexpatcher api level.
// TODO: Warn on incorrect android defaultConfig info.

// Parsing of 'apktool.yml' (contains defaultConfig info):
// http://yaml.org/
// https://bitbucket.org/asomov/snakeyaml
// https://github.com/iBotPeaches/Apktool/blob/master/brut.apktool/apktool-lib/src/main/java/brut/androlib/Androlib.java#L243

// Pending plugins:

// TODO: apktool-application (may bring in dexptacher if java plugin is applied too)
// TODO: maybe apktool-dexpatcher-application
// TODO: maybe apktool-smali-application

@CompileStatic
abstract class AbstractPatcherPlugin extends AbstractPlugin {

    protected abstract AbstractPatcherExtension getPatcherExtension()
    protected abstract BaseExtension getAndroidExtension()
    protected abstract DomainObjectSet<? extends BaseVariant> getAndroidVariants()

    protected void applyAndroid() {

        project.afterEvaluate {

            // TODO: Conditionally disable workarounds once issues are fixed.
            applyWorkaroundForJarsInExplodedAarBug()
            if (/* patcherExtension.patchResources */ true) applyWorkaroundForPublicXmlMergeBug()

            androidVariants.all { BaseVariant variant ->
                setupExplodedApkLibraryDir(variant)
                //if (!patcherExtension.patchManifest) bypassManifest(variant)            // TODO
                //if (!patcherExtension.patchResources) bypassAndroidResources(variant)   // TODO
                if (patcherExtension.patchCode) processCode(variant)
                else bypassCode(variant)
            }

        }

    }

    // APK Library

    protected void afterPrepareDependencies(BaseVariant variant, Closure closure) {
        project.tasks.getByName("prepare${variant.name.capitalize()}Dependencies").doLast {
            closure.maximumNumberOfParameters ? closure(getExplodedApkLibraryDir(variant)) : closure()
        }
    }

    protected File getExplodedApkLibraryDir(BaseVariant variant) {
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

    protected void processCode(BaseVariant variant) {}

    protected void bypassCode(BaseVariant variant) {
        variant.generateBuildConfig.enabled = false
        variant.aidlCompile.enabled = false
        variant.javaCompiler.enabled = false
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
