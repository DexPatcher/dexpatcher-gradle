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

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

@CompileStatic
abstract class AbstractApktoolTask extends AbstractJavaExecTask {

/*
Apktool v2.2.1 - a tool for reengineering Android apk files
with smali v2.1.3 and baksmali v2.1.3
Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
Updated by Connor Tumbleson <connor.tumbleson@gmail.com>
Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

usage: apktool [-q|--quiet OR -v|--verbose]
 -advance,--advanced   prints advance information.
 -version,--version    prints the version then exits
usage: apktool [-q|--quiet OR -v|--verbose] if|install-framework [options] <framework.apk>
 -p,--frame-path <dir>   Stores framework files into <dir>.
 -t,--tag <tag>          Tag frameworks using <tag>.
usage: apktool [-q|--quiet OR -v|--verbose] d[ecode] [options] <file_apk>
    --api <API>          The numeric api-level of the file to generate, e.g. 14 for ICS.
 -b,--no-debug-info      don't write out debug info (.local, .param, .line, etc.)
 -f,--force              Force delete destination directory.
 -k,--keep-broken-res    Use if there was an error and some resources were dropped, e.g.
                         "Invalid config flags detected. Dropping resources", but you
                         want to decode them anyway, even with errors. You will have to
                         fix them manually before building.
 -m,--match-original     Keeps files to closest to original as possible. Prevents rebuild.
 -o,--output <dir>       The name of folder that gets written. Default is apk.out
 -p,--frame-path <dir>   Uses framework files located in <dir>.
 -r,--no-res             Do not decode resources.
 -s,--no-src             Do not decode sources.
 -t,--frame-tag <tag>    Uses framework files tagged by <tag>.
usage: apktool [-q|--quiet OR -v|--verbose] b[uild] [options] <app_path>
 -a,--aapt <loc>         Loads aapt from specified location.
 -c,--copy-original      Copies original AndroidManifest.xml and META-INF. See project page for more info.
 -d,--debug              Sets android:debuggable to "true" in the APK's compiled manifest
 -f,--force-all          Skip changes detection and build all files.
 -o,--output <dir>       The name of apk that gets written. Default is dist/name.apk
 -p,--frame-path <dir>   Uses framework files located in <dir>.
usage: apktool [-q|--quiet OR -v|--verbose] publicize-resources <file_path>

usage: apktool [-q|--quiet OR -v|--verbose] empty-framework-dir [options]
 -f,--force              Force delete destination directory.
 -p,--frame-path <dir>   Stores framework files into <dir>.

For additional info, see: http://ibotpeaches.github.io/Apktool/
For smali/baksmali info, see: https://github.com/JesusFreke/smali
*/

    enum Verbosity {
        QUIET,
        NORMAL,
        VERBOSE
    }

    protected final String command

    @Console final Property<Verbosity> verbosity
    @Optional @Input final DirectoryProperty frameworkDir
    @Optional @InputDirectory final DirectoryProperty frameworkDirAsInput
    @Optional @OutputDirectory final DirectoryProperty frameworkDirAsOutput

    @Internal protected final Provider<Directory> resolvedFrameworkDir

    AbstractApktoolTask(String command) {

        this.command = command
        main = 'brut.apktool.Main'

        verbosity = project.objects.property(Verbosity)
        frameworkDir = project.layout.directoryProperty()
        frameworkDirAsInput = project.layout.directoryProperty()
        frameworkDirAsOutput = project.layout.directoryProperty()

        resolvedFrameworkDir = project.providers.<Directory>provider {
            def dir = frameworkDir.orNull
            if (dir) return dir
            def dirAsInput = frameworkDirAsInput.orNull
            def dirAsOutput = frameworkDirAsOutput.orNull
            if (dirAsInput && dirAsOutput && project.file(dirAsInput) != project.file(dirAsOutput)) {
                throw new RuntimeException('Ambiguous framework directory')
            }
            return dirAsInput ?: dirAsOutput
        }

    }

    @Override List<String> getArgs() {

        ArrayList<String> args = new ArrayList()

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
            default:
                throw new AssertionError('Unexpected verbosity', null)
        }

        args.add(command)

        def fwDir = frameworkDir.orNull
        if (fwDir) args.addAll(['--frame-path', fwDir as String])

        return args;

    }

    @Override protected boolean defaultAddBlankLines() {
        switch (verbosity.orNull) {
            case Verbosity.QUIET:
                return false
                break
            case Verbosity.NORMAL:
            case null:
            case Verbosity.VERBOSE:
                return true
                break
            default:
                throw new AssertionError('Unexpected verbosity', null)
        }
    }

}
