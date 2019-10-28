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

import lanchon.dexpatcher.gradle.NewProperty

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CompileStatic
abstract class AbstractApktoolTask extends AbstractJavaExecTask {

/*
Apktool v2.4.0 - a tool for reengineering Android apk files
with smali v2.2.6 and baksmali v2.2.6
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
 -api,--api-level <API>   The numeric api-level of the file to generate, e.g. 14 for ICS.
 -b,--no-debug-info       don't write out debug info (.local, .param, .line, etc.)
 -f,--force               Force delete destination directory.
    --force-manifest      Decode the APK's compiled manifest, even if decoding of resources is set to "false".
 -k,--keep-broken-res     Use if there was an error and some resources were dropped, e.g.
                          "Invalid config flags detected. Dropping resources", but you
                          want to decode them anyway, even with errors. You will have to
                          fix them manually before building.
 -m,--match-original      Keeps files to closest to original as possible. Prevents rebuild.
    --no-assets           Do not decode assets.
 -o,--output <dir>        The name of folder that gets written. Default is apk.out
 -p,--frame-path <dir>    Uses framework files located in <dir>.
 -r,--no-res              Do not decode resources.
 -s,--no-src              Do not decode sources.
 -t,--frame-tag <tag>     Uses framework files tagged by <tag>.
usage: apktool [-q|--quiet OR -v|--verbose] b[uild] [options] <app_path>
 -a,--aapt <loc>         Loads aapt from specified location.
 -c,--copy-original      Copies original AndroidManifest.xml and META-INF. See project page for more info.
 -d,--debug              Sets android:debuggable to "true" in the APK's compiled manifest
 -f,--force-all          Skip changes detection and build all files.
 -nc,--no-crunch         Disable crunching of resource files during the build step.
 -o,--output <dir>       The name of apk that gets written. Default is dist/name.apk
 -p,--frame-path <dir>   Uses framework files located in <dir>.
    --use-aapt2          Upgrades apktool to use experimental aapt2 binary.
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

    @Console final Property<Verbosity> verbosity = project.objects.property(Verbosity)

    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional @InputDirectory final DirectoryProperty frameworkInDir = NewProperty.dir(project)

    @PathSensitive(PathSensitivity.RELATIVE)
    @OutputDirectory final DirectoryProperty frameworkOutDir = NewProperty.dir(project)

    AbstractApktoolTask(String command) {
        this.command = command
        main = 'brut.apktool.Main'
    }

    @Override List<String> getArgs() {

        def args = new ArrayList<String>()

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

        args.addAll(['--frame-path', frameworkOutDir.get() as String])

        return args;

    }

    @Override protected void beforeExec() {
        def inDir = frameworkInDir.orNull
        def outDir = frameworkOutDir.get()
        if (inDir) {
            project.sync {
                it.from inDir
                it.into outDir
            }
        } else {
            deleteOutputDirContents outDir
        }
    }

    @Override protected void afterExec() {}

}
