/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.plugins

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.extensions.ApkLibraryExtension
import lanchon.dexpatcher.gradle.tasks.LazyZipTask
import lanchon.dexpatcher.gradle.tasks.SourceAppTask

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class ApkLibraryPlugin extends AbstractDecoderPlugin<ApkLibraryExtension> {

    protected TaskProvider<LazyZipTask> apkLibrary

    @Override
    void apply(Project project) {

        super.apply(project)

        extension = (ApkLibraryExtension) subextensions.create(
                EXT_PLUGIN_APK_LIBRARY, ApkLibraryExtension, project, dexpatcherConfig)

        project.plugins.apply(BasePlugin)

        afterApply()

    }

    @Override
    protected void afterApply() {

        super.afterApply()

        def apkLibFile = project.<RegularFile>provider {
            def name = project.name ?: 'source'
            def files = sourceApp.get().sourceAppFiles.files
            if (files.size() == 1) {
                def newName = files[0].name
                if (newName) {
                    newName = removeExtensions(newName, FILE_EXTS_SOURCE_APP)
                    if (newName) name = newName
                }
            }
            name += FILE_EXT_APK_LIBRARY
            return project.layout.buildDirectory.get().file(DIR_BUILD_APK_LIBRARY + '/' + name)
        }

        apkLibrary = registerApkLibraryTask(project, TASK_APK_LIBRARY, GROUP_DEXPATCHER, sourceApp, apkLibFile)
        project.artifacts.add(Dependency.DEFAULT_CONFIGURATION, apkLibrary)

        project.tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure {
            it.dependsOn apkLibrary
        }

    }

    private static String removeExtensions(String name, Iterable<String> extensions) {
        for (def it in extensions) {
            if (name.endsWith(it)) {
                return name.substring(0, name.length() - it.length())
            }
        }
        return name
    }

    static TaskProvider<LazyZipTask> registerApkLibraryTask(Project project, String taskName, String taskGroup,
            TaskProvider<SourceAppTask> sourceApp, Provider<RegularFile> apkLibFile) {
        def apkLibrary = project.tasks.register(taskName, LazyZipTask) {
            it.description = 'Packs the decoded source application as a DexPatcher APK library.'
            it.group = taskGroup
            it.extension = FILE_EXT_APK_LIBRARY.substring(1)
            it.zip64 = true
            it.reproducibleFileOrder = true
            it.preserveFileTimestamps = false
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            //it.dependsOn sourceApp
            it.from sourceApp
            it.archiveFile.set apkLibFile
        }
        return apkLibrary
    }

    /*
    static Zip createTaskChain(Project project, String taskGroup, Closure<String> taskNameModifier,
            Closure<Directory> dirModifier, Provider<RegularFile> theApkFile) {

        def buildDir = project.layout.buildDirectory.get()

        def modIntermediateDir = dirModifier(buildDir.dir('intermediates'))
        def modOutputDir = dirModifier(buildDir.dir('outputs'))

        def apktoolDir = modIntermediateDir.dir('apktool')
        def apktoolFrameworkDir = modIntermediateDir.dir('apktool-framework')
        def dex2jarFile = modIntermediateDir.file('dex2jar/classes.zip')
        def dex2jarExceptionFile = modIntermediateDir.file('dex2jar/dex2jar-error.zip')
        def resourcesDir = modIntermediateDir.dir('resources')
        def libraryDir = modOutputDir.dir('aar')

        def decodeApk = project.tasks.create(taskNameModifier('decodeApk'), DecodeApkTask)
        decodeApk.with {
            description = "Unpacks an Android application and decodes its manifest and resources."
            group = taskGroup
            apkFile.set theApkFile
            outputDir.set apktoolDir
            frameworkDir.set ((Directory) null)
            frameworkDirAsInput.set ((Directory) null)
            frameworkDirAsOutput.set apktoolFrameworkDir
            decodeClasses.set false
            //keepBrokenResources.set true
        }

        def dex2jar = project.tasks.create(taskNameModifier('dex2jar'), Dex2jarTask)
        dex2jar.with {
            description = "Translates Dalvik bytecode into Java bytecode."
            group = taskGroup
            dependsOn decodeApk
            dexFiles.from decodeApk.apkFile
            outputFile.set dex2jarFile
            exceptionFile.set dex2jarExceptionFile
        }

        def resources = createResourcesTask(project, taskNameModifier('resources'), apktoolDir)
        resources.with {
            description = "Packs extra resources into a jar"
            group = taskGroup
            dependsOn decodeApk
            destinationDir = resourcesDir.asFile
        }

        def apkLibrary = createApkLibraryTask(project, taskNameModifier('apkLibrary'), apktoolDir, dex2jarFile,
                dex2jarExceptionFile, resourcesDir)
        apkLibrary.with {
            description = "Packs the processed application into an apk library."
            group = taskGroup
            dependsOn decodeApk, dex2jar, resources
            destinationDir = libraryDir.asFile
            extension = 'aar'
            // WARNING: archiveName is set eagerly.
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

    static Zip createResourcesTask(Project project, String name, Directory apktoolDir) {
        def resources = project.tasks.create(name, Zip)
        resources.with {
            duplicatesStrategy = DuplicatesStrategy.FAIL
            archiveName = 'classes.jar'
            from(apktoolDir.dir('unknown'))
            from(apktoolDir.dir('original/META-INF')) { CopySpec spec ->
                spec.into 'META-INF'
            }
        }
        return resources
    }

    static Zip createApkLibraryTask(Project project, String name, Directory apktoolDir, RegularFile dex2jarFile,
            RegularFile dex2jarExceptionFile, Directory resourcesDir) {

        def apkLibrary = project.tasks.create(name, Zip)
        apkLibrary.with {

            extension = 'apk.aar'
            it.zip64 = true
            duplicatesStrategy = DuplicatesStrategy.FAIL

            / *
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
            * /

            from(apktoolDir) { CopySpec spec ->
                spec.with {
                    include 'AndroidManifest.xml'
                    include 'res/'
                    include 'assets/'
                }
            }
            from(apktoolDir.dir('lib')) { CopySpec spec ->
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
    */

}
