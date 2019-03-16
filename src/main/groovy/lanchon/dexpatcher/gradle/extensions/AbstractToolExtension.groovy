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
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

@CompileStatic
abstract class AbstractToolExtension extends AbstractSubextension {

    protected static final String PREFIX = super.PREFIX + 'tool.'

    final DirectoryProperty dir
    final ListProperty<String> extraArgs
    final Property<Boolean> addBlankLines
    //boolean deleteOutputs = true

    final Provider<Directory> resolvedDir
    final Provider<FileCollection> classpath

    AbstractToolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {

        super(project, dexpatcherConfig)

        dir = project.layout.directoryProperty()
        extraArgs = project.objects.listProperty(String)
        addBlankLines = project.objects.property(Boolean)
        //addBlankLines.set ((Boolean) null)

        resolvedDir = Resolver.getDirectory(project, dir, dexpatcherConfig.resolvedToolDir, name)
        classpath = Resolver.getJars(project, resolvedDir)

    }

    protected abstract String getName()

}
