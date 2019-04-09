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

import lanchon.dexpatcher.gradle.tasks.AbstractApktoolTask.Verbosity

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class ApktoolExtension extends AbstractToolExtension {

    private static final String PREFIX = super.PREFIX + ToolNames.APKTOOL + '.'

    private static final String DIR_PROPERTY = PREFIX + 'dir'
    private static final String FRAMEWORK_DIR_PROPERTY = PREFIX + 'framework.dir'
    private static final String AAPT_FILE_PROPERTY = PREFIX + 'aapt.file'

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE

    // Base
    final Property<Verbosity> verbosity
    final DirectoryProperty frameworkDir

    // Decode
    final Property<String> frameworkTag
    final Property<Integer> apiLevel
    final Property<Boolean> decodeAssets
    final Property<Boolean> decodeResources
    final Property<Boolean> decodeClasses
    final Property<Boolean> keepBrokenResources
    final Property<Boolean> stripDebugInfo
    final Property<Boolean> matchOriginal

    // Build
    final RegularFileProperty aaptFile
    final Property<Boolean> copyOriginal
    final Property<Boolean> forceDebuggableBuild
    final Property<Boolean> forceCleanBuild

    ApktoolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {

        super(project, dexpatcherConfig)
        def properties = dexpatcherConfig.properties
        dir.set properties.getAsDirectory(DIR_PROPERTY)

        verbosity = project.objects.property(Verbosity)
        frameworkDir = project.layout.directoryProperty()
        frameworkDir.set properties.getAsDirectory(FRAMEWORK_DIR_PROPERTY)

        frameworkTag = project.objects.property(String)
        apiLevel = project.objects.property(Integer)
        decodeAssets = project.objects.property(Boolean)
        decodeAssets.set true
        decodeResources = project.objects.property(Boolean)
        decodeResources.set true
        decodeClasses = project.objects.property(Boolean)
        decodeClasses.set true
        keepBrokenResources = project.objects.property(Boolean)
        stripDebugInfo = project.objects.property(Boolean)
        matchOriginal = project.objects.property(Boolean)

        aaptFile = project.layout.fileProperty()
        aaptFile.set properties.getAsRegularFile(AAPT_FILE_PROPERTY)
        copyOriginal = project.objects.property(Boolean)
        forceDebuggableBuild = project.objects.property(Boolean)
        forceCleanBuild = project.objects.property(Boolean)

    }

    @Override
    protected String getName() { ToolNames.APKTOOL }

}
