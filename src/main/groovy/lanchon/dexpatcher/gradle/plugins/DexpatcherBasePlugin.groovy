/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.plugins

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.Utils
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

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class DexpatcherBasePlugin implements Plugin<Project> {

    protected Project project
    protected DexpatcherConfigExtension dexpatcherConfig
    protected DexpatcherExtension dexpatcher
    protected ApktoolExtension apktool
    protected Dex2jarExtension dex2jar

    void apply(Project project) {

        this.project = project
        setExtensions()

        project.tasks.withType(DexpatcherTask).configureEach {
            setupToolTask it, dexpatcher
            it.apiLevel.set dexpatcher.apiLevel
            it.multiDex.set dexpatcher.multiDex
            it.multiDexThreaded.set dexpatcher.multiDexThreaded
            it.multiDexJobs.set dexpatcher.multiDexJobs
            it.maxDexPoolSize.set dexpatcher.maxDexPoolSize
            it.annotationPackage.set dexpatcher.annotationPackage
            it.constructorAutoIgnore.set dexpatcher.constructorAutoIgnore
            it.compatDexTag.set dexpatcher.compatDexTag
            it.verbosity.set dexpatcher.verbosity
            it.logSourcePath.set dexpatcher.logSourcePath
            it.logSourcePathRoot.set dexpatcher.logSourcePathRoot
            it.logStats.set dexpatcher.logStats
        }

        project.tasks.withType(AbstractApktoolTask).configureEach {
            setupToolTask it, apktool
            it.verbosity.set apktool.verbosity
            it.frameworkDir.set apktool.frameworkDir
            it.frameworkDirAsInput.set apktool.frameworkDirAsInput
            it.frameworkDirAsOutput.set apktool.frameworkDirAsOutput
        }
        project.tasks.withType(DecodeApkTask).configureEach {
            it.frameworkTag.set apktool.frameworkTag
            it.apiLevel.set apktool.apiLevel
            it.decodeAssets.set apktool.decodeAssets
            it.decodeResources.set apktool.decodeResources
            it.decodeClasses.set apktool.decodeClasses
            it.keepBrokenResources.set apktool.keepBrokenResources
            it.stripDebugInfo.set apktool.stripDebugInfo
            it.matchOriginal.set apktool.matchOriginal
            it.apiLevel.set apktool.apiLevel
        }
        project.tasks.withType(BuildApkTask).configureEach {
            it.aaptFile.set apktool.aaptFile
            it.copyOriginal.set apktool.copyOriginal
            it.forceDebuggableBuild.set apktool.forceDebuggableBuild
            it.forceCleanBuild.set apktool.forceCleanBuild
        }

        project.tasks.withType(AbstractDex2jarTask).configureEach {
            setupToolTask it, dex2jar
        }
        project.tasks.withType(Dex2jarTask).configureEach {
            it.translateCode.set dex2jar.translateCode
            it.translateDebugInfo.set dex2jar.translateDebugInfo
            it.optimizeSynchronized.set dex2jar.optimizeSynchronized
            it.reuseRegisters.set dex2jar.reuseRegisters
            it.topologicalSort.set dex2jar.topologicalSort
            it.handleExceptions.set dex2jar.handleExceptions
        }

    }

    private void setupToolTask(AbstractJavaExecTask task, AbstractToolExtension extension) {
        task.classpath Utils.getJars(project, extension.resolvedDir)
        task.extraArgs.set extension.extraArgs
    }

    private void setExtensions() {
        dexpatcherConfig = project.extensions.create(EXT_DEXPATCHER_CONFIG, DexpatcherConfigExtension, project)
        def subextensions = (dexpatcherConfig as ExtensionAware).extensions
        dexpatcher = subextensions.create(EXT_TOOL_DEXPATCHER, DexpatcherExtension, project, dexpatcherConfig)
        apktool = subextensions.create(EXT_TOOL_APKTOOL, ApktoolExtension, project, dexpatcherConfig)
        dex2jar = subextensions.create(EXT_TOOL_DEX2JAR, Dex2jarExtension, project, dexpatcherConfig)
    }

}