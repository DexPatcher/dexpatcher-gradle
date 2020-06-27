/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.plugins

import groovy.transform.CompileStatic

import lanchon.dexpatcher.gradle.extensions.PatchedAppExtension
import lanchon.dexpatcher.gradle.helpers.AndroidPluginHelperInitializer
import lanchon.dexpatcher.gradle.helpers.VariantHelper
import lanchon.dexpatcher.gradle.tasks.CollectDexTask
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.dexing.DexingType
import com.android.utils.StringHelper
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.tasks.TaskProvider

import static lanchon.dexpatcher.gradle.Constants.*

@CompileStatic
class PatchedAppPlugin extends AbstractPatcherPlugin<PatchedAppExtension, AppExtension, ApplicationVariant> {

    @Override
    void apply(Project project) {

        super.apply(project)

        extension = (PatchedAppExtension) basePlugin.createSubextension(ExtensionNames.PLUGIN_PATCHED_APPLICATION,
                PatchedAppExtension)
        project.plugins.apply(AppPlugin)
        AndroidPluginHelperInitializer.init()
        androidExtension = project.extensions.getByType(AppExtension)
        androidVariants = androidExtension.applicationVariants

        afterApply()

    }

    @Override
    protected void afterApply() {

        super.afterApply()

        // Patch the bytecode of the source application.
        androidVariants.all { ApplicationVariant variant ->
            def packageApplication = variant.packageApplicationProvider

            def collectDex = project.tasks.register(
                    StringHelper.appendCapitalized(TaskNames.COLLECT_DEX_PREFIX, variant.name),
                    CollectDexTask)
            def patchDex = project.tasks.register(
                    StringHelper.appendCapitalized(TaskNames.PATCH_DEX_PREFIX, variant.name),
                    DexpatcherTask)
            def prePackage = project.tasks.register(
                    StringHelper.appendCapitalized(TaskNames.PRE_PACKAGE_PREFIX, variant.name))
            variant.assembleProvider.configure {
                it.extensions.add TaskNames.COLLECT_DEX_PREFIX, collectDex
                it.extensions.add TaskNames.PATCH_DEX_PREFIX, patchDex
                it.extensions.add TaskNames.PRE_PACKAGE_PREFIX, prePackage
                return
            }

            // Copy the dex files from the output folders into a single multi-dex container.
            collectDex.configure {
                it.description = "Collects and combines the bytecode of the patch."
                it.group = TASK_GROUP_NAME
                it.dependsOn {
                    def pack = packageApplication.get()
                    def dexBuilders = []
                    dexBuilders.addAll pack.dexFolders.buildDependencies.getDependencies(pack)
                    dexBuilders.addAll pack.dependsOn
                    dexBuilders.remove prePackage
                    if (dexBuilders.size() == 0) throw new RuntimeException("Dex builder tasks not found")
                    if (project.logger.debugEnabled) {
                        project.logger.debug "Dex builder tasks in variant '${variant.name}': " + dexBuilders
                    }
                    return dexBuilders
                }
                it.inputDirs.from {
                    packageApplication.get().dexFolders
                }
                it.outputDir.set project.layout.buildDirectory.dir(BuildDir.DIR_PATCH_DEX + '/' + variant.dirName)
                return
            }

            // Patch the bytecode of the source application using the collected dex files.
            patchDex.configure {
                it.description = "Patches the bytecode of the source application."
                it.group = TASK_GROUP_NAME
                it.source.set project.layout.buildDirectory.dir(BuildDir.DIR_DECODED_APP)
                it.outputDir.set project.layout.buildDirectory.dir(BuildDir.DIR_PATCHED_DEX + '/' + variant.dirName)
                it.dependsOn collectDex
                it.patch.set project.<Directory>provider {
                    collectDex.get().outputDir.get()
                }
                it.doFirst {
                    // Perform sanity checks.
                    def pack = packageApplication.get()
                    if (pack.inInstantRunMode) {
                        throw new RuntimeException("Instant Run is not supported: please disable it")
                    }
                    def featureDex = pack.featureDexFolder
                    if (featureDex && !featureDex.empty) {
                        throw new RuntimeException("Feature splits are not supported")
                    }
                }
                return
            }

            // Configure the package task to build the variant using the patched bytecode.
            prePackage.configure {
                it.description = "Prepares to package the patched application."
                it.group = TASK_GROUP_NAME
                it.dependsOn patchDex
                it.doLast {
                    def pack = packageApplication.get()
                    def dexFolders = (ConfigurableFileCollection) pack.dexFolders
                    dexFolders.setFrom patchDex
                }
                return
            }

            packageApplication.configure {
                it.dependsOn prePackage
                return
            }
        }

        project.afterEvaluate {
            androidVariants.all { ApplicationVariant variant ->
                variant.packageApplicationProvider.configure { pack ->
                    def patch = ((TaskProvider<DexpatcherTask>) variant.assembleProvider.get().
                            extensions.getByName(TaskNames.PATCH_DEX_PREFIX)).get()

                    // Configure multi-dex of debuggable variants.
                    if (variant.buildType.debuggable) {
                        if (((PatchedAppExtension) extension).multiDexThreadedForAllDebugBuilds.get()) {
                            patch.multiDex.set true
                            patch.multiDexThreaded.set true
                        } else if (((PatchedAppExtension) extension).multiDexThreadedForMultiDexDebugBuilds.get()) {
                            patch.multiDexThreaded.set true
                        }
                    }

                    def dexingType = VariantHelper.getData(variant).scope.dexingType
                    def legacyMultiDexMessage = "Legacy multi-dex is not supported by DexPatcher: " +
                            "please increase minSdkVersion to $NATIVE_MULTI_DEX_MIN_API_LEVEL or disable multi-dex"

                    // Automatically configure multi-dex if needed.
                    if (!patch.multiDex.get()) {
                        if (dexingType != DexingType.MONO_DEX) {
                            if (dexingType == DexingType.LEGACY_MULTIDEX) {
                                throw new RuntimeException(legacyMultiDexMessage + '\n' +
                                        '(Manually enable DexPatcher multi-dex patching mode to override this check)')
                            }
                            project.logger.info "Variant '${variant.name}': " +
                                    'Automatically enabling DexPatcher multi-dex patching mode'
                            patch.multiDex.set true
                        }
                    }

                    // Warn if multi-dex is enabled and target requires legacy multi-dex.
                    if (patch.multiDex.get() && (pack.minSdkVersion < NATIVE_MULTI_DEX_MIN_API_LEVEL ||
                            dexingType == DexingType.LEGACY_MULTIDEX)) {
                        project.logger.warn "Variant '${variant.name}': " + legacyMultiDexMessage + '\n' +
                                '(The application being built will not run on Android versions prior to 5.0)'
                    }
                }
            }
        }

    }

}
