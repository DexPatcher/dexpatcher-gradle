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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

/*
Apktool v2.2.1 - a tool for reengineering Android apk files
usage: apktool [-q|--quiet OR -v|--verbose] b[uild] [options] <app_path>
 -a,--aapt <loc>         Loads aapt from specified location.
 -c,--copy-original      Copies original AndroidManifest.xml and META-INF. See project page for more info.
 -d,--debug              Sets android:debuggable to "true" in the APK's compiled manifest
 -f,--force-all          Skip changes detection and build all files.
 -o,--output <dir>       The name of apk that gets written. Default is dist/name.apk
 -p,--frame-path <dir>   Uses framework files located in <dir>.
*/

@CompileStatic
class BuildApkTask extends AbstractApktoolTask {

    @InputDirectory final DirectoryProperty inputDir
    @OutputFile final RegularFileProperty apkFile

    @Optional @InputFile final RegularFileProperty aaptFile
    @Input final Property<Boolean> copyOriginal
    @Input final Property<Boolean> forceDebuggableBuild
    @Input final Property<Boolean> forceCleanBuild

    BuildApkTask() {

        super('build')

        inputDir = project.layout.directoryProperty()
        apkFile = project.layout.fileProperty()
        aaptFile = project.layout.fileProperty()
        copyOriginal = project.objects.property(Boolean)
        forceDebuggableBuild = project.objects.property(Boolean)
        forceCleanBuild = project.objects.property(Boolean)

    }

    @Override List<String> getArgs() {

        def args = super.getArgs()

        args.addAll(['--output', apkFile.get() as String])

        def aapt = aaptFile.orNull
        if (aapt) args.addAll(['--aapt', aapt as String])
        if (copyOriginal.orNull) args.add('--copy-original')
        if (forceDebuggableBuild.orNull) args.add('--debug')
        if (forceCleanBuild.orNull) args.add('--force-all')

        addExtraArgsTo args

        args.add(inputDir.get() as String)

        return args;

    }

    @Override protected void beforeExec() {
        deleteOutputFileOrDir apkFile.get()
    }

    @Override protected void afterExec() {
        checkOutputFile apkFile.get()
    }

}
