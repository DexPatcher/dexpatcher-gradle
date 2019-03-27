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

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
abstract class AbstractDecoderPlugin<E extends AbstractDecoderExtension> extends AbstractPlugin {

    protected E extension

    protected DecodedSourceAppTask decodedSourceApp

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

        decodedSourceApp = createDecodedSourceAppTaskChain(project, DexpatcherBasePlugin.TASK_GROUP,
                { it }, { it }, sourceApk, sourceApkLib, extension.printAppInfo)

    }

    static DecodedSourceAppTask createDecodedSourceAppTaskChain(Project project, String taskGroup,
            Closure<String> taskNameModifier, Closure<Provider<Directory>> dirModifier,
            Configuration sourceApk, Configuration sourceApkLib, Provider<Boolean> printSourceAppInfo) {

        /*
        // This had to be moved to afterEvaluate because Gradle checks the inputs of skipped tasks.

        def checkSourceApp = project.tasks.create(taskNameModifier(TASK_CHECK_SOURCE_APP))
        checkSourceApp.with {
            description = 'Verifies the availability of a source application.'
            group = taskGroup
            doLast {
                def n = (sourceApk.is(null) ? 0 : sourceApk.files.size()) +
                        (sourceApkLib.is(null) ? 0 : sourceApkLib.files.size())
                if (n != 1) {
                    if (!n) throw new RuntimeException('No source application found')
                    else throw new RuntimeException('Multiple source applications found')
                }
            }
        }
        */

        def decodedSourceApp = project.tasks.create(taskNameModifier(TASK_DECODED_SOURCE_APP), DecodedSourceAppTask)
        decodedSourceApp.with {
            description = 'Produces the decoded source application.'
            group = taskGroup
            outputDir.set dirModifier(project.layout.buildDirectory.dir(DIR_DECODED_SOURCE_APP))
        }

        def sourceAppInfo = project.tasks.create(taskNameModifier(TASK_SOURCE_APP_INFO))
        sourceAppInfo.with {
            description = 'Displays package and version information of the source application.'
            group = taskGroup
            dependsOn decodedSourceApp
            doLast {
                //decodedSourceApp.check()
                def pattern = ~/^\s\s(minSdkVersion|targetSdkVersion|versionCode|versionName):/
                decodedSourceApp.apktoolYmlFile.get().asFile.eachLine { line ->
                    if (pattern.matcher(line).find()) System.out.println line.substring(2)
                }
            }
        }

        DecodeApkTask decodeSourceApk = null
        if (!sourceApk.is(null)) {
            decodeSourceApk = createDecodeSourceApkTask(project,
                    taskNameModifier(TASK_DECODE_SOURCE_APK), taskGroup,
                    dirModifier(project.layout.buildDirectory.dir(DIR_APKTOOL_FRAMEWORK)),
                    decodedSourceApp.outputDir, sourceApk)
            /*
            decodeSourceApk.dependsOn checkSourceApp
            decodedSourceApp.dependsOn decodeSourceApk
            decodeSourceApk.onlyIf {
                sourceApk.files.size()
            }
            */
        }

        Sync unpackSourceApkLibrary = null
        if (!sourceApkLib.is(null)) {
            unpackSourceApkLibrary = createUnpackSourceApkLibraryTask(project,
                    taskNameModifier(TASK_UNPACK_SOURCE_APK_LIBRARY), taskGroup,
                    decodedSourceApp.outputDir, sourceApkLib)
            /*
            unpackSourceApkLibrary.dependsOn checkSourceApp
            decodedSourceApp.dependsOn unpackSourceApkLibrary
            unpackSourceApkLibrary.onlyIf {
                sourceApkLib.files.size()
            }
            */
        }

        project.afterEvaluate {
            def nApk = sourceApk.is(null) ? 0 : sourceApk.files.size()
            def nApkLib = sourceApkLib.is(null) ? 0 : sourceApkLib.files.size()
            def n = nApk + nApkLib
            if (n != 1) {
                if (!n) throw new RuntimeException('No source application found')
                else throw new RuntimeException('Multiple source applications found')
            }
            if (nApk) decodedSourceApp.dependsOn decodeSourceApk
            if (nApkLib) decodedSourceApp.dependsOn unpackSourceApkLibrary
            if (printSourceAppInfo.get()) decodedSourceApp.finalizedBy sourceAppInfo
        }

        //decodedSourceApp.extensions.add TASK_CHECK_SOURCE_APP, checkSourceApp
        decodedSourceApp.extensions.add TASK_DECODE_SOURCE_APK, decodeSourceApk
        decodedSourceApp.extensions.add TASK_UNPACK_SOURCE_APK_LIBRARY, unpackSourceApkLibrary
        decodedSourceApp.extensions.add TASK_SOURCE_APP_INFO, sourceAppInfo

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

    static DecodeApkTask createDecodeSourceApkTask(Project project, String taskName, String taskGroup,
            Provider<Directory> frameworkOutDir, Provider<Directory> outDir, Configuration sourceApk) {
        def decodeSourceApk = project.tasks.create(taskName, DecodeApkTask)
        decodeSourceApk.with {
            description = 'Unpacks an Android APK and decodes its manifest and resources.'
            group = taskGroup
            dependsOn sourceApk
            apkFile.set project.<RegularFile> provider {
                Utils.getRegularFile(project, sourceApk.singleFile)
            }
            frameworkDir.set((Directory) null)
            frameworkDirAsInput.set((Directory) null)
            frameworkDirAsOutput.set frameworkOutDir
            outputDir.set outDir
            decodeClasses.set false
            //keepBrokenResources.set true
        }
        return decodeSourceApk
    }

    static Sync createUnpackSourceApkLibraryTask(Project project, String taskName, String taskGroup,
            Provider<Directory> outDir, Configuration sourceApkLib) {
        def unpackSourceApkLibrary = project.tasks.create(taskName, Sync)
        unpackSourceApkLibrary.with {
            description = 'Unpacks a DexPatcher APK library.'
            group = taskGroup
            dependsOn sourceApkLib
            from {
                project.zipTree(sourceApkLib.singleFile)
            }
            into outDir
        }
        return unpackSourceApkLibrary
    }

}
