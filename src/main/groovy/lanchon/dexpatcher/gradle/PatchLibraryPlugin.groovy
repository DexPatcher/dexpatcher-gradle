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

import lanchon.dexpatcher.gradle.extensions.AbstractPatcherExtension
import lanchon.dexpatcher.gradle.extensions.PatchLibraryExtension

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.plugins.ExtensionAware

@CompileStatic
class PatchLibraryPlugin extends AbstractPatcherPlugin {

    protected PatchLibraryExtension patchLibraryExtension
    protected LibraryExtension libraryExtension
    protected DefaultDomainObjectSet<LibraryVariant> libraryVariants

    @Override protected AbstractPatcherExtension getPatcherExtension() { patchLibraryExtension }
    @Override protected BaseExtension getAndroidExtension() { libraryExtension }
    @Override protected DomainObjectSet<? extends BaseVariant> getAndroidVariants() { libraryVariants }

    @Override
    void apply(Project project) {

        super.apply(project)

        def subextensions = (dexpatcherConfig as ExtensionAware).extensions
        patchLibraryExtension = (PatchLibraryExtension) subextensions.create(PatchLibraryExtension.EXTENSION_NAME, PatchLibraryExtension)

        project.plugins.apply(LibraryPlugin)
        libraryExtension = project.extensions.getByType(LibraryExtension)
        libraryVariants = libraryExtension.libraryVariants

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

    @Override
    protected String getScopeForCompileLibs() { 'provided' }

}
