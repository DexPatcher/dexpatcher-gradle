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
    final Property<Verbosity> verbosity = project.objects.property(Verbosity)
    final DirectoryProperty frameworkDir = project.layout.directoryProperty()

    // Decode
    final Property<String> frameworkTag = project.objects.property(String)
    final Property<Integer> apiLevel = project.objects.property(Integer)
    final Property<Boolean> decodeAssets = project.objects.property(Boolean)
    final Property<Boolean> decodeResources = project.objects.property(Boolean)
    final Property<Boolean> decodeClasses = project.objects.property(Boolean)
    final Property<Boolean> forceDecodeManifest = project.objects.property(Boolean)
    final Property<Boolean> keepBrokenResources = project.objects.property(Boolean)
    final Property<Boolean> stripDebugInfo = project.objects.property(Boolean)
    final Property<Boolean> matchOriginal = project.objects.property(Boolean)

    // Build
    final RegularFileProperty aaptFile = project.layout.fileProperty()
    final Property<Boolean> useAapt2 = project.objects.property(Boolean)
    final Property<Boolean> crunchResources = project.objects.property(Boolean)
    final Property<Boolean> copyOriginal = project.objects.property(Boolean)
    final Property<Boolean> forceDebuggableBuild = project.objects.property(Boolean)
    final Property<Boolean> forceCleanBuild = project.objects.property(Boolean)

    ApktoolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig, DIR_PROPERTY)
        frameworkDir.set dexpatcherConfig.properties.getAsDirectory(FRAMEWORK_DIR_PROPERTY)
        decodeAssets.set true
        decodeResources.set true
        decodeClasses.set true
        aaptFile.set dexpatcherConfig.properties.getAsRegularFile(AAPT_FILE_PROPERTY)
        crunchResources.set true
    }

    @Override
    protected String getName() { ToolNames.APKTOOL }

}
