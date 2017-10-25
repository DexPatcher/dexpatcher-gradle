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

    static def resolve(def object) {
        object instanceof Closure ? resolve(object.call()) : object
    }

    static <T> T resolve(def object, Closure<T> closure) {
        object instanceof Closure ? resolve(object.call(), closure) : closure.call(object)
    }

    static <T> T resolveNullable(def object, Closure<T> closure) {
        object instanceof Closure ? resolveNullable(object.call(), closure) : object ? closure.call(object) : null
    }

    static File resolveNullableFile(Project project, def file) {
        resolveNullable(file) { project.file(it) }
    }

    static FileCollection resolveNullableFiles(Project project, def files) {
        resolveNullable(files) { project.files(it) }
    }

    static File resolveSingleFile(Project project, def fileOrDir, String... includes) {
        def path = Resolver.resolveNullableFile(project, fileOrDir)
        if (path.isFile()) return path
        def tree = project.fileTree(path)
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
