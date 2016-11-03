package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.DexpatcherVerbosity
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

/*
DexPatcher Version 1.2.0 by Lanchon
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
 -o,--output <dex-or-dir>     name of output file or directory
 -p,--path                    output relative paths of source code files
    --path-root <root>        output absolute paths of source code files
 -q,--quiet                   do not output warnings
    --stats                   output timing statistics
 -v,--verbose                 output extra information
    --version                 print version information and exit
*/

@CompileStatic
class DexpatcherTask extends DexpatcherBaseTask {

    def source
    def patches
    def outputFile
    def outputDir
    def apiLevel
    def multiDex
    def multiDexThreaded
    def multiDexJobs
    def maxDexPoolSize
    def annotationPackage
    def compatDexTag
    def verbosity
    def sourcePath
    def sourcePathRoot
    def stats

    @Input File getSource() { Resolver.resolveNullableFile(project, source) }
    @InputFiles private FileCollection getSourceFiles() {
        FileCollection files = project.files()
        def file = getSource()
        if (file) files = file.directory ? (files + project.fileTree(file)) : (files + project.files(file))
        return files
    }

    @Input List<File> getPatches() {
        Resolver.resolve(patches) {
            it instanceof Iterable ? it.collect { each -> project.file(each) } : [project.file(it)]
        }
    }
    @InputFiles private FileCollection getPatchFiles() {
        FileCollection files = project.files()
        for (def file : getPatches()) {
            files = file.directory ? (files + project.fileTree(file)) : (files + project.files(file))
        }
        return files
    }

    @Optional @OutputFile File getOutputFile() { Resolver.resolveNullableFile(project, outputFile) }
    @Optional @OutputDirectory File getOutputDir() { Resolver.resolveNullableFile(project, outputDir) }

    @Optional @Input Integer getApiLevel() { Resolver.resolve(apiLevel) as Integer }

    @Optional @Input Boolean getMultiDex() { Resolver.resolve(multiDex) as Boolean }
    @Optional @Input Boolean getMultiDexThreaded() { Resolver.resolve(multiDexThreaded) as Boolean }
    @Optional @Input Integer getMultiDexJobs() { Resolver.resolve(multiDexJobs) as Integer }

    @Optional @Input Integer getMaxDexPoolSize() { Resolver.resolve(maxDexPoolSize) as Integer }

    @Optional @Input String getAnnotationPackage() { Resolver.resolve(annotationPackage) as String }
    @Optional @Input Boolean getCompatDexTag() { Resolver.resolve(compatDexTag) as Boolean }

    DexpatcherVerbosity getVerbosity() { Resolver.resolve(verbosity) as DexpatcherVerbosity }

    Boolean getSourcePath() { Resolver.resolve(sourcePath) as Boolean }
    String getSourcePathRoot() { Resolver.resolve(sourcePathRoot) as String }

    Boolean getStats() { Resolver.resolve(stats) as Boolean }

    @Override List<String> getArgs() {

        ArrayList<String> args = new ArrayList()

        def outFile = getOutputFile()
        def outDir = getOutputDir()
        if (!outFile && !outDir) throw new RuntimeException('No output file or directory specified')
        if (outFile && outDir) throw new RuntimeException('Output file and directory must not both be specified')
        args.addAll(['--output', (outFile ? outFile : outDir) as String])

        def api = getApiLevel()
        if (api) args.addAll(['--api-level', api as String])

        if (getMultiDex()) args.add('--multi-dex')
        if (getMultiDexThreaded()) args.add('--multi-dex-threaded')
        def jobs = getMultiDexJobs()
        if (jobs) args.addAll(['--multi-dex-jobs', jobs as String])

        def poolSize = getMaxDexPoolSize()
        if (poolSize) args.addAll(['--max-dex-pool-size', poolSize as String])

        def annotations = getAnnotationPackage()
        if (annotations) args.addAll(['--annotations', annotations])
        if (getCompatDexTag()) args.add('--compat-dextag')

        switch (getVerbosity()) {
            case DexpatcherVerbosity.QUIET: args.add('--quiet'); break
            case DexpatcherVerbosity.NORMAL: break
            case DexpatcherVerbosity.VERBOSE: args.add('--verbose'); break
            case DexpatcherVerbosity.DEBUG: args.add('--debug'); break
            case null: break
        }

        if (getSourcePath()) args.add('--path')
        def pathRoot = getSourcePathRoot()
        if (pathRoot != null) args.addAll(['--path-root', pathRoot])

        if (getStats()) args.add('--stats')

        args.addAll(getExtraArgs())

        args.add(getSource() as String)
        getPatches().each { args.add(it as String) }

        return args;

    }

    @Override void afterExec() {
        def outFile = getOutputFile()
        if (outFile && !outFile.file) throw new RuntimeException('No output generated')
    }

}
