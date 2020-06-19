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

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.android.build.gradle.internal.variant.ApkVariantData
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.internal.variant.LibraryVariantData

@CompileStatic
abstract class VariantHelper {

    static void init() {}

    private static final Method getVariantData_METHOD

    static {
        try {
            getVariantData_METHOD = BaseVariantImpl.class.getDeclaredMethod('getVariantData')
            getVariantData_METHOD.accessible = true
        } catch (Exception e) {
            throw new Error("Cannot link method 'BaseVariantImpl.getVariantData'", e)
        }
    }

    static BaseVariantData getData(BaseVariant variant) {
        try {
            return getVariantData_METHOD.invoke(variant) as BaseVariantData
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

}
