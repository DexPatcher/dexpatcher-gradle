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

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.NewProperty

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

    @PathSensitive(PathSensitivity.NONE)
    @InputDirectory final DirectoryProperty inputDir = NewProperty.dir(project)
    @PathSensitive(PathSensitivity.NONE)
    @OutputFile final RegularFileProperty apkFile = NewProperty.file(project)

    @PathSensitive(PathSensitivity.NONE)
    @Optional @InputFile final RegularFileProperty aaptFile = NewProperty.file(project)

    @Input final Property<Boolean> useAapt2 = NewProperty.from(project, false)
    @Input final Property<Boolean> crunchResources = NewProperty.from(project, true)
    @Input final Property<Boolean> copyOriginal = NewProperty.from(project, false)
    @Input final Property<Boolean> forceDebuggableBuild = NewProperty.from(project, false)
    @Input final Property<Boolean> forceCleanBuild = NewProperty.from(project, false)

    BuildApkTask() {
        super('build')
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
        args.add('--')

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
