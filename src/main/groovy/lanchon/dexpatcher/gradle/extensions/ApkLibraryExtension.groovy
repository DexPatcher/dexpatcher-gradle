/*
 * DexPatcher - Copyright 2015-2017 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

@CompileStatic
class ApkLibraryExtension extends AbstractSubextension {

    static final String EXTENSION_NAME = 'apkLibrary'

    final Property<FileSystemLocation> apkFileOrDir
    final Property<Boolean> disableClean

    ApkLibraryExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {

        super(project, dexpatcherConfig)

        apkFileOrDir = project.objects.property(FileSystemLocation)
        apkFileOrDir.set project.layout.projectDirectory.dir('apk')
        disableClean = project.objects.property(Boolean)

    }

    Provider<RegularFile> getResolvedApkFile() {
        return project.providers.<RegularFile>provider {
            def apk = apkFileOrDir.orNull
            switch (apk) {
                case RegularFile:
                    return ((RegularFile) apk)
                case Directory:
                    // TODO: Resolver.resolveSingleFile(project, apkFileOrDir.get(), '*.apk')
                    return ((Directory) apk).file('*.apk')
                default:
                    return null
            }
        }

    }

}
