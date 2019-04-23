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

import java.lang.reflect.Method
import groovy.transform.CompileStatic

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.android.build.gradle.internal.variant.ApkVariantData
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.internal.variant.LibraryVariantData
import com.android.build.gradle.tasks.MergeResources
import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.build.gradle.tasks.ProcessAndroidResources
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

@CompileStatic
abstract class VariantHelper {

    // Access internal variant data

    private static final Method getVariantDataMethod

    static {
        // FIXME: Discard exceptions or delay throwing them?
        getVariantDataMethod = BaseVariantImpl.class.getDeclaredMethod('getVariantData')
        getVariantDataMethod.setAccessible true
    }

    static BaseVariantData getData(BaseVariant variant) {
        (BaseVariantData) getVariantDataMethod.invoke(variant)
    }

    static LibraryVariantData getData(LibraryVariant variant) {
        (LibraryVariantData) getVariantDataMethod.invoke(variant)
    }

    static ApkVariantData getData(ApplicationVariant variant) {
        (ApkVariantData) getVariantDataMethod.invoke(variant)
    }

    // Adapters for Android Gradle plugins earlier than 3.3.0

    private static <T extends Task> TaskProvider<T> getProvider(T task) {
        (TaskProvider<T>) task.project.tasks.named(task.name)
    }

    static TaskProvider<MergeResources> getMergeResources(BaseVariant variant) {
        try {
            return variant.mergeResourcesProvider
        } catch (NoSuchMethodError e) {
            return getProvider(variant.mergeResources)
        }
    }

    static TaskProvider<ProcessAndroidResources> getProcessResources(BaseVariantOutput output) {
        try {
            return output.processResourcesProvider
        } catch (NoSuchMethodError e) {
            return getProvider(output.processResources)
        }
    }

    static TaskProvider<PackageAndroidArtifact> getPackageApplication(ApkVariant variant) {
        try {
            return variant.packageApplicationProvider
        } catch (NoSuchMethodError e) {
            return getProvider(variant.packageApplication)
        }
    }

    static TaskProvider<Task> getAssemble(BaseVariant variant) {
        try {
            return variant.assembleProvider
        } catch (NoSuchMethodError e) {
            return getProvider(variant.assemble)
        }
    }

}
