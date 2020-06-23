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

import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import org.gradle.api.file.FileCollection

@CompileStatic
abstract class LinkApplicationAndroidResourcesTaskHelper {

    static void init() {}

    private static final Field dependenciesFileCollection_FIELD

    static {
        try {
            dependenciesFileCollection_FIELD = LinkApplicationAndroidResourcesTask.class.getDeclaredField('dependenciesFileCollection')
            dependenciesFileCollection_FIELD.accessible = true
        } catch (Exception e) {
            throw new Error("Cannot link field 'LinkApplicationAndroidResourcesTask.dependenciesFileCollection'", e)
        }
    }

    static FileCollection getDependenciesFileCollection(LinkApplicationAndroidResourcesTask task) {
        try {
            return dependenciesFileCollection_FIELD.get(task) as FileCollection
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field 'LinkApplicationAndroidResourcesTask.dependenciesFileCollection'", e)
        }
    }

    static void setDependenciesFileCollection(LinkApplicationAndroidResourcesTask task, FileCollection value) {
        try {
            dependenciesFileCollection_FIELD.set(task, value)
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field 'LinkApplicationAndroidResourcesTask.dependenciesFileCollection'", e)
        }
    }

}
