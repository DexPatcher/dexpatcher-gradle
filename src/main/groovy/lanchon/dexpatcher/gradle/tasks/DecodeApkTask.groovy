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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/*
Apktool v2.3.0 - a tool for reengineering Android apk files
usage: apktool [-q|--quiet OR -v|--verbose] d[ecode] [options] <file_apk>
 -api,--api-level <API>   The numeric api-level of the file to generate, e.g. 14 for ICS.
 -b,--no-debug-info       don't write out debug info (.local, .param, .line, etc.)
 -f,--force               Force delete destination directory.
 -k,--keep-broken-res     Use if there was an error and some resources were dropped, e.g.
                          "Invalid config flags detected. Dropping resources", but you
                          want to decode them anyway, even with errors. You will have to
                          fix them manually before building.
 -m,--match-original      Keeps files to closest to original as possible. Prevents rebuild.
    --no-assets           Do not decode assets.
 -o,--output <dir>        The name of folder that gets written. Default is apk.out
 -p,--frame-path <dir>    Uses framework files located in <dir>.
 -r,--no-res              Do not decode resources.
 -s,--no-src              Do not decode sources.
 -t,--frame-tag <tag>     Uses framework files tagged by <tag>.
*/

@CompileStatic
@CacheableTask
class DecodeApkTask extends AbstractApktoolTask {

    @InputFile @PathSensitive(PathSensitivity.NAME_ONLY) final RegularFileProperty apkFile
    @OutputDirectory @PathSensitive(PathSensitivity.NONE) final DirectoryProperty outputDir

    @Optional @Input final Property<String> frameworkTag
    @Input final Property<Integer> apiLevel
    @Input final Property<Boolean> decodeAssets
    @Input final Property<Boolean> decodeResources
    @Input final Property<Boolean> decodeClasses
    @Input final Property<Boolean> keepBrokenResources
    @Input final Property<Boolean> stripDebugInfo
    @Input final Property<Boolean> matchOriginal

    DecodeApkTask() {

        super('decode')

        apkFile = project.layout.fileProperty()
        outputDir = project.layout.directoryProperty()

        frameworkTag = project.objects.property(String)
        apiLevel = project.objects.property(Integer)
        decodeAssets = project.objects.property(Boolean)
        decodeAssets.set true
        decodeResources = project.objects.property(Boolean)
        decodeResources.set true
        decodeClasses = project.objects.property(Boolean)
        decodeClasses.set true
        keepBrokenResources = project.objects.property(Boolean)
        stripDebugInfo = project.objects.property(Boolean)
        matchOriginal = project.objects.property(Boolean)

    }

    @Override List<String> getArgs() {

        def args = super.getArgs()

        args.addAll(['--output', outputDir.get() as String])

        def fwTag = frameworkTag.orNull
        if (fwTag) args.addAll(['--frame-tag', fwTag])
        def api = apiLevel.get()
        if (api) args.addAll(['--api', api as String])
        if (!decodeAssets.get()) args.add('--no-assets')
        if (!decodeResources.get()) args.add('--no-res')
        if (!decodeClasses.get()) args.add('--no-src')
        if (keepBrokenResources.get()) args.add('--keep-broken-res')
        if (stripDebugInfo.get()) args.add('--no-debug-info')
        if (matchOriginal.get()) args.add('--match-original')

        //if (forceOverwrite.get()) args.add('--force')
        addExtraArgsTo args

        args.add(apkFile.get() as String)

        return args;

    }

    @Override protected void beforeExec() {
        deleteOutputFileOrDir outputDir.get()
    }

    @Override protected void afterExec() {
        checkOutputDir outputDir.get()
    }

}
