package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic

@CompileStatic
class Dex2jarBaseTask extends CustomJavaExecTask {

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
