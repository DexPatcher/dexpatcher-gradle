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

import org.gradle.api.Project

@CompileStatic
class Dex2jarExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'dex2jar'

    private static final String PREFIX = super.PREFIX + EXTENSION_NAME + '.'

    private static final String DIR_PROPERTY = PREFIX + 'dir'

    Boolean translateCode = true
    Boolean translateDebugInfo
    Boolean optimizeSynchronized
    Boolean reuseRegisters
    Boolean topologicalSort
    Boolean handleExceptions

    Dex2jarExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig)
        def properties = dexpatcherConfig.properties
        dir = properties.getAsFile(DIR_PROPERTY)
    }

    @Override
    protected String getName() { EXTENSION_NAME }

}
