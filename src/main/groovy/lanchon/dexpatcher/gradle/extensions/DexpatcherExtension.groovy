package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.DexpatcherVerbosity

@CompileStatic
class DexpatcherExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'dexpatcher'

    private static final String DIR_PROPERTY = 'dexpatcher.dexpatcher.dir'

    private static final String DEFAULT_SUBDIR_NAME = EXTENSION_NAME

    static final def QUIET = DexpatcherVerbosity.QUIET
    static final def NORMAL = DexpatcherVerbosity.NORMAL
    static final def VERBOSE = DexpatcherVerbosity.VERBOSE
    static final def DEBUG = DexpatcherVerbosity.DEBUG

    Integer apiLevel
    Boolean multiDex
    Boolean multiDexThreaded
    Integer multiDexJobs
    Integer maxDexPoolSize
    String annotationPackage
    Boolean compatDexTag
    DexpatcherVerbosity verbosity
    Boolean sourcePath
    String sourcePathRoot
    Boolean stats

    DexpatcherExtension(DexpatcherConfigExtension dexpatcherConfig, Closure getProperty) {
        super(dexpatcherConfig, DEFAULT_SUBDIR_NAME)
        dir = getProperty(DIR_PROPERTY)
    }

}
