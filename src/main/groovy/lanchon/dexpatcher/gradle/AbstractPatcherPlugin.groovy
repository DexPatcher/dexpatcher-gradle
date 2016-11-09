package lanchon.dexpatcher.gradle

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.PrepareLibraryTask
import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.extensions.AbstractPatcherExtension
import org.gradle.api.DomainObjectSet
import org.gradle.api.Task

import java.nio.file.Files
import java.nio.file.StandardCopyOption

// TODO: Locate apk lib based on bundle extension at afterEvaluate time.

// TODO: Warn on outdated apk lib based on plugin version that created it.
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

    protected static class ApkLibraryPaths {

        final File rootDir
        final File dexpatcherDir
        final File dexDir
        final File dedexFile
        final File publicXmlFile

        protected ApkLibraryPaths(File apkLibDir) {
            rootDir = apkLibDir
            dexpatcherDir = new File(rootDir, 'dexpatcher')
            dexDir = new File(dexpatcherDir, 'dex')
            dedexFile = new File(dexpatcherDir, 'dedex/classes.zip')
            publicXmlFile = new File(dexpatcherDir, 'values/public.xml')
        }

    }

    protected File dexpatcherDir
    protected ApkLibraryPaths apkLibraryUnchecked

    protected ApkLibraryPaths getApkLibrary() {
        if (!apkLibraryUnchecked) throw new RuntimeException('Apk library not found')
        return apkLibraryUnchecked
    }

    protected abstract AbstractPatcherExtension getPatcherExtension()
    protected abstract BaseExtension getAndroidExtension()
    protected abstract DomainObjectSet<? extends BaseVariant> getAndroidVariants()

    protected void applyAfterAndroid() {

        dexpatcherDir = new File(project.buildDir, 'intermediates/dexpatcher')

        // The 'compile' scope could also be used for patched apps (but not for patch libs).
        def libClasspath = project.dependencies.create(dexpatcherConfig.getLibClasspath())
        project.configurations.getByName('provided').dependencies.add(libClasspath)

        // Setup 'apkLibrary' property.
        project.afterEvaluate {
            afterPrepareApkLibrary { PrepareLibraryTask task ->
                if (apkLibraryUnchecked) throw new RuntimeException('Multiple apk libraries found')
                apkLibraryUnchecked = new ApkLibraryPaths(task.explodedDir)
            }
        }

        addDedexedClassesToProvidedScope()
        workaroundForPublicXmlMergeBug()

    }

    private addDedexedClassesToProvidedScope() {
        // Add a non-existent jar file to the 'provided' scope.
        def dedexFile = new File(dexpatcherDir, 'dedex/classes.jar')
        def dedexDependency = project.dependencies.create(project.files(dedexFile))
        project.configurations.getByName('provided').dependencies.add(dedexDependency)
        // And later copy the dedexed classes of the apk library into that empty slot.
        project.afterEvaluate {
            afterPrepareApkLibrary { PrepareLibraryTask task ->
                com.google.common.io.Files.createParentDirs(dedexFile)
                Files.copy(apkLibraryUnchecked.dedexFile.toPath(), dedexFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private void workaroundForPublicXmlMergeBug() {
        project.afterEvaluate {
            // Get 'public.xml' out of the resource merge inputs.
            prepareApkLibraryDoLast { PrepareLibraryTask task ->
                def apkLibrary = new ApkLibraryPaths(task.explodedDir)
                def fromFile = new File(apkLibrary.rootDir, 'res/values/public.xml')
                com.google.common.io.Files.createParentDirs(apkLibrary.publicXmlFile)
                Files.move(fromFile.toPath(), apkLibrary.publicXmlFile.toPath())
            }
            // And later add a copy to the output of the merge.
            androidVariants.all { BaseVariant variant ->
                def task = variant.mergeResources
                task << {
                    def toFile = new File(task.outputDir, 'values/dexpatcher-public.xml')
                    com.google.common.io.Files.createParentDirs(toFile)
                    Files.copy(apkLibrary.publicXmlFile.toPath(), toFile.toPath())
                }
            }
        }
    }

    // APK Library

    protected void prepareApkLibraryDoLast(Closure closure) {
        project.tasks.withType(PrepareLibraryTask).each { task ->
            task.doLast {
                if (isPrepareApkLibraryTask(task)) {
                    closure.maximumNumberOfParameters ? closure(task) : closure()
                }
            }
        }
    }

    protected void afterPrepareApkLibrary(Closure closure) {
        project.gradle.taskGraph.afterTask { task ->
            if (task instanceof PrepareLibraryTask && task.project.is(project)) {
                if (isPrepareApkLibraryTask(task)) {
                    closure.maximumNumberOfParameters ? closure(task) : closure()
                }
            }
        }
    }

    private boolean isPrepareApkLibraryTask (PrepareLibraryTask task) {
        return new File(task.explodedDir, 'dexpatcher').directory
    }

    // Task Graph

    protected void beforeTask(Task task, Closure closure) {
        project.gradle.taskGraph.beforeTask {
            if (it.is(task)) {
                closure.maximumNumberOfParameters ? closure(task) : closure()
            }
        }
    }

    protected void afterTask(Task task, Closure closure) {
        project.gradle.taskGraph.afterTask {
            if (it.is(task)) {
                closure.maximumNumberOfParameters ? closure(task) : closure()
            }
        }
    }

    /*
    protected void prepareDependenciesDoLast(BaseVariant variant, Closure closure) {
        PrepareDependenciesTask task = (PrepareDependenciesTask) project.tasks
                .getByName("prepare${variant.name.capitalize()}Dependencies")
        task.doLast {
            closure.maximumNumberOfParameters ? closure(task) : closure()
        }
    }

    private PrepareLibraryTask getPrepareApkLibraryTask(BaseVariant variant) {
        def apkLibTasks = project.tasks.withType(PrepareLibraryTask).findAll { PrepareLibraryTask task ->
            isPrepareApkLibraryTask(task) && task.dependsOn.contains(variant.preBuild)
        }
        if (apkLibTasks.isEmpty()) throw new RuntimeException("No apk library found in variant '${variant.name}'")
        if (apkLibTasks.size() > 1) throw new RuntimeException("Multiple apk libraries found in variant '${variant.name}'")
        return (PrepareLibraryTask) apkLibTasks[0]
    }

    /*
    // BYPASS PROCESSING

        project.afterEvaluate {
            androidVariants.all { BaseVariant variant ->
                //if (!patcherExtension.patchManifest) bypassManifest(variant)
                //if (!patcherExtension.patchResources) bypassAndroidResources(variant)
                if (patcherExtension.patchCode) processCode(variant)
                else bypassCode(variant)
            }
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
        afterPrepareDependencies(variant) { File libDir ->
            importResources.from new File(libDir, 'res')
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
    */

}
