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
import org.gradle.api.tasks.util.PatternFilterable

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

        // Extensions

        dexpatcherConfig = project.extensions.create(ExtensionNames.DEXPATCHER_CONFIG, DexpatcherConfigExtension,
                project)

        dexpatcher = createSubextension(ExtensionNames.TOOL_DEXPATCHER, DexpatcherExtension)
        apktool = createSubextension(ExtensionNames.TOOL_APKTOOL, ApktoolExtension)
        dex2jar = createSubextension(ExtensionNames.TOOL_DEX2JAR, Dex2jarExtension)

        // Configurations

        List<Configuration> configurations = []

        def dexpatcherCfg = project.configurations.maybeCreate(ConfigurationNames.DEXPATCHER)
        dexpatcherCfg.description = 'DexPatcher tool dependency.'
        dexpatcherCfg.canBeResolved = true
        dexpatcherCfg.canBeConsumed = false
        configurations << dexpatcherCfg
        dexpatcher.classpath.from { dexpatcherCfg.singleFile }

        def dexpatcherAnnotationCfg = project.configurations.maybeCreate(ConfigurationNames.DEXPATCHER_ANNOTATION)
        dexpatcherAnnotationCfg.description = 'DexPatcher tool annotation dependency.'
        dexpatcherAnnotationCfg.canBeResolved = true
        dexpatcherAnnotationCfg.canBeConsumed = false
        configurations << dexpatcherAnnotationCfg
        dexpatcher.annotationClasspath.from {
            if (!dexpatcherAnnotationCfg.resolve().empty) {
                return dexpatcherAnnotationCfg.singleFile
            } else {
                def file = dexpatcherCfg.singleFile
                def files = file.isDirectory() ? project.fileTree(file) : project.zipTree(file)
                return files.matching { PatternFilterable filer ->
                    filer.include FileNames.DEXPATCHER_ANNOTATION
                }
            }
        }

        def apktoolCfg = project.configurations.maybeCreate(ConfigurationNames.APKTOOL)
        apktoolCfg.description = 'Apktool dependency.'
        apktoolCfg.canBeResolved = true
        apktoolCfg.canBeConsumed = false
        configurations << apktoolCfg
        apktool.classpath.from { apktoolCfg.singleFile }

        def dex2jarCfg = project.configurations.maybeCreate(ConfigurationNames.DEX2JAR)
        dex2jarCfg.description = 'Dex2jar dex-tools dependency.'
        dex2jarCfg.canBeResolved = true
        dex2jarCfg.canBeConsumed = false
        configurations << dex2jarCfg
        dex2jar.classpath.from {
            def file = dex2jarCfg.singleFile
            def files = file.isDirectory() ? project.fileTree(file) : project.zipTree(file)
            return files.matching { PatternFilterable filer ->
                filer.include '**/*.jar'
            }
        }

        project.afterEvaluate {
            for (def cfg : configurations) {
                // Note that the contents of these configurations can be overridden by defining
                // 'dexpatcher.configurationOverride.<config-name>' as a project property or as an
                // entry in 'local.properties' (typically not posted to the VCS). Its value must
                // either be empty or be a single local path relative to the root project.
                def value = dexpatcherConfig.properties.getConfigurationOverride(cfg.name)
                if (!value.is(null)) {
                    cfg.dependencies.clear()
                    if (value) {
                        cfg.dependencies.add project.dependencies.create(project.rootProject.files(value))
                    }
                }
            }
        }

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

    protected <T> T createSubextension(String name, Class<T> type) {
        dexpatcherConfig.extensions.create(name, type, project, dexpatcherConfig)
    }

}
