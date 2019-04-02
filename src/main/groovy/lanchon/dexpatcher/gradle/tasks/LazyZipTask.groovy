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

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Zip

// FIXME: This hack apparently does not work on Gradle 5.1 or later.
// (See: https://github.com/gradle/gradle/issues/8401)

@CompileStatic
class LazyZipTask extends Zip {

    @Internal final Property<String> lazyArchiveFileName
    @Internal final DirectoryProperty lazyDestinationDirectory

    LazyZipTask() {
        lazyArchiveFileName = project.objects.property(String)
        lazyArchiveFileName.set project.<String>provider {
            super.getArchiveName()
        }
        lazyDestinationDirectory = project.layout.directoryProperty()
        //lazyDestinationDirectory.set project.<Directory>provider {
        //    Utils.getDirectory(project, super.getDestinationDir())
        //}
    }

    @Override @Internal public String getArchiveName() {
        return lazyArchiveFileName.get()
    }

    @Override @Internal public File getDestinationDir() {
        //return lazyDestinationDirectory.get().asFile
        super.destinationDir = lazyDestinationDirectory.get().asFile
        return super.getDestinationDir()
    }

}
