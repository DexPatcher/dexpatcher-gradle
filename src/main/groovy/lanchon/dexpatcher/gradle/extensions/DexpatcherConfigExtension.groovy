package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.Project

@CompileStatic
class DexpatcherConfigExtension {

    static final String EXTENSION_NAME = 'dexpatcherConfig'

    private static final String DIR_PROPERTY = 'dexpatcher.dir'
    private static final String TOOL_DIR_PROPERTY = 'dexpatcher.toolDir'
    private static final String LIB_DIR_PROPERTY = 'dexpatcher.libDir'

    private static final String DEFAULT_TOOL_SUBDIR_NAME = 'tools'
    private static final String DEFAULT_LIB_SUBDIR_NAME = 'libs'

    private static File resolvePath(File file, File defaultBaseDir, String defaultSubdirName) {
        file ?: (defaultBaseDir ? new File(defaultBaseDir, defaultSubdirName) : null)
    }

    final Project project

    def dir
    def toolDir
    def libDir

    DexpatcherConfigExtension(Project project, Closure getProperty) {
        this.project = project
        dir = getProperty(DIR_PROPERTY)
        toolDir = getProperty(TOOL_DIR_PROPERTY)
        libDir = getProperty(LIB_DIR_PROPERTY)
    }

    File getDir() { Resolver.resolveNullableFile(project, dir) }
    File getToolDir() { Resolver.resolveNullableFile(project, toolDir) }
    File getLibDir() { Resolver.resolveNullableFile(project, libDir) }

    File getResolvedToolDir() { resolvePath(getToolDir(), getDir(), DEFAULT_TOOL_SUBDIR_NAME) }
    File getResolvedLibDir() { resolvePath(getLibDir(), getDir(), DEFAULT_LIB_SUBDIR_NAME) }

    File getResolvedToolDir(File specificToolDir, String defaultSubdirName) {
        resolvePath(specificToolDir, getResolvedToolDir(), defaultSubdirName)
    }

}
