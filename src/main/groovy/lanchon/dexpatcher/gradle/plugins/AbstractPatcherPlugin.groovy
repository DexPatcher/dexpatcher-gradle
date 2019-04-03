/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.plugins


import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.Utils
import lanchon.dexpatcher.gradle.extensions.AbstractPatcherExtension
import lanchon.dexpatcher.gradle.tasks.Dex2jarTask
import lanchon.dexpatcher.gradle.tasks.LazyZipTask

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.ZipEntryCompression

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

        dexpatcherDir = project.layout.directoryProperty(project.layout.buildDirectory.dir(BuildDir.DIR_INTERMEDIATES))
        apkLibRootDirUnchecked = project.layout.directoryProperty()
        apkLibrary = new ApkLibraryPaths(project, apkLibRootDirUnchecked)

        // Add the DexPatcher annotations as a compile-only dependency.
        def providedLibs = Utils.getJars(project, dexpatcherConfig.resolvedProvidedLibDir)
        project.dependencies.add JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, providedLibs

        def dedexClasses = project.tasks.register(TaskNames.DEDEX_CLASSES, Dex2jarTask) {
            it.description = "Translates the Dalvik bytecode of the source application to Java bytecode."
            it.group = TASK_GROUP_NAME
            it.dependsOn provideDecodedApp
            it.dexFiles.from provideDecodedApp.get().sourceAppFile
            it.outputFile.set project.layout.buildDirectory.file(BuildDir.FILE_DEDEXED_CLASSES)
            it.exceptionFile.set project.layout.buildDirectory.file(BuildDir.FILE_DEX2JAR_EXCEPTIONS)
        }

        // Conditionally add the dedexed source classes as a compile-only dependency.
        project.afterEvaluate {
            if (((AbstractPatcherExtension) extension).importSymbols.get()) {
                def symbolLib = project.files(dedexClasses.get().outputFile)
                symbolLib.builtBy dedexClasses
                project.dependencies.add JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, symbolLib
            }
        }

        def packExtraResources = project.tasks.register(TaskNames.PACK_EXTRA_RESOURCES, LazyZipTask) {
            it.description = "Packs extra resources of the source application."
            it.group = TASK_GROUP_NAME
            it.zip64 = true
            it.reproducibleFileOrder = true
            it.preserveFileTimestamps = false
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            it.entryCompression = ZipEntryCompression.STORED
            it.lazyArchiveFileName.set AppAar.FILE_CLASSES_JAR
            it.lazyDestinationDirectory.set project.layout.buildDirectory.dir(BuildDir.DIR_EXTRA_RESOURCES)
            it.dependsOn provideDecodedApp
            it.from(provideDecodedApp.get().outputDir.dir(ApkLib.DIR_UNKNOWN))
            it.from(provideDecodedApp.get().outputDir.dir(ApkLib.DIR_META_INF)) { CopySpec spec ->
                spec.into FileNames.META_INF
            }
            return
        }



/*
        def apkLibrary = createApkLibraryTask(project, taskNameModifier('apkLibrary'), apktoolDir, dex2jarFile,
                dex2jarExceptionFile, resourcesDir)
        apkLibrary.with {
            description = "Packs the processed application into an apk library."
            group = taskGroup
            dependsOn decodeApk, dex2jar, resources
            destinationDir = libraryDir.asFile
            extension = 'aar'
            // WARNING: archiveName is set eagerly.
            def apkName = decodeApk.apkFile.orNull?.asFile?.name ?: project.name ?: 'source'
            if (!apkName.toLowerCase(Locale.ENGLISH).endsWith('.apk')) apkName += '.apk'
            archiveName = apkName + '.' + extension
        }


        static Zip createApkLibraryTask(Project project, String name, Directory apktoolDir, RegularFile dex2jarFile,
                RegularFile dex2jarExceptionFile, Directory resourcesDir) {

            def apkLibrary = project.tasks.create(name, Zip)
            apkLibrary.with {

                extension = 'apk.aar'
                it.zip64 = true
                duplicatesStrategy = DuplicatesStrategy.FAIL

                /*
                AAR Format:
                    /AndroidManifest.xml (mandatory)
                /classes.jar (mandatory)
                    /res/ (mandatory)
                /R.txt (mandatory)
                    /assets/ (optional)
                /libs/*.jar (optional)
                /jni/<abi>/*.so (optional)
                    /proguard.txt (optional)
                    /lint.jar (optional)
                * /

                from(apktoolDir) { CopySpec spec ->
                    spec.with {
                        include 'AndroidManifest.xml'
                        include 'res/'
                        include 'assets/'
                    }
                }
                from(apktoolDir.dir('lib')) { CopySpec spec ->
                    spec.into 'jni'
                }
                from(apktoolDir) { CopySpec spec ->
                    spec.with {
                        include '*.dex'
                        into 'dexpatcher/dex'
                    }
                }
                from(apktoolDir) { CopySpec spec ->
                    spec.with {
                        exclude 'AndroidManifest.xml'
                        exclude 'res'
                        exclude 'assets'
                        exclude 'lib'
                        exclude 'unknown'
                        exclude '*.dex'
                        into 'dexpatcher/apktool'
                    }
                }
                from(dex2jarFile) { CopySpec spec ->
                    spec.into 'dexpatcher/dedex'
                }
                from(dex2jarExceptionFile) { CopySpec spec ->
                    spec.into 'dexpatcher/dex2jar'
                }
                from(resourcesDir)

            }
            return apkLibrary

        }

*/

        /*
        def decodedSourceAppDir = project.files {
            sourceApp.get().outputDir.get().asFile
        }
        //def config = project.configurations.detachedConfiguration(project.dependencies.create(file))
        Dependency aar = project.dependencies.create(decodedSourceAppDir)
        def config = project.configurations.create('testExplodedAar')
        config.dependencies.add aar
        //config.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage, Usage.JAVA_RUNTIME))
        config.attributes.attribute(ARTIFACT_FORMAT, AndroidArtifacts.ArtifactType.EXPLODED_AAR.getType())
        def implementationConfig = project.configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
        implementationConfig.extendsFrom config
        */

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
