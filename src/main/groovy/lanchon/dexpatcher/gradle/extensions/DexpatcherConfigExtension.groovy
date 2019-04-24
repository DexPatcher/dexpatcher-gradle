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

import lanchon.dexpatcher.gradle.DexpatcherProperties
import lanchon.dexpatcher.gradle.NewProperty
import lanchon.dexpatcher.gradle.Utils

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

@CompileStatic
class DexpatcherConfigExtension extends AbstractExtension {

    private static final String DIR_PROPERTY = PREFIX + 'dir'

    private static final String TOOL_DIR_PROPERTY = PREFIX + 'tool.dir'
    private static final String LIB_DIR_PROPERTY = PREFIX + 'lib.dir'
    private static final String PROVIDED_LIB_DIR_PROPERTY = PREFIX + 'lib.provided.dir'
    //private static final String COMPILE_LIB_DIR_PROPERTY = PREFIX + 'lib.compile.dir'
    //private static final String RUNTIME_LIB_DIR_PROPERTY = PREFIX + 'lib.runtime.dir'

    private static final String DEFAULT_TOOL_SUBDIR_NAME = 'tools'
    private static final String DEFAULT_LIB_SUBDIR_NAME = 'libs'
    private static final String DEFAULT_PROVIDED_SUBDIR_NAME = 'provided'
    //private static final String DEFAULT_COMPILE_SUBDIR_NAME = 'compile'
    //private static final String DEFAULT_RUNTIME_SUBDIR_NAME = 'runtime'

    final DexpatcherProperties properties = new DexpatcherProperties(project)

    final DirectoryProperty dir = newDirProperty(DIR_PROPERTY)

    final DirectoryProperty toolDir = newDirProperty(TOOL_DIR_PROPERTY)
    final DirectoryProperty libDir = newDirProperty(LIB_DIR_PROPERTY)
    final DirectoryProperty providedLibDir = newDirProperty(PROVIDED_LIB_DIR_PROPERTY)
    //final DirectoryProperty compileLibDir = newDirProperty(COMPILE_LIB_DIR_PROPERTY)
    //final DirectoryProperty runtimeLibDir = newDirProperty(RUNTIME_LIB_DIR_PROPERTY)

    final Provider<Directory> resolvedToolDir
    final Provider<Directory> resolvedLibDir
    final Provider<Directory> resolvedProvidedLibDir
    //final Provider<Directory> resolvedCompileLibDir
    //final Provider<Directory> resolvedRuntimeLibDir

    DexpatcherConfigExtension(Project project) {
        super(project)
        resolvedToolDir = Utils.getResolvedDir(project, toolDir, dir, DEFAULT_TOOL_SUBDIR_NAME)
        resolvedLibDir = Utils.getResolvedDir(project, libDir, dir, DEFAULT_LIB_SUBDIR_NAME)
        resolvedProvidedLibDir = Utils.getResolvedDir(project, providedLibDir, resolvedLibDir, DEFAULT_PROVIDED_SUBDIR_NAME)
        //resolvedCompileLibDir = Utils.getResolvedDir(project, compileLibDir, resolvedLibDir, DEFAULT_COMPILE_SUBDIR_NAME)
        //resolvedRuntimeLibDir = Utils.getResolvedDir(project, runtimeLibDir, resolvedLibDir, DEFAULT_RUNTIME_SUBDIR_NAME)
    }

    private DirectoryProperty newDirProperty(String dirProperty) {
        NewProperty.dir(project, this, dirProperty)
    }

}
