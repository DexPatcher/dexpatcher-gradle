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
import org.gradle.api.provider.Property

@CompileStatic
class ApkLibraryExtension extends AbstractSubextension {

    static final String EXTENSION_NAME = 'apkLibrary'

    final Property<File> apkFileOrDir
    final Property<Boolean> disableClean

    ApkLibraryExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig)

        apkFileOrDir = project.objects.property(File)
        apkFileOrDir.set project.file('apk')
        disableClean = project.objects.property(Boolean)
    }

}
