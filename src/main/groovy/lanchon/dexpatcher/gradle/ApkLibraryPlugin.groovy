package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.extensions.DexpatcherConfigExtension
import lanchon.dexpatcher.gradle.tasks.DecodeApkTask
import lanchon.dexpatcher.gradle.tasks.Dex2jarTask
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip

// TODO: Maybe select apktool decode api level automatically.
// (But it might only be used by baksmali, which is bypassed.)

@CompileStatic
class ApkLibraryPlugin implements Plugin<Project> {

    protected Project project
    protected DexpatcherConfigExtension dexpatcherConfig

    void apply(Project project) {

        this.project = project
        project.plugins.apply(DexpatcherBasePlugin)
        dexpatcherConfig = project.extensions.getByType(DexpatcherConfigExtension)

        project.plugins.apply(BasePlugin)

        def apkLibrary = createTaskChain(project, DexpatcherBasePlugin.TASK_GROUP, { it }, { it })
        project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(apkLibrary)
        project.artifacts.add(Dependency.DEFAULT_CONFIGURATION, apkLibrary)

        createCleanTasks(DexpatcherBasePlugin.TASK_GROUP)

    }

    static Zip createTaskChain(Project project, String taskGroup, Closure<String> taskNameModifier, Closure<File> dirModifier) {

        def modApkDir = dirModifier(project.file('apk'))
        def modIntermediateDir = dirModifier(new File(project.buildDir, 'intermediates'))
        def modOutputDir = dirModifier(new File(project.buildDir, 'outputs'))

        def apktoolDir = new File(modIntermediateDir, 'apktool')
        def dex2jarFile = new File(modIntermediateDir, 'dex2jar/classes.jar')
        def libraryDir = new File(modOutputDir, 'aar')

        def decodeApk = project.tasks.create(taskNameModifier('decodeApk'), DecodeApkTask)
        decodeApk.with {
            description = "Unpacks an Android application and decodes its manifest and resources."
            group = taskGroup
            apkFile = {
                def tree = project.fileTree(modApkDir)
                tree.include '*.apk'
                def files = tree.getFiles()
                if (files.isEmpty()) throw new RuntimeException("No apk file found in '$modApkDir'")
                if (files.size() > 1) throw new RuntimeException("Multiple apk files found in '$modApkDir'")
                return files[0]
            }
            outputDir = apktoolDir
            decodeClasses = false
            //keepBrokenResources = true
            forceOverwrite = true
        }

        def dex2jar = project.tasks.create(taskNameModifier('dex2jar'), Dex2jarTask)
        dex2jar.with {
            description = "Translates Dalvik bytecode into Java bytecode."
            group = taskGroup
            dependsOn decodeApk
            dexFiles = {
                def tree = project.fileTree(apktoolDir)
                tree.include '*.dex'
            }
            jarFile = dex2jarFile
            forceOverwrite = true
        }

        def apkLibrary = createApkLibraryTask(project, taskNameModifier('apkLibrary'), apktoolDir, dex2jarFile)
        apkLibrary.with {
            description = "Packs the decoded and translated application into an apk library."
            group = taskGroup
            dependsOn dex2jar
            destinationDir = libraryDir
            baseName = project.name
        }

        apkLibrary.extensions.add 'decodeApkTask', decodeApk
        apkLibrary.extensions.add 'dex2jarTask', dex2jar
        apkLibrary.extensions.add 'apkLibraryTask', apkLibrary

        return apkLibrary

    }

    static Zip createApkLibraryTask(Project project, String name, File apktoolDir, File dex2jarFile) {

        def apkLibrary = project.tasks.create(name, Zip)
        apkLibrary.with {

            duplicatesStrategy = DuplicatesStrategy.FAIL
            appendix = 'apk'
            extension = 'aar'

            /*
            AAR Format:
                /AndroidManifest.xml (mandatory)
                /classes.jar (mandatory)
                /res/ (mandatory)
                /R.txt (mandatory)
                /assets/ (optional)
                /libs/*.jar (optional)
                /jni/<abi>/*.so (optional)
                /proguard.txt (optional)
                /lint.jar (optional)
             */

            from(apktoolDir) { CopySpec spec ->
                spec.with {
                    include 'AndroidManifest.xml'
                    include 'res/'
                    include 'assets/'
                }
            }
            from(new File(apktoolDir, 'lib')) { CopySpec spec ->
                spec.into 'jni'
            }
            from(new File(apktoolDir, 'unknown')) { CopySpec spec ->
                spec.into 'resources'
            }
            from(apktoolDir) { CopySpec spec ->
                spec.with {
                    include '*.dex'
                    into 'dexpatcher/dex'
                }
            }
            from(apktoolDir) { CopySpec spec ->
                spec.with {
                    exclude 'AndroidManifest.xml'
                    exclude 'res'
                    exclude 'assets'
                    exclude 'lib'
                    exclude 'unknown'
                    exclude '*.dex'
                    into 'dexpatcher/apktool'
                }
            }
            from(dex2jarFile)

        }
        return apkLibrary

    }

    private void createCleanTasks(String taskGroup) {

        def clean = (Delete) project.tasks.getByName(BasePlugin.CLEAN_TASK_NAME)

        def cleanApkLibrary = project.tasks.create('cleanApkLibrary', Delete)
        cleanApkLibrary.with {
            description = "Deletes the build directory of an apk library project."
            group = taskGroup
            dependsOn { clean.dependsOn }
            delete { clean.delete }
        }

        def cleanAll = project.tasks.create('cleanAll')
        cleanAll.with {
            description = "Cleans all projects, including the apk library project."
            group = BasePlugin.BUILD_GROUP
            dependsOn project.rootProject.getAllTasks(true)
                    .collectMany { entry -> entry.value }
                    .findAll { Task task -> task.name == BasePlugin.CLEAN_TASK_NAME }
        }

        project.afterEvaluate {
            if (dexpatcherConfig.apkLibraryDisableClean) {
                clean.enabled = false
                cleanAll.dependsOn cleanApkLibrary
            }
        }

    }

}
