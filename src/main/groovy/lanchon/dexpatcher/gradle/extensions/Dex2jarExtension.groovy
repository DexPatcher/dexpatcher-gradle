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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.gradle.api.tasks.util.PatternFilterable

@CompileStatic
class Dex2jarExtension extends AbstractToolExtension {

    final Property<Boolean> translateCode = NewProperty.from(project, true)
    final Property<Boolean> translateDebugInfo = NewProperty.from(project, false)
    final Property<Boolean> optimizeSynchronized = NewProperty.from(project, false)
    final Property<Boolean> reuseRegisters = NewProperty.from(project, false)
    final Property<Boolean> topologicalSort = NewProperty.from(project, false)
    final Property<Boolean> handleExceptions = NewProperty.from(project, false) // or true?

    Dex2jarExtension(Project project, DexpatcherConfigExtension dexpatcherConfig, Configuration dex2jarCfg) {
        super(project, dexpatcherConfig)
        classpath.from {
            def file = dex2jarCfg.singleFile
            def files = file.isDirectory() ? project.fileTree(file) : project.zipTree(file)
            def filteredFiles = files.matching { PatternFilterable filter ->
                filter.include 'lib/*.jar', '*/lib/*.jar'
            }
            return filteredFiles
        }
    }

}
