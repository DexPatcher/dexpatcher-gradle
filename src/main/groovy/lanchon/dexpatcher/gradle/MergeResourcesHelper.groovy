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

import java.lang.reflect.Field
import groovy.transform.CompileStatic

import com.android.build.gradle.tasks.MergeResources

@CompileStatic
abstract class MergeResourcesHelper {

    private static final Field validateEnabled_FIELD

    static {
        try {
            validateEnabled_FIELD = MergeResources.class.getDeclaredField('validateEnabled')
            validateEnabled_FIELD.accessible = true
        } catch (Exception e) {
            throw new Error("Cannot link field 'MergeResources.validateEnabled'", e)
        }
    }

    static boolean getValidateEnabled(MergeResources mergeResources) {
        try {
            return validateEnabled_FIELD.get(mergeResources) as boolean
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field 'MergeResources.validateEnabled'", e)
        }
    }

    static void setValidateEnabled(MergeResources mergeResources, boolean value) {
        try {
            validateEnabled_FIELD.set(mergeResources, value)
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field 'MergeResources.validateEnabled'", e)
        }
    }

}
