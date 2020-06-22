/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.plugins

import groovy.transform.CompileStatic

import com.android.builder.model.Version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

@CompileStatic
class AbstractPlugin implements Plugin<Project> {

    static final String GRADLE_VERSION_MIN = '5.6.4'
    static final String GRADLE_VERSION_MAX = null

    static final String ANDROID_PLUGIN_VERSION_MIN = '3.6'
    static final String ANDROID_PLUGIN_VERSION_MAX = '4.0'

    protected Project project
    protected DexpatcherBasePlugin basePlugin

    void apply(Project project) {

        this.project = project
        basePlugin = (DexpatcherBasePlugin) project.plugins.apply(DexpatcherBasePlugin)

        def requires = 'The configured DexPatcher Gradle plugins require'

        def gradleVer = GradleVersion.current()
        if (GRADLE_VERSION_MIN && gradleVer < GradleVersion.version(GRADLE_VERSION_MIN)) {
            throw new RuntimeException("$requires Gradle version $GRADLE_VERSION_MIN or later")
        }
        if (GRADLE_VERSION_MAX && gradleVer >= GradleVersion.version(GRADLE_VERSION_MAX)) {
            throw new RuntimeException("$requires Gradle version earlier than $GRADLE_VERSION_MAX")
        }

        def androidVer = Version.ANDROID_GRADLE_PLUGIN_VERSION
        if (ANDROID_PLUGIN_VERSION_MIN && !versionAtLeast(androidVer, ANDROID_PLUGIN_VERSION_MIN)) {
            throw new RuntimeException("$requires Android Gradle plugin version $ANDROID_PLUGIN_VERSION_MIN or later")
        }
        if (ANDROID_PLUGIN_VERSION_MAX && versionAtLeast(androidVer, ANDROID_PLUGIN_VERSION_MAX)) {
            throw new RuntimeException("$requires Android Gradle plugin version earlier than $ANDROID_PLUGIN_VERSION_MAX")
        }

    }

    protected void afterApply() {}

    private static boolean versionAtLeast(String version, String reference) {
        GradleVersion.version(version) >= GradleVersion.version(reference)
    }

}
