/*
 * DexPatcher - Copyright 2015-2017 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.Resolver

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@CompileStatic
abstract class AbstractToolExtension extends AbstractSubextension {

    protected static final String PREFIX = super.PREFIX + 'tool.'

    def dir
    def extraArgs
    //boolean addBlankLines
    //boolean deleteOutputs = true

    AbstractToolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig)
    }

    protected abstract String getName()

    File getDir() { Resolver.resolveNullableFile(project, dir) }
    File getResolvedDir() { Resolver.getFile(getDir(), dexpatcherConfig.getResolvedToolDir(), name) }

    FileCollection getClasspath() { Resolver.getJars(project, getResolvedDir()) }

    List<String> getExtraArgs() { Resolver.resolve(extraArgs).collect() { it as String } }

}
