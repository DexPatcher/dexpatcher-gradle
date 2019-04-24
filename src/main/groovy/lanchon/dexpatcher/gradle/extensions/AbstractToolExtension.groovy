/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.NewProperty
import lanchon.dexpatcher.gradle.Utils

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

@CompileStatic
abstract class AbstractToolExtension extends AbstractSubextension {

    protected static final String PREFIX = super.PREFIX + 'tool.'

    final DirectoryProperty dir
    final ListProperty<String> extraArgs = NewProperty.list(project, String)

    final Provider<Directory> resolvedDir

    AbstractToolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig, String dirProperty) {
        super(project, dexpatcherConfig)
        dir = NewProperty.dir(project, dexpatcherConfig, dirProperty)
        resolvedDir = Utils.getResolvedDir(project, dir, dexpatcherConfig.resolvedToolDir, name)
    }

    protected abstract String getName()

}
