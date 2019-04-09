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
import lanchon.dexpatcher.gradle.tasks.ProvideDecodedAppTask

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

    protected TaskProvider<ProvideDecodedAppTask> provideDecodedApp

    @Override
    protected void afterApply() {

        super.afterApply()

        def sourceApk = project.configurations.create(ConfigurationNames.SOURCE_APK)
        sourceApk.canBeResolved = true
        sourceApk.canBeConsumed = false
        sourceApk.transitive = false
        def apks = project.fileTree(ProjectDir.DIR_SOURCE_APK)
        apks.include FileNames.EXTS_SOURCE_APK.collect { '*' + it }
        sourceApk.dependencies.add project.dependencies.create(apks)

        def sourceApkLib = project.configurations.create(ConfigurationNames.SOURCE_APK_LIBRARY)
        sourceApkLib.canBeResolved = true
        sourceApkLib.canBeConsumed = false
        sourceApkLib.transitive = false
        def apkLibs = project.fileTree(ProjectDir.DIR_SOURCE_APK_LIBRARY)
        apkLibs.include '*' + FileNames.EXT_APK_LIBRARY
        sourceApkLib.dependencies.add project.dependencies.create(apkLibs)

        provideDecodedApp = registerProvideDecodedAppTaskChain(project, TASK_GROUP_NAME,
                { String it -> it }, { Provider<Directory> it -> it },
                sourceApk, sourceApkLib, extension.printAppInfo)

    }

    static TaskProvider<ProvideDecodedAppTask> registerProvideDecodedAppTaskChain(Project project, String taskGroup,
            Closure<String> taskNameModifier, Closure<Provider<Directory>> dirModifier,
            Configuration sourceApk, Configuration sourceApkLib, Provider<Boolean> printAppInfo) {

        def provideDecodedApp = project.tasks.register(taskNameModifier(TaskNames.PROVIDE_DECODED_APP), ProvideDecodedAppTask) {
            it.description = 'Provides the decoded source application.'
            it.group = taskGroup
            it.outputDir.set dirModifier(project.layout.buildDirectory.dir(BuildDir.DIR_DECODED_APP))
            if (!sourceApk.is(null)) it.sourceAppFiles.from sourceApk
            if (!sourceApkLib.is(null)) it.sourceAppFiles.from sourceApkLib
            return
        }

        def sourceAppInfo = project.tasks.register(taskNameModifier(TaskNames.SOURCE_APP_INFO)) {
            it.description = 'Displays package and version information of the source application.'
            it.group = taskGroup
            it.dependsOn provideDecodedApp
            it.doLast {
                def pattern = ~/^\s\s(minSdkVersion|targetSdkVersion|versionCode|versionName):/
                provideDecodedApp.get().apktoolYmlFile.get().asFile.eachLine { line ->
                    if (pattern.matcher(line).find()) println line.substring(2)
                }
            }
        }

        def outputDir = project.<Directory>provider {
            provideDecodedApp.get().outputDir.get()
        }

        def sourceAppFile = project.<RegularFile>provider {
            provideDecodedApp.get().sourceAppFile.get()
        }

        TaskProvider<DecodeApkTask> decodeApk = null
        if (!sourceApk.is(null)) {
            decodeApk = registerDecodeApkTask(project,
                    taskNameModifier(TaskNames.DECODE_APK), taskGroup,
                    dirModifier(project.layout.buildDirectory.dir(BuildDir.DIR_APKTOOL_FRAMEWORK)),
                    outputDir, sourceAppFile)
            decodeApk.configure {
                it.dependsOn sourceApk, provideDecodedApp.get().sourceAppFiles
                it.onlyIf {
                    sourceApk.files.size()
                }
            }
            provideDecodedApp.configure {
                it.dependsOn decodeApk
            }
        }

        TaskProvider<Sync> unpackApkLibrary = null
        if (!sourceApkLib.is(null)) {
            unpackApkLibrary = registerUnpackApkLibraryTask(project,
                    taskNameModifier(TaskNames.UNPACK_APK_LIBRARY), taskGroup,
                    outputDir, sourceAppFile)
            unpackApkLibrary.configure {
                it.dependsOn sourceApkLib, provideDecodedApp.get().sourceAppFiles
                it.onlyIf {
                    sourceApkLib.files.size()
                }
            }
            provideDecodedApp.configure {
                it.dependsOn unpackApkLibrary
            }
        }

        project.afterEvaluate {
            provideDecodedApp.configure {
                if (printAppInfo.get()) {
                    it.finalizedBy sourceAppInfo
                }
            }
        }

        provideDecodedApp.configure {
            def extensions = it.extensions
            if (!sourceApk.is(null)) extensions.add TaskNames.DECODE_APK, decodeApk
            if (!sourceApkLib.is(null)) extensions.add TaskNames.UNPACK_APK_LIBRARY, unpackApkLibrary
            extensions.add TaskNames.SOURCE_APP_INFO, sourceAppInfo
        }

        return provideDecodedApp

    }

    static TaskProvider<DecodeApkTask> registerDecodeApkTask(Project project, String taskName, String taskGroup,
            Provider<Directory> frameworkOutDir, Provider<Directory> outputDir, Provider<RegularFile> apkFile) {
        def decodeApk = project.tasks.register(taskName, DecodeApkTask) {
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
        return decodeApk
    }

    static TaskProvider<Sync> registerUnpackApkLibraryTask(Project project, String taskName, String taskGroup,
            Provider<Directory> outputDir, Provider<RegularFile> apkLibFile) {
        def unpackApkLibrary = project.tasks.register(taskName, Sync) {
            it.description = 'Unpacks a DexPatcher APK library.'
            it.group = taskGroup
            it.from {
                project.zipTree(apkLibFile)
            }
            it.into outputDir
            return
        }
        return unpackApkLibrary
    }

}
