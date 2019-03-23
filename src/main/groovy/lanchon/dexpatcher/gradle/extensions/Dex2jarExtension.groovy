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

import org.gradle.api.Project
import org.gradle.api.provider.Property

@CompileStatic
class Dex2jarExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'dex2jar'

    private static final String PREFIX = super.PREFIX + EXTENSION_NAME + '.'

    private static final String DIR_PROPERTY = PREFIX + 'dir'

    final Property<Boolean> translateCode
    final Property<Boolean> translateDebugInfo
    final Property<Boolean> optimizeSynchronized
    final Property<Boolean> reuseRegisters
    final Property<Boolean> topologicalSort
    final Property<Boolean> handleExceptions

    Dex2jarExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {

        super(project, dexpatcherConfig)
        def properties = dexpatcherConfig.properties
        dir.set properties.getAsDirectory(DIR_PROPERTY)

        translateCode = project.objects.property(Boolean)
        translateCode.set true
        translateDebugInfo = project.objects.property(Boolean)
        optimizeSynchronized = project.objects.property(Boolean)
        reuseRegisters = project.objects.property(Boolean)
        topologicalSort = project.objects.property(Boolean)
        handleExceptions = project.objects.property(Boolean)
        //handleExceptions.set true

    }

    @Override
    protected String getName() { EXTENSION_NAME }

}
