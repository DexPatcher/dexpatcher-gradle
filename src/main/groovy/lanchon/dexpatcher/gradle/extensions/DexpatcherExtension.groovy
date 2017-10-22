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
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask.Verbosity

import org.gradle.api.Project

@CompileStatic
class DexpatcherExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'dexpatcher'

    private static final String DIR_PROPERTY = 'dexpatcher.dexpatcher.dir'

    private static final String DEFAULT_SUBDIR_NAME = EXTENSION_NAME

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE
    static final def DEBUG = Verbosity.DEBUG

    Integer apiLevel
    Boolean multiDex
    Boolean multiDexThreaded
    Integer multiDexJobs
    Integer maxDexPoolSize
    String annotationPackage
    Boolean constructorAutoIgnore = true
    Boolean compatDexTag
    Verbosity verbosity
    Boolean logSourcePath
    def logSourcePathRoot
    Boolean logStats

    DexpatcherExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig, DEFAULT_SUBDIR_NAME)
        def properties = dexpatcherConfig.properties
        dir = properties.getAsFile(DIR_PROPERTY)
    }

    String getLogSourcePathRoot() { Resolver.resolve(logSourcePathRoot) as String }

}
