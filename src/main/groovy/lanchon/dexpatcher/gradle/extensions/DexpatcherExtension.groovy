package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.DexpatcherVerbosity
import org.gradle.api.Project

@CompileStatic
class DexpatcherExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'dexpatcher'

    private static final String SUBDIR_NAME = EXTENSION_NAME

    static final def QUIET = DexpatcherVerbosity.QUIET
    static final def NORMAL = DexpatcherVerbosity.NORMAL
    static final def VERBOSE = DexpatcherVerbosity.VERBOSE
    static final def DEBUG = DexpatcherVerbosity.DEBUG

    Integer apiLevel
    DexpatcherVerbosity verbosity

    DexpatcherExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig, SUBDIR_NAME)
    }

}
