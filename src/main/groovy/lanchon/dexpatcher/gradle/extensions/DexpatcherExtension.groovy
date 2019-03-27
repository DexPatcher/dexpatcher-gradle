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

import lanchon.dexpatcher.gradle.Constants
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask.Verbosity

import org.gradle.api.Project
import org.gradle.api.provider.Property

@CompileStatic
class DexpatcherExtension extends AbstractToolExtension {

    private static final String PREFIX = super.PREFIX + Constants.TOOL_DEXPATCHER + '.'

    private static final String DIR_PROPERTY = PREFIX + 'dir'

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE
    static final def DEBUG = Verbosity.DEBUG

    final Property<Integer> apiLevel
    final Property<Boolean> multiDex
    final Property<Boolean> multiDexThreaded
    final Property<Integer> multiDexJobs
    final Property<Integer> maxDexPoolSize
    final Property<String> annotationPackage
    final Property<Boolean> constructorAutoIgnore
    final Property<Boolean> compatDexTag
    final Property<Verbosity> verbosity
    final Property<Boolean> logSourcePath
    final Property<String> logSourcePathRoot
    final Property<Boolean> logStats

    DexpatcherExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {

        super(project, dexpatcherConfig)
        def properties = dexpatcherConfig.properties
        dir.set properties.getAsDirectory(DIR_PROPERTY)

        apiLevel = project.objects.property(Integer)
        multiDex = project.objects.property(Boolean)
        multiDexThreaded = project.objects.property(Boolean)
        multiDexJobs = project.objects.property(Integer)
        maxDexPoolSize = project.objects.property(Integer)
        annotationPackage = project.objects.property(String)
        constructorAutoIgnore = project.objects.property(Boolean)
        constructorAutoIgnore.set true
        compatDexTag = project.objects.property(Boolean)
        verbosity = project.objects.property(Verbosity)
        logSourcePath = project.objects.property(Boolean)
        logSourcePathRoot = project.objects.property(String)
        logStats = project.objects.property(Boolean)

    }

    @Override
    protected String getName() { Constants.TOOL_DEXPATCHER }

}
