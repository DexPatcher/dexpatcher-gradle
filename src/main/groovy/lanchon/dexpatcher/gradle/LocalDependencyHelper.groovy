/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
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
import org.gradle.api.plugins.JavaPlugin
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier

// FIXME: Avoid futile download attempts of decorated dependencies.

@CompileStatic
abstract class LocalDependencyHelper {

    private static final boolean DECORATE_DEPENDENCIES = true

    private static final Field targetComponentId_FIELD
    private static final Exception targetComponentId_EXCEPTION

    static {
        try {
            def field = DefaultSelfResolvingDependency.getDeclaredField('targetComponentId')
            field.accessible = true
            targetComponentId_FIELD = field
            targetComponentId_EXCEPTION = null
        } catch (Exception e) {
            targetComponentId_FIELD = null
            targetComponentId_EXCEPTION = e
        }
    }

    static void addDexpatcherAnnotations(Project project, def files, boolean decorate) {
        addKnown(project, JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, files, decorate,
                'DexPatcher Annotations', 'dexpatcher-annotation')
    }

    static void addAppClasses(Project project, def files, boolean decorate) {
        addKnown(project, JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, files, decorate,
                'App Classes', 'app-classes')
    }

    static void addAppComponents(Project project, def files, boolean decorate) {
        addKnown(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, files, decorate,
                'App Components', 'app-components')
    }

    private static void addKnown(Project project, String configurationName, def files,
            boolean decorate, String group, String name) {
        add(project, project.configurations.getByName(configurationName), files, decorate,
                "< $group > ", name, 'unspecified')
    }

    private static void add(Project project, Configuration configuration, def localDependencyNotation,
            boolean decorate, String group, String name, String version) {
        def dependency = project.dependencies.create(localDependencyNotation)
        if (DECORATE_DEPENDENCIES && decorate) {
            try {
                if (targetComponentId_EXCEPTION) throw targetComponentId_EXCEPTION
                targetComponentId_FIELD.set dependency,
                        new DefaultModuleComponentIdentifier(DefaultModuleIdentifier.newId(group, name), version)
            } catch (Exception e) {
                def wrapper = new RuntimeException(
                        "Cannot set field 'DefaultSelfResolvingDependency.targetComponentId'", e)
                project.logger.warn "Cannot decorate local dependency '$name': $wrapper.message"
                if (project.logger.debugEnabled) {
                    project.logger.debug "Cannot decorate local dependency '$name'", wrapper
                }
            }
        }
        configuration.dependencies.add dependency
    }

}
