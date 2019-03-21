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
import org.gradle.api.artifacts.Configuration

@CompileStatic
enum Configurations {

    PROVIDED ( AndroidGradlePlugin.V3 ? 'compileOnly'    : 'provided' ),
    COMPILE  ( AndroidGradlePlugin.V3 ? 'implementation' : 'compile'  ),
    RUNTIME  ( AndroidGradlePlugin.V3 ? 'runtimeOnly'    : 'apk'      );

    private static abstract class AndroidGradlePlugin {
        private final static boolean V3 = true
    }

    final String name

    Configurations(String name) {
        this.name = name
    }

    Configuration get(Project project) {
        project.configurations.getByName(name)
    }

}