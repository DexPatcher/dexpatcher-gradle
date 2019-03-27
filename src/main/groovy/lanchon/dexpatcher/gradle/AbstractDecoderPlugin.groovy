/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.extensions.AbstractDecoderExtension
import lanchon.dexpatcher.gradle.tasks.DecodeApkTask

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
abstract class AbstractDecoderPlugin<E extends AbstractDecoderExtension> extends AbstractPlugin {

    protected E extension

    protected TaskProvider<DecodedSourceAppTask> decodedSourceApp

    @Override
    protected void afterApply() {

        super.afterApply()

        def sourceApk = project.configurations.create(CONFIG_SOURCE_APK)
        sourceApk.transitive = false
        def apks = project.fileTree(DIR_SOURCE_APK)
        apks.include '*.apk', '*.jar', '*.zip'
        sourceApk.dependencies.add(project.dependencies.create(apks))

        def sourceApkLib = project.configurations.create(CONFIG_SOURCE_APK_LIBRARY)
        sourceApkLib.transitive = false
        def apklibs = project.fileTree(DIR_SOURCE_APK_LIBRARY)
        apklibs.include '*.apklib'
        sourceApkLib.dependencies.add(project.dependencies.create(apklibs))

        decodedSourceApp = registerDecodedSourceAppTaskChain(project, TASK_GROUP_DEXPATCHER,
                { it }, { it }, sourceApk, sourceApkLib, extension.printAppInfo)

    }

    static TaskProvider<DecodedSourceAppTask> registerDecodedSourceAppTaskChain(Project project, String taskGroup,
            Closure<String> taskNameModifier, Closure<Provider<Directory>> dirModifier,
            Configuration sourceApk, Configuration sourceApkLib, Provider<Boolean> printSourceAppInfo) {

        def decodedSourceApp = project.tasks.register(taskNameModifier(TASK_DECODED_SOURCE_APP), DecodedSourceAppTask) {
            it.description = 'Produces the decoded source application.'
            it.group = taskGroup
            it.outputDir.set dirModifier(project.layout.buildDirectory.dir(DIR_DECODED_SOURCE_APP))
        }

        def outputDir = project.<Directory>provider {
            decodedSourceApp.get().outputDir.get()
        }

        def sourceAppInfo = project.tasks.register(taskNameModifier(TASK_SOURCE_APP_INFO)) {
            it.description = 'Displays package and version information of the source application.'
            it.group = taskGroup
            it.dependsOn decodedSourceApp
            it.doLast {
                //decodedSourceApp.check()
                def pattern = ~/^\s\s(minSdkVersion|targetSdkVersion|versionCode|versionName):/
                decodedSourceApp.get().apktoolYmlFile.get().asFile.eachLine { line ->
                    if (pattern.matcher(line).find()) System.out.println line.substring(2)
                }
            }
        }

        TaskProvider<DecodeApkTask> decodeSourceApk = null
        if (!sourceApk.is(null)) {
            decodeSourceApk = registerDecodeSourceApkTask(project,
                    taskNameModifier(TASK_DECODE_SOURCE_APK), taskGroup,
                    dirModifier(project.layout.buildDirectory.dir(DIR_APKTOOL_FRAMEWORK)),
                    outputDir, sourceApk)
        }

        TaskProvider<Sync> unpackSourceApkLibrary = null
        if (!sourceApkLib.is(null)) {
            unpackSourceApkLibrary = registerUnpackSourceApkLibraryTask(project,
                    taskNameModifier(TASK_UNPACK_SOURCE_APK_LIBRARY), taskGroup,
                    outputDir, sourceApkLib)
        }

        project.afterEvaluate {
            def nApk = sourceApk.is(null) ? 0 : sourceApk.files.size()
            def nApkLib = sourceApkLib.is(null) ? 0 : sourceApkLib.files.size()
            def n = nApk + nApkLib
            if (n != 1) {
                if (!n) throw new RuntimeException('No source application found')
                else throw new RuntimeException('Multiple source applications found')
            }
            if (nApk) decodedSourceApp.configure { it.dependsOn decodeSourceApk; return }
            if (nApkLib) decodedSourceApp.configure { it.dependsOn unpackSourceApkLibrary; return }
            if (printSourceAppInfo.get()) decodedSourceApp.configure { it.finalizedBy sourceAppInfo; return }
        }

        decodedSourceApp.configure {
            it.extensions.add TASK_DECODE_SOURCE_APK, decodeSourceApk
            it.extensions.add TASK_UNPACK_SOURCE_APK_LIBRARY, unpackSourceApkLibrary
            it.extensions.add TASK_SOURCE_APP_INFO, sourceAppInfo
        }

        return decodedSourceApp

    }

    static class DecodedSourceAppTask extends DefaultTask {

        @Input final DirectoryProperty outputDir
        @Internal final Provider<RegularFile> apktoolYmlFile

        DecodedSourceAppTask() {
            outputDir = project.layout.directoryProperty()
            apktoolYmlFile = outputDir.file(FILE_APKTOOL_YML)
        }

        @TaskAction
        void check() {
            def apktoolYmlFile = apktoolYmlFile.get().asFile
            if (!apktoolYmlFile.isFile()) {
                throw new RuntimeException("Cannot find '$FILE_APKTOOL_YML' file in decoded application")
            }
        }

    }

    static TaskProvider<DecodeApkTask> registerDecodeSourceApkTask(Project project, String taskName, String taskGroup,
            Provider<Directory> frameworkOutDir, Provider<Directory> outputDir, Configuration sourceApk) {
        def decodeSourceApk = project.tasks.register(taskName, DecodeApkTask) {
            it.description = 'Unpacks an Android APK and decodes its manifest and resources.'
            it.group = taskGroup
            it.dependsOn sourceApk
            it.apkFile.set project.<RegularFile> provider {
                Utils.getRegularFile(project, sourceApk.singleFile)
            }
            it.frameworkDir.set((Directory) null)
            it.frameworkDirAsInput.set((Directory) null)
            it.frameworkDirAsOutput.set frameworkOutDir
            it.outputDir.set outputDir
            it.decodeClasses.set false
            //it.keepBrokenResources.set true
        }
        return decodeSourceApk
    }

    static TaskProvider<Sync> registerUnpackSourceApkLibraryTask(Project project, String taskName, String taskGroup,
            Provider<Directory> outputDir, Configuration sourceApkLib) {
        def unpackSourceApkLibrary = project.tasks.register(taskName, Sync) {
            it.description = 'Unpacks a DexPatcher APK library.'
            it.group = taskGroup
            it.dependsOn sourceApkLib
            it.from {
                project.zipTree(sourceApkLib.singleFile)
            }
            it.into outputDir
        }
        return unpackSourceApkLibrary
    }

}
