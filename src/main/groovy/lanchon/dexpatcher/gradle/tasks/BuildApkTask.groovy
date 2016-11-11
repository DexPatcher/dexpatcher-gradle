package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

/*
usage: apktool [-q|--quiet OR -v|--verbose] b[uild] [options] <app_path>
 -a,--aapt <loc>         Loads aapt from specified location.
 -c,--copy-original      Copies original AndroidManifest.xml and META-INF. See project page for more info.
 -d,--debug              Builds in debug mode. Check project page for more info.
 -f,--force-all          Skip changes detection and build all files.
 -o,--output <dir>       The name of apk that gets written. Default is dist/name.apk
 -p,--frame-path <dir>   Uses framework files located in <dir>.
*/

@CompileStatic
class BuildApkTask extends AbstractApktoolTask {

    def inputDir
    def apkFile
    def frameworkDir
    def aaptFile
    @Input boolean forceRebuild

    @InputDirectory File getInputDir() { project.file(inputDir) }
    @OutputFile File getApkFile() { project.file(apkFile) }
    @Optional @InputDirectory File getFrameworkDir() { Resolver.resolveNullableFile(project, frameworkDir) }
    @Optional @InputFile File getAaptFile() { Resolver.resolveNullableFile(project, aaptFile) }

    @Override List<String> getArgs() {
        ArrayList<String> args = new ArrayList()
        args.add('build')
        args.addAll(['--output', getApkFile() as String])
        if (getFrameworkDir()) args.addAll(['--frame-path', getFrameworkDir() as String])
        if (getAaptFile()) args.addAll(['--aapt', getAaptFile() as String])
        if (forceRebuild) args.add('--force-all')
        args.addAll(getExtraArgs())
        args.add(getInputDir() as String)
        return args;
    }

    @Override void beforeExec() {
        deleteOutputFile getApkFile()
    }

    @Override void afterExec() {
        checkOutputFile getApkFile()
    }

}
