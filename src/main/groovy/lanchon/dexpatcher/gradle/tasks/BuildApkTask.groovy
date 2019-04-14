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

import groovy.transform.CompileStatic

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/*
Apktool v2.4.0 - a tool for reengineering Android apk files
usage: apktool [-q|--quiet OR -v|--verbose] b[uild] [options] <app_path>
 -a,--aapt <loc>         Loads aapt from specified location.
 -c,--copy-original      Copies original AndroidManifest.xml and META-INF. See project page for more info.
 -d,--debug              Sets android:debuggable to "true" in the APK's compiled manifest
 -f,--force-all          Skip changes detection and build all files.
 -nc,--no-crunch         Disable crunching of resource files during the build step.
 -o,--output <dir>       The name of apk that gets written. Default is dist/name.apk
 -p,--frame-path <dir>   Uses framework files located in <dir>.
    --use-aapt2          Upgrades apktool to use experimental aapt2 binary.
*/

@CompileStatic
@CacheableTask
class BuildApkTask extends AbstractApktoolTask {

    @InputDirectory @PathSensitive(PathSensitivity.NONE) final DirectoryProperty inputDir
    @OutputFile @PathSensitive(PathSensitivity.NONE) final RegularFileProperty apkFile

    @Optional @InputFile @PathSensitive(PathSensitivity.NONE) final RegularFileProperty aaptFile
    @Input final Property<Boolean> useAapt2
    @Input final Property<Boolean> crunchResources
    @Input final Property<Boolean> copyOriginal
    @Input final Property<Boolean> forceDebuggableBuild
    @Input final Property<Boolean> forceCleanBuild

    BuildApkTask() {

        super('build')

        inputDir = project.layout.directoryProperty()
        apkFile = project.layout.fileProperty()
        aaptFile = project.layout.fileProperty()
        useAapt2 = project.objects.property(Boolean)
        crunchResources = project.objects.property(Boolean)
        crunchResources.set true
        copyOriginal = project.objects.property(Boolean)
        forceDebuggableBuild = project.objects.property(Boolean)
        forceCleanBuild = project.objects.property(Boolean)

    }

    @Override List<String> getArgs() {

        def args = super.getArgs()

        args.addAll(['--output', apkFile.get() as String])

        def aapt = aaptFile.orNull
        if (aapt) args.addAll(['--aapt', aapt as String])
        if (useAapt2.get()) args.add('--use-aapt2')
        if (!crunchResources.get()) args.add('--no-crunch')
        if (copyOriginal.get()) args.add('--copy-original')
        if (forceDebuggableBuild.get()) args.add('--debug')
        if (forceCleanBuild.get()) args.add('--force-all')

        addExtraArgsTo args

        args.add(inputDir.get() as String)

        return args;

    }

    @Override protected void beforeExec() {
        super.beforeExec()
        deleteOutputFileOrDir apkFile.get()
    }

    @Override protected void afterExec() {
        super.afterExec()
        checkOutputFile apkFile.get()
    }

}
