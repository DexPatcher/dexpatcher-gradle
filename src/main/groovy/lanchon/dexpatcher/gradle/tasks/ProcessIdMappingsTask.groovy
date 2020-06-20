/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
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

import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.aapt.WorkerExecutorResourceCompilationService
import com.android.build.gradle.internal.res.namespaced.Aapt2DaemonManagerService
import com.android.build.gradle.internal.tasks.Workers
import com.android.build.gradle.options.SyncOptions.ErrorFormatMode
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

@CompileStatic
@CacheableTask
class ProcessIdMappingsTask extends DefaultTask {

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFile final RegularFileProperty publicXmlFile = project.objects.fileProperty()
    @OutputDirectory final DirectoryProperty outputDir = project.objects.directoryProperty()

    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional @InputFiles final ConfigurableFileCollection aapt2FromMaven = project.files()
    @Optional @Input final Property<String> aapt2Version = project.objects.property(String)
    @Internal final Property<ErrorFormatMode> errorFormatMode = project.objects.property(ErrorFormatMode)
    @Input final Property<Boolean> processResources = project.objects.property(Boolean).value(false)

    private final WorkerExecutorFacade workerExecutorFacade

    @Inject
    ProcessIdMappingsTask(WorkerExecutor workerExecutor) {
        workerExecutorFacade = Workers.INSTANCE.getWorkerForAapt2(project.name, path, workerExecutor);
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
            // Compile the file.
            def aapt2ServiceKey = Aapt2DaemonManagerService.registerAaptService(
                    aapt2FromMaven, new LoggerWrapper(logger))
            return new WorkerExecutorResourceCompilationService(
                    workerExecutorFacade, aapt2ServiceKey, errorFormatMode.get())
        } else {
            // Or just copy it instead.
            return CopyToOutputDirectoryResourceCompilationService.INSTANCE
        }
    }

}
