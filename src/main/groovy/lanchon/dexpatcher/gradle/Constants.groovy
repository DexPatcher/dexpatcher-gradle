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

    static final String CONFIG_SOURCE_APK = 'sourceApk'
    static final String CONFIG_SOURCE_APK_LIBRARY = 'sourceApkLibrary'

    static final String DIR_SOURCE_APK = 'apk'
    static final String DIR_SOURCE_APK_LIBRARY = 'apklib'

    static final String TOOL_DEXPATCHER = 'dexpatcher'
    static final String TOOL_APKTOOL = 'apktool'
    static final String TOOL_DEX2JAR = 'dex2jar'

    static final String EXT_DEXPATCHER_CONFIG = 'dexpatcherConfig'
    static final String EXT_TOOL_DEXPATCHER = TOOL_DEXPATCHER
    static final String EXT_TOOL_APKTOOL = TOOL_APKTOOL
    static final String EXT_TOOL_DEX2JAR = TOOL_DEX2JAR
    static final String EXT_PLUGIN_APK_LIBRARY = 'apkLibrary'
    static final String EXT_PLUGIN_PATCHED_APPLICATION = 'patchedApplication'
    static final String EXT_PLUGIN_PATCH_LIBRARY = 'patchLibrary'

    static final String TASK_GROUP_DEXPATCHER = 'DexPatcher'

    static final String TASK_DECODE_SOURCE_APK = 'decodeSourceApk'
    static final String TASK_UNPACK_SOURCE_APK_LIBRARY = 'unpackSourceApkLibrary'
    static final String TASK_DECODED_SOURCE_APP = 'decodedSourceApp'
    static final String TASK_SOURCE_APP_INFO = 'sourceAppInfo'

    static final String DIR_INTERMEDIATES = 'intermediates/dexpatcher'
    static final String DIR_DECODED_SOURCE_APP = DIR_INTERMEDIATES + '/decoded-source-app'
    static final String DIR_APKTOOL_FRAMEWORK = DIR_INTERMEDIATES + '/apktool-framework'

    static final String FILE_APKTOOL_YML = 'apktool.yml'

}
