package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import org.gradle.api.file.FileCollection

@CompileStatic
abstract class AbstractToolExtension {

    protected final DexpatcherConfigExtension dexpatcherConfig
    protected final String defaultSubdirName;

    def dir

    AbstractToolExtension(DexpatcherConfigExtension dexpatcherConfig, String defaultSubdirName) {
        this.dexpatcherConfig = dexpatcherConfig
        this.defaultSubdirName = defaultSubdirName
    }

    File getDir() { dexpatcherConfig.resolveClosures(dir) }

    FileCollection getClasspath() { dexpatcherConfig.getToolClasspath(getDir(), defaultSubdirName) }

}
