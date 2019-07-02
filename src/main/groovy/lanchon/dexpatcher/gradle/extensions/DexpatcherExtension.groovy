/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.Constants
import lanchon.dexpatcher.gradle.FileHelper
import lanchon.dexpatcher.gradle.NewProperty
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask.Verbosity

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.util.PatternFilterable

@CompileStatic
class DexpatcherExtension extends AbstractToolExtension {

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE
    static final def DEBUG = Verbosity.DEBUG

    final Provider<RegularFile> bundledAnnotationFile

    final Property<Integer> apiLevel = NewProperty.from(project, 0)
    final Property<Boolean> multiDex = NewProperty.from(project, false)
    final Property<Boolean> multiDexThreaded = NewProperty.from(project, false)
    final Property<Integer> multiDexJobs = NewProperty.from(project, 0)
    final Property<Integer> maxDexPoolSize = NewProperty.from(project, 0)
    final Property<String> annotationPackage = project.objects.property(String)
    final Property<Boolean> constructorAutoIgnore = NewProperty.from(project, true)
    final Property<Boolean> compatDexTag = NewProperty.from(project, false)
    final Property<Verbosity> verbosity = project.objects.property(Verbosity)
    final Property<Boolean> logSourcePath = NewProperty.from(project, false)
    final Property<String> logSourcePathRoot = project.objects.property(String)
    final Property<Boolean> logStats = NewProperty.from(project, false)

    DexpatcherExtension(Project project, DexpatcherConfigExtension dexpatcherConfig, Configuration dexpatcherCfg) {
        super(project, dexpatcherConfig)
        classpath.from { dexpatcherCfg.singleFile }
        bundledAnnotationFile = project.<RegularFile>provider {
            def file = dexpatcherCfg.singleFile
            def files = file.isDirectory() ? project.fileTree(file) : project.zipTree(file)
            def annotationFile = files.matching { PatternFilterable filer ->
                filer.include Constants.FileNames.DEXPATCHER_ANNOTATION
            }.singleFile
            return FileHelper.getRegularFile(project, annotationFile)
        }
    }

}
