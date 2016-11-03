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
            task.multiDex = { dexpatcher.multiDex }
            task.multiDexThreaded = { dexpatcher.multiDexThreaded }
            task.multiDexJobs = { dexpatcher.multiDexJobs }
            task.maxDexPoolSize = { dexpatcher.maxDexPoolSize }
            task.annotationPackage = { dexpatcher.annotationPackage }
            task.compatDexTag = { dexpatcher.compatDexTag }
            task.verbosity = { dexpatcher.verbosity }
            task.logSourcePath = { dexpatcher.logSourcePath }
            task.logSourcePathRoot = { dexpatcher.logSourcePathRoot }
            task.logStats = { dexpatcher.logStats }
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
        def getResolvedProperty = { String key -> Resolver.resolveNullableFile(project.rootProject, getProperty(key)) }
        dexpatcherConfig = project.extensions.create(DexpatcherConfigExtension.EXTENSION_NAME, DexpatcherConfigExtension, project, getResolvedProperty)
        def subextensions = (dexpatcherConfig as ExtensionAware).extensions
        dexpatcher = subextensions.create(DexpatcherExtension.EXTENSION_NAME, DexpatcherExtension, dexpatcherConfig, getResolvedProperty)
        apktool = subextensions.create(ApktoolExtension.EXTENSION_NAME, ApktoolExtension, dexpatcherConfig, getResolvedProperty)
        dex2jar = subextensions.create(Dex2jarExtension.EXTENSION_NAME, Dex2jarExtension, dexpatcherConfig, getResolvedProperty)
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
