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

import com.android.SdkConstants

@CompileStatic
enum Platform {

    LINUX('linux', ''),
    MACOSX('macosx', ''),
    WINDOWS('windows', '.exe'),

    private static final Platform current = getPlatform()

    private static Platform getPlatform() {
        try {
            return getPlatformFromAndroidPlugin()
        } catch (Throwable t) {
            return getPlatformFromSystemProperty()
        }
    }

    private static Platform getPlatformFromAndroidPlugin() {
        switch (SdkConstants.CURRENT_PLATFORM) {
            case SdkConstants.PLATFORM_LINUX: return LINUX
            case SdkConstants.PLATFORM_DARWIN: return MACOSX
            case SdkConstants.PLATFORM_WINDOWS: return WINDOWS
            default: return null
        }
    }

    private static Platform getPlatformFromSystemProperty() {
        String osName = System.getProperty('os.name').toLowerCase(Locale.ROOT)
        if (osName.startsWith('linux')) return LINUX
        if (osName.startsWith('mac os')) return MACOSX
        if (osName.startsWith('windows')) return WINDOWS
        return null
    }

    static Platform getCurrent() {
        return current ?: throwUnsupportedPlatform()
    }
    static Platform getCurrentOrNull() {
        return current
    }

    private static Platform throwUnsupportedPlatform() {
        throw new RuntimeException('Unsupported platform')
    }

    final String binaryDirectoryName
    final String executableExtension

    Platform(String binaryDirectoryName, String executableExtension) {
        this.binaryDirectoryName = binaryDirectoryName
        this.executableExtension = executableExtension
    }

}
