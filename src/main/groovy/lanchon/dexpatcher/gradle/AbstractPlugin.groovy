package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.extensions.DexpatcherConfigExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class AbstractPlugin implements Plugin<Project> {

    protected Project project
    protected DexpatcherConfigExtension dexpatcherConfig

    void apply(Project project) {
        this.project = project
        project.plugins.apply(DexpatcherBasePlugin)
        dexpatcherConfig = project.extensions.getByType(DexpatcherConfigExtension)
    }

}
