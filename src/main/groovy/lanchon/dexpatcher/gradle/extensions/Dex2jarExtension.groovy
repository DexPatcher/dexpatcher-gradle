package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class Dex2jarExtension extends AbstractToolExtension {

    static final String EXTENSION_NAME = 'dex2jar'

    private static final String SUBDIR_NAME = EXTENSION_NAME

    Dex2jarExtension(Project project, DexpatcherConfigExtension dexpatcherConfig) {
        super(project, dexpatcherConfig, SUBDIR_NAME)
    }

}
