/*
 * DexPatcher - Copyright 2015, 2016 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.extensions.AbstractToolExtension
import lanchon.dexpatcher.gradle.extensions.ApktoolExtension
import lanchon.dexpatcher.gradle.extensions.Dex2jarExtension
import lanchon.dexpatcher.gradle.extensions.DexpatcherConfigExtension
import lanchon.dexpatcher.gradle.extensions.DexpatcherExtension
import lanchon.dexpatcher.gradle.tasks.AbstractApktoolTask
import lanchon.dexpatcher.gradle.tasks.AbstractDex2jarTask
import lanchon.dexpatcher.gradle.tasks.AbstractJavaExecTask
import lanchon.dexpatcher.gradle.tasks.BuildApkTask
import lanchon.dexpatcher.gradle.tasks.DecodeApkTask
import lanchon.dexpatcher.gradle.tasks.Dex2jarTask
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

@CompileStatic
class DexpatcherBasePlugin implements Plugin<Project> {

    static final String TASK_GROUP = 'DexPatcher'

    private static final String LOCAL_PROPERTIES = 'local.properties'

    protected Project project
    protected DexpatcherConfigExtension dexpatcherConfig
    protected DexpatcherExtension dexpatcher
    protected ApktoolExtension apktool
    protected Dex2jarExtension dex2jar

    void apply(Project project) {

        this.project = project
        setExtensions()

        project.tasks.withType(DexpatcherTask) { DexpatcherTask task ->
            setupToolTask task, dexpatcher
            task.apiLevel = { dexpatcher.apiLevel }
            task.multiDex = { dexpatcher.multiDex }
            task.multiDexThreaded = { dexpatcher.multiDexThreaded }
            task.multiDexJobs = { dexpatcher.multiDexJobs }
            task.maxDexPoolSize = { dexpatcher.maxDexPoolSize }
            task.annotationPackage = { dexpatcher.annotationPackage }
            task.compatDexTag = { dexpatcher.compatDexTag }
            task.verbosity = { dexpatcher.verbosity }
            task.logSourcePath = { dexpatcher.logSourcePath }
            task.logSourcePathRoot = { dexpatcher.getLogSourcePathRoot() }
            task.logStats = { dexpatcher.logStats }
        }

        project.tasks.withType(AbstractApktoolTask) { AbstractApktoolTask task ->
            setupToolTask task, apktool
            task.verbosity = { apktool.verbosity }
            task.frameworkDir = { apktool.getFrameworkDir() }
            task.frameworkDirAsInput = { apktool.getFrameworkDirAsInput() }
            task.frameworkDirAsOutput = { apktool.getFrameworkDirAsOutput() }
            task.frameworkTag = { apktool.getFrameworkTag() }
        }
        project.tasks.withType(DecodeApkTask) { DecodeApkTask task ->
            task.apiLevel = { apktool.apiLevel }
            task.decodeResources = { apktool.decodeResources }
            task.decodeClasses = { apktool.decodeClasses }
            task.keepBrokenResources = { apktool.keepBrokenResources }
            task.stripDebugInfo = { apktool.stripDebugInfo }
            task.matchOriginal = { apktool.matchOriginal }
            task.apiLevel = { apktool.apiLevel }
        }
        project.tasks.withType(BuildApkTask) { BuildApkTask task ->
            task.aaptFile = { apktool.getAaptFile() }
            task.copyOriginal = { apktool.copyOriginal }
            task.forceDebuggableBuild = { apktool.forceDebuggableBuild }
            task.forceCleanBuild = { apktool.forceCleanBuild }
        }

        project.tasks.withType(AbstractDex2jarTask) { AbstractDex2jarTask task ->
            setupToolTask task, dex2jar
        }
        project.tasks.withType(Dex2jarTask) { Dex2jarTask task ->
            task.translateCode = { dex2jar.translateCode }
            task.translateDebugInfo = { dex2jar.translateDebugInfo }
            task.optimizeSynchronized = { dex2jar.optimizeSynchronized }
            task.reuseRegisters = { dex2jar.reuseRegisters }
            task.topologicalSort = { dex2jar.topologicalSort }
            task.handleExceptions = { dex2jar.handleExceptions }
        }

    }

    private void setupToolTask(AbstractJavaExecTask task, AbstractToolExtension extension) {
        task.classpath { extension.getClasspath() }
        task.extraArgs = { extension.getExtraArgs() }
        //task.addBlankLines = { extension.addBlankLines }
        //task.deleteOutputs = { extension.deleteOutputs }
    }

    private void setExtensions() {
        Properties localProperties = getLocalPropertiesRecursive(project)
        def getProperty = { String key -> project.properties.get(key) ?: localProperties.getProperty(key) }
        def getResolvedProperty = { String key -> Resolver.resolveNullableFile(project.rootProject, getProperty(key)) }
        dexpatcherConfig = project.extensions.create(DexpatcherConfigExtension.EXTENSION_NAME, DexpatcherConfigExtension, project, getResolvedProperty)
        def subextensions = (dexpatcherConfig as ExtensionAware).extensions
        dexpatcher = subextensions.create(DexpatcherExtension.EXTENSION_NAME, DexpatcherExtension, project, dexpatcherConfig, getResolvedProperty)
        apktool = subextensions.create(ApktoolExtension.EXTENSION_NAME, ApktoolExtension, project, dexpatcherConfig, getResolvedProperty)
        dex2jar = subextensions.create(Dex2jarExtension.EXTENSION_NAME, Dex2jarExtension, project, dexpatcherConfig, getResolvedProperty)
    }

    private static Properties getLocalPropertiesRecursive(Project project) {
        Properties parentProperties = project.parent ? getLocalPropertiesRecursive(project.parent) : null
        Properties properties = new Properties(parentProperties)
        File file = project.file(LOCAL_PROPERTIES)
        if (file.exists()) {
            file.withInputStream {
                properties.load(it)
            }
        }
        return properties
    }

}
