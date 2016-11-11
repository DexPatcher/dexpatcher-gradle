package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractApktoolTask extends AbstractJavaExecTask {

    AbstractApktoolTask() {
        main = 'brut.apktool.Main'
        blankLines = true
    }

}
