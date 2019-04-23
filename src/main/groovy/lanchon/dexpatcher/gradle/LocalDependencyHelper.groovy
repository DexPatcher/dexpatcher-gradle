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
    private static final Exception targetComponentIdException

    static {
        Field field
        try {
            field = DefaultSelfResolvingDependency.getDeclaredField('targetComponentId')
            field.accessible = true
            targetComponentIdException = null
        } catch (Exception e) {
            field = null
            targetComponentIdException = e
        }
        targetComponentIdField = field
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
            try {
                if (targetComponentIdException) throw targetComponentIdException
                targetComponentIdField.set dependency,
                        new DefaultModuleComponentIdentifier(DefaultModuleIdentifier.newId(group, name), version)
            } catch (Exception e) {
                def wrapper = new RuntimeException(
                        "Cannot access field 'DefaultSelfResolvingDependency.targetComponentId'", e)
                project.logger.warn "Cannot decorate local dependency '$name': $wrapper.message"
                if (project.logger.debugEnabled) {
                    project.logger.debug "Cannot decorate local dependency '$name'", wrapper
                }
            }
        }
        configuration.dependencies.add dependency
    }

}
