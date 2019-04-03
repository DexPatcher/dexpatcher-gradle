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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.Utils

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.GradleVersion

@CompileStatic
class LazyZipTask extends Zip {

    // Conditionally enable workaround for Gradle 5.1 bug (https://github.com/gradle/gradle/issues/8401).
    private static final boolean GRADLE_5_1 = (GradleVersion.current() >= GradleVersion.version('5.1'))

    // Enable workaround for Gradle < 5.1 unexpected behavior seemingly related to convention implementation.
    private static final boolean DESTINATION_DIR_WORKAROUND = true

    // Client must configure the output archive only using these properties:
    @Internal final Property<String> lazyArchiveFileName
    @Internal final DirectoryProperty lazyDestinationDirectory

    LazyZipTask() {
        if (GRADLE_5_1) {
            // On Gradle >= 5.1 simply alias existing properties.
            lazyArchiveFileName = gradle_5_1_getArchiveFileName()
            lazyDestinationDirectory = gradle_5_1_getDestinationDirectory()
        } else {
            // On Gradle < 5.1 create new properties and override existing getters.
            lazyArchiveFileName = project.objects.property(String)
            lazyArchiveFileName.set project.<String> provider {
                super.getArchiveName()
            }
            lazyDestinationDirectory = project.layout.directoryProperty()
            if (!DESTINATION_DIR_WORKAROUND) {
                lazyDestinationDirectory.set project.<Directory> provider {
                    Utils.getDirectory(project, super.getDestinationDir())
                }
            }
        }
    }

    @CompileDynamic
    private Property<String> gradle_5_1_getArchiveFileName() {
        return this.getArchiveFileName()
    }

    @CompileDynamic
    private DirectoryProperty gradle_5_1_getDestinationDirectory() {
        return this.getDestinationDirectory()
    }

    @Override @Internal public String getArchiveName() {
        if (GRADLE_5_1) {
            return super.getArchiveName()
        } else {
            return lazyArchiveFileName.get()
        }
    }

    @Override @Internal public File getDestinationDir() {
        if (GRADLE_5_1) {
            return super.getDestinationDir()
        } else {
            if (!DESTINATION_DIR_WORKAROUND) {
                return lazyDestinationDirectory.get().asFile
            } else {
                destinationDir = lazyDestinationDirectory.get().asFile
                return super.getDestinationDir()
            }
        }
    }

}
