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

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

/*
Apktool v2.2.1 - a tool for reengineering Android apk files
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
*/

@CompileStatic
class DecodeApkTask extends AbstractApktoolTask {

    def apkFile
    def outputDir
    def apiLevel
    def decodeResources = true
    def decodeClasses = true
    def keepBrokenResources
    def stripDebugInfo
    def matchOriginal
    def forceOverwrite

    DecodeApkTask() {
        super('decode')
    }

    @InputFile File getApkFile() { project.file(apkFile) }
    @OutputDirectory File getOutputDir() { project.file(outputDir) }
    @Optional @Input Integer getApiLevel() { Resolver.resolve(apiLevel) as Integer }
    @Optional @Input Boolean getDecodeResources() { Resolver.resolve(decodeResources) as Boolean }
    @Optional @Input Boolean getDecodeClasses() { Resolver.resolve(decodeClasses) as Boolean }
    @Optional @Input Boolean getKeepBrokenResources() { Resolver.resolve(keepBrokenResources) as Boolean }
    @Optional @Input Boolean getStripDebugInfo() { Resolver.resolve(stripDebugInfo) as Boolean }
    @Optional @Input Boolean getMatchOriginal() { Resolver.resolve(matchOriginal) as Boolean }
    @Optional @Input Boolean getForceOverwrite() { Resolver.resolve(forceOverwrite) as Boolean }

    @Override List<String> getArgs() {
        def args = super.getArgs()
        args.addAll(['--output', getOutputDir() as String])
        def apiLevel = getApiLevel()
        if (apiLevel) args.addAll(['--api', apiLevel as String])
        if (!getDecodeResources()) args.add('--no-res')
        if (!getDecodeClasses()) args.add('--no-src')
        if (getKeepBrokenResources()) args.add('--keep-broken-res')
        if (getStripDebugInfo()) args.add('--no-debug-info')
        if (getMatchOriginal()) args.add('--match-original')
        if (getForceOverwrite()) args.add('--force')
        args.addAll(getExtraArgs())
        args.add(getApkFile() as String)
        return args;
    }


    @Override void beforeExec() {
        def dir = getOutputDir()
        deleteOutputDir dir
        deleteOutputFile dir
    }

    @Override void afterExec() {
        checkOutputDir getOutputDir()
    }

}
