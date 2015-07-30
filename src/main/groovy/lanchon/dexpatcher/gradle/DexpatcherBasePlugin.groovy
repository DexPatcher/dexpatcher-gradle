package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.extensions.ApktoolExtension
import lanchon.dexpatcher.gradle.extensions.Dex2jarExtension
import lanchon.dexpatcher.gradle.extensions.DexpatcherConfigExtension
import lanchon.dexpatcher.gradle.extensions.DexpatcherExtension
import lanchon.dexpatcher.gradle.tasks.ApktoolBaseTask
import lanchon.dexpatcher.gradle.tasks.BuildApkTask
import lanchon.dexpatcher.gradle.tasks.DecodeApkTask
import lanchon.dexpatcher.gradle.tasks.Dex2jarBaseTask
import lanchon.dexpatcher.gradle.tasks.DexpatcherTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

@CompileStatic
class DexpatcherBasePlugin implements Plugin<Project> {

    static final String TASK_GROUP = 'DexPatcher'

    private static final String LOCAL_PROPERTIES = 'local.properties'

    private static final String BASE_DIR = 'dexpatcher.dir'
    private static final String TOOL_DIR = 'dexpatcher.toolDir'
    private static final String LIB_DIR = 'dexpatcher.libDir'

    private static final String DEXPATCHER_DIR = 'dexpatcher.dexpatcherTool.dir'
    private static final String APKTOOL_DIR = 'dexpatcher.apktool.dir'
    private static final String DEX2JAR_DIR = 'dexpatcher.dex2jar.dir'

    private static final String APKTOOL_FRAMEWORK_DIR = 'dexpatcher.apktool.frameworkDir'
    private static final String APKTOOL_AAPT_FILE = 'dexpatcher.apktool.aaptFile'

    protected Project project
    protected DexpatcherConfigExtension dexpatcherConfig
    protected DexpatcherExtension dexpatcher
    protected ApktoolExtension apktool
    protected Dex2jarExtension dex2jar

    void apply(Project project) {

        this.project = project
        setExtensions()

        project.tasks.withType(DexpatcherTask) { DexpatcherTask task ->
            task.classpath { dexpatcher.getClasspath() }
            task.apiLevel = { dexpatcher.apiLevel }
            task.verbosity = { dexpatcher.verbosity }
        }

        project.tasks.withType(ApktoolBaseTask) { ApktoolBaseTask task ->
            task.classpath { apktool.getClasspath() }
        }
        project.tasks.withType(DecodeApkTask) { DecodeApkTask task ->
            task.frameworkDir = { apktool.getFrameworkDir() }
            task.frameworkTag = { apktool.getFrameworkTag() }
            task.apiLevel = { apktool.getApiLevel() }
        }
        project.tasks.withType(BuildApkTask) { BuildApkTask task ->
            task.aaptFile = { apktool.getAaptFile() }
            task.frameworkDir = { apktool.getFrameworkDir() }
        }

        project.tasks.withType(Dex2jarBaseTask) { Dex2jarBaseTask task ->
            task.classpath { dex2jar.getClasspath() }
        }

    }

    private void setExtensions() {

        Properties localProperties = getLocalPropertiesRecursive(project)
        def getProperty = { String key -> project.properties.get(key) ?: localProperties.getProperty(key) }
        def getFile = { String key -> Resolver.resolveNullableFile(project, getProperty(key)) }
        def get = getFile

        dexpatcherConfig = project.extensions.create(DexpatcherConfigExtension.EXTENSION_NAME, DexpatcherConfigExtension, project)
        dexpatcherConfig.with {

            dir = get(BASE_DIR)
            toolDir = get(TOOL_DIR)
            libDir = get(LIB_DIR)

        }

        def subextensions = (dexpatcherConfig as ExtensionAware).extensions

        dexpatcher = subextensions.create(DexpatcherExtension.EXTENSION_NAME, DexpatcherExtension, project, dexpatcherConfig)
        dexpatcher.with {
            setDir(get(DEXPATCHER_DIR))
        }

        apktool = subextensions.create(ApktoolExtension.EXTENSION_NAME, ApktoolExtension, project, dexpatcherConfig)
        apktool.with {
            setDir(get(APKTOOL_DIR))
            frameworkDir = get(APKTOOL_FRAMEWORK_DIR)
            aaptFile = get(APKTOOL_AAPT_FILE)
        }

        dex2jar = subextensions.create(Dex2jarExtension.EXTENSION_NAME, Dex2jarExtension, project, dexpatcherConfig)
        dex2jar.with {
            setDir(get(DEX2JAR_DIR))
        }

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
