/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.plugins

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class AbstractPlugin implements Plugin<Project> {

    protected Project project
    protected DexpatcherBasePlugin basePlugin

    void apply(Project project) {

        this.project = project
        basePlugin = project.plugins.apply(DexpatcherBasePlugin)

    }

    protected void afterApply() {}

}
