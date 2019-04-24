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

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/*
d2j-dex2jar -- convert dex to jar
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
version: reader-2.1-SNAPSHOT, translator-2.1-SNAPSHOT, ir-2.1-SNAPSHOT
*/

@CompileStatic
@CacheableTask
class Dex2jarTask extends AbstractDex2jarTask {

    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputFiles final ConfigurableFileCollection dexFiles = project.files()
    @PathSensitive(PathSensitivity.NONE)
    @Optional @OutputFile final RegularFileProperty outputFile = project.layout.fileProperty()
    @PathSensitive(PathSensitivity.NONE)
    @Optional @OutputDirectory final DirectoryProperty outputDir = project.layout.directoryProperty()
    @PathSensitive(PathSensitivity.NONE)
    @Optional @OutputFile final RegularFileProperty exceptionFile = project.layout.fileProperty()

    @Input final Property<Boolean> translateCode = project.objects.property(Boolean)
    @Input final Property<Boolean> translateDebugInfo = project.objects.property(Boolean)
    @Input final Property<Boolean> optimizeSynchronized = project.objects.property(Boolean)
    @Input final Property<Boolean> reuseRegisters = project.objects.property(Boolean)
    @Input final Property<Boolean> topologicalSort = project.objects.property(Boolean)
    @Input final Property<Boolean> handleExceptions = project.objects.property(Boolean)

    Dex2jarTask() {
        main = 'com.googlecode.dex2jar.tools.Dex2jarCmd'
        translateCode.set true
        //handleExceptions.set true
    }

    @Override List<String> getArgs() {

        def args = new ArrayList<String>()

        def outFile = outputFile.orNull
        def outDir = outputDir.orNull
        if (!outFile && !outDir) throw new RuntimeException('No output file or directory specified')
        if (outFile && outDir) throw new RuntimeException('Output file and directory must not both be specified')
        if (outFile) {
            args.addAll(['--output', outFile as String])
            workingDir = outFile.asFile.parentFile
        }
        else workingDir = outDir.asFile

        def exceptFile = exceptionFile.orNull
        if (exceptFile) args.addAll(['--exception-file', exceptFile as String])

        if (!translateCode.get()) args.add('--no-code')
        if (translateDebugInfo.get()) args.add('--debug-info')
        if (optimizeSynchronized.get()) args.add('-os')   // typo in long option '--optmize-synchronized'
        if (reuseRegisters.get()) args.add('--reuse-reg')
        if (topologicalSort.get()) args.add('--topological-sort')
        if (!handleExceptions.get()) args.add('--not-handle-exception')

        //if (forceOverwrite.get()) args.add('--force')
        addExtraArgsTo args

        if (dexFiles.isEmpty()) throw new RuntimeException('No input dex files specified')
        args.addAll(dexFiles as List<String>)

        return args;

    }

    @Override protected void beforeExec() {
        deleteOutputFileOrDir outputFile.orNull
        deleteOutputDirContents outputDir.orNull
        deleteOutputFileOrDir exceptionFile.orNull
    }

    @Override protected void afterExec() {
        checkOutputFile outputFile.orNull
        checkOutputDir outputDir.orNull
    }

}
