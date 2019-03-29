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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.extensions.PatchedAppExtension
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.google.common.collect.ImmutableSet
import org.gradle.api.Project
import org.gradle.api.Task

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class PatchedAppPlugin extends AbstractPatcherPlugin<PatchedAppExtension, AppExtension, ApplicationVariant> {

    @Override
    void apply(Project project) {

        super.apply(project)

        extension = (PatchedAppExtension) subextensions.create(
                EXT_PLUGIN_PATCHED_APPLICATION, PatchedAppExtension, project, dexpatcherConfig)

        project.plugins.apply(AppPlugin)
        androidExtension = project.extensions.getByType(AppExtension)
        androidVariants = androidExtension.applicationVariants

        afterApply()

    }

    @Override
    protected void afterApply() {

        super.afterApply()

//        dexpatcherConfig.addLibDependencies(true)

        /*
        project.afterEvaluate {
            androidVariants.all { ApplicationVariant variant ->
                def patchDexTask = createPatchDexTask(variant)
                if (variant.buildType.debuggable) {
                    if (extension.multiDexThreadedForAllDebugBuilds) {
                        patchDexTask.multiDex.set true
                        patchDexTask.multiDexThreaded.set true
                    } else if (extension.multiDexThreadedForMultiDexDebugBuilds) {
                        patchDexTask.multiDexThreaded.set true
                    }
                }
            }
        }
        */

    }

    private DexpatcherTask createPatchDexTask(ApplicationVariant variant) {

        def patch = project.layout.directoryProperty()
        def patchDex = project.tasks.create("patch${variant.name.capitalize()}Dex", DexpatcherTask)
        patchDex.with {
            description = "Patches the source dex from an apk library using the just-built patch dex."
            group = GROUP_DEXPATCHER
            source.set apkLibrary.dexDir
            patch.set patch
            outputDir.set dexpatcherDir.dir("patched-dex/${variant.dirName}")
        }

        variant.outputs.each {
            if (it instanceof ApkVariantOutput) {

                //def output = (ApkVariantOutput) it
                //def packageApp = output.packageApplication
                def packageApp = getPackageTask(it);

                patchDex.mustRunAfter { packageApp.dependsOn - patchDex }
                packageApp.dependsOn patchDex

                beforeTask(patchDex) {

                    //def dexFolders = packageApp.dexFolders
                    def dexFolders = getPackageTaskDexFolders(packageApp)

                    if (dexFolders.is(null)) throw new RuntimeException(
                            "Output of variant '${variant.name}' has null dex folders")
                    if (dexFolders.empty) throw new RuntimeException(
                            "Output of variant '${variant.name}' has no dex folders")
                    if (dexFolders.size() > 1) throw new RuntimeException(
                            "Output of variant '${variant.name}' has multiple dex folders")

                    def dexFolder = dexFolders[0]
                    if (dexFolder.is(null)) throw new RuntimeException(
                            "Output of variant '${variant.name}' has null dex folder")

                    def oldPatch = patch.orNull
                    patch.set dexFolder
                    if (oldPatch && oldPatch != patch.orNull) throw new RuntimeException(
                        "Outputs of variant '${variant.name}' do not share dex folders")

                    Set<File> patchedDexFolders = ImmutableSet.of(patchDex.outputDir.get().asFile)
                    //packageApp.dexFolders = patchedDexFolders
                    setPackageTaskDexFolders(packageApp, patchedDexFolders)

                }

            }
        }

        variant.assemble.extensions.add 'patchDex', patchDex
        return patchDex

    }

    @CompileDynamic
    private static Task getPackageTask(def apkVariantOutput) {
        // The type of 'apkVariantOutput.packageApplication' is:
        //  -> 'PackageApplication' in Android plugin up to v2.1.3.
        //  -> 'PackageAndroidArtifact' in Android plugin v2.2.0 and higher.
        return apkVariantOutput.packageApplication
    }

    @CompileDynamic
    private static Set<File> getPackageTaskDexFolders(def packageTask) {
        return packageTask.dexFolders
    }

    //@CompileDynamic
    private static void setPackageTaskDexFolders(def packageTask, Set<File> dexFolders) {
        // 'packageApp.dexFolders' is not writable in Android plugin v2.1.3 and earlier.
        //packageTask.dexFolders = dexFolders
        // The class of 'packageTask' is subclassed by some presumably run-time mechanism.
        //def dexFoldersField = packageTask.getClass().getDeclaredField('dexFolders')
        def dexFoldersField
        def theClass = packageTask.getClass()
        for (;;) {
            try {
                dexFoldersField = theClass.getDeclaredField('dexFolders')
                break
            } catch (NoSuchFieldException e) {
                theClass = theClass.superclass
                if (!theClass) throw e
            }
        }
        dexFoldersField.accessible = true
        dexFoldersField.set(packageTask, dexFolders)
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

}
