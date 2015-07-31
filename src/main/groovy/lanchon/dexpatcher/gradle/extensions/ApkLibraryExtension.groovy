package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic

@CompileStatic
class ApkLibraryExtension {

    static final String EXTENSION_NAME = 'apkLibrary'

    boolean disableClean

}
