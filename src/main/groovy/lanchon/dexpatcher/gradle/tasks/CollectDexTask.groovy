/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.NewProperty

import com.android.utils.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class CollectDexTask extends DefaultTask {

    @InputFiles final ConfigurableFileCollection inputDirs = project.files()
    @OutputDirectory final DirectoryProperty outputDir = NewProperty.dir(project)

    @TaskAction
    void exec() {
        def outDir = outputDir.get()
        FileUtils.deleteDirectoryContents outDir.asFile
        int n = 0;
        for (def dir : inputDirs.files.toSorted()) {
            def files = project.fileTree(dir)
            files.include '*.dex'
            for (File file : files.toSorted()) {
                String name = n++ ? "classes${n}.dex" : "classes.dex"
                FileUtils.copyFile file, outDir.file(name).asFile
            }
        }
    }

}
