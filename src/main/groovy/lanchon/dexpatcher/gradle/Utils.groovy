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
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
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

    static FileCollection getJars(Project project, def rootDir) {
        def jars = project.fileTree(rootDir)
        jars.include '**/*.jar'
        return jars
    }

    static void addJarDependency(Project project, Configurations configuration, Directory jarDir) {
        def jars = Utils.getJars(project, jarDir)
        def dependency = project.dependencies.create(jars)
        configuration.get(project).dependencies.add(dependency)
    }

    // Horrible Gradle hacks that should not be necessary

    private static Provider<RegularFile> getRegularFileProvider(Project project, File file) {
        //project.layout.file(getProvider(project, file))
        def p = project.layout.fileProperty()
        p.set file
        return p
    }

    private static Provider<Directory> getDirectoryProvider(Project project, File dir) {
        def p = project.layout.directoryProperty()
        p.set dir
        return p
    }

    static RegularFile getRegularFile(Project project, File file) {
        getRegularFileProvider(project, file).get()
    }

    static Provider<Directory> getDirectory(Project project, File dir) {
        getDirectoryProvider(project, dir)
    }

}
