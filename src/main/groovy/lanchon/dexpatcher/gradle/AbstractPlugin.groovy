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

import lanchon.dexpatcher.gradle.extensions.DexpatcherConfigExtension

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer

@CompileStatic
class AbstractPlugin implements Plugin<Project> {

    protected Project project
    protected DexpatcherConfigExtension dexpatcherConfig

    protected ExtensionContainer getSubextensions() {
        (dexpatcherConfig as ExtensionAware).extensions
    }

    void apply(Project project) {

        this.project = project

        project.plugins.apply(DexpatcherBasePlugin)
        dexpatcherConfig = project.extensions.getByType(DexpatcherConfigExtension)

    }

    protected void afterApply() {}

}
