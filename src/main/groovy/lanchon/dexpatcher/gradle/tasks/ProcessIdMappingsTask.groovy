/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.tasks

import javax.inject.Inject
import groovy.transform.CompileStatic

import com.android.build.gradle.internal.aapt.WorkerExecutorResourceCompilationService
import com.android.build.gradle.internal.res.namespaced.Aapt2DaemonManagerService
import com.android.build.gradle.internal.tasks.Workers
import com.android.builder.core.AndroidBuilder
import com.android.ide.common.resources.CompileResourceRequest
import com.android.ide.common.resources.CopyToOutputDirectoryResourceCompilationService
import com.android.ide.common.resources.ResourceCompilationService
import com.android.ide.common.workers.WorkerExecutorFacade
import com.android.resources.ResourceFolderType
import com.android.utils.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor

// TODO: Maybe make this work on Android Gradle plugin 3.1.0 on which there is no ResourceCompilationService.

@CompileStatic
@CacheableTask
class ProcessIdMappingsTask extends DefaultTask {

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFile final RegularFileProperty publicXmlFile = project.layout.fileProperty()
    @PathSensitive(PathSensitivity.NONE)
    @OutputDirectory final DirectoryProperty outputDir = project.layout.directoryProperty()

    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional @InputFiles final ConfigurableFileCollection aapt2FromMaven = project.files()

    @Internal final Property<AndroidBuilder> androidBuilder = project.objects.property(AndroidBuilder)
    @Input final Property<Boolean> processResources = project.objects.property(Boolean)

    private final WorkerExecutorFacade workerExecutorFacade

    @Inject
    ProcessIdMappingsTask(WorkerExecutor workerExecutor) {
        workerExecutorFacade = Workers.INSTANCE.getWorker(workerExecutor)
    }

    @Input String getBuildToolsVersion() {
        androidBuilder.get().buildToolInfo.revision.toString()
    }

    @TaskAction
    void exec() {
        def outDir = outputDir.get().asFile
        FileUtils.cleanOutputDir(outDir)
        ResourceCompilationService resourceCompiler = getResourceCompiler()
        try {
            resourceCompiler.submitCompile(new CompileResourceRequest(publicXmlFile.get().asFile,
                    outDir, ResourceFolderType.VALUES.getName()))
        } finally {
            resourceCompiler.close()
        }
    }

    private ResourceCompilationService getResourceCompiler() {
        if (processResources.get()) {
            // When using AAPT2, compile the file.
            def builder = androidBuilder.get()
            return new WorkerExecutorResourceCompilationService(workerExecutorFacade,
                    Aapt2DaemonManagerService.registerAaptService(aapt2FromMaven, builder.buildToolInfo,
                            builder.logger))
        } else {
            // When using AAPT1, just copy it instead.
            return CopyToOutputDirectoryResourceCompilationService.INSTANCE
        }
    }

}
