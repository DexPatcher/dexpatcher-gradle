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

import lanchon.dexpatcher.gradle.NewProperty

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
Apktool v2.4.0 - a tool for reengineering Android apk files
usage: apktool [-q|--quiet OR -v|--verbose] d[ecode] [options] <file_apk>
 -api,--api-level <API>   The numeric api-level of the file to generate, e.g. 14 for ICS.
 -b,--no-debug-info       don't write out debug info (.local, .param, .line, etc.)
 -f,--force               Force delete destination directory.
    --force-manifest      Decode the APK's compiled manifest, even if decoding of resources is set to "false".
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

    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputFile final RegularFileProperty apkFile = project.layout.fileProperty()
    @PathSensitive(PathSensitivity.NONE)
    @OutputDirectory final DirectoryProperty outputDir = project.layout.directoryProperty()

    @Optional @Input final Property<String> frameworkTag = project.objects.property(String)
    @Input final Property<Integer> apiLevel = NewProperty.from(project, 0)
    @Input final Property<Boolean> decodeAssets = NewProperty.from(project, true)
    @Input final Property<Boolean> decodeResources = NewProperty.from(project, true)
    @Input final Property<Boolean> decodeClasses = NewProperty.from(project, true)
    @Input final Property<Boolean> forceDecodeManifest = NewProperty.from(project, false)
    @Input final Property<Boolean> keepBrokenResources = NewProperty.from(project, false)
    @Input final Property<Boolean> stripDebugInfo = NewProperty.from(project, false)
    @Input final Property<Boolean> matchOriginal = NewProperty.from(project, false)

    DecodeApkTask() {
        super('decode')
    }

    @Override List<String> getArgs() {

        def args = super.getArgs()

        args.addAll(['--output', outputDir.get() as String])

        def fwTag = frameworkTag.orNull
        if (fwTag) args.addAll(['--frame-tag', fwTag])
        def api = apiLevel.get()
        if (api) args.addAll(['--api-level', api as String])
        if (!decodeAssets.get()) args.add('--no-assets')
        if (!decodeResources.get()) args.add('--no-res')
        if (!decodeClasses.get()) args.add('--no-src')
        if (forceDecodeManifest.get()) args.add('--force-manifest')
        if (keepBrokenResources.get()) args.add('--keep-broken-res')
        if (stripDebugInfo.get()) args.add('--no-debug-info')
        if (matchOriginal.get()) args.add('--match-original')

        //if (forceOverwrite.get()) args.add('--force')
        addExtraArgsTo args

        args.add(apkFile.get() as String)

        return args;

    }

    @Override protected void beforeExec() {
        super.beforeExec()
        deleteOutputFileOrDir outputDir.get()
    }

    @Override protected void afterExec() {
        super.afterExec()
        checkOutputDir outputDir.get()
    }

}
