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
import org.gradle.api.file.FileCollection

@CompileStatic
abstract class Resolver {

    static def resolve(Object object) {
        if (object instanceof Closure) resolve(object.call())
        else object
    }

    static <T> T resolve(Object object, Closure<T> closure) {
        if (object instanceof Closure) resolve(object.call(), closure)
        else closure.call(object)
    }

    static <T> T resolveNullable(Object object, Closure<T> closure) {
        if (object instanceof Closure) resolveNullable(object.call(), closure)
        else if (object) closure.call(object)
        else null
    }

    static File resolveNullableFile(Project project, Object object) {
        resolveNullable(object) { project.file(it) }
    }

    static FileCollection resolveNullableFiles(Project project, Object object) {
        resolveNullable(object) { project.files(it) }
    }

    static File resolveSingleFile(Project project, Object object, String... includes) {
        def fileOrDir = Resolver.resolveNullableFile(project, object)
        if (fileOrDir.isFile()) return fileOrDir
        def tree = project.fileTree(fileOrDir)
        tree.include includes
        return tree.singleFile
    }

    static File getFile(File parent, String child) {
        if (parent == null) throw new NullPointerException();
        return new File(parent, child)
    }

    static File getFile(File file, File defaultParent, String defaultChild) {
        file ?: (defaultParent ? new File(defaultParent, defaultChild) : null)
    }

    static FileCollection getJars(Project project, File fileOrDir) {
        def jars = project.fileTree(fileOrDir)
        jars.include '**/*.jar'
        return jars
    }

}
