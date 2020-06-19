/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
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

        def sourceApkCfg = project.configurations.maybeCreate(ConfigurationNames.SOURCE_APK)
        sourceApkCfg.description = 'The source application provided as an Android APK.'
        sourceApkCfg.canBeResolved = true
        sourceApkCfg.canBeConsumed = false
        def apks = project.fileTree(ProjectDir.DIR_SOURCE_APK)
        apks.include FileNames.EXTS_SOURCE_APK.collect { '*' + it }
        sourceApkCfg.dependencies.add project.dependencies.create(apks)

        def sourceApkLibCfg = project.configurations.maybeCreate(ConfigurationNames.SOURCE_APK_LIBRARY)
        sourceApkLibCfg.description = 'The source application provided as a DexPatcher APK library.'
        sourceApkLibCfg.canBeResolved = true
        sourceApkLibCfg.canBeConsumed = false
        def apkLibs = project.fileTree(ProjectDir.DIR_SOURCE_APK_LIBRARY)
        apkLibs.include '*' + FileNames.EXT_APK_LIBRARY
        sourceApkLibCfg.dependencies.add project.dependencies.create(apkLibs)

        provideDecodedApp = registerProvideDecodedAppTaskChain(project, TASK_GROUP_NAME,
                { String it -> it }, { Provider<Directory> it -> it },
                sourceApkCfg, sourceApkLibCfg, extension.printAppInfo)

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

        TaskProvider<DecodeApkTask> decodeApk = null
        if (!sourceApk.is(null)) {
            def inputFile = project.<RegularFile>provider {
                provideDecodedApp.get().sourceAppFile.get()
                project.layout.projectDirectory.file(sourceApk.singleFile.path)
            }
            decodeApk = registerDecodeApkTask(project,
                    taskNameModifier(TaskNames.DECODE_APK), taskGroup,
                    dirModifier(project.layout.buildDirectory.dir(BuildDir.DIR_APKTOOL_FRAMEWORK)),
                    outputDir, inputFile)
            decodeApk.configure {
                it.dependsOn sourceApk
            }
            provideDecodedApp.configure {
                it.dependsOn {
                    !sourceApk.empty ? decodeApk : []
                }
            }
        }

        TaskProvider<Sync> unpackApkLibrary = null
        if (!sourceApkLib.is(null)) {
            def inputFile = project.<RegularFile>provider {
                provideDecodedApp.get().sourceAppFile.get()
                project.layout.projectDirectory.file(sourceApkLib.singleFile.path)
            }
            unpackApkLibrary = registerUnpackApkLibraryTask(project,
                    taskNameModifier(TaskNames.UNPACK_APK_LIBRARY), taskGroup,
                    outputDir, inputFile)
            unpackApkLibrary.configure {
                it.dependsOn sourceApkLib
            }
            provideDecodedApp.configure {
                it.dependsOn {
                    !sourceApkLib.empty ? unpackApkLibrary : []
                }
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
            it.frameworkOutDir.set frameworkOutDir
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
                // See 'Project.zipTree' bug: https://github.com/gradle/gradle/issues/9150
                project.zipTree apkLibFile
            }
            it.into outputDir
            return
        }
        return unpackApkLibrary
    }

}
