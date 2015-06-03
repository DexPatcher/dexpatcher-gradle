package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
abstract class Resolver {

    static def resolve(Object object) {
        if (object instanceof Closure) resolve(object.call())
        else object
    }

    static <T> T resolve(Object object, Closure<T> closure) {
        if (object instanceof Closure) resolve(object.call(), closure)
        else closure.call(object)
    }

    static <T> T resolveNullable(Object object, Closure<T> closure) {
        if (object instanceof Closure) resolveNullable(object.call(), closure)
        else if (object) closure.call(object)
        else null
    }

    static File resolveNullableFile(Project project, Object object) {
        resolveNullable(object) { project.file(it) }
    }

}
