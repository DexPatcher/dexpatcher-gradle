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

import java.util.zip.ZipException
import java.util.zip.ZipFile
import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.FileHelper
import lanchon.dexpatcher.gradle.NewProperty
import lanchon.dexpatcher.gradle.Platform
import lanchon.dexpatcher.gradle.tasks.AbstractApktoolTask.Verbosity

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.util.PatternFilterable

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class ApktoolExtension extends AbstractToolExtension {

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE

    // Base
    final Property<Verbosity> verbosity = project.objects.property(Verbosity)
    final DirectoryProperty frameworkInDir = NewProperty.dir(project)

    // Decode
    final Property<String> frameworkTag = project.objects.property(String)
    final Property<Integer> apiLevel = NewProperty.from(project, 0)
    final Property<Boolean> decodeAssets = NewProperty.from(project, true)
    final Property<Boolean> decodeResources = NewProperty.from(project, true)
    final Property<Boolean> decodeClasses = NewProperty.from(project, true)
    final Property<Boolean> forceDecodeManifest = NewProperty.from(project, false)
    final Property<Boolean> keepBrokenResources = NewProperty.from(project, false)
    final Property<Boolean> stripDebugInfo = NewProperty.from(project, false)
    final Property<Boolean> matchOriginal = NewProperty.from(project, false)

    // Build
    final RegularFileProperty aaptFile = NewProperty.file(project)
    final Property<Boolean> useAapt2 = NewProperty.from(project, false)
    final Property<Boolean> crunchResources = NewProperty.from(project, true)
    final Property<Boolean> copyOriginal = NewProperty.from(project, false)
    final Property<Boolean> forceDebuggableBuild = NewProperty.from(project, false)
    final Property<Boolean> forceCleanBuild = NewProperty.from(project, false)

    final Provider<RegularFile> bundledAaptFile
    final Provider<RegularFile> configuredAaptFile
    final Provider<RegularFile> resolvedAaptFile

    final Provider<RegularFile> bundledAapt2File
    final Provider<RegularFile> configuredAapt2File
    final Provider<RegularFile> resolvedAapt2File

    ApktoolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig, Configuration apktoolCfg,
            Configuration aaptCfg, Configuration aapt2Cfg) {
        super(project, dexpatcherConfig)
        classpath.from { apktoolCfg.singleFile }
        bundledAaptFile = getBundledTool(apktoolCfg, FileNames.AAPT)
        configuredAaptFile = getConfiguredTool(aaptCfg, FileNames.AAPT)
        resolvedAaptFile = getResolvedTool(bundledAaptFile, configuredAaptFile)
        bundledAapt2File = getBundledTool(apktoolCfg, FileNames.AAPT2)
        configuredAapt2File = getConfiguredTool(aapt2Cfg, FileNames.AAPT2)
        resolvedAapt2File = getResolvedTool(bundledAapt2File, configuredAapt2File)
    }

    private Provider<RegularFile> getBundledTool(Configuration apktoolCfg, String tool) {
        project.<RegularFile>provider {
            def file = apktoolCfg.singleFile
            def files = file.isDirectory() ? project.fileTree(file) : project.zipTree(file)
            def platformDir = Platform.current.binaryDirectoryName
            def exeExtension = Platform.current.executableExtension
            def filteredFiles = files.matching { PatternFilterable filter ->
                filter.include"prebuilt/${tool}/${platformDir}/${tool}${exeExtension}"  // Apktool < 2.4.0
                filter.include"prebuilt/${platformDir}/${tool}_64${exeExtension}"       // Apktool >= 2.4.0
            }
            if (filteredFiles.empty) {
                throw new RuntimeException("Bundled Apktool ${tool.toUpperCase(Locale.ROOT)} not found")
            }
            return FileHelper.getRegularFile(project, filteredFiles.singleFile)
        }
    }

    private Provider<RegularFile> getConfiguredTool(Configuration toolCfg, String tool) {
        project.<RegularFile>provider {
            if (toolCfg.empty) return null;
            def file = toolCfg.singleFile
            def files
            if (file.isDirectory()) {
                files = project.fileTree(file)
            } else {
                try {
                    new ZipFile(file).close()
                } catch (ZipException e) {
                    // If the file is not an archive, assume it is an executable:
                    return FileHelper.getRegularFile(project, file)
                }
                files = project.zipTree(file)
                // Expand the complete archive in case it contains libraries or other necessary files:
                files.files
            }
            def platformDir = Platform.current.binaryDirectoryName
            def exeExtension = Platform.current.executableExtension
            def filteredFiles = files.matching { PatternFilterable filter ->
                filter.include"${tool}${exeExtension}"
                filter.include"${platformDir}/${tool}${exeExtension}"
            }
            if (filteredFiles.empty) {
                throw new RuntimeException("Configured Apktool ${tool.toUpperCase(Locale.ROOT)} not found")
            }
            return FileHelper.getRegularFile(project, filteredFiles.singleFile)
        }
    }

    private Provider<RegularFile> getResolvedTool(Provider<RegularFile> bundledToolFile,
            Provider<RegularFile> configuredToolFile) {
        project.<RegularFile>provider {
            configuredToolFile.orNull ?: bundledToolFile.get()
        }
    }

}
