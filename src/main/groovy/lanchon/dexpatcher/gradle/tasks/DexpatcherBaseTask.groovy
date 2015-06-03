package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic

@CompileStatic
class DexpatcherBaseTask extends CustomJavaExecTask {

    DexpatcherBaseTask() {
        setMain('lanchon.dexpatcher.Main')
    }

}
