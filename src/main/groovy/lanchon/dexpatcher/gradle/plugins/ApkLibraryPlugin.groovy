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

import lanchon.dexpatcher.gradle.extensions.ApkLibraryExtension
import lanchon.dexpatcher.gradle.tasks.LazyZipTask
import lanchon.dexpatcher.gradle.tasks.ProvideDecodedAppTask

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class ApkLibraryPlugin extends AbstractDecoderPlugin<ApkLibraryExtension> {

    protected TaskProvider<LazyZipTask> createApkLibrary

    @Override
    void apply(Project project) {

        super.apply(project)

        extension = basePlugin.createSubextension(ExtensionNames.PLUGIN_APK_LIBRARY, ApkLibraryExtension)
        project.plugins.apply(BasePlugin)

        afterApply()

    }

    @Override
    protected void afterApply() {

        super.afterApply()

        def apkLibFileName = project.<String>provider {
            def name = project.name ?: BuildDir.FILENAME_APK_LIBRARY_DEFAULT_BASE
            def files = provideDecodedApp.get().sourceAppFiles.files
            if (files.size() == 1) {
                def newName = files[0].name
                if (newName) {
                    newName = removeExtensions(newName, FileNames.EXTS_SOURCE_APP)
                    if (newName) name = newName
                }
            }
            name += FileNames.EXT_APK_LIBRARY
            name
        }

        createApkLibrary = registerCreateApkLibraryTask(project, TaskNames.CREATE_APK_LIBRARY, TASK_GROUP_NAME,
                provideDecodedApp, apkLibFileName, project.layout.buildDirectory.dir(BuildDir.DIR_APK_LIBRARY))

        project.artifacts.add(Dependency.DEFAULT_CONFIGURATION, createApkLibrary)

        project.tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure {
            it.dependsOn createApkLibrary
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

    static TaskProvider<LazyZipTask> registerCreateApkLibraryTask(Project project, String taskName, String taskGroup,
            TaskProvider<ProvideDecodedAppTask> provideDecodedApp, Provider<String> apkLibFileName,
            Provider<Directory> apkLibDirectory) {
        def apkLibrary = project.tasks.register(taskName, LazyZipTask) {
            it.description = 'Packs the decoded source application as a DexPatcher APK library.'
            it.group = taskGroup
            it.extension = FileNames.EXT_APK_LIBRARY.substring(1)
            it.zip64 = true
            it.reproducibleFileOrder = true
            it.preserveFileTimestamps = false
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            it.from provideDecodedApp
            it.lazyArchiveFileName.set apkLibFileName
            it.lazyDestinationDirectory.set apkLibDirectory
        }
        return apkLibrary
    }

}
