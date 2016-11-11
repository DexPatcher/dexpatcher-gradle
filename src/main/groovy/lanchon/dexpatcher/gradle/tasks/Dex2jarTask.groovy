/*
 * DexPatcher - Copyright 2015, 2016 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.Resolver

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

/*
usage: d2j-dex2jar [options] <file0> [file1 ... fileN]
options:
 -d,--debug-info              translate debug info
 -e,--exception-file <file>   detail exception file, default is $current_dir/[fi
                              le-name]-error.zip
 -f,--force                   force overwrite
 -h,--help                    Print this help message
 -n,--not-handle-exception    not handle any exception throwed by dex2jar
 -nc,--no-code
 -o,--output <out-jar-file>   output .jar file, default is $current_dir/[file-na
                              me]-dex2jar.jar
 -os,--optmize-synchronized   optmize-synchronized
 -p,--print-ir                print ir to Syste.out
 -r,--reuse-reg               reuse regiter while generate java .class file
 -s                           same with --topological-sort/-ts
 -ts,--topological-sort       sort block by topological, that will generate more
                               readable code, default enabled
*/

@CompileStatic
class Dex2jarTask extends AbstractDex2jarTask {

    def dexFiles
    def outputFile
    def outputDir
    def exceptionFile
    @Input boolean translateCode = true
    @Input boolean translateDebugInfo
    @Input boolean optimizeSynchronized
    @Input boolean reuseRegisters
    @Input boolean handleExceptions
    @Input boolean forceOverwrite

    Dex2jarTask() {
        main = MAIN_DEX2JAR
    }

    @InputFiles FileCollection getDexFiles() { project.files(dexFiles) }

    @Optional @OutputFile File getOutputFile() { Resolver.resolveNullableFile(project, outputFile) }
    @Optional @OutputDirectory File getOutputDir() { Resolver.resolveNullableFile(project, outputDir) }

    @Optional @OutputFile File getExceptionFile() { Resolver.resolveNullableFile(project, exceptionFile) }

    @Override List<String> getArgs() {

        ArrayList<String> args = new ArrayList()

        def outFile = getOutputFile()
        def outDir = getOutputDir()
        if (!outFile && !outDir) throw new RuntimeException('No output file or directory specified')
        if (outFile && outDir) throw new RuntimeException('Output file and directory must not both be specified')
        if (outFile) args.addAll(['--output', outFile as String])
        else workingDir = outDir

        def theExceptionFile = getExceptionFile()
        if (theExceptionFile) args.addAll(['--exception-file', theExceptionFile as String])

        if (!translateCode) args.add('--no-code')
        if (translateDebugInfo) args.add('--debug-info')
        if (optimizeSynchronized) args.add('-os')   // typo in long option: --optmize-synchronized
        if (reuseRegisters) args.add('--reuse-reg')
        if (!handleExceptions) args.add('--not-handle-exception')
        if (forceOverwrite) args.add('--force')

        args.addAll(getExtraArgs())

        def dexFileCollection = getDexFiles()
        if (dexFileCollection.empty) throw new RuntimeException('No input dex files specified')
        args.addAll(dexFileCollection as List<String>)

        return args;

    }

    @Override void beforeExec() {
        deleteOutputFile getOutputFile()
        deleteOutputDir getOutputDir()
    }

    @Override void afterExec() {
        checkOutputFile getOutputFile()
        checkOutputDir getOutputDir()
    }

}
