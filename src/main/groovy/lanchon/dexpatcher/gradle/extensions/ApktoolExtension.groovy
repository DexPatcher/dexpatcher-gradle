package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import lanchon.dexpatcher.gradle.tasks.AbstractApktoolTask.Verbosity

@CompileStatic
class ApktoolExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'apktool'

    private static final String DIR_PROPERTY = 'dexpatcher.apktool.dir'
    private static final String FRAMEWORK_DIR_PROPERTY = 'dexpatcher.apktool.frameworkDir'
    private static final String AAPT_FILE_PROPERTY = 'dexpatcher.apktool.aaptFile'

    private static final String DEFAULT_SUBDIR_NAME = EXTENSION_NAME

    static final def QUIET = Verbosity.QUIET
    static final def NORMAL = Verbosity.NORMAL
    static final def VERBOSE = Verbosity.VERBOSE

    Verbosity verbosity
    def frameworkDir
    def frameworkDirAsInput
    def frameworkDirAsOutput
    def frameworkTag

    // Decode
    Integer apiLevel

    // Build
    def aaptFile

    ApktoolExtension(DexpatcherConfigExtension dexpatcherConfig, Closure getProperty) {
        super(dexpatcherConfig, DEFAULT_SUBDIR_NAME)
        dir = getProperty(DIR_PROPERTY)
        frameworkDir = getProperty(FRAMEWORK_DIR_PROPERTY)
        aaptFile = getProperty(AAPT_FILE_PROPERTY)
    }

    File getFrameworkDir() { Resolver.resolveNullableFile(dexpatcherConfig.project, frameworkDir) }
    File getFrameworkDirAsInput() { Resolver.resolveNullableFile(dexpatcherConfig.project, frameworkDirAsInput) }
    File getFrameworkDirAsOutput() { Resolver.resolveNullableFile(dexpatcherConfig.project, frameworkDirAsOutput) }
    String getFrameworkTag() { Resolver.resolve(frameworkTag) as String }

    File getAaptFile() { Resolver.resolveNullableFile(dexpatcherConfig.project, aaptFile) }

}
