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
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider

@CompileStatic
abstract class Utils {

    static File getFile(File parent, String child) {
        if (parent.is(null)) throw new NullPointerException();
        return new File(parent, child)
    }

    static Provider<Directory> getResolvedDir(Project project, Provider<Directory> directory, Provider<Directory> defaultParent, String defaultChild) {
        project.providers.<Directory>provider {
            directory.orNull ?: defaultParent.orNull?.dir(defaultChild)
        }
    }

    static FileCollection getJars(Project project, Directory rootDir) {
        def jars = project.fileTree(rootDir)
        jars.include '**/*.jar'
        return jars
    }

    static Provider<FileCollection> getJars(Project project, Provider<Directory> rootDir) {
        project.providers.<FileCollection>provider {
            Directory dir = rootDir.orNull
            return !dir.is(null) ? getJars(project, dir) : null
        }
    }

    static void addJarDependency(Project project, String configuration, Directory jarDir) {
        def jars = Utils.getJars(project, jarDir)
        def dependency = project.dependencies.create(jars)
        project.configurations.getByName(configuration).dependencies.add(dependency)
    }

}
