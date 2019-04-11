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
import lanchon.dexpatcher.gradle.VariantHelper
import lanchon.dexpatcher.gradle.extensions.PatchedAppExtension
import lanchon.dexpatcher.gradle.tasks.CollectDexTask
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.utils.StringHelper
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskProvider

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class PatchedAppPlugin extends AbstractPatcherPlugin<PatchedAppExtension, AppExtension, ApplicationVariant> {

    @Override
    void apply(Project project) {

        super.apply(project)

        extension = (PatchedAppExtension) subextensions.create(
                ExtensionNames.PLUGIN_PATCHED_APPLICATION, PatchedAppExtension, project, dexpatcherConfig)

        project.plugins.apply(AppPlugin)
        androidExtension = project.extensions.getByType(AppExtension)
        androidVariants = androidExtension.applicationVariants

        afterApply()

    }

    @Override
    protected void afterApply() {

        super.afterApply()

        // Patch the bytecode of the source application.
        // FIXME: Make this work on Android Gradle plugin 3.2.0.
        androidVariants.all { ApplicationVariant variant ->
            def packageApplication = VariantHelper.getPackageApplication(variant)

            def collectDex = project.tasks.register(
                    StringHelper.appendCapitalized(TaskNames.COLLECT_DEX_PREFIX, variant.name),
                    CollectDexTask) {

                // Find the dex output folders of the variant and their builder tasks.
                def pack = packageApplication.get()
                def dexFolders = pack.dexFolders.files
                def dexBuilders = pack.dexFolders.buildDependencies.getDependencies(pack)
                /*
                // FIXME: Make this work with option 'android.enableDexingArtifactTransform=false'.
                if (dexFolders.size() == 0) {
                    dexFolders = new HashSet<File>()
                    dexBuilders.findAll { it instanceof StreamBasedTask }.collect(dexFolders) {
                        ((StreamBasedTask) it).streamOutputFolder
                    }
                }
                */
                if (dexFolders.size() == 0) throw new RuntimeException("Dex folders not found")
                if (dexBuilders.size() == 0) throw new RuntimeException("Dex builder tasks not found")
                if (project.logger.isEnabled(LogLevel.DEBUG)) {
                    //def builderMap = new LinkedHashMap<String, String>()
                    //dexBuilders.each { builderMap.put(it.name, it.class.canonicalName) }
                    project.logger.debug("Dex builder tasks in variant '${variant.name}': " + dexBuilders)
                }

                // Copy the dex files from the output folders into a single multi-dex container.
                it.description = "Collects and combines the bytecode of the patch."
                it.group = TASK_GROUP_NAME
                it.dependsOn dexBuilders
                it.inputDirs.from dexFolders
                it.outputDir.set project.layout.buildDirectory.dir(BuildDir.DIR_PATCH_DEX + '/' + variant.dirName)
                return
            }
            VariantHelper.getAssemble(variant).configure {
                it.extensions.add TaskNames.COLLECT_DEX_PREFIX, collectDex
                return
            }

            def patchDex = project.tasks.register(
                    StringHelper.appendCapitalized(TaskNames.PATCH_DEX_PREFIX, variant.name),
                    DexpatcherTask) {

                // Perform sanity checks.
                def pack = packageApplication.get()
                if (pack.inInstantRunMode) {
                    throw new RuntimeException("Instant Run is not supported, please disable it")
                }
                collectDex.get()       // collectDex must configure first
                // FIXME: Investigate featureDexFolder.
                if (pack.featureDexFolder) throw new RuntimeException("Feature dex folder not supported")

                // Patch the bytecode of the source application using the collected dex files.
                it.description = "Patches the bytecode of the source application."
                it.group = TASK_GROUP_NAME
                it.source.set project.layout.buildDirectory.dir(BuildDir.DIR_DECODED_APP)
                it.outputDir.set project.layout.buildDirectory.dir(BuildDir.DIR_PATCHED_DEX + '/' + variant.dirName)

                // Skip the collect task if the dex merger tasks produce a single output folder.
                def dexFolders = pack.dexFolders.files
                if (dexFolders.size() != 1) {
                    it.dependsOn collectDex
                    it.patch.set collectDex.get().outputDir
                } else {
                    it.dependsOn collectDex.get().dependsOn
                    it.patch.set Utils.getDirectory(project, dexFolders[0])
                }
                return
            }
            VariantHelper.getAssemble(variant).configure {
                it.extensions.add TaskNames.PATCH_DEX_PREFIX, patchDex
                return
            }

            // Build the APK using the patched bytecode.
            packageApplication.configure {
                patchDex.get()      // patchDex must configure first
                ((ConfigurableFileCollection) it.dexFolders).setFrom patchDex
                return
            }
        }

        project.afterEvaluate {
            androidVariants.all { ApplicationVariant variant ->
                VariantHelper.getPackageApplication(variant).configure { pack ->
                    def patch = ((TaskProvider<DexpatcherTask>) VariantHelper.getAssemble(variant).get().
                            extensions.getByName(TaskNames.PATCH_DEX_PREFIX)).get()

                    // Configure multi-dex of debuggable variants.
                    if (variant.buildType.debuggable) {
                        if (extension.multiDexThreadedForAllDebugBuilds.get()) {
                            patch.multiDex.set true
                            patch.multiDexThreaded.set true
                        } else if (extension.multiDexThreadedForMultiDexDebugBuilds.get()) {
                            patch.multiDexThreaded.set true
                        }
                    }

                    // Warn if multi-dex is enabled and target requires legacy multi-dex.
                    if (patch.multiDex.get() && pack.minSdkVersion < NATIVE_MULTI_DEX_MIN_API_LEVEL) {
                        project.logger.warn("Variant '${variant.name}': Legacy multi-dex is not supported, " +
                                "please increase minSdkVersion to $NATIVE_MULTI_DEX_MIN_API_LEVEL or disable multi-dex")
                    }
                }
            }
        }

    }

}

/*
    // Extract data from DexMergingTask instances

                dexBuilders.each {
                    if (!(it instanceof DexMergingTask)) {
                        throw new RuntimeException("Unexpected type of dex builder $it: ${it.class.canonicalName}")
                    }
                }
                def dexMergers = dexBuilders as List<DexMergingTask>

                def dexingType = coalesce(dexMergers, 'DexMergingTask.dexingType')
                        { task -> task.dexingType }
                def dexMerger = coalesce(dexMergers, 'DexMergingTask.dexMerger')
                        { task -> task.dexMerger }
                def minSdkVersion = coalesce(dexMergers, 'DexMergingTask.minSdkVersion')
                        { task -> task.minSdkVersion }
                //def isDebuggable = coalesce(dexMergers, 'DexMergingTask.isDebuggable')
                //        { task -> task.isDebuggable }
                def messageReceiver = coalesce(dexMergers, 'DexMergingTask.messageReceiver')
                        { task -> task.messageReceiver }


    private static <I, V> V coalesce(Iterable<I> items, String property,
            @ClosureParams(FirstParam.FirstGenericType) Closure<V> transform) {
        def iterator = items.iterator()
        def value = transform(iterator.next())
        while (iterator.hasNext()) {
            def other = transform(iterator.next())
            if (value != other) throw new RuntimeException("Cannot coalesce property '$property'")
        }
        return value
    }
*/

/*
    // BYPASS PROCESSING

    // Code

    @Override
    protected void processCode(BaseVariant variant) {
        super.processCode(variant)
        def appVariant = (ApplicationVariant) variant
        createPatchDexTask(appVariant)
    }

    @Override
    protected void bypassCode(BaseVariant variant) {
        super.bypassCode(variant)
        def appVariant = (ApplicationVariant) variant
        project.tasks.findByName("preDex${appVariant.name.capitalize()}")?.setEnabled(false)
        appVariant.dex?.setEnabled(false)
        createImportDexTask(appVariant)
    }

    private Sync createImportDexTask(ApplicationVariant variant) {
        Task dexCreator = variant.dex ?: variant.javaCompiler
        File dexDir = variant.dex ? variant.dex.outputFolder : variant.javaCompiler.destinationDir
        def importDex = project.tasks.create("import${variant.name.capitalize()}Dex", Sync)
        importDex.with {
            description = "Imports the dex file(s) from an apk library."
            group = DexpatcherBasePlugin.TASK_GROUP
            dependsOn dexCreator
            into dexDir
        }
        afterPrepareDependencies(variant) { File libDir ->
            importDex.from Resolver.getFile(libDir, 'dexpatcher/dex')
        }
        variant.outputs.each {
            if (it instanceof ApkVariantOutput) {
                it.packageApplication.dependsOn importDex
            }
        }
        variant.assemble.extensions.add 'importDex', importDex
        return importDex
    }
*/
