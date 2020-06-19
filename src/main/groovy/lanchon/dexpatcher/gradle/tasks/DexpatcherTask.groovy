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
DexPatcher version 1.7.0 by Lanchon (https://dexpatcher.github.io/)
usage: dexpatcher [<option> ...] [--output <patched-dex-or-dir>]
                  <source-dex-apk-or-dir> [<patch-dex-apk-or-dir> ...]
 -?,--help                    print this help message and exit
 -a,--api-level <n>           android api level (default: auto-detect)
    --annotations <package>   package name of DexPatcher annotations
                              (default: 'lanchon.dexpatcher.annotation')
    --debug                   output debugging information
    --dry-run                 do not write output files (much faster)
 -J,--multi-dex-jobs <n>      multi-dex thread count (implies: -m -M)
                              (default: available processors up to 4)
 -m,--multi-dex               enable multi-dex support
 -M,--multi-dex-threaded      multi-threaded multi-dex (implies: -m)
    --max-dex-pool-size <n>   maximum size of dex pools (default: 65536)
    --no-auto-ignore          no trivial default constructor auto-ignore
 -o,--output <dex-or-dir>     name of output file or directory
 -p,--path                    output relative paths of source code files
 -P,--path-root <root>        output absolute paths of source code files
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

    @PathSensitive(PathSensitivity.NONE)
    @InputFiles final Property<FileSystemLocation> source = project.objects.property(FileSystemLocation)
    @PathSensitive(PathSensitivity.NONE)
    @Optional @InputFiles final Property<FileSystemLocation> patch = project.objects.property(FileSystemLocation)
    @PathSensitive(PathSensitivity.NONE)
    @Optional @InputFiles final ListProperty<FileSystemLocation> patches = NewProperty.list(project, FileSystemLocation)
    @PathSensitive(PathSensitivity.NONE)
    @Optional @OutputFile final RegularFileProperty outputFile = NewProperty.file(project)
    @PathSensitive(PathSensitivity.NONE)
    @Optional @OutputDirectory final DirectoryProperty outputDir = NewProperty.dir(project)

    @Input final Property<Integer> apiLevel = NewProperty.from(project, 0)
    @Input final Property<Boolean> multiDex = NewProperty.from(project, false)
    @Input final Property<Boolean> multiDexThreaded = NewProperty.from(project, false)
    @Input final Property<Integer> multiDexJobs = NewProperty.from(project, 0)
    @Input final Property<Integer> maxDexPoolSize = NewProperty.from(project, 0)
    @Optional @Input final Property<String> annotationPackage = project.objects.property(String)
    @Input final Property<Boolean> constructorAutoIgnore = NewProperty.from(project, true)
    @Console final Property<Verbosity> verbosity = project.objects.property(Verbosity)
    @Console final Property<Boolean> logSourcePath = NewProperty.from(project, false)
    @Console final Property<String> logSourcePathRoot = project.objects.property(String)
    @Console final Property<Boolean> logStats = NewProperty.from(project, false)

    DexpatcherTask() {
        main = 'lanchon.dexpatcher.Main'
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
        args.add('--')

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
