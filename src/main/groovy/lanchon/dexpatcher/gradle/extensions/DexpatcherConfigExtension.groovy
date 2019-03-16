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

import lanchon.dexpatcher.gradle.ProjectProperties
import lanchon.dexpatcher.gradle.Resolver

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

@CompileStatic
class DexpatcherConfigExtension extends AbstractExtension {

    static final String EXTENSION_NAME = 'dexpatcherConfig'

    private static final String DIR_PROPERTY = PREFIX + 'dir'
    private static final String TOOL_DIR_PROPERTY = PREFIX + 'tool.dir'
    private static final String LIB_DIR_PROPERTY = PREFIX + 'lib.dir'
    private static final String ADDED_LIB_DIR_PROPERTY = PREFIX + 'lib.added.dir'
    private static final String PROVIDED_LIB_DIR_PROPERTY = PREFIX + 'lib.provided.dir'

    private static final String DEFAULT_TOOL_SUBDIR_NAME = 'tools'
    private static final String DEFAULT_LIB_SUBDIR_NAME = 'libs'
    private static final String DEFAULT_ADDED_SUBDIR_NAME = 'added'
    private static final String DEFAULT_PROVIDED_SUBDIR_NAME = 'provided'

    final ProjectProperties properties

    final DirectoryProperty dir
    final DirectoryProperty toolDir
    final DirectoryProperty libDir
    final DirectoryProperty addedLibDir
    final DirectoryProperty providedLibDir

    final Provider<Directory> resolvedToolDir
    final Provider<Directory> resolvedLibDir
    final Provider<Directory> resolvedAddedLibDir
    final Provider<Directory> resolvedProvidedLibDir

    DexpatcherConfigExtension(Project project) {

        super(project)
        properties = new ProjectProperties(project)

        dir = project.layout.directoryProperty()
        dir.set properties.getAsDirectory(DIR_PROPERTY)
        toolDir = project.layout.directoryProperty()
        toolDir.set properties.getAsDirectory(TOOL_DIR_PROPERTY)
        libDir = project.layout.directoryProperty()
        libDir.set properties.getAsDirectory(LIB_DIR_PROPERTY)
        addedLibDir = project.layout.directoryProperty()
        addedLibDir.set properties.getAsDirectory(ADDED_LIB_DIR_PROPERTY)
        providedLibDir = project.layout.directoryProperty()
        providedLibDir.set properties.getAsDirectory(PROVIDED_LIB_DIR_PROPERTY)

        resolvedToolDir = Resolver.getDirectory(project, toolDir, dir, DEFAULT_TOOL_SUBDIR_NAME)
        resolvedLibDir = Resolver.getDirectory(project, libDir, dir, DEFAULT_LIB_SUBDIR_NAME)
        resolvedAddedLibDir = Resolver.getDirectory(project, addedLibDir, resolvedLibDir, DEFAULT_ADDED_SUBDIR_NAME)
        resolvedProvidedLibDir = Resolver.getDirectory(project, providedLibDir, resolvedLibDir, DEFAULT_PROVIDED_SUBDIR_NAME)

    }

}
