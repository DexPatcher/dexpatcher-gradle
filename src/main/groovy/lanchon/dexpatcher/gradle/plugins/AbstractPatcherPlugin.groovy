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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.LocalDependencyHelper
import lanchon.dexpatcher.gradle.Utils
import lanchon.dexpatcher.gradle.VariantHelper
import lanchon.dexpatcher.gradle.extensions.AbstractPatcherExtension
import lanchon.dexpatcher.gradle.tasks.Dex2jarTask
import lanchon.dexpatcher.gradle.tasks.LazyZipTask
import lanchon.dexpatcher.gradle.tasks.ProcessIdMappingsTask

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.tasks.MergeResources
import com.android.builder.core.AndroidBuilder
import com.android.utils.StringHelper
import org.gradle.api.DomainObjectSet
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.ZipEntryCompression

import static lanchon.dexpatcher.gradle.Constants.*

// TODO: Warn on incorrect android defaultConfig info.
// Parsing of 'apktool.yml' (contains defaultConfig info):
// http://yaml.org/
// https://bitbucket.org/asomov/snakeyaml
// https://github.com/iBotPeaches/Apktool/blob/master/brut.apktool/apktool-lib/src/main/java/brut/androlib/Androlib.java#L243

// TODO: Pending plugins:
// -apktool-application (may bring in dexptacher if java plugin is applied too)
// -maybe apktool-dexpatcher-application
// -maybe apktool-smali-application

@CompileStatic
abstract class AbstractPatcherPlugin<
                E extends AbstractPatcherExtension, AE extends BaseExtension, AV extends BaseVariant
        > extends AbstractDecoderPlugin<E> {

    protected AE androidExtension
    protected DomainObjectSet<? extends AV> androidVariants

    @Override
    protected void afterApply() {

        super.afterApply()

        // Add the DexPatcher annotations as a compile-only dependency.
        def providedLibs = Utils.getJars(project, dexpatcherConfig.resolvedProvidedLibDir)
        //project.dependencies.add JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, providedLibs
        LocalDependencyHelper.addDexpatcherAnnotations project, providedLibs, false

        // Dedex the bytecode of the source application.
        def dedexAppClasses = project.tasks.register(TaskNames.DEDEX_APP_CLASSES, Dex2jarTask) {
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
                def symbolLib = project.files(dedexAppClasses.get().outputFile)
                symbolLib.builtBy dedexAppClasses
                //project.dependencies.add JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, symbolLib
                LocalDependencyHelper.addAppClasses project, symbolLib, true
            }
        }

        // Creates a JAR with files from the source application that will be added verbatim to the patched APK.
        def packExtraAppResources = project.tasks.register(TaskNames.PACK_EXTRA_APP_RESOURCES, LazyZipTask) {
            it.description = "Packs extra resources of the source application."
            it.group = TASK_GROUP_NAME
            it.zip64 = true
            it.reproducibleFileOrder = true
            it.preserveFileTimestamps = false
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            it.entryCompression = ZipEntryCompression.STORED
            it.lazyArchiveFileName.set BuildDir.FILENAME_EXTRA_RESOURCES
            it.lazyDestinationDirectory.set project.layout.buildDirectory.dir(BuildDir.DIR_EXTRA_RESOURCES)
            it.dependsOn provideDecodedApp
            def decodedAppDir = provideDecodedApp.get().outputDir
            it.from(decodedAppDir.dir(ApkLib.DIR_UNKNOWN))
            it.from(decodedAppDir.dir(ApkLib.DIR_ORIGINAL_META_INF)) { CopySpec spec ->
                spec.into FileNames.META_INF
            }
            return
        }

        // Creates an AAR with components of the source application that will be used to build the patched APK.
        def packAppComponents = project.tasks.register(TaskNames.PACK_APP_COMPONENTS, LazyZipTask) {
            it.description = "Packs major components of the source application."
            it.group = TASK_GROUP_NAME
            it.zip64 = true
            it.reproducibleFileOrder = true
            it.preserveFileTimestamps = false
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            it.entryCompression = ZipEntryCompression.STORED
            it.lazyArchiveFileName.set BuildDir.FILENAME_COMPONENT_LIBRARY
            it.lazyDestinationDirectory.set project.layout.buildDirectory.dir(BuildDir.DIR_COMPONENT_LIBRARY)
            //it.dependsOn provideDecodedApp, packExtraAppResources
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
            */
            def decodedAppDir = provideDecodedApp.get().outputDir
            /*
            it.from(decodedAppDir) { CopySpec spec ->
                //spec.exclude ApkLib.FILE_APKTOOL_YML
                spec.include ApkLib.FILE_ANDROID_MANIFEST_XML
                //spec.exclude ApkLib.DIR_ORIGINAL + '/**'
                //spec.exclude ApkLib.DIR_LIB + '/**'
                spec.include ApkLib.DIR_LIBS + '/'
                spec.include ApkLib.DIR_RES + '/'
                spec.exclude ApkLib.FILE_PUBLIC_XML
                spec.include ApkLib.DIR_ASSETS + '/'
                spec.include ApkLib.DIR_KOTLIN + '/'
                //spec.exclude ApkLib.DIR_UNKNOWN + '/**'
            }
            */
            it.from(decodedAppDir) { CopySpec spec ->
                spec.exclude ApkLib.FILE_APKTOOL_YML
                //spec.include ApkLib.FILE_ANDROID_MANIFEST_XML
                spec.exclude ApkLib.DIR_ORIGINAL + '/'
                spec.exclude ApkLib.DIR_LIB + '/'
                //spec.include ApkLib.DIR_LIBS + '/'
                //spec.include ApkLib.DIR_RES + '/'
                spec.exclude ApkLib.FILE_PUBLIC_XML
                //spec.include ApkLib.DIR_ASSETS + '/'
                //spec.include ApkLib.DIR_KOTLIN + '/'
                spec.exclude ApkLib.DIR_UNKNOWN + '/'
                spec.exclude 'classes*.dex'
            }
            it.from(decodedAppDir.dir(ApkLib.DIR_LIB)) { CopySpec spec ->
                spec.into ComponentLib.DIR_JNI
            }
            it.from(packExtraAppResources)
            return
        }

        // Add the source app component library as an implementation dependency.
        // This library should be added after any patch libraries added by the user.
        // (But see: https://issuetracker.google.com/issues/130463010)
        project.afterEvaluate {
            def componentLib = project.files(packAppComponents)
            componentLib.builtBy packAppComponents
            //project.dependencies.add JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, componentLib
            LocalDependencyHelper.addAppComponents project, componentLib, true
        }

        // Android's resource merger build step ignores existing resource ID mappings ('public.xml' files),
        // so the ID mappings of the source app must be processed and added the the output of the merger.
        androidVariants.all { BaseVariant variant ->
            def mergeResources = VariantHelper.getMergeResources(variant)

            // Copy (AAPT1) or compile (AAPT2) the source app resource ID mapping file.
            def processIdMappings = project.tasks.register(
                    StringHelper.appendCapitalized(TaskNames.PROCESS_ID_MAPPINGS_PREFIX, variant.name),
                    ProcessIdMappingsTask) {
                it.description = "Compiles the resource ID mappings of the source application."
                it.group = TASK_GROUP_NAME
                it.dependsOn provideDecodedApp
                it.publicXmlFile.set provideDecodedApp.get().outputDir.file(ApkLib.FILE_PUBLIC_XML)
                it.processResources.set project.<Boolean>provider {
                    mergeResources.get().processResources && !isUsingAapt1(mergeResources.get())
                }
                it.outputDir.set project.layout.buildDirectory.dir(
                        BuildDir.DIR_RESOURCE_ID_MAPPINGS + '/' + variant.dirName)
                it.aapt2FromMaven.from {
                    mergeResources.get().aapt2FromMaven
                }
                it.androidBuilder.set project.<AndroidBuilder>provider {
                    //getAndroidBuilder(mergeResources.get())
                    VariantHelper.getData(variant).scope.globalScope.androidBuilder
                }
                return
            }
            VariantHelper.getAssemble(variant).configure {
                it.extensions.add TaskNames.PROCESS_ID_MAPPINGS_PREFIX, processIdMappings
                return
            }

            // Inject the processed ID mappings into the output of the resource merger task.
            mergeResources.configure {
                it.dependsOn processIdMappings
                it.doLast {
                    project.copy { CopySpec spec ->
                        spec.from processIdMappings
                        spec.into mergeResources.get().outputDir
                    }
                }
                return
            }
        }

        // Remove empty 'R.java' files generated from the source app component library.
        androidVariants.all { BaseVariant variant ->
            variant.outputs.all { BaseVariantOutput output ->
                VariantHelper.getProcessResources(output).configure {
                    it.doLast { task ->
                        removeEmptyRFiles it.sourceOutputDir
                    }
                }
            }
        }

    }

    private void removeEmptyRFiles(File sourceTreeDir) {
        def rFiles = project.fileTree(sourceTreeDir)
        rFiles.include '**/R.java'
        for (def rFile : rFiles) {
            if (isEmptyRFile(rFile)) {
                if (project.logger.debugEnabled) {
                    project.logger.debug("Removing empty R file '$rFile' containing:\n" + rFile.text)
                }
                project.delete rFile
            }
        }
    }

    private boolean isEmptyRFile(File rFile) {
        def reader = new BufferedReader(new FileReader(rFile))
        try {
            for (;;) {
                def line = reader.readLine()
                if (line.is(null)) break
                line = line.trim()
                def prefix = 'public static '
                if (line.startsWith(prefix)) {
                    String rest = line.substring(prefix.length())
                    if (rest.startsWith('class ') || rest.startsWith('final class ')) continue
                    return false
                }
            }
        } finally {
            reader.close()
        }
        return true
    }

    @CompileDynamic
    private boolean isUsingAapt1(MergeResources task) {
        // FIXME: What happens on Android Gradle plugins earlier than 3.2.0?
        try {
            return task.aaptGeneration == 'AAPT_V1'
        } catch (MissingPropertyException e) {
            return false
        }
    }

}

/*
        // Inject EXPLODED_AAR

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

/*
    // Android builder can also be accessed through global scope

    private static AndroidBuilder getAndroidBuilder(AndroidBuilderTask task) {
        def getBuilder = AndroidBuilderTask.class.getDeclaredMethod('getBuilder')
        getBuilder.setAccessible true
        return (AndroidBuilder) getBuilder.invoke(task)
    }
*/

/*
    // APK Library

    afterApply:
        // Setup 'apkLibrary' property.
        project.afterEvaluate {
            afterPrepareApkLibrary { PrepareLibraryTask task ->
                if (apkLibRootDirUnchecked.orNull) throw new RuntimeException('Multiple apk libraries found')
                apkLibRootDirUnchecked.set task.explodedDir
            }
        }

    protected void prepareApkLibraryDoLast(Closure closure) {
        project.tasks.withType(PrepareLibraryTask).each { task ->
            task.doLast {
                if (isPrepareApkLibraryTask(task)) {
                    closure.maximumNumberOfParameters ? closure(task) : closure()
                }
            }
        }
    }

    protected void afterPrepareApkLibrary(Closure closure) {
        project.gradle.taskGraph.afterTask { task ->
            if (task instanceof PrepareLibraryTask && task.project.is(project)) {
                if (isPrepareApkLibraryTask(task)) {
                    closure.maximumNumberOfParameters ? closure(task) : closure()
                }
            }
        }
    }

    private boolean isPrepareApkLibraryTask (PrepareLibraryTask task) {
        return Utils.getFile(task.explodedDir, 'dexpatcher').isDirectory()
    }

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
*/

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
