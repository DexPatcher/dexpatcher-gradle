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
    private static final Exception validateEnabled_EXCEPTION

    static {
        try {
            def field = MergeResources.class.getDeclaredField('validateEnabled')
            field.accessible = true
            validateEnabled_FIELD = field
            validateEnabled_EXCEPTION = null
        } catch (Exception e) {
            validateEnabled_FIELD = null
            validateEnabled_EXCEPTION = e
        }
    }

    static boolean getValidateEnabled(MergeResources mergeResources) {
        try {
            if (validateEnabled_EXCEPTION) throw validateEnabled_EXCEPTION
            return validateEnabled_FIELD.get(mergeResources) as boolean
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field 'MergeResources.validateEnabled'", e)
        }
    }

    static void setValidateEnabled(MergeResources mergeResources, boolean value) {
        try {
            if (validateEnabled_EXCEPTION) throw validateEnabled_EXCEPTION
            validateEnabled_FIELD.set(mergeResources, value)
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field 'MergeResources.validateEnabled'", e)
        }
    }

}
