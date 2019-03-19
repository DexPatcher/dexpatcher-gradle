/*
 * DexPatcher - Copyright 2015-2017 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic

import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.process.JavaExecSpec

@CompileStatic
abstract class AbstractJavaExecTask extends JavaExec {

    @Input final ListProperty<String> extraArgs
    @Console final Property<Boolean> addBlankLines

    @Internal final Provider<Boolean> resolvedAddBlankLines

    AbstractJavaExecTask() {

        extraArgs = project.objects.listProperty(String)
        addBlankLines = project.objects.property(Boolean)
        //addBlankLines.set ((Boolean) null)

        resolvedAddBlankLines = project.providers.<Boolean>provider {
            def add = addBlankLines.orNull
            !add.is(null) ? add : defaultAddBlankLines()
        }

    }

    @Override void exec() {
        boolean blankLines = resolvedAddBlankLines.get()
        if (blankLines) println()
        def args = getArgs()
        super.setArgs args
        beforeExec()
        super.exec()
        afterExec()
        if (blankLines) println()
    }

    @Override final JavaExec setArgs(Iterable<?> args) { throw new UnsupportedOperationException() }
    @Override final JavaExec args(Object... args) { throw new UnsupportedOperationException() }
    @Override final JavaExecSpec args(Iterable<?> args) { throw new UnsupportedOperationException() }

    @Override abstract List<String> getArgs()

    protected abstract boolean defaultAddBlankLines()

    protected abstract void beforeExec()
    protected abstract void afterExec()

    protected void addExtraArgsTo(Collection<? super String> args) {
        def extras = extraArgs.orNull
        if (extras) args.addAll(extras)
    }

    protected void deleteOutputFileOrDir(FileSystemLocation file) {
        if (file) project.delete file
    }

    protected void deleteOutputDirContents(Directory dir) {
        if (dir) project.delete project.fileTree(dir)
    }

    protected void checkOutputFile(RegularFile file) {
        if (file && !file.asFile.isFile()) throw new RuntimeException('No output generated')
    }

    protected void checkOutputDir(Directory dir) {
        if (dir && project.fileTree(dir).isEmpty()) throw new RuntimeException('No output generated')
    }

}
