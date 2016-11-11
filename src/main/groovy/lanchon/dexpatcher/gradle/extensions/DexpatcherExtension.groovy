package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask.Verbosity

@CompileStatic
class DexpatcherExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'dexpatcher'

    private static final String DIR_PROPERTY = 'dexpatcher.dexpatcher.dir'

    private static final String DEFAULT_SUBDIR_NAME = EXTENSION_NAME

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE
    static final def DEBUG = Verbosity.DEBUG

    Integer apiLevel
    Boolean multiDex
    Boolean multiDexThreaded
    Integer multiDexJobs
    Integer maxDexPoolSize
    String annotationPackage
    Boolean compatDexTag
    Verbosity verbosity
    Boolean logSourcePath
    def logSourcePathRoot
    Boolean logStats

    DexpatcherExtension(DexpatcherConfigExtension dexpatcherConfig, Closure getProperty) {
        super(dexpatcherConfig, DEFAULT_SUBDIR_NAME)
        dir = getProperty(DIR_PROPERTY)
    }

    String getLogSourcePathRoot() { Resolver.resolve(logSourcePathRoot) as String }

}
