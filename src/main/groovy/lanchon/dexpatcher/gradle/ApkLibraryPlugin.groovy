/*
 * DexPatcher - Copyright 2015, 2016 Rodrigo Balerdi
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

    static Zip createTaskChain(Project project, String taskGroup, Closure<String> taskNameModifier,
            Closure<File> dirModifier) {

        def modApkDir = dirModifier(project.file('apk'))
        def modIntermediateDir = dirModifier(new File(project.buildDir, 'intermediates'))
        def modOutputDir = dirModifier(new File(project.buildDir, 'outputs'))

        def apktoolDir = new File(modIntermediateDir, 'apktool')
        def apktoolFrameworkDir = new File(modIntermediateDir, 'apktool-framework')
        def dex2jarFile = new File(modIntermediateDir, 'dex2jar/classes.zip')
        def dex2jarExceptionFile = new File(modIntermediateDir, 'dex2jar/dex2jar-error.zip')
        def resourcesDir = new File(modIntermediateDir, 'resources')
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
            setFrameworkDirAsOutput apktoolFrameworkDir
            decodeClasses = false
            //keepBrokenResources = true
        }
        decodeApk << {
            printApkInfo decodeApk
        }

        def dex2jar = project.tasks.create(taskNameModifier('dex2jar'), Dex2jarTask)
        dex2jar.with {
            description = "Translates Dalvik bytecode into Java bytecode."
            group = taskGroup
            dependsOn decodeApk
            dexFiles = { decodeApk.getApkFile() }
            outputFile = dex2jarFile
            exceptionFile = dex2jarExceptionFile
        }

        def resources = createResourcesTask(project, taskNameModifier('resources'), apktoolDir)
        resources.with {
            description = "Packs extra resources into a jar"
            group = taskGroup
            dependsOn decodeApk
            destinationDir = resourcesDir
        }

        def apkLibrary = createApkLibraryTask(project, taskNameModifier('apkLibrary'), apktoolDir, dex2jarFile,
                resourcesDir)
        apkLibrary.with {
            description = "Packs the processed application into an apk library."
            group = taskGroup
            dependsOn decodeApk, dex2jar, resources
            destinationDir = libraryDir
            extension = 'aar'
            def apkName = decodeApk.getApkFile().name ?: project.name ?: 'source'
            if (!apkName.toLowerCase().endsWith('.apk')) apkName += '.apk'
            archiveName = apkName + '.' + extension
        }

        apkLibrary.extensions.add 'decodeApkTask', decodeApk
        apkLibrary.extensions.add 'dex2jarTask', dex2jar
        apkLibrary.extensions.add 'resources', resources
        apkLibrary.extensions.add 'apkLibraryTask', apkLibrary

        return apkLibrary

    }

    private static void printApkInfo(DecodeApkTask task) {
        def apktoolYmlFile = new File(task.getOutputDir(), 'apktool.yml')
        if (apktoolYmlFile.file) {
            def pattern = ~/^\s*(minSdkVersion|targetSdkVersion|versionCode|versionName):/
            println 'APK information:'
            apktoolYmlFile.eachLine { line ->
                if (pattern.matcher(line).find()) println line
            }
            if (task.getAddBlankLines()) println()
        }
    }

    static Zip createResourcesTask(Project project, String name, File apktoolDir) {

        def resources = project.tasks.create(name, Zip)
        resources.with {
            duplicatesStrategy = DuplicatesStrategy.FAIL
            archiveName = 'classes.jar'
            from(new File(apktoolDir, 'unknown'))
            from(new File(apktoolDir, 'original/META-INF')) { CopySpec spec ->
                spec.into 'META-INF'
            }
        }
        return resources

    }

    static Zip createApkLibraryTask(Project project, String name, File apktoolDir, File dex2jarFile,
            File resourcesDir) {

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
            from(new File(apktoolDir, 'lib')) { CopySpec spec ->
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
