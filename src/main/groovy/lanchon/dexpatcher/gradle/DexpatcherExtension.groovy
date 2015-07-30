package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@CompileStatic
class DexpatcherExtension {

    static final def QUIET = DexpatcherVerbosity.QUIET
    static final def NORMAL = DexpatcherVerbosity.NORMAL
    static final def VERBOSE = DexpatcherVerbosity.VERBOSE
    static final def DEBUG = DexpatcherVerbosity.DEBUG

    private static final String TOOL_SUBDIR = 'tools'
    private static final String LIB_SUBDIR = 'libs'

    private static final String DEXPATCHER_SUBDIR = 'dexpatcher'
    private static final String APKTOOL_SUBDIR = 'apktool'
    private static final String DEX2JAR_SUBDIR = 'dex2jar'

    protected final Project project

    def dir
    def toolDir
    def libDir

    def dexpatcherDir
    def apktoolDir
    def dex2jarDir

    Integer dexpatcherApiLevel
    DexpatcherVerbosity dexpatcherVerbosity

    def apktoolAaptFile
    def apktoolFrameworkDir
    String apktoolFrameworkTag
    Integer apktoolApiLevel

    // TODO: Move these properties to sub-extension objects.

    //boolean patchManifest = true      // TODO
    //boolean patchResources = true     // TODO
    boolean patchCode = true

    boolean apkLibraryDisableClean

    DexpatcherExtension(Project project) {
        this.project = project
    }

    private File resolveClosures(def dir) { Resolver.resolveNullableFile(project, dir) }

    File getDir() { resolveClosures(dir) }
    File getToolDir() { resolveClosures(toolDir) }
    File getLibDir() { resolveClosures(libDir) }

    File getDexpatcherDir() { resolveClosures(dexpatcherDir) }
    File getApktoolDir() { resolveClosures(apktoolDir) }
    File getDex2jarDir() { resolveClosures(dex2jarDir) }

    File getApktoolAaptFile() { resolveClosures(apktoolAaptFile) }
    File getApktoolFrameworkDir() { resolveClosures(apktoolFrameworkDir) }

    private File resolvePath(File file, File defaultBaseDir, String defaultSubdirName) {
        file ?: (defaultBaseDir ? new File(defaultBaseDir, defaultSubdirName) : null)
    }

    private FileCollection getJars(File rootDir) {
        def jars = project.fileTree(rootDir)
        jars.include '**/*.jar'
        return jars
    }

    FileCollection getLibClasspath() {
        def resolvedLibDir = resolvePath(getLibDir(), getDir(), LIB_SUBDIR)
        return getJars(resolvedLibDir)
    }

    private FileCollection getToolClasspath(File specificToolDir, String defaultSubdirName) {
        def resolvedToolDir = resolvePath(getToolDir(), getDir(), TOOL_SUBDIR)
        def resolvedSpecificToolDir = resolvePath(specificToolDir, resolvedToolDir, defaultSubdirName)
        return getJars(resolvedSpecificToolDir)
    }

    FileCollection getDexpatcherClasspath() { getToolClasspath(getDexpatcherDir(), DEXPATCHER_SUBDIR) }
    FileCollection getApktoolClasspath() { getToolClasspath(getApktoolDir(), APKTOOL_SUBDIR) }
    FileCollection getDex2jarClasspath() { getToolClasspath(getDex2jarDir(), DEX2JAR_SUBDIR) }

}
