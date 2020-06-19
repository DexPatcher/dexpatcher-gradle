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

import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

@CompileStatic
abstract class NewProperty {

    static Property<Boolean> from(Project project, boolean value) {
        def property = project.objects.property(Boolean)
        property.set value
        return property
    }

    static Property<Integer> from(Project project, int value) {
        def property = project.objects.property(Integer)
        property.set value
        return property
    }

    static DirectoryProperty dir(Project project) {
        project.objects.directoryProperty()
    }

    static RegularFileProperty file(Project project) {
        project.objects.fileProperty()
    }

    static <T> ListProperty<T> list(Project project, Class<T> elementType) {
        def list = project.objects.listProperty(elementType)
        list.set Collections.<T>emptyList()
        return list
    }

}
