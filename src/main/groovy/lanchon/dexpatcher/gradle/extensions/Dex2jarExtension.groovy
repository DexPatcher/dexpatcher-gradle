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

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class Dex2jarExtension extends AbstractToolExtension {

    private static final String PREFIX = super.PREFIX + ToolNames.DEX2JAR + '.'

    private static final String DIR_PROPERTY = PREFIX + 'dir'

    final Property<Boolean> translateCode = project.objects.property(Boolean)
    final Property<Boolean> translateDebugInfo = project.objects.property(Boolean)
    final Property<Boolean> optimizeSynchronized = project.objects.property(Boolean)
    final Property<Boolean> reuseRegisters = project.objects.property(Boolean)
    final Property<Boolean> topologicalSort = project.objects.property(Boolean)
    final Property<Boolean> handleExceptions = project.objects.property(Boolean)

    Dex2jarExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig)
        def properties = dexpatcherConfig.properties
        dir.set properties.getAsDirectory(DIR_PROPERTY)
        translateCode.set true
        //handleExceptions.set true
    }

    @Override
    protected String getName() { ToolNames.DEX2JAR }

}
