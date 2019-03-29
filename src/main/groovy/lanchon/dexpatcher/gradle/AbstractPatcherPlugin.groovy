/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipOutputStream
import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.extensions.AbstractPatcherExtension
import lanchon.dexpatcher.gradle.tasks.Dex2jarTask

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider

import static lanchon.dexpatcher.gradle.Constants.*

// TODO: Locate apk lib based on bundle extension at afterEvaluate time.

// TODO: Warn on outdated apk lib based on plugin version that created it.
// TODO: Warn on incorrect android defaultConfig info.

// Parsing of 'apktool.yml' (contains defaultConfig info):
// http://yaml.org/
// https://bitbucket.org/asomov/snakeyaml
// https://github.com/iBotPeaches/Apktool/blob/master/brut.apktool/apktool-lib/src/main/java/brut/androlib/Androlib.java#L243

// Pending plugins:

// TODO: apktool-application (may bring in dexptacher if java plugin is applied too)
// TODO: maybe apktool-dexpatcher-application
// TODO: maybe apktool-smali-application

@CompileStatic
abstract class AbstractPatcherPlugin<
                E extends AbstractPatcherExtension, AE extends BaseExtension, AV extends BaseVariant
        > extends AbstractDecoderPlugin<E> {

    protected static class ApkLibraryPaths {

        //final static String PUBLIC_XML_FILE = 'dexpatcher/values/public.xml'

        final DirectoryProperty rootDir
        final DirectoryProperty dexpatcherDir
        final DirectoryProperty dexDir
        final RegularFileProperty dedexFile

        protected ApkLibraryPaths(Project project, Provider<Directory> apkLibDir) {
            rootDir = project.layout.directoryProperty(apkLibDir.<Directory>map {
                if (!it) throw new RuntimeException('Apk library not found')
                return it
            })
            dexpatcherDir = project.layout.directoryProperty(rootDir.dir('dexpatcher'))
            dexDir = project.layout.directoryProperty(dexpatcherDir.dir('dex'))
            dedexFile = project.layout.fileProperty(dexpatcherDir.file('dedex/classes.zip'))
        }

    }

    protected AE androidExtension
    protected DomainObjectSet<? extends AV> androidVariants

    protected DirectoryProperty dexpatcherDir
    private DirectoryProperty apkLibRootDirUnchecked
    protected ApkLibraryPaths apkLibrary

    @Override
    protected void afterApply() {

        super.afterApply()

        dexpatcherDir = project.layout.directoryProperty(project.layout.buildDirectory.dir(DIR_BUILD_INTERMEDIATES))
        apkLibRootDirUnchecked = project.layout.directoryProperty()
        apkLibrary = new ApkLibraryPaths(project, apkLibRootDirUnchecked)

        def dedexSourceClasses = project.tasks.register(TASK_DEDEX_SOURCE_CLASSES, Dex2jarTask) {
            it.description = "Translates the Dalvik bytecode of the source application to Java bytecode."
            it.group = GROUP_DEXPATCHER
            it.dependsOn decodedSourceApp
            it.dexFiles.from decodedSourceApp.get().sourceAppFile
            it.outputFile.set project.layout.buildDirectory.file(FILE_BUILD_DEDEXED_CLASSES)
            it.exceptionFile.set project.layout.buildDirectory.file(FILE_BUILD_DEX2JAR_EXCEPTIONS)
        }

        // Add the DexPatcher annotations as a compile-only dependency.
        if (true) {
            def providedLibs = Utils.getJars(project, dexpatcherConfig.resolvedProvidedLibDir)
            def compileOnlyConfig = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)
            compileOnlyConfig.dependencies.add(project.dependencies.create(providedLibs))
        }

        // Conditionally add the dedexed source classes as a compile-only dependency.
        project.afterEvaluate {
            if (((AbstractPatcherExtension) extension).importSymbols.get()) {
                def symbolLib = project.files(dedexSourceClasses.get().outputFile)
                symbolLib.builtBy dedexSourceClasses
                def compileOnlyConfig = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)
                compileOnlyConfig.dependencies.add(project.dependencies.create(symbolLib))
            }
        }

        // Setup 'apkLibrary' property.
/*
        project.afterEvaluate {
            afterPrepareApkLibrary { PrepareLibraryTask task ->
                if (apkLibRootDirUnchecked.orNull) throw new RuntimeException('Multiple apk libraries found')
                apkLibRootDirUnchecked.set task.explodedDir
            }
        }
*/

        //addDedexedClassesAsProvided()
        //workaroundForPublicXmlMergeBug()

    }

    private void workaroundForPublicXmlMergeBug() {
/*
        project.afterEvaluate {
            // Get 'public.xml' out of the resource merge inputs.
            prepareApkLibraryDoLast { PrepareLibraryTask task ->
                def fromFile = Utils.getFile(task.explodedDir, 'res/values/public.xml')
                def toFile = Utils.getFile(task.explodedDir, ApkLibraryPaths.PUBLIC_XML_FILE)
                com.google.common.io.Files.createParentDirs(toFile)
                Files.move(fromFile.toPath(), toFile.toPath())
            }
            // And later add a copy to the output of the merge.
            androidVariants.all { BaseVariant variant ->
                def task = variant.mergeResources
                task.doLast {
                    def fromFile = apkLibrary.rootDir.get().file(ApkLibraryPaths.PUBLIC_XML_FILE).asFile
                    def toFile = Utils.getFile(task.outputDir, 'values/dexpatcher-public.xml')
                    com.google.common.io.Files.createParentDirs(toFile)
                    Files.copy(fromFile.toPath(), toFile.toPath())
                }
            }
        }
*/
    }

    // APK Library

    protected void prepareApkLibraryDoLast(Closure closure) {
/*
        project.tasks.withType(PrepareLibraryTask).each { task ->
            task.doLast {
                if (isPrepareApkLibraryTask(task)) {
                    closure.maximumNumberOfParameters ? closure(task) : closure()
                }
            }
        }
*/
    }

    protected void afterPrepareApkLibrary(Closure closure) {
/*
        project.gradle.taskGraph.afterTask { task ->
            if (task instanceof PrepareLibraryTask && task.project.is(project)) {
                if (isPrepareApkLibraryTask(task)) {
                    closure.maximumNumberOfParameters ? closure(task) : closure()
                }
            }
        }
*/
    }

/*
    private boolean isPrepareApkLibraryTask (PrepareLibraryTask task) {
        return Utils.getFile(task.explodedDir, 'dexpatcher').isDirectory()
    }
*/

    // Task Graph

    protected void beforeTask(Task task, Closure closure) {
        project.gradle.taskGraph.beforeTask {
            if (it.is(task)) {
                closure.maximumNumberOfParameters ? closure(task) : closure()
            }
        }
    }

    protected void afterTask(Task task, Closure closure) {
        project.gradle.taskGraph.afterTask {
            if (it.is(task)) {
                closure.maximumNumberOfParameters ? closure(task) : closure()
            }
        }
    }

    /*
    protected void prepareDependenciesDoLast(BaseVariant variant, Closure closure) {
        PrepareDependenciesTask task = (PrepareDependenciesTask) project.tasks
                .getByName("prepare${variant.name.capitalize()}Dependencies")
        task.doLast {
            closure.maximumNumberOfParameters ? closure(task) : closure()
        }
    }

    private PrepareLibraryTask getPrepareApkLibraryTask(BaseVariant variant) {
        def apkLibTasks = project.tasks.withType(PrepareLibraryTask).findAll { PrepareLibraryTask task ->
            isPrepareApkLibraryTask(task) && task.dependsOn.contains(variant.preBuild)
        }
        if (apkLibTasks.isEmpty()) throw new RuntimeException("No apk library found in variant '${variant.name}'")
        if (apkLibTasks.size() > 1) throw new RuntimeException("Multiple apk libraries found in variant '${variant.name}'")
        return (PrepareLibraryTask) apkLibTasks[0]
    }

    /*
    // BYPASS PROCESSING

        project.afterEvaluate {
            androidVariants.all { BaseVariant variant ->
                //if (!extension.patchManifest) bypassManifest(variant)
                //if (!extension.patchResources) bypassAndroidResources(variant)
                if (extension.patchCode) processCode(variant)
                else bypassCode(variant)
            }
        }

    // Android Resources

    private void bypassAndroidResources(BaseVariant variant) {
        // TODO: Fix 9Patch issues and enable functionality.
        def mergeResources = variant.mergeResources
        project.tasks.getByName("generate${variant.name.capitalize()}ResValues").enabled = false
        mergeResources.enabled = false
        def importResources = project.tasks.create("import${variant.name.capitalize()}Resources", Sync)
        importResources.with {
            description = "Imports the resources from an apk library."
            group = DexpatcherBasePlugin.TASK_GROUP
            dependsOn = mergeResources.dependsOn
            into mergeResources.outputDir
        }
        afterPrepareDependencies(variant) { File libDir ->
            importResources.from Resolver.getFile(libDir, 'res')
        }
        mergeResources.dependsOn importResources
        variant.assemble.extensions.add 'importResources', importResources
    }

    // Code

    protected void processCode(BaseVariant variant) {}

    protected void bypassCode(BaseVariant variant) {
        variant.generateBuildConfig.enabled = false
        variant.aidlCompile.enabled = false
        variant.javaCompiler.enabled = false
    }
    */

}
