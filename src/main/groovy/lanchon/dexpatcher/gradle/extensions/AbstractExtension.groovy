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
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer

@CompileStatic
abstract class AbstractExtension<P> {

    protected final Project project
    protected final P parent;

    ExtensionContainer getExtensions() {
        (this as ExtensionAware).extensions
    }

    AbstractExtension(Project project, P parent) {
        this.project = project
        this.parent = parent
    }

}
