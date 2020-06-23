/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.helpers

import java.lang.reflect.Field
import groovy.transform.CompileStatic

import com.android.build.gradle.internal.DependencyResourcesComputer

@CompileStatic
abstract class DependencyResourcesComputerHelper {

    static void init() {}

    private static final Field validateEnabled_FIELD

    static {
        try {
            validateEnabled_FIELD = DependencyResourcesComputer.class.getDeclaredField('validateEnabled')
            validateEnabled_FIELD.accessible = true
        } catch (Exception e) {
            throw new Error("Cannot link field 'DependencyResourcesComputer.validateEnabled'", e)
        }
    }

    static boolean getValidateEnabled(DependencyResourcesComputer resourcesComputer) {
        try {
            return validateEnabled_FIELD.get(resourcesComputer) as boolean
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field 'DependencyResourcesComputer.validateEnabled'", e)
        }
    }

    static void setValidateEnabled(DependencyResourcesComputer resourcesComputer, boolean value) {
        try {
            validateEnabled_FIELD.set(resourcesComputer, value)
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field 'DependencyResourcesComputer.validateEnabled'", e)
        }
    }

}
