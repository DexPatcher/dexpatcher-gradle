/*
 * DexPatcher - Copyright 2015-2017 Rodrigo Balerdi
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
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

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
class DexpatcherTask extends AbstractJavaExecTask {

    enum Verbosity {
        QUIET,
        NORMAL,
        VERBOSE,
        DEBUG
    }

    @Input final Property<File> source
    @Optional @InputFiles final Property<FileCollection> patches
    @Optional @OutputFile final RegularFileProperty outputFile
    @Optional @OutputDirectory final DirectoryProperty outputDir

    @Optional @Input final Property<Integer> apiLevel
    @Optional @Input final Property<Boolean> multiDex
    @Optional @Input final Property<Boolean> multiDexThreaded
    @Optional @Input final Property<Integer> multiDexJobs
    @Optional @Input final Property<Integer> maxDexPoolSize
    @Optional @Input final Property<String> annotationPackage
    @Optional @Input final Property<Boolean> constructorAutoIgnore
    @Optional @Input final Property<Boolean> compatDexTag
    @Console final Property<Verbosity> verbosity
    @Console final Property<Boolean> logSourcePath
    @Console final Property<String> logSourcePathRoot
    @Console final Property<Boolean> logStats

    DexpatcherTask() {

        main = 'lanchon.dexpatcher.Main'

        source = project.objects.property(File)
        patches = project.objects.property(FileCollection)
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

    @Optional @InputFile protected File getSourceFile() {
        File src = source.orNull
        if (!src) return null
        src = project.file(src)
        return src.isFile() ? src : null
    }

    @Optional @InputDirectory protected File getSourceDir() {
        File src = source.orNull
        if (!src) return null
        src = project.file(src)
        return src.isDirectory() ? src : null
    }

    @Override List<String> getArgs() {

        ArrayList<String> args = new ArrayList()

        def outFile = outputFile.orNull
        def outDir = outputDir.orNull
        if (!outFile && !outDir) throw new RuntimeException('No output file or directory specified')
        if (outFile && outDir) throw new RuntimeException('Output file and directory must not both be specified')
        args.addAll(['--output', (outFile ? outFile : outDir) as String])

        def api = apiLevel.orNull
        if (api) args.addAll(['--api-level', api as String])

        if (multiDex.orNull) {
            if (!multiDexThreaded.orNull) {
                args.add('--multi-dex')
            } else {
                args.add('--multi-dex-threaded')
                def jobs = multiDexJobs.orNull
                if (jobs) args.addAll(['--multi-dex-jobs', jobs as String])
            }
        }

        def poolSize = maxDexPoolSize.orNull
        if (poolSize) args.addAll(['--max-dex-pool-size', poolSize as String])

        def annotations = annotationPackage.orNull
        if (!annotations.is(null)) args.addAll(['--annotations', annotations])
        if (!constructorAutoIgnore.orNull) args.add('--no-auto-ignore')
        if (compatDexTag.orNull) args.add('--compat-dextag')

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

        if (logSourcePath.orNull) args.add('--path')
        def pathRoot = logSourcePathRoot.orNull
        if (!pathRoot.is(null)) args.addAll(['--path-root', pathRoot])

        if (logStats.orNull) args.add('--stats')

        addExtraArgsTo args

        args.add(source.get() as String)
        def patchList = patches.orNull
        if (patchList) args.addAll(patchList as List<String>)

        return args;

    }

    @Override protected boolean defaultAddBlankLines() {
        def alwaysNeedsBlankLines = true
        if (alwaysNeedsBlankLines) return true;
        switch (verbosity.orNull) {
            case Verbosity.QUIET:
            case Verbosity.NORMAL:
            case null:
                return false
                break
            case Verbosity.VERBOSE:
            case Verbosity.DEBUG:
                return true
                break
            default:
                throw new AssertionError('Unexpected verbosity', null)
        }
    }

    @Override protected void beforeExec() {
        deleteOutputFile outputFile.orNull
        deleteOutputDir outputDir.orNull
    }

    @Override protected void afterExec() {
        checkOutputFile outputFile.orNull
        checkOutputDir outputDir.orNull
    }

}
