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

@CompileStatic
class DexpatcherConfigExtension {

    static final String EXTENSION_NAME = 'dexpatcherConfig'

    private static final String PREFIX = 'dexpatcher.'

    private static final String DIR_PROPERTY = PREFIX + 'dir'
    private static final String TOOL_DIR_PROPERTY = PREFIX + 'tool.dir'
    private static final String LIB_DIR_PROPERTY = PREFIX + 'lib.dir'
    private static final String COMPILE_LIB_DIR_PROPERTY = PREFIX + 'lib.compile.dir'
    private static final String PROVIDED_LIB_DIR_PROPERTY = PREFIX + 'lib.provided.dir'

    private static final String DEFAULT_TOOL_SUBDIR_NAME = 'tools'
    private static final String DEFAULT_LIB_SUBDIR_NAME = 'libs'
    private static final String DEFAULT_COMPILE_SUBDIR_NAME = 'compile'
    private static final String DEFAULT_PROVIDED_SUBDIR_NAME = 'provided'

    protected final Project project
    protected final ProjectProperties properties

    def dir
    def toolDir
    def libDir
    def compileLibDir
    def providedLibDir

    DexpatcherConfigExtension(Project project) {
        this.project = project
        properties = new ProjectProperties(project)
        dir = properties.getAsFile(DIR_PROPERTY)
        toolDir = properties.getAsFile(TOOL_DIR_PROPERTY)
        libDir = properties.getAsFile(LIB_DIR_PROPERTY)
        compileLibDir = properties.getAsFile(COMPILE_LIB_DIR_PROPERTY)
        providedLibDir = properties.getAsFile(PROVIDED_LIB_DIR_PROPERTY)
    }

    File getDir() { Resolver.resolveNullableFile(project, dir) }
    File getToolDir() { Resolver.resolveNullableFile(project, toolDir) }
    File getLibDir() { Resolver.resolveNullableFile(project, libDir) }
    File getCompileLibDir() { Resolver.resolveNullableFile(project, compileLibDir) }
    File getProvidedLibDir() { Resolver.resolveNullableFile(project, providedLibDir) }

    File getResolvedToolDir() { Resolver.getFile(getToolDir(), getDir(), DEFAULT_TOOL_SUBDIR_NAME) }
    File getResolvedLibDir() { Resolver.getFile(getLibDir(), getDir(), DEFAULT_LIB_SUBDIR_NAME) }

    File getResolvedCompileLibDir() { Resolver.getFile(getCompileLibDir(), getResolvedLibDir(), DEFAULT_COMPILE_SUBDIR_NAME) }
    File getResolvedProvidedLibDir() { Resolver.getFile(getProvidedLibDir(), getResolvedLibDir(), DEFAULT_PROVIDED_SUBDIR_NAME) }

    ProjectProperties getProperties() { properties }

}
