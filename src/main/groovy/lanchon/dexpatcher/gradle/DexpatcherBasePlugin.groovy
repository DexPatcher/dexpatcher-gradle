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
            task.apiLevel.set dexpatcher.apiLevel
            task.multiDex.set dexpatcher.multiDex
            task.multiDexThreaded.set dexpatcher.multiDexThreaded
            task.multiDexJobs.set dexpatcher.multiDexJobs
            task.maxDexPoolSize.set dexpatcher.maxDexPoolSize
            task.annotationPackage.set dexpatcher.annotationPackage
            task.constructorAutoIgnore.set dexpatcher.constructorAutoIgnore
            task.compatDexTag.set dexpatcher.compatDexTag
            task.verbosity.set dexpatcher.verbosity
            task.logSourcePath.set dexpatcher.logSourcePath
            task.logSourcePathRoot.set dexpatcher.logSourcePathRoot
            task.logStats.set dexpatcher.logStats
        }

        project.tasks.withType(AbstractApktoolTask) { AbstractApktoolTask task ->
            setupToolTask task, apktool
            task.verbosity.set apktool.verbosity
            task.frameworkDir.set apktool.frameworkDir
            task.frameworkDirAsInput.set apktool.frameworkDirAsInput
            task.frameworkDirAsOutput.set apktool.frameworkDirAsOutput
        }
        project.tasks.withType(DecodeApkTask) { DecodeApkTask task ->
            task.frameworkTag.set apktool.frameworkTag
            task.apiLevel.set apktool.apiLevel
            task.decodeAssets.set apktool.decodeAssets
            task.decodeResources.set apktool.decodeResources
            task.decodeClasses.set apktool.decodeClasses
            task.keepBrokenResources.set apktool.keepBrokenResources
            task.stripDebugInfo.set apktool.stripDebugInfo
            task.matchOriginal.set apktool.matchOriginal
            task.apiLevel.set apktool.apiLevel
        }
        project.tasks.withType(BuildApkTask) { BuildApkTask task ->
            task.aaptFile.set apktool.aaptFile
            task.copyOriginal.set apktool.copyOriginal
            task.forceDebuggableBuild.set apktool.forceDebuggableBuild
            task.forceCleanBuild.set apktool.forceCleanBuild
        }

        project.tasks.withType(AbstractDex2jarTask) { AbstractDex2jarTask task ->
            setupToolTask task, dex2jar
        }
        project.tasks.withType(Dex2jarTask) { Dex2jarTask task ->
            task.translateCode.set dex2jar.translateCode
            task.translateDebugInfo.set dex2jar.translateDebugInfo
            task.optimizeSynchronized.set dex2jar.optimizeSynchronized
            task.reuseRegisters.set dex2jar.reuseRegisters
            task.topologicalSort.set dex2jar.topologicalSort
            task.handleExceptions.set dex2jar.handleExceptions
        }

    }

    private void setupToolTask(AbstractJavaExecTask task, AbstractToolExtension extension) {
        task.classpath Utils.getJars(project, extension.resolvedDir)
        task.extraArgs.set extension.extraArgs
        task.addBlankLines.set extension.addBlankLines
    }

    private void setExtensions() {
        dexpatcherConfig = project.extensions.create(DexpatcherConfigExtension.EXTENSION_NAME, DexpatcherConfigExtension, project)
        def subextensions = (dexpatcherConfig as ExtensionAware).extensions
        dexpatcher = subextensions.create(DexpatcherExtension.EXTENSION_NAME, DexpatcherExtension, project, dexpatcherConfig)
        apktool = subextensions.create(ApktoolExtension.EXTENSION_NAME, ApktoolExtension, project, dexpatcherConfig)
        dex2jar = subextensions.create(Dex2jarExtension.EXTENSION_NAME, Dex2jarExtension, project, dexpatcherConfig)
    }

}
