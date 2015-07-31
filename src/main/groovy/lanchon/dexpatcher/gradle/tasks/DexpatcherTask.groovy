package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.DexpatcherVerbosity
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

/*
usage: dexpatcher [<option> ...] [--output <patched-dex>]
                  <source-dex-or-apk> [<patch-dex-or-apk> ...]
 -?,--help                   print this help message and exit
 -a,--api-level <n>          api level of dex files (defaults to 14)
                             (needed for android 3 and earlier dex files)
    --debug                  output debugging information
 -o,--output <patched-dex>   name of patched dex file to write
 -q,--quiet                  do not output warnings
 -v,--verbose                output extra information
    --version                print version information and exit
*/

@CompileStatic
class DexpatcherTask extends DexpatcherBaseTask {

    def sourceFile
    def patchFiles
    def outputFile
    def apiLevel
    def verbosity

    @InputFile File getSourceFile() { project.file(sourceFile) }
    @InputFiles List<File> getPatchFiles() {
        Resolver.resolve(patchFiles) {
            it instanceof Iterable ? it.collect { each -> project.file(each) } : [project.file(it)]
        }
    }
    @OutputFile File getOutputFile() { project.file(outputFile) }
    @Optional @Input Integer getApiLevel() { Resolver.resolve(apiLevel) as Integer }
    DexpatcherVerbosity getVerbosity() { Resolver.resolve(verbosity) as DexpatcherVerbosity }

    @Override List<String> getArgs() {
        ArrayList<String> args = new ArrayList()
        args.addAll(['--output', getOutputFile() as String])
        def theApiLevel = getApiLevel()
        if (theApiLevel) args.addAll(['--api-level', theApiLevel as String])
        switch (getVerbosity()) {
            case DexpatcherVerbosity.QUIET: args.add('--quiet'); break
            case DexpatcherVerbosity.NORMAL: break
            case DexpatcherVerbosity.VERBOSE: args.add('--verbose'); break
            case DexpatcherVerbosity.DEBUG: args.add('--debug'); break
            case null: break
        }
        args.addAll(getExtraArgs())
        args.add(getSourceFile() as String)
        getPatchFiles().each { args.add(it as String) }
        return args;
    }

    @Override void afterExec() {
        if (!getOutputFile().isFile()) throw new RuntimeException('No output generated')
    }

}
