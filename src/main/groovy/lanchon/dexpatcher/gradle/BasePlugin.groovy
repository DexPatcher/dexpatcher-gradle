package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.tasks.ApktoolBaseTask
import lanchon.dexpatcher.gradle.tasks.BuildApkTask
import lanchon.dexpatcher.gradle.tasks.DecodeApkTask
import lanchon.dexpatcher.gradle.tasks.Dex2jarBaseTask
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class BasePlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = 'dexpatcher'
    static final String TASK_GROUP = 'DexPatcher'

    private static final String LOCAL_PROPERTIES = 'local.properties'

    private static final String BASE_DIR = 'dexpatcher.dir'
    private static final String TOOL_DIR = 'dexpatcher.toolDir'
    private static final String LIB_DIR = 'dexpatcher.libDir'

    private static final String DEXPATCHER_DIR = 'dexpatcher.dexpatcherDir'
    private static final String APKTOOL_DIR = 'dexpatcher.apktoolDir'
    private static final String DEX2JAR_DIR = 'dexpatcher.dex2jarDir'

    private static final String APKTOOL_FRAMEWORK_DIR = 'dexpatcher.apktoolFrameworkDir'
    private static final String APKTOOL_AAPT_FILE = 'dexpatcher.apktoolAaptFile'

    protected Project project
    protected DexpatcherExtension dexpatcher

    void apply(Project project) {

        this.project = project
        dexpatcher = setExtension()

        project.tasks.withType(DexpatcherTask) { DexpatcherTask task ->
            task.classpath { dexpatcher.getDexpatcherClasspath() }
            task.apiLevel = { dexpatcher.dexpatcherApiLevel }
            task.verbosity = { dexpatcher.dexpatcherVerbosity }
        }

        project.tasks.withType(ApktoolBaseTask) { ApktoolBaseTask task ->
            task.classpath { dexpatcher.getApktoolClasspath() }
        }
        project.tasks.withType(DecodeApkTask) { DecodeApkTask task ->
            task.frameworkDir = { dexpatcher.getApktoolFrameworkDir() }
        }
        project.tasks.withType(BuildApkTask) { BuildApkTask task ->
            task.frameworkDir = { dexpatcher.getApktoolFrameworkDir() }
            task.aaptFile = { dexpatcher.getApktoolAaptFile() }
        }

        project.tasks.withType(Dex2jarBaseTask) { Dex2jarBaseTask task ->
            task.classpath { dexpatcher.getDex2jarClasspath() }
        }

    }

    private DexpatcherExtension setExtension() {
        Properties localProperties = getLocalPropertiesRecursive(project)
        def getProperty = { String key -> project.properties.get(key) ?: localProperties.getProperty(key) }
        def getFile = { String key -> Resolver.resolveNullableFile(project, getProperty(key)) }
        def get = getFile
        def extension = project.extensions.create(EXTENSION_NAME, DexpatcherExtension, project)
        extension.with {

            dir = get(BASE_DIR)
            toolDir = get(TOOL_DIR)
            libDir = get(LIB_DIR)

            dexpatcherDir = get(DEXPATCHER_DIR)
            apktoolDir = get(APKTOOL_DIR)
            dex2jarDir = get(DEX2JAR_DIR)

            apktoolFrameworkDir = get(APKTOOL_FRAMEWORK_DIR)
            apktoolAaptFile = get(APKTOOL_AAPT_FILE)

        }
        return extension
    }

    private static Properties getLocalPropertiesRecursive(Project project) {
        Properties parentProperties = project.parent ? getLocalPropertiesRecursive(project.parent) : null
        Properties properties = new Properties(parentProperties)
        File file = project.file(LOCAL_PROPERTIES)
        if (file.exists()) {
            file.withInputStream {
                properties.load(it)
            }
        }
        return properties
    }

}
