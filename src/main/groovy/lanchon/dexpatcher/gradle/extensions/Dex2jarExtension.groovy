/*
 * DexPatcher - Copyright 2015, 2016 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic

import org.gradle.api.Project

@CompileStatic
class Dex2jarExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'dex2jar'

    private static final String DIR_PROPERTY = 'dexpatcher.dex2jar.dir'

    private static final String DEFAULT_SUBDIR_NAME = EXTENSION_NAME

    Boolean translateCode = true
    Boolean translateDebugInfo
    Boolean optimizeSynchronized
    Boolean reuseRegisters
    Boolean topologicalSort
    Boolean handleExceptions

    Dex2jarExtension(Project project, DexpatcherConfigExtension dexpatcherConfig, Closure getProperty) {
        super(project, dexpatcherConfig, DEFAULT_SUBDIR_NAME)
        dir = getProperty(DIR_PROPERTY)
    }

}
