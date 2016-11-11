package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@CompileStatic
abstract class AbstractToolExtension {

    protected final Project project
    protected final DexpatcherConfigExtension dexpatcherConfig
    protected final String defaultSubdirName;

    def dir
    def extraArgs
    //boolean addBlankLines
    //boolean deleteOutputs = true

    AbstractToolExtension(Project project, DexpatcherConfigExtension dexpatcherConfig, String defaultSubdirName) {
        this.project = project
        this.dexpatcherConfig = dexpatcherConfig
        this.defaultSubdirName = defaultSubdirName
    }

    File getDir() { Resolver.resolveNullableFile(project, dir) }
    File getResolvedDir() { dexpatcherConfig.getResolvedToolDir(getDir(), defaultSubdirName) }

    FileCollection getClasspath() { Resolver.getJars(project, getResolvedDir()) }

    List<String> getExtraArgs() { Resolver.resolve(extraArgs).collect() { it as String } }

}
