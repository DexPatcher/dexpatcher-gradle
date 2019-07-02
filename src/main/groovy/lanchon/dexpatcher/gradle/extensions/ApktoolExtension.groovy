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
import lanchon.dexpatcher.gradle.tasks.AbstractApktoolTask.Verbosity

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

@CompileStatic
class ApktoolExtension extends AbstractToolExtension {

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE

    // Base
    final Property<Verbosity> verbosity = project.objects.property(Verbosity)
    final DirectoryProperty frameworkDir = NewProperty.dir(project)

    // Decode
    final Property<String> frameworkTag = project.objects.property(String)
    final Property<Integer> apiLevel = NewProperty.from(project, 0)
    final Property<Boolean> decodeAssets = NewProperty.from(project, true)
    final Property<Boolean> decodeResources = NewProperty.from(project, true)
    final Property<Boolean> decodeClasses = NewProperty.from(project, true)
    final Property<Boolean> forceDecodeManifest = NewProperty.from(project, false)
    final Property<Boolean> keepBrokenResources = NewProperty.from(project, false)
    final Property<Boolean> stripDebugInfo = NewProperty.from(project, false)
    final Property<Boolean> matchOriginal = NewProperty.from(project, false)

    // Build
    final RegularFileProperty aaptFile = NewProperty.file(project)
    final Property<Boolean> useAapt2 = NewProperty.from(project, false)
    final Property<Boolean> crunchResources = NewProperty.from(project, true)
    final Property<Boolean> copyOriginal = NewProperty.from(project, false)
    final Property<Boolean> forceDebuggableBuild = NewProperty.from(project, false)
    final Property<Boolean> forceCleanBuild = NewProperty.from(project, false)

    ApktoolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig, Configuration apktoolCfg) {
        super(project, dexpatcherConfig)
        classpath.from { apktoolCfg.singleFile }
    }

}
