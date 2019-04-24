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

import lanchon.dexpatcher.gradle.tasks.DexpatcherTask.Verbosity

import org.gradle.api.Project
import org.gradle.api.provider.Property

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class DexpatcherExtension extends AbstractToolExtension {

    private static final String PREFIX = super.PREFIX + ToolNames.DEXPATCHER + '.'

    private static final String DIR_PROPERTY = PREFIX + 'dir'

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE
    static final def DEBUG = Verbosity.DEBUG

    final Property<Integer> apiLevel = project.objects.property(Integer)
    final Property<Boolean> multiDex = project.objects.property(Boolean)
    final Property<Boolean> multiDexThreaded = project.objects.property(Boolean)
    final Property<Integer> multiDexJobs = project.objects.property(Integer)
    final Property<Integer> maxDexPoolSize = project.objects.property(Integer)
    final Property<String> annotationPackage = project.objects.property(String)
    final Property<Boolean> constructorAutoIgnore = project.objects.property(Boolean)
    final Property<Boolean> compatDexTag = project.objects.property(Boolean)
    final Property<Verbosity> verbosity = project.objects.property(Verbosity)
    final Property<Boolean> logSourcePath = project.objects.property(Boolean)
    final Property<String> logSourcePathRoot = project.objects.property(String)
    final Property<Boolean> logStats = project.objects.property(Boolean)

    DexpatcherExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig)
        def properties = dexpatcherConfig.properties
        dir.set properties.getAsDirectory(DIR_PROPERTY)
        constructorAutoIgnore.set true
    }

    @Override
    protected String getName() { ToolNames.DEXPATCHER }

}
