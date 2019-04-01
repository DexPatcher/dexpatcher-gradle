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

import lanchon.dexpatcher.gradle.extensions.AbstractDecoderExtension
import lanchon.dexpatcher.gradle.tasks.DecodeApkTask
import lanchon.dexpatcher.gradle.tasks.DecodedSourceAppTask

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
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
        def apkLibs = project.fileTree(DIR_SOURCE_APK_LIBRARY)
        apkLibs.include '*.apklib'
        sourceApkLib.dependencies.add(project.dependencies.create(apkLibs))

        decodedSourceApp = registerDecodedSourceAppTaskChain(project, GROUP_DEXPATCHER,
                { it }, { it }, sourceApk, sourceApkLib, extension.printAppInfo)

    }

    static TaskProvider<DecodedSourceAppTask> registerDecodedSourceAppTaskChain(Project project, String taskGroup,
            Closure<String> taskNameModifier, Closure<Provider<Directory>> dirModifier,
            Configuration sourceApk, Configuration sourceApkLib, Provider<Boolean> printAppInfo) {

        def decodedSourceApp = project.tasks.register(taskNameModifier(TASK_DECODED_SOURCE_APP), DecodedSourceAppTask) {
            it.description = 'Produces the decoded source application.'
            it.group = taskGroup
            it.outputDir.set dirModifier(project.layout.buildDirectory.dir(DIR_BUILD_DECODED_SOURCE_APP))
            if (!sourceApk.is(null)) it.sourceApp.from sourceApk
            if (!sourceApkLib.is(null)) it.sourceApp.from sourceApkLib
        }

        def sourceAppInfo = project.tasks.register(taskNameModifier(TASK_SOURCE_APP_INFO)) {
            it.description = 'Displays package and version information of the source application.'
            it.group = taskGroup
            it.dependsOn decodedSourceApp
            it.doLast {
                def pattern = ~/^\s\s(minSdkVersion|targetSdkVersion|versionCode|versionName):/
                decodedSourceApp.get().apktoolYmlFile.get().asFile.eachLine { line ->
                    if (pattern.matcher(line).find()) System.out.println line.substring(2)
                }
            }
        }

        def outputDir = project.<Directory>provider {
            decodedSourceApp.get().outputDir.get()
        }

        def sourceAppFile = project.<RegularFile>provider {
            decodedSourceApp.get().sourceAppFile.get()
        }

        TaskProvider<DecodeApkTask> decodeSourceApk = null
        if (!sourceApk.is(null)) {
            decodeSourceApk = registerDecodeSourceApkTask(project,
                    taskNameModifier(TASK_DECODE_SOURCE_APK), taskGroup,
                    dirModifier(project.layout.buildDirectory.dir(DIR_BUILD_APKTOOL_FRAMEWORK)),
                    outputDir, sourceAppFile)
            decodeSourceApk.configure {
                it.dependsOn sourceApk, decodedSourceApp.get().sourceApp
                it.onlyIf {
                    sourceApk.files.size()
                }
            }
            decodedSourceApp.configure {
                it.dependsOn decodeSourceApk
            }
        }

        TaskProvider<Sync> unpackSourceApkLibrary = null
        if (!sourceApkLib.is(null)) {
            unpackSourceApkLibrary = registerUnpackSourceApkLibraryTask(project,
                    taskNameModifier(TASK_UNPACK_SOURCE_APK_LIBRARY), taskGroup,
                    outputDir, sourceAppFile)
            unpackSourceApkLibrary.configure {
                it.dependsOn sourceApkLib, decodedSourceApp.get().sourceApp
                it.onlyIf {
                    sourceApkLib.files.size()
                }
            }
            decodedSourceApp.configure {
                it.dependsOn unpackSourceApkLibrary
            }
        }

        project.afterEvaluate {
            if (printAppInfo.get()) {
                decodedSourceApp.configure {
                    it.finalizedBy sourceAppInfo
                }
            }
        }

        decodedSourceApp.configure {
            def extensions = it.extensions
            if (!sourceApk.is(null)) extensions.add TASK_DECODE_SOURCE_APK, decodeSourceApk
            if (!sourceApkLib.is(null)) extensions.add TASK_UNPACK_SOURCE_APK_LIBRARY, unpackSourceApkLibrary
            extensions.add TASK_SOURCE_APP_INFO, sourceAppInfo
        }

        return decodedSourceApp

    }

    static TaskProvider<DecodeApkTask> registerDecodeSourceApkTask(Project project, String taskName, String taskGroup,
            Provider<Directory> frameworkOutDir, Provider<Directory> outputDir, Provider<RegularFile> apkFile) {
        def decodeSourceApk = project.tasks.register(taskName, DecodeApkTask) {
            it.description = 'Unpacks an Android APK and decodes its manifest and resources.'
            it.group = taskGroup
            it.apkFile.set apkFile
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
            Provider<Directory> outputDir, Provider<RegularFile> apkLibFile) {
        def unpackSourceApkLibrary = project.tasks.register(taskName, Sync) {
            it.description = 'Unpacks a DexPatcher APK library.'
            it.group = taskGroup
            it.from {
                project.zipTree(apkLibFile)
            }
            it.into outputDir
        }
        return unpackSourceApkLibrary
    }

}
