/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
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

import lanchon.dexpatcher.gradle.NewProperty

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.GradleVersion

// WARNING: getDestinationDir() should not be used by client code as it will not reflect the
// actual destination directory if it has been set via the lazyDestinationDirectory property.

@CompileStatic
class LazyZipTask extends Zip {

    // Conditionally enable workaround for AbstractArchiveTask regression in Gradle 5.1 and later.
    // See: https://github.com/gradle/gradle/issues/8401
    private static final boolean GRADLE_5_1 = (GradleVersion.current() >= GradleVersion.version('5.1'))

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
            lazyDestinationDirectory = NewProperty.dir(project)
            // And also mimic the removed '@OutputFile' annotation on 'archivePath'.
            // See: https://github.com/gradle/gradle/issues/9204
            outputs.files({ archivePath }).withPropertyName('archivePath')
        }
    }

    @CompileDynamic
    private Property<String> gradle_5_1_getArchiveFileName() {
        this.getArchiveFileName()
    }

    @CompileDynamic
    private DirectoryProperty gradle_5_1_getDestinationDirectory() {
        this.getDestinationDirectory()
    }

    @Override @Internal String getArchiveName() {
        GRADLE_5_1 ?
                super.archiveName :
                lazyArchiveFileName.orNull ?: super.archiveName
    }

    @Override @Internal File getArchivePath() {
        GRADLE_5_1 ?
                super.archivePath :
                new File((lazyDestinationDirectory.orNull?.asFile) ?: destinationDir, archiveName)
    }

}
