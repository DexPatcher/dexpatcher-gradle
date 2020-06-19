/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
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
    private static final Exception getVariantDataException

    static {
        Method method
        try {
            method = BaseVariantImpl.class.getDeclaredMethod('getVariantData')
            method.accessible = true
            getVariantDataException = null
        } catch (Exception e) {
            method = null
            getVariantDataException = e
        }
        getVariantDataMethod = method
    }

    static BaseVariantData getData(BaseVariant variant) {
        try {
            if (getVariantDataException) throw getVariantDataException
            return getVariantDataMethod.invoke(variant) as BaseVariantData
        } catch (Exception e) {
            throw new RuntimeException("Cannot invoke method 'BaseVariantImpl.getVariantData'", e)
        }
    }

    static LibraryVariantData getData(LibraryVariant variant) {
        (LibraryVariantData) getData(variant as BaseVariant)
    }

    static ApkVariantData getData(ApplicationVariant variant) {
        (ApkVariantData) getData(variant as BaseVariant)
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
