/*
 * DexPatcher - Copyright 2015-2020 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic

import com.android.build.gradle.internal.res.Aapt2MavenUtils

@CompileStatic
abstract class Aapt2MavenUtilsHelper {

    public static final String AAPT2_CONFIG_NAME

    static {
        def field
        try {
            field = Aapt2MavenUtils.class.getDeclaredField('AAPT2_CONFIG_NAME')
            field.accessible = true
        } catch (Exception e) {
            throw new Error("Cannot link field 'Aapt2MavenUtils.AAPT2_CONFIG_NAME'", e)
        }
        try {
            AAPT2_CONFIG_NAME = field.get(null)
        } catch (Exception e) {
            throw new Error("Cannot get field 'Aapt2MavenUtils.AAPT2_CONFIG_NAME'", e)
        }
    }

    public static final String TYPE_EXTRACTED_AAPT2_BINARY

    static {
        def field
        try {
            field = Aapt2MavenUtils.class.getDeclaredField('TYPE_EXTRACTED_AAPT2_BINARY')
            field.accessible = true
        } catch (Exception e) {
            throw new Error("Cannot link field 'Aapt2MavenUtils.TYPE_EXTRACTED_AAPT2_BINARY'", e)
        }
        try {
            TYPE_EXTRACTED_AAPT2_BINARY = field.get(null)
        } catch (Exception e) {
            throw new Error("Cannot get field 'Aapt2MavenUtils.TYPE_EXTRACTED_AAPT2_BINARY'", e)
        }
    }

}
