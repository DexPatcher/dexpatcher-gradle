/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle

import java.lang.reflect.Field
import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier

@CompileStatic
abstract class LocalDependencyHelper {

    private static final boolean DECORATE_DEPENDENCIES = true

    private static final Field targetComponentIdField

    static {
        // FIXME: Discard exceptions or delay throwing them?
        targetComponentIdField = DefaultSelfResolvingDependency.getDeclaredField('targetComponentId')
        targetComponentIdField.accessible = true
    }

    static void addDexpatcherAnnotations(Project project, String configurationName, def files) {
        addKnown(project, configurationName, files, 'DexPatcher Annotations', 'dexpatcher-annotation')
    }

    static void addAppClasses(Project project, String configurationName, def files) {
        addKnown(project, configurationName, files, 'App Classes', 'app-classes')
    }

    static void addAppComponents(Project project, String configurationName, def files) {
        addKnown(project, configurationName, files, 'App Components', 'app-components')
    }

    private static void addKnown(Project project, String configurationName, def files, String group, String name) {
        add(project, project.configurations.getByName(configurationName), files,
                "< $group > ", name, 'unspecified')
    }

    private static void add(Project project, Configuration configuration, def localDependencyNotation,
            String group, String name, String version) {
        def dependency = project.dependencies.create(localDependencyNotation)
        if (DECORATE_DEPENDENCIES) {
            targetComponentIdField.set dependency,
                    new DefaultModuleComponentIdentifier(DefaultModuleIdentifier.newId(group, name), version)
        }
        configuration.dependencies.add dependency
    }

}
