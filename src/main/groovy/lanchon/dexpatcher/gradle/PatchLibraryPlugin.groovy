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

import lanchon.dexpatcher.gradle.extensions.PatchLibraryExtension

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

@CompileStatic
class PatchLibraryPlugin extends AbstractPatcherPlugin<PatchLibraryExtension, LibraryExtension, LibraryVariant> {

    @Override
    void apply(Project project) {

        super.apply(project)

        def subextensions = (dexpatcherConfig as ExtensionAware).extensions
        patcherExtension = (PatchLibraryExtension) subextensions.create(PatchLibraryExtension.EXTENSION_NAME,
                PatchLibraryExtension, project, dexpatcherConfig)

        project.plugins.apply(LibraryPlugin)
        androidExtension = project.extensions.getByType(LibraryExtension)
        androidVariants = androidExtension.libraryVariants

        dexpatcherConfig.addLibDependencies(false)

        applyAfterAndroid()

    }

    @Override
    protected void applyAfterAndroid() {

        super.applyAfterAndroid()

        //project.afterEvaluate {
        //    libraryVariants.all { LibraryVariant variant ->
        //    }
        //}

    }

}
