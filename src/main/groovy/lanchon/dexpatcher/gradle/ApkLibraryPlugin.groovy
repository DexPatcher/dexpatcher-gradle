/*
 * DexPatcher - Copyright 2015-2017 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.extensions.ApkLibraryExtension
import lanchon.dexpatcher.gradle.tasks.DecodeApkTask
import lanchon.dexpatcher.gradle.tasks.Dex2jarTask

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
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
        apkLibrary = (ApkLibraryExtension) subextensions.create(ApkLibraryExtension.EXTENSION_NAME,
                ApkLibraryExtension, project, dexpatcherConfig)

        project.plugins.apply(BasePlugin)

        def apkLibrary = createTaskChain(project, DexpatcherBasePlugin.TASK_GROUP, { it }, { it },
                apkLibrary.apkFileOrDir)
        project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(apkLibrary)
        project.artifacts.add(Dependency.DEFAULT_CONFIGURATION, apkLibrary)

        createCleanTasks(DexpatcherBasePlugin.TASK_GROUP)

    }

    static Zip createTaskChain(Project project, String taskGroup, Closure<String> taskNameModifier,
            Closure<File> dirModifier, Provider<File> apkFileOrDir) {

        def modIntermediateDir = dirModifier(Resolver.getFile(project.buildDir, 'intermediates'))
        def modOutputDir = dirModifier(Resolver.getFile(project.buildDir, 'outputs'))

        def apktoolDir = Resolver.getFile(modIntermediateDir, 'apktool')
        def apktoolFrameworkDir = Resolver.getFile(modIntermediateDir, 'apktool-framework')
        def dex2jarFile = Resolver.getFile(modIntermediateDir, 'dex2jar/classes.zip')
        def dex2jarExceptionFile = Resolver.getFile(modIntermediateDir, 'dex2jar/dex2jar-error.zip')
        def resourcesDir = Resolver.getFile(modIntermediateDir, 'resources')
        def libraryDir = Resolver.getFile(modOutputDir, 'aar')

        def decodeApk = project.tasks.create(taskNameModifier('decodeApk'), DecodeApkTask)
        decodeApk.with {
            description = "Unpacks an Android application and decodes its manifest and resources."
            group = taskGroup
            apkFile.set project.layout.file(project.providers.<File>provider {
                Resolver.resolveSingleFile(project, apkFileOrDir.get(), '*.apk')
            })
            outputDir.set apktoolDir
            frameworkDir.set null
            frameworkDirAsInput.set null
            frameworkDirAsOutput.set apktoolFrameworkDir
            decodeClasses.set false
            //keepBrokenResources.set true
        }
        decodeApk.doLast {
            printApkInfo decodeApk
        }

        def dex2jar = project.tasks.create(taskNameModifier('dex2jar'), Dex2jarTask)
        dex2jar.with {
            description = "Translates Dalvik bytecode into Java bytecode."
            group = taskGroup
            dependsOn decodeApk
            dexFiles.set  project.providers.<FileCollection>provider {
                project.files(decodeApk.apkFile)
            }
            outputFile.set dex2jarFile
            exceptionFile.set dex2jarExceptionFile
        }

        def resources = createResourcesTask(project, taskNameModifier('resources'), apktoolDir)
        resources.with {
            description = "Packs extra resources into a jar"
            group = taskGroup
            dependsOn decodeApk
            destinationDir = resourcesDir
        }

        def apkLibrary = createApkLibraryTask(project, taskNameModifier('apkLibrary'), apktoolDir, dex2jarFile,
                dex2jarExceptionFile, resourcesDir)
        apkLibrary.with {
            description = "Packs the processed application into an apk library."
            group = taskGroup
            dependsOn decodeApk, dex2jar, resources
            destinationDir = libraryDir
            extension = 'aar'
            def apkName = decodeApk.apkFile.orNull?.asFile?.name ?: project.name ?: 'source'
            if (!apkName.toLowerCase(Locale.ENGLISH).endsWith('.apk')) apkName += '.apk'
            archiveName = apkName + '.' + extension
        }

        apkLibrary.extensions.add 'decodeApkTask', decodeApk
        apkLibrary.extensions.add 'dex2jarTask', dex2jar
        apkLibrary.extensions.add 'resources', resources
        apkLibrary.extensions.add 'apkLibraryTask', apkLibrary

        return apkLibrary

    }

    private static void printApkInfo(DecodeApkTask task) {
        def apktoolYmlFile = task.outputDir.get().file('apktool.yml').asFile
        if (apktoolYmlFile.isFile()) {
            def pattern = ~/^\s*(minSdkVersion|targetSdkVersion|versionCode|versionName):/
            println 'APK information:'
            apktoolYmlFile.eachLine { line ->
                if (pattern.matcher(line).find()) println line
            }
            if (task.resolvedAddBlankLines) println()
        }
    }

    static Zip createResourcesTask(Project project, String name, File apktoolDir) {

        def resources = project.tasks.create(name, Zip)
        resources.with {
            duplicatesStrategy = DuplicatesStrategy.FAIL
            archiveName = 'classes.jar'
            from(Resolver.getFile(apktoolDir, 'unknown'))
            from(Resolver.getFile(apktoolDir, 'original/META-INF')) { CopySpec spec ->
                spec.into 'META-INF'
            }
        }
        return resources

    }

    static Zip createApkLibraryTask(Project project, String name, File apktoolDir, File dex2jarFile,
            File dex2jarExceptionFile, File resourcesDir) {

        def apkLibrary = project.tasks.create(name, Zip)
        apkLibrary.with {

            duplicatesStrategy = DuplicatesStrategy.FAIL
            extension = 'apk.aar'

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
            from(Resolver.getFile(apktoolDir, 'lib')) { CopySpec spec ->
                spec.into 'jni'
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
            from(dex2jarFile) { CopySpec spec ->
                spec.into 'dexpatcher/dedex'
            }
            from(dex2jarExceptionFile) { CopySpec spec ->
                spec.into 'dexpatcher/dex2jar'
            }
            from(resourcesDir)

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
