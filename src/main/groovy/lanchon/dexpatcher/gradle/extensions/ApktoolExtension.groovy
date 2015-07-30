package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class ApktoolExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'apktool'

    private static final String SUBDIR_NAME = EXTENSION_NAME

    def aaptFile
    def frameworkDir
    String frameworkTag
    Integer apiLevel

    ApktoolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig, SUBDIR_NAME)
    }

    File getAaptFile() { resolveClosures(aaptFile) }
    File getFrameworkDir() { resolveClosures(frameworkDir) }

}
