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

import lanchon.dexpatcher.gradle.Resolver
import lanchon.dexpatcher.gradle.tasks.AbstractApktoolTask.Verbosity

import org.gradle.api.Project

@CompileStatic
class ApktoolExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'apktool'

    private static final String DIR_PROPERTY = 'dexpatcher.apktool.dir'
    private static final String FRAMEWORK_DIR_PROPERTY = 'dexpatcher.apktool.frameworkDir'
    private static final String AAPT_FILE_PROPERTY = 'dexpatcher.apktool.aaptFile'

    private static final String DEFAULT_SUBDIR_NAME = EXTENSION_NAME

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE

    // Base
    Verbosity verbosity
    def frameworkDir
    def frameworkDirAsInput
    def frameworkDirAsOutput

    // Decode
    def frameworkTag
    Integer apiLevel
    Boolean decodeAssets = true
    Boolean decodeResources = true
    Boolean decodeClasses = true
    Boolean keepBrokenResources
    Boolean stripDebugInfo
    Boolean matchOriginal

    // Build
    def aaptFile
    Boolean copyOriginal
    Boolean forceDebuggableBuild
    Boolean forceCleanBuild

    ApktoolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig, DEFAULT_SUBDIR_NAME)
        def properties = dexpatcherConfig.properties
        dir = properties.getAsFile(DIR_PROPERTY)
        frameworkDir = properties.getAsFile(FRAMEWORK_DIR_PROPERTY)
        aaptFile = properties.getAsFile(AAPT_FILE_PROPERTY)
    }

    // Base
    File getFrameworkDir() { Resolver.resolveNullableFile(project, frameworkDir) }
    File getFrameworkDirAsInput() { Resolver.resolveNullableFile(project, frameworkDirAsInput) }
    File getFrameworkDirAsOutput() { Resolver.resolveNullableFile(project, frameworkDirAsOutput) }

    // Decode
    String getFrameworkTag() { Resolver.resolve(frameworkTag) as String }

    // Build
    File getAaptFile() { Resolver.resolveNullableFile(project, aaptFile) }

}
