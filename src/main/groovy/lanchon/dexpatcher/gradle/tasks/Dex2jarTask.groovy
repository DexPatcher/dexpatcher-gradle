package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
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
class Dex2jarTask extends Dex2jarBaseTask {

    def dexFiles
    def jarFile
    def exceptionFile
    @Input boolean translateCode = true
    @Input boolean translateDebugInfo
    @Input boolean optimizeSynchronized
    @Input boolean reuseRegisters
    @Input boolean handleExceptions
    @Input boolean forceOverwrite

    Dex2jarTask() {
        setMain(MAIN_DEX2JAR)
    }

    @InputFiles FileCollection getDexFiles() { project.files(dexFiles) }
    @OutputFile File getJarFile() { project.file(jarFile) }
    @Optional @OutputFile File getExceptionFile() { Resolver.resolveNullableFile(project, exceptionFile) }

    @Override List<String> getArgs() {
        ArrayList<String> args = new ArrayList()
        args.addAll(['--output', getJarFile() as String])
        if (getExceptionFile()) args.addAll(['--exception-file', getExceptionFile() as String])
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

    @Override void afterExec() {
        if (!getJarFile().isFile()) throw new RuntimeException('No output generated')
    }

}
