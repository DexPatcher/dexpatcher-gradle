package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaExecSpec

@CompileStatic
abstract class AbstractJavaExecTask extends JavaExec {

    def addBlankLines
    def deleteOutputs = true
    def extraArgs

    Boolean getAddBlankLines() { Resolver.resolve(addBlankLines) as Boolean }
    @Optional @Input Boolean getDeleteOutputs() { Resolver.resolve(deleteOutputs) as Boolean }
    @Input List<String> getExtraArgs() { Resolver.resolve(extraArgs).collect() { it as String } }

    @Override abstract List<String> getArgs()

    @Override JavaExec setArgs(Iterable<?> args) { throw new UnsupportedOperationException() }
    @Override JavaExec args(Object... args) { throw new UnsupportedOperationException() }
    @Override JavaExecSpec args(Iterable<?> args) { throw new UnsupportedOperationException() }

    @Override void exec() {
        def blankLines = getAddBlankLines()
        if (blankLines) println()
        super.setArgs(getArgs())
        beforeExec()
        super.exec()
        afterExec()
        if (blankLines) println()
    }

    protected abstract void beforeExec()
    protected abstract void afterExec()

    protected void deleteOutputFile(File file) {
        if (deleteOutputs && file) project.delete project.files(file)
    }

    protected void deleteOutputDir(File dir) {
        if (deleteOutputs && dir) project.delete project.fileTree(dir)
    }

    protected void checkOutputFile(File file) {
        if (file && !file.file) throw new RuntimeException('No output generated')
    }

    protected void checkOutputDir(File dir) {
        if (dir && project.fileTree(dir).empty) throw new RuntimeException('No output generated')
    }

}
