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
        def path = resolveNullableFile(project, fileOrDir)
        if (path.isFile()) return path
        def tree = project.fileTree(path)
        tree.include includes
        return tree.singleFile
    }

    static File getFile(File parent, String child) {
        if (parent.is(null)) throw new NullPointerException();
        return new File(parent, child)
    }

    /*
    static Directory getDirectory(Directory directory, Directory defaultParent, String defaultChild) {
        directory ?: (defaultParent ? defaultParent.dir(defaultChild) : null)
    }
    */

    static Provider<Directory> getDirectory(Project project, Provider<Directory> directory, Provider<Directory> defaultParent, String defaultChild) {
        project.providers.<Directory>provider {
            directory.orNull ?: defaultParent.orNull?.dir(defaultChild)
        }
    }

    static Provider<FileCollection> getJars(Project project, Provider<Directory> rootDir) {
        project.providers.<FileCollection>provider {
            Directory dir = rootDir.orNull
            if (dir.is(null)) return null
            def jars = project.fileTree(dir)
            jars.include '**/*.jar'
            return jars
        }
    }

    /*
    static <T> Provider<T> getConstantProvider(ProviderFactory factory, T value) {
        factory.<T>provider {
            value
        }
    }

    static <T> Provider<T> getDefaultProvider(ProviderFactory factory, Provider<T> value, Provider<T> defaultValue) {
        factory.<T>provider {
            value.orNull ?: defaultValue.orNull
        }
    }
    */

}
