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
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

@CompileStatic
abstract class FileHelper {

    // Horrible Gradle hacks that should not be necessary

    private static Provider<RegularFile> getRegularFileProvider(Project project, File file) {
        def p = NewProperty.file(project)
        p.set file
        return p
    }

    private static Provider<Directory> getDirectoryProvider(Project project, File dir) {
        def p = NewProperty.dir(project)
        p.set dir
        return p
    }

    static RegularFile getRegularFile(Project project, File file) {
        getRegularFileProvider(project, file).get()
    }

    static Directory getDirectory(Project project, File dir) {
        getDirectoryProvider(project, dir).get()
    }

}
