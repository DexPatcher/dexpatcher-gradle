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

import com.android.build.gradle.internal.DependencyResourcesComputer
import com.android.build.gradle.tasks.ResourceAwareTask

@CompileStatic
abstract class ResourceAwareTaskHelper {

    static void init() {}

    private static final Field resourcesComputer_FIELD

    static {
        try {
            resourcesComputer_FIELD = ResourceAwareTask.class.getDeclaredField('resourcesComputer')
            resourcesComputer_FIELD.accessible = true
        } catch (Exception e) {
            throw new Error("Cannot link field 'ResourceAwareTask.resourcesComputer'", e)
        }
    }

    static DependencyResourcesComputer getResourcesComputer(ResourceAwareTask resourceAwareTask) {
        try {
            return resourcesComputer_FIELD.get(resourceAwareTask) as DependencyResourcesComputer
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field 'ResourceAwareTask.resourcesComputer'", e)
        }
    }

}
