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

import groovy.transform.CompileStatic

import org.gradle.api.Project

@CompileStatic
class ParsedProperties extends ProjectProperties {

    ParsedProperties(Project project) {
        super(project)
    }

    boolean getAsBoolean(String key, boolean defaultValue) {
        String value = get(key)?.trim()
        return value ? Boolean.parseBoolean(value) : defaultValue
    }

    int getAsInteger(String key, int defaultValue) {
        String value = get(key)?.trim()
        return value ? Integer.parseInt(value) : defaultValue
    }

    /*
    // NOTE: File and directory properties resolve against the root project, not the current project.

    Directory getAsDirectory(String key) {
        String value = get(key)
        return value ? project.rootProject.layout.projectDirectory.dir(value) : null
    }

    RegularFile getAsRegularFile(String key) {
        String value = get(key)
        return value ? project.rootProject.layout.projectDirectory.file(value) : null
    }
    */

}
