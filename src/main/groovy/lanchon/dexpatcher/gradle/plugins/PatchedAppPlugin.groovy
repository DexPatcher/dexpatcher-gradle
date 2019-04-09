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
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.utils.StringHelper
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection

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
        androidVariants.all { ApplicationVariant variant ->
            def packageApplication = VariantHelper.getPackageApplication(variant)

            def patchDex = project.tasks.register(StringHelper.appendCapitalized(
                    TaskNames.PATCH_DEX_PREFIX, variant.name, TaskNames.PATCH_DEX_SUFFIX),
                    DexpatcherTask) {
                def error = { msg ->
                    throw new RuntimeException("Variant '${variant.name}': $msg")
                }
                def pack = packageApplication.get()
                if (pack.inInstantRunMode) error("Instant Run is not supported, please disable it")

                // Find the variant's main dex output and its builder task(s).
                def dexFolders = pack.dexFolders.files
                if (dexFolders.size() == 0) error("Dex folder not found")
                if (dexFolders.size() != 1) error("Multiple dex folders found")
                def dexFolder = dexFolders[0]
                if (pack.featureDexFolder) error("Feature dex folders not supported")
                def dexBuilders = pack.dexFolders.buildDependencies.getDependencies(pack)
                        //.findAll { it instanceof DexMergingTask }
                if (dexBuilders.size() == 0) error("Main dex builder task not found")
                //if (dexBuilders.size() != 1) error("Multiple dex builder tasks found")
                //Task dexBuilder = dexBuilders[0]

                // Patch the bytecode of the source application using the main dex output as the patch.
                it.description = "Patches the Dalvik bytecode of the source application."
                it.group = TASK_GROUP_NAME
                it.dependsOn dexBuilders
                it.source.set project.layout.buildDirectory.dir(BuildDir.DIR_DECODED_APP)
                it.patch.set Utils.getDirectory(project, dexFolder)
                it.outputDir.set project.layout.buildDirectory.dir(BuildDir.DIR_PATCHED_DEX + '/' + variant.dirName)
                return
            }
            VariantHelper.getAssemble(variant).configure {
                it.extensions.add TaskNames.PATCH_DEX_TAG, patchDex
                return
            }

            // Build the APK using the patched bytecode.
            packageApplication.configure {
                patchDex.get()      // patchDex must configure first
                ((ConfigurableFileCollection) it.dexFolders).setFrom patchDex
                return
            }
        }

    }

}

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
