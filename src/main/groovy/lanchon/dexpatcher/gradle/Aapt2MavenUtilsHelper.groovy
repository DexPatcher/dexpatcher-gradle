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

    private static final String AAPT2_CONFIG_NAME_VALUE
    private static final Exception AAPT2_CONFIG_NAME_EXCEPTION

    static {
        try {
            def field = Aapt2MavenUtils.class.getDeclaredField('AAPT2_CONFIG_NAME')
            field.accessible = true
            AAPT2_CONFIG_NAME_VALUE = field.get(null)
            AAPT2_CONFIG_NAME_EXCEPTION = null
        } catch (Exception e) {
            AAPT2_CONFIG_NAME_VALUE = null
            AAPT2_CONFIG_NAME_EXCEPTION = e
        }
    }

    static String get_AAPT2_CONFIG_NAME() {
        try {
            if (AAPT2_CONFIG_NAME_EXCEPTION) throw AAPT2_CONFIG_NAME_EXCEPTION
            return AAPT2_CONFIG_NAME_VALUE
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field 'Aapt2MavenUtils.AAPT2_CONFIG_NAME'", e)
        }
    }

    private static final String TYPE_EXTRACTED_AAPT2_BINARY_VALUE
    private static final Exception TYPE_EXTRACTED_AAPT2_BINARY_EXCEPTION

    static {
        try {
            def field = Aapt2MavenUtils.class.getDeclaredField('TYPE_EXTRACTED_AAPT2_BINARY')
            field.accessible = true
            TYPE_EXTRACTED_AAPT2_BINARY_VALUE = field.get(null)
            TYPE_EXTRACTED_AAPT2_BINARY_EXCEPTION = null
        } catch (Exception e) {
            TYPE_EXTRACTED_AAPT2_BINARY_VALUE = null
            TYPE_EXTRACTED_AAPT2_BINARY_EXCEPTION = e
        }
    }

    static String get_TYPE_EXTRACTED_AAPT2_BINARY() {
        try {
            if (TYPE_EXTRACTED_AAPT2_BINARY_EXCEPTION) throw TYPE_EXTRACTED_AAPT2_BINARY_EXCEPTION
            return TYPE_EXTRACTED_AAPT2_BINARY_VALUE
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field 'Aapt2MavenUtils.TYPE_EXTRACTED_AAPT2_BINARY'", e)
        }
    }

}
