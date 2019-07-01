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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.util.GradleVersion

@CompileStatic
abstract class NewProperty {

    private static final boolean GRADLE_5_0 = (GradleVersion.current() >= GradleVersion.version('5.0'))

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

    @CompileDynamic
    private static DirectoryProperty gradle_5_0_directoryProperty(ObjectFactory objects) {
        objects.directoryProperty()
    }

    @CompileDynamic
    private static RegularFileProperty gradle_5_0_fileProperty(ObjectFactory objects) {
        objects.fileProperty()
    }

    static DirectoryProperty dir(Project project) {
        GRADLE_5_0 ? gradle_5_0_directoryProperty(project.objects) : project.layout.directoryProperty()
    }

    static RegularFileProperty file(Project project) {
        GRADLE_5_0 ? gradle_5_0_fileProperty(project.objects) : project.layout.fileProperty()
    }

    static <T> ListProperty<T> list(Project project, Class<T> elementType) {
        def list = project.objects.listProperty(elementType)
        list.set Collections.<T>emptyList()
        return list
    }

}
