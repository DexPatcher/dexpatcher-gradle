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
import lanchon.dexpatcher.gradle.VariantHelper
import lanchon.dexpatcher.gradle.extensions.AbstractPatcherExtension
import lanchon.dexpatcher.gradle.tasks.Dex2jarTask
import lanchon.dexpatcher.gradle.tasks.LazyZipTask
import lanchon.dexpatcher.gradle.tasks.ProcessIdMappingsTask

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.options.BooleanOption
import com.android.build.gradle.options.StringOption
import com.android.build.gradle.tasks.MergeResources
import com.android.builder.core.AndroidBuilder
import com.android.ide.common.resources.FileResourceNameValidator
import com.android.resources.ResourceFolderType
import com.android.utils.StringHelper
import com.google.common.base.Strings
import org.gradle.api.DomainObjectSet
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCopyDetails
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

        def dexpatcherAnnotationCfg = project.configurations.maybeCreate(ConfigurationNames.DEXPATCHER_ANNOTATION)
        dexpatcherAnnotationCfg.description = 'DexPatcher tool annotation dependency.'
        dexpatcherAnnotationCfg.canBeResolved = true
        dexpatcherAnnotationCfg.canBeConsumed = false
        basePlugin.setupConfigurationOverride dexpatcherAnnotationCfg

        (extension as AbstractPatcherExtension).dexpatcherAnnotationClasspath.from {
            dexpatcherAnnotationCfg.resolve().empty ?
                    basePlugin.dexpatcher.bundledAnnotationFile.get().asFile :
                    dexpatcherAnnotationCfg.singleFile
        }

        def decorateDependencies = basePlugin.dexpatcherConfig.properties.decorateDependencies

        // Add the DexPatcher annotations as a compile-only dependency.
        def annotationClasspath = (extension as AbstractPatcherExtension).dexpatcherAnnotationClasspath
        //project.dependencies.add JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, annotationClasspath
        LocalDependencyHelper.addDexpatcherAnnotations project, annotationClasspath, decorateDependencies && false

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
            if ((extension as AbstractPatcherExtension).importSymbols.get()) {
                def symbolLib = project.files(dedexAppClasses.get().outputFile)
                symbolLib.builtBy dedexAppClasses
                //project.dependencies.add JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, symbolLib
                LocalDependencyHelper.addAppClasses project, symbolLib, decorateDependencies
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
        def checkInvalidResources = new boolean[1]
        def invalidResourcesFound = new boolean[1]
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
            it.dependsOn provideDecodedApp
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
                spec.exclude ApkLib.DIR_RES + '/'
                //spec.include ApkLib.DIR_ASSETS + '/'
                //spec.include ApkLib.DIR_KOTLIN + '/'
                spec.exclude ApkLib.DIR_UNKNOWN + '/'
                spec.exclude 'classes*.dex'
            }
            it.from(decodedAppDir.dir(ApkLib.DIR_RES)) { CopySpec spec ->
                spec.exclude FileNames.VALUES_PUBLIC_XML
                spec.eachFile { FileCopyDetails details ->
                    if (checkInvalidResources[0]) {
                        def path = details.relativeSourcePath;
                        def pathSegments = path.segments;
                        if (pathSegments.length == 2) {
                            def type = ResourceFolderType.getFolderType(pathSegments[0])
                            def name = pathSegments[1]
                            def errorText = FileResourceNameValidator.getErrorTextForFileResource(name, type);
                            if (errorText) {
                                // For resource removal to work, references must also be removed from 'public.xml'.
                                //println "Removing invalid resource '$path': $errorText"
                                //details.exclude()
                                project.logger.warn "Invalid resource '$path': $errorText"
                                invalidResourcesFound[0] = true
                            }
                        }
                    }
                }
                spec.into ComponentLib.DIR_RES
            }
            it.from(decodedAppDir.dir(ApkLib.DIR_LIB)) { CopySpec spec ->
                spec.into ComponentLib.DIR_JNI
            }
            it.from(packExtraAppResources)
            it.doFirst {
                boolean checkResources = true
                if (!androidVariants.empty) {
                    def variant = androidVariants[0]
                    def options = VariantHelper.getData(variant).scope.globalScope.projectOptions
                    if (options.get(BooleanOption.DISABLE_RESOURCE_VALIDATION) &&
                            !Strings.isNullOrEmpty(options.get(StringOption.AAPT2_FROM_MAVEN_OVERRIDE))) {
                        checkResources = false
                    }
                }
                checkInvalidResources[0] = checkResources
                invalidResourcesFound[0] = false
            }
            it.doLast {
                if (invalidResourcesFound[0]) {
                    project.logger.warn "The source application contains invalid resources: please disable " +
                            "resource validation ('android.disableResourceValidation=true' in gradle.properties) " +
                            "and use a patched AAPT2 binary ('android.aapt2FromMavenOverride=path/to/aapt2' " +
                            "in gradle.properties) to work around the issue"
                }
            }
            return
        }

        // Add the source app component library as an implementation dependency.
        // This library should be added after any patch libraries added by the user.
        // (But see: https://issuetracker.google.com/issues/130463010)
        project.afterEvaluate {
            def componentLib = project.files(packAppComponents)
            componentLib.builtBy packAppComponents
            //project.dependencies.add JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, componentLib
            LocalDependencyHelper.addAppComponents project, componentLib, decorateDependencies
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
                it.outputDir.set project.layout.buildDirectory.dir(
                        BuildDir.DIR_RESOURCE_ID_MAPPINGS + '/' + variant.dirName)
                it.aapt2FromMaven.from {
                    mergeResources.get().aapt2FromMaven
                }
                it.androidBuilder.set project.<AndroidBuilder>provider {
                    //getAndroidBuilder(mergeResources.get())
                    VariantHelper.getData(variant).scope.globalScope.androidBuilder
                }
                it.processResources.set project.<Boolean>provider {
                    mergeResources.get().processResources && !isUsingAapt1(mergeResources.get())
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
                    project.logger.debug "Removing empty R file '$rFile' containing:\n" + rFile.text
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
        // NOFIX: For Android Gradle < 3.2.0: What happens here?
        try {
            return task.aaptGeneration == 'AAPT_V1'
        } catch (MissingPropertyException e) {
            return false
        }
    }

}
