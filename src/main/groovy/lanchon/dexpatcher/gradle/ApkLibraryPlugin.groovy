package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.extensions.ApkLibraryExtension
import lanchon.dexpatcher.gradle.tasks.DecodeApkTask
import lanchon.dexpatcher.gradle.tasks.Dex2jarTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip

// TODO: Add plugin version to apk libs.
// TODO: Maybe select apktool decode api level automatically.
// (But it might only be used by baksmali, which is bypassed.)

@CompileStatic
class ApkLibraryPlugin extends AbstractPlugin {

    protected ApkLibraryExtension apkLibrary

    void apply(Project project) {

        super.apply(project)

        def subextensions = (dexpatcherConfig as ExtensionAware).extensions
        apkLibrary = (ApkLibraryExtension) subextensions.create(ApkLibraryExtension.EXTENSION_NAME, ApkLibraryExtension)

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
        def dex2jarDir = new File(modIntermediateDir, 'dex2jar')
        def dex2jarUnifiedDir = new File(modIntermediateDir, 'dex2jar-unified')
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
            outputDir = dex2jarDir
        }

        def dex2jarUnify = createDex2jarUnifyTask(project, taskNameModifier('dex2jarUnify'), dex2jarDir)
        dex2jarUnify.with {
            description = "Pack translated Java bytecode into a unified jar"
            group = taskGroup
            dependsOn dex2jar
            destinationDir = dex2jarUnifiedDir
        }

        def apkLibrary = createApkLibraryTask(project, taskNameModifier('apkLibrary'), apktoolDir, dex2jarUnifiedDir)
        apkLibrary.with {
            description = "Packs the decoded and translated application into an apk library."
            group = taskGroup
            dependsOn dex2jarUnify
            destinationDir = libraryDir
            baseName = project.name
        }

        apkLibrary.extensions.add 'decodeApkTask', decodeApk
        apkLibrary.extensions.add 'dex2jarTask', dex2jar
        apkLibrary.extensions.add 'dex2jarUnifyTask', dex2jarUnify
        apkLibrary.extensions.add 'apkLibraryTask', apkLibrary

        return apkLibrary

    }

    static Zip createDex2jarUnifyTask(Project project, String name, File dex2jarDir) {

        def dex2jarUnify = project.tasks.create(name, Zip)
        dex2jarUnify.with {
            duplicatesStrategy = DuplicatesStrategy.FAIL
            archiveName = 'classes.jar'
            inputs.sourceDir(dex2jarDir)
        }

        dex2jarUnify.doFirst {
            project.fileTree(dex2jarDir).each {
                dex2jarUnify.from(project.zipTree(it))
            }
        }

        return dex2jarUnify

    }

    static Zip createApkLibraryTask(Project project, String name, File apktoolDir, File dex2jarUnifiedDir) {

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
            from(dex2jarUnifiedDir)

        }
        return apkLibrary

    }

    private void createCleanTasks(String taskGroup) {

        def clean = (Delete) project.tasks.getByName(BasePlugin.CLEAN_TASK_NAME)
        clean.actions.clear()

        def cleanApkLibrary = project.tasks.create('cleanApkLibrary', Delete)
        cleanApkLibrary.with {
            description = "Deletes the build directory of an apk library project."
            group = taskGroup
            dependsOn { clean.dependsOn - cleanApkLibrary }
            delete { clean.delete }
        }

        clean.mustRunAfter cleanApkLibrary

        def cleanAll = project.tasks.create('cleanAll')
        cleanAll.with {
            description = "Cleans all projects, including the apk library project."
            group = BasePlugin.BUILD_GROUP
            dependsOn {
                project.rootProject.allprojects.findResults { it.tasks.findByName(BasePlugin.CLEAN_TASK_NAME) }
            }
            dependsOn cleanApkLibrary
        }

        project.afterEvaluate {
            if (!apkLibrary.disableClean) clean.dependsOn cleanApkLibrary
        }

    }

}
