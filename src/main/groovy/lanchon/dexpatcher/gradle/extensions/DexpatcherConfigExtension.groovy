package lanchon.dexpatcher.gradle.extensions

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@CompileStatic
class DexpatcherConfigExtension extends AbstractExtension {

    static final String EXTENSION_NAME = 'dexpatcherConfig'

    private static final String TOOL_SUBDIR_NAME = 'tools'
    private static final String LIB_SUBDIR_NAME = 'libs'

    def dir
    def toolDir
    def libDir

    // TODO: Move these properties to sub-extension objects.

    //boolean patchManifest = true      // TODO
    //boolean patchResources = true     // TODO
    boolean patchCode = true

    boolean apkLibraryDisableClean

    DexpatcherConfigExtension(Project project) {
        super(project)
    }

    File getDir() { resolveClosures(dir) }
    File getToolDir() { resolveClosures(toolDir) }
    File getLibDir() { resolveClosures(libDir) }

    private File resolvePath(File file, File defaultBaseDir, String defaultSubdirName) {
        file ?: (defaultBaseDir ? new File(defaultBaseDir, defaultSubdirName) : null)
    }

    private FileCollection getJars(File rootDir) {
        def jars = project.fileTree(rootDir)
        jars.include '**/*.jar'
        return jars
    }

    FileCollection getLibClasspath() {
        def resolvedLibDir = resolvePath(getLibDir(), getDir(), LIB_SUBDIR_NAME)
        return getJars(resolvedLibDir)
    }

    FileCollection getToolClasspath(File specificToolDir, String defaultSubdirName) {
        def resolvedToolDir = resolvePath(getToolDir(), getDir(), TOOL_SUBDIR_NAME)
        def resolvedSpecificToolDir = resolvePath(specificToolDir, resolvedToolDir, defaultSubdirName)
        return getJars(resolvedSpecificToolDir)
    }

}
