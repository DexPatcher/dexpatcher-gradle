package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.Project

@CompileStatic
abstract class AbstractExtension {

    protected final Project project

    AbstractExtension(Project project) {
        this.project = project
    }

    protected File resolveClosures(def dir) {
        Resolver.resolveNullableFile(project, dir)
    }

}
