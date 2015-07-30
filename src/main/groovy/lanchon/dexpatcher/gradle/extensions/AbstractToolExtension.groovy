package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@CompileStatic
abstract class AbstractToolExtension extends AbstractExtension {

    protected final DexpatcherConfigExtension dexpatcherConfig
    protected final String subdirName;

    def dir

    AbstractToolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig, String subdirName) {
        super(project)
        this.dexpatcherConfig = dexpatcherConfig
        this.subdirName = subdirName
    }

    File getDir() { resolveClosures(dir) }

    FileCollection getClasspath() { dexpatcherConfig.getToolClasspath(getDir(), subdirName) }

}
