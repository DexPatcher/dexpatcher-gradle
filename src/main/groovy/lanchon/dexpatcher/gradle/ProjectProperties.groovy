/*
 * DexPatcher - Copyright 2015-2017 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile

@CompileStatic
class ProjectProperties {

    private static final String LOCAL_PROPERTIES = 'local.properties'

    protected final Project project
    protected final Properties localProperties

    ProjectProperties(Project project) {
        this.project = project
        localProperties = getPropertiesRecursive(project, LOCAL_PROPERTIES)
    }

    String get(String key) {
        project.properties.get(key) ?: localProperties.getProperty(key)
    }

    // TODO: Decide whether to resolve file properties against the current project instead of the root project
    // (This would be a backwards-breaking change.)

    Directory getAsDirectory(String key) {
        String value = get(key)
        return value ? project.rootProject.layout.projectDirectory.dir(value) : null
    }

    RegularFile getAsRegularFile(String key) {
        String value = get(key)
        return value ? project.rootProject.layout.projectDirectory.file(value) : null
    }

    static Properties getPropertiesRecursive(Project project, String filename) {
        Properties parentProperties = project.parent ? getPropertiesRecursive(project.parent, filename) : null
        Properties properties = new Properties(parentProperties)
        File file = project.file(filename)
        if (file.exists()) {
            file.withInputStream {
                properties.load(it)
            }
        }
        return properties
    }

}
