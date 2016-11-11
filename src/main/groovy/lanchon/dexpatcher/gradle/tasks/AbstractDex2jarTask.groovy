/*
 * DexPatcher - Copyright 2015, 2016 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractDex2jarTask extends AbstractJavaExecTask {

    static final String MAIN_DEX2JAR                 = 'com.googlecode.dex2jar.tools.Dex2jarCmd'
    static final String MAIN_JAR2DEX                 = 'com.googlecode.dex2jar.tools.Jar2Dex'
    static final String MAIN_JAR2JASMIN              = 'com.googlecode.d2j.jasmin.Jar2JasminCmd'
    static final String MAIN_JASMIN2JAR              = 'com.googlecode.d2j.jasmin.Jasmin2JarCmd'
    static final String MAIN_SMALI                   = 'com.googlecode.d2j.smali.SmaliCmd'
    static final String MAIN_BAKSMALI                = 'com.googlecode.d2j.smali.BaksmaliCmd'
    static final String MAIN_DEX2SMALI               = 'com.googlecode.d2j.smali.BaksmaliCmd'
    static final String MAIN_DEX_RECOMPUTE_CHECKSUM  = 'com.googlecode.dex2jar.tools.DexRecomputeChecksum'
    static final String MAIN_STD_APK                 = 'com.googlecode.dex2jar.tools.StdApkCmd'

}
