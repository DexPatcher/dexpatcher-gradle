package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic

@CompileStatic
class ApktoolBaseTask extends CustomJavaExecTask {

    ApktoolBaseTask() {
        setMain('brut.apktool.Main')
    }

}
