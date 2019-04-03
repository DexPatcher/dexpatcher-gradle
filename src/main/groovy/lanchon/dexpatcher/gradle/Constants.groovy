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

import groovy.transform.CompileStatic

@CompileStatic
abstract class Constants {

    static abstract class ExtensionNames {
        static final String DEXPATCHER_CONFIG = 'dexpatcherConfig'
        static final String TOOL_DEXPATCHER = 'dexpatcher'
        static final String TOOL_APKTOOL = 'apktool'
        static final String TOOL_DEX2JAR = 'dex2jar'
        static final String PLUGIN_APK_LIBRARY = 'apkLibrary'
        static final String PLUGIN_PATCHED_APPLICATION = 'patchedApplication'
        static final String PLUGIN_PATCH_LIBRARY = 'patchLibrary'
    }

    static abstract class ToolNames {
        static final String DEXPATCHER = 'dexpatcher'
        static final String APKTOOL = 'apktool'
        static final String DEX2JAR = 'dex2jar'
    }

    static final String TASK_GROUP_NAME = 'DexPatcher'

    static abstract class TaskNames {
        static final String DECODE_APK = 'decodeApk'
        static final String UNPACK_APK_LIBRARY = 'unpackApkLibrary'
        static final String PROVIDE_DECODED_APP = 'provideDecodedApp'
        static final String SOURCE_APP_INFO = 'sourceAppInfo'
        static final String CREATE_APK_LIBRARY = 'createApkLibrary'
        static final String DEDEX_APP_CLASSES = 'dedexAppClasses'
        static final String PACK_EXTRA_APP_RESOURCES = 'packExtraAppResources'
    }

    static abstract class ConfigurationNames {
        static final String SOURCE_APK = 'sourceApk'
        static final String SOURCE_APK_LIBRARY = 'sourceApkLibrary'
    }

    static abstract class FileNames {
        static final String LOCAL_PROPERTIES = 'local.properties'
        static final String META_INF = 'META-INF'
        static final String EXT_APK_LIBRARY = '.apklib'
        static final List<String> EXTS_SOURCE_APK = ['.apk', '.jar', '.zip'].asImmutable()
        static final List<String> EXTS_SOURCE_APP = (EXTS_SOURCE_APK + EXT_APK_LIBRARY).asImmutable()
    }

    static abstract class ProjectDir {
        static final String DIR_SOURCE_APK = 'apk'
        static final String DIR_SOURCE_APK_LIBRARY = 'apklib'
    }

    static abstract class BuildDir {
        // Intermediates:
        static final String DIR_INTERMEDIATES = 'intermediates/dexpatcher'
        static final String DIR_DECODED_APP = DIR_INTERMEDIATES + '/decoded-app'
        static final String DIR_APKTOOL_FRAMEWORK = DIR_INTERMEDIATES + '/apktool-framework'
        static final String FILE_DEDEXED_CLASSES = DIR_INTERMEDIATES + '/dedexed-classes/app-classes.jar'
        static final String FILE_DEX2JAR_EXCEPTIONS = DIR_INTERMEDIATES + '/dedexed-classes/dex2jar-exceptions.zip'
        static final String DIR_EXTRA_RESOURCES = DIR_INTERMEDIATES + '/extra-resources'
        static final String FILENAME_EXTRA_RESOURCES = ComponentLib.FILE_CLASSES_JAR
        // Outputs:
        static final String DIR_OUTPUTS = 'outputs'
        static final String DIR_APK_LIBRARY = DIR_OUTPUTS + '/apk-library'
        static final String FILENAME_APK_LIBRARY_DEFAULT_BASE = 'source'
    }

    static abstract class ApkLib {
        static final String FILE_APKTOOL_YML = 'apktool.yml'
        static final String DIR_ORIGINAL = 'original'
        static final String DIR_META_INF = DIR_ORIGINAL + '/' + FileNames.META_INF
        static final String DIR_UNKNOWN = 'unknown'
    }

    static abstract class ComponentLib {
        static final String FILE_CLASSES_JAR = 'classes.jar'
    }

}
