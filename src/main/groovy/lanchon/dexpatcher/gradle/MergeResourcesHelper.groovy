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

import java.lang.reflect.Field
import groovy.transform.CompileStatic

import com.android.build.gradle.tasks.MergeResources

@CompileStatic
abstract class MergeResourcesHelper {

    private static final Field validateEnabledField
    private static final Exception validateEnabledException

    static {
        Field field
        try {
            field = MergeResources.class.getDeclaredField('validateEnabled')
            field.accessible = true
            validateEnabledException = null
        } catch (Exception e) {
            field = null
            validateEnabledException = e
        }
        validateEnabledField = field
    }

    static boolean getValidateEnabled(MergeResources mergeResources) {
        try {
            if (validateEnabledException) throw validateEnabledException
            return validateEnabledField.get(mergeResources) as boolean
        } catch (Exception e) {
            throw new RuntimeException("Cannot access field 'MergeResources.validateEnabled'", e)
        }
    }

    static void setValidateEnabled(MergeResources mergeResources, boolean value) {
        try {
            if (validateEnabledException) throw validateEnabledException
            validateEnabledField.set(mergeResources, value)
        } catch (Exception e) {
            throw new RuntimeException("Cannot access field 'MergeResources.validateEnabled'", e)
        }
    }

}
