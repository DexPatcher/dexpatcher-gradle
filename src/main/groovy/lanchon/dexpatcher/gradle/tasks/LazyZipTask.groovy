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

import groovy.transform.CompileStatic

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Zip

// WARNING: getDestinationDir() should not be used by client code as it will not reflect the
// actual destination directory if it has been set via the lazyDestinationDirectory property.

@CompileStatic
class LazyZipTask extends Zip {

    @Internal final Property<String> lazyArchiveFileName = archiveFileName
    @Internal final DirectoryProperty lazyDestinationDirectory = destinationDirectory

}
