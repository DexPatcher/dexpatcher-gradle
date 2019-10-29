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
import org.gradle.api.artifacts.Configuration

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

        // Configurations

        def dexpatcherCfg = project.configurations.maybeCreate(ConfigurationNames.DEXPATCHER)
        dexpatcherCfg.description = 'The DexPatcher tool to use for this project.'
        dexpatcherCfg.canBeResolved = true
        dexpatcherCfg.canBeConsumed = false
        setupConfigurationOverride dexpatcherCfg

        def dexpatcherAnnotationCfg = project.configurations.maybeCreate(ConfigurationNames.DEXPATCHER_ANNOTATION)
        dexpatcherAnnotationCfg.description = 'The DexPatcher annotations to use for this project.'
        dexpatcherAnnotationCfg.canBeResolved = true
        dexpatcherAnnotationCfg.canBeConsumed = false
        setupConfigurationOverride dexpatcherAnnotationCfg

        def apktoolCfg = project.configurations.maybeCreate(ConfigurationNames.APKTOOL)
        apktoolCfg.description = 'The Apktool to use for this project.'
        apktoolCfg.canBeResolved = true
        apktoolCfg.canBeConsumed = false
        setupConfigurationOverride apktoolCfg

        def dex2jarCfg = project.configurations.maybeCreate(ConfigurationNames.DEX2JAR)
        dex2jarCfg.description = 'The dex2jar dex-tools to use for this project.'
        dex2jarCfg.canBeResolved = true
        dex2jarCfg.canBeConsumed = false
        setupConfigurationOverride dex2jarCfg

        def aaptCfg = project.configurations.maybeCreate(ConfigurationNames.AAPT)
        aaptCfg.description = 'The AAPT to use for this project.'
        aaptCfg.canBeResolved = true
        aaptCfg.canBeConsumed = false
        setupConfigurationOverride aaptCfg

        def aapt2Cfg = project.configurations.maybeCreate(ConfigurationNames.AAPT2)
        aapt2Cfg.description = 'The AAPT2 to use for this project.'
        aapt2Cfg.canBeResolved = true
        aapt2Cfg.canBeConsumed = false
        setupConfigurationOverride aapt2Cfg

        // Extensions

        dexpatcherConfig = project.extensions.create(ExtensionNames.DEXPATCHER_CONFIG, DexpatcherConfigExtension,
                project)

        dexpatcher = createSubextension(ExtensionNames.TOOL_DEXPATCHER, DexpatcherExtension, dexpatcherCfg,
                dexpatcherAnnotationCfg)
        apktool = createSubextension(ExtensionNames.TOOL_APKTOOL, ApktoolExtension, apktoolCfg, aaptCfg, aapt2Cfg)
        dex2jar = createSubextension(ExtensionNames.TOOL_DEX2JAR, Dex2jarExtension, dex2jarCfg)

        // Task Defaults

        project.tasks.withType(DexpatcherTask).configureEach {
            setupToolTask it, dexpatcher
            it.apiLevel.set dexpatcher.apiLevel
            it.multiDex.set dexpatcher.multiDex
            it.multiDexThreaded.set dexpatcher.multiDexThreaded
            it.multiDexJobs.set dexpatcher.multiDexJobs
            it.maxDexPoolSize.set dexpatcher.maxDexPoolSize
            it.annotationPackage.set dexpatcher.annotationPackage
            it.constructorAutoIgnore.set dexpatcher.constructorAutoIgnore
            it.verbosity.set dexpatcher.verbosity
            it.logSourcePath.set dexpatcher.logSourcePath
            it.logSourcePathRoot.set dexpatcher.logSourcePathRoot
            it.logStats.set dexpatcher.logStats
        }

        project.tasks.withType(AbstractApktoolTask).configureEach {
            setupToolTask it, apktool
            it.verbosity.set apktool.verbosity
            it.frameworkInDir.set apktool.frameworkInDir
        }
        project.tasks.withType(DecodeApkTask).configureEach {
            it.frameworkTag.set apktool.frameworkTag
            it.apiLevel.set apktool.apiLevel
            it.decodeAssets.set apktool.decodeAssets
            it.decodeResources.set apktool.decodeResources
            it.decodeClasses.set apktool.decodeClasses
            it.forceDecodeManifest.set apktool.forceDecodeManifest
            it.keepBrokenResources.set apktool.keepBrokenResources
            it.stripDebugInfo.set apktool.stripDebugInfo
            it.matchOriginal.set apktool.matchOriginal
            it.apiLevel.set apktool.apiLevel
        }
        project.tasks.withType(BuildApkTask).configureEach {
            it.aaptFile.set apktool.aaptFile
            it.useAapt2.set apktool.useAapt2
            it.crunchResources.set apktool.crunchResources
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
        task.classpath { extension.classpath }
        task.extraArgs.set extension.extraArgs
    }

    void setupConfigurationOverride(Configuration cfg) {
        // Note that the contents of supported configurations can be overridden by defining
        // 'dexpatcher.configurationOverride.<config-name>' as a project property or as an
        // entry in 'local.properties' (typically not posted to the VCS). Its value must
        // either be empty or be a single local path relative to the root project.
        project.afterEvaluate {
            def value = dexpatcherConfig.properties.getConfigurationOverride(cfg.name)
            if (!value.is(null)) {
                cfg.dependencies.clear()
                if (value) {
                    cfg.dependencies.add project.dependencies.create(project.rootProject.files(value))
                }
            }
        }
    }

    public <T> T createSubextension(String name, Class<T> type, Object... extraArgs) {
        dexpatcherConfig.subextensions.create(name, type, ([project, dexpatcherConfig] as Object[]) + extraArgs)
    }

}
