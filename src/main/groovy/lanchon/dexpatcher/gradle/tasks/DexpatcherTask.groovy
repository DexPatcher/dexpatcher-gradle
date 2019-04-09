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
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/*
DexPatcher Version 1.4.1 by Lanchon
           https://dexpatcher.github.io/
usage: dexpatcher [<option> ...] [--output <patched-dex-or-dir>]
                  <source-dex-apk-or-dir> [<patch-dex-apk-or-dir> ...]
 -?,--help                    print this help message and exit
 -a,--api-level <n>           android api level (default: auto-detect)
    --annotations <package>   package name of DexPatcher annotations
                              (default: 'lanchon.dexpatcher.annotation')
    --compat-dextag           enable support for the deprecated DexTag
    --debug                   output debugging information
    --dry-run                 do not write output files (much faster)
 -J,--multi-dex-jobs <n>      multi-dex thread count (implies: -m -M)
                              (default: available processors up to 4)
 -M,--multi-dex-threaded      multi-threaded multi-dex (implies: -m)
 -m,--multi-dex               enable multi-dex support
    --max-dex-pool-size <n>   maximum size of dex pools (default: 65536)
    --no-auto-ignore          no trivial default constructor auto-ignore
 -o,--output <dex-or-dir>     name of output file or directory
 -P,--path-root <root>        output absolute paths of source code files
 -p,--path                    output relative paths of source code files
 -q,--quiet                   do not output warnings
    --stats                   output timing statistics
 -v,--verbose                 output extra information
    --version                 print version information and exit
*/

@CompileStatic
@CacheableTask
class DexpatcherTask extends AbstractJavaExecTask {

    enum Verbosity {
        QUIET,
        NORMAL,
        VERBOSE,
        DEBUG
    }

    @InputFiles @PathSensitive(PathSensitivity.NONE) final Property<FileSystemLocation> source
    @Optional @InputFiles @PathSensitive(PathSensitivity.NONE) final Property<FileSystemLocation> patch
    @Optional @InputFiles @PathSensitive(PathSensitivity.NONE) final ListProperty<FileSystemLocation> patches
    @Optional @OutputFile @PathSensitive(PathSensitivity.NONE) final RegularFileProperty outputFile
    @Optional @OutputDirectory @PathSensitive(PathSensitivity.NONE) final DirectoryProperty outputDir

    @Input final Property<Integer> apiLevel
    @Input final Property<Boolean> multiDex
    @Input final Property<Boolean> multiDexThreaded
    @Input final Property<Integer> multiDexJobs
    @Input final Property<Integer> maxDexPoolSize
    @Optional @Input final Property<String> annotationPackage
    @Input final Property<Boolean> constructorAutoIgnore
    @Input final Property<Boolean> compatDexTag
    @Console final Property<Verbosity> verbosity
    @Console final Property<Boolean> logSourcePath
    @Console final Property<String> logSourcePathRoot
    @Console final Property<Boolean> logStats

    DexpatcherTask() {

        main = 'lanchon.dexpatcher.Main'

        source = project.objects.property(FileSystemLocation)
        patch = project.objects.property(FileSystemLocation)
        patches = project.objects.listProperty(FileSystemLocation)
        outputFile = project.layout.fileProperty()
        outputDir = project.layout.directoryProperty()

        apiLevel = project.objects.property(Integer)
        multiDex = project.objects.property(Boolean)
        multiDexThreaded = project.objects.property(Boolean)
        multiDexJobs = project.objects.property(Integer)
        maxDexPoolSize = project.objects.property(Integer)
        annotationPackage = project.objects.property(String)
        constructorAutoIgnore = project.objects.property(Boolean)
        constructorAutoIgnore.set true
        compatDexTag = project.objects.property(Boolean)
        verbosity = project.objects.property(Verbosity)
        logSourcePath = project.objects.property(Boolean)
        logSourcePathRoot = project.objects.property(String)
        logStats = project.objects.property(Boolean)

    }

    @Override List<String> getArgs() {

        def args = new ArrayList<String>()

        def outFile = outputFile.orNull
        def outDir = outputDir.orNull
        if (!outFile && !outDir) throw new RuntimeException('No output file or directory specified')
        if (outFile && outDir) throw new RuntimeException('Output file and directory must not both be specified')
        args.addAll(['--output', (outFile ? outFile : outDir) as String])

        def api = apiLevel.get()
        if (api) args.addAll(['--api-level', api as String])

        if (multiDex.get()) {
            if (!multiDexThreaded.get()) {
                args.add('--multi-dex')
            } else {
                args.add('--multi-dex-threaded')
                def jobs = multiDexJobs.get()
                if (jobs) args.addAll(['--multi-dex-jobs', jobs as String])
            }
        }

        def poolSize = maxDexPoolSize.get()
        if (poolSize) args.addAll(['--max-dex-pool-size', poolSize as String])

        def annotations = annotationPackage.orNull
        if (!annotations.is(null)) args.addAll(['--annotations', annotations])
        if (!constructorAutoIgnore.get()) args.add('--no-auto-ignore')
        if (compatDexTag.get()) args.add('--compat-dextag')

        switch (verbosity.orNull) {
            case Verbosity.QUIET:
                args.add('--quiet')
                break
            case Verbosity.NORMAL:
            case null:
                break
            case Verbosity.VERBOSE:
                args.add('--verbose')
                break
            case Verbosity.DEBUG:
                args.add('--debug')
                break
            default:
                throw new AssertionError('Unexpected verbosity', null)
        }

        if (logSourcePath.get()) args.add('--path')
        def pathRoot = logSourcePathRoot.orNull
        if (!pathRoot.is(null)) args.addAll(['--path-root', pathRoot])

        if (logStats.get()) args.add('--stats')

        addExtraArgsTo args

        args.add(source.get() as String)

        def singlePatch = patch.orNull
        def patchList = patches.orNull
        if (singlePatch && patchList) throw new RuntimeException('Properties patch and patches must not both be specified')
        if (singlePatch) args.add(singlePatch as String)
        if (patchList) args.addAll(patchList as List<String>)

        return args;

    }

    @Override protected void beforeExec() {
        deleteOutputFileOrDir outputFile.orNull
        deleteOutputDirContents outputDir.orNull
    }

    @Override protected void afterExec() {
        checkOutputFile outputFile.orNull
        checkOutputDir outputDir.orNull
    }

}
