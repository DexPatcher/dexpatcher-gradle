package lanchon.dexpatcher.gradle

import com.android.build.gradle.AppPlugin
import groovy.transform.CompileStatic
import org.gradle.api.Project;

@CompileStatic
class PatchedAppPlugin extends PatcherPlugin {

    @Override
    void apply(Project project) {
        super.apply(project)
        project.plugins.apply(AppPlugin)
    }

}
