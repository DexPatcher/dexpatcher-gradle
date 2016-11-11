package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.process.JavaExecSpec

@CompileStatic
class CustomJavaExecTask extends JavaExec {

    def blankLines
    def deleteOutputs = true
    def extraArgs

    boolean getBlankLines() { Resolver.resolve(blankLines) as boolean }
    @Input boolean getDeleteOutputs() { Resolver.resolve(deleteOutputs) as boolean }
    @Input List<String> getExtraArgs() { Resolver.resolve(extraArgs).collect() { it as String } }

    @Override List<String> getArgs() {
        return getExtraArgs();
    }

    @Override JavaExec setArgs(Iterable<?> args) { throw new UnsupportedOperationException() }
    @Override JavaExec args(Object... args) { throw new UnsupportedOperationException() }
    @Override JavaExecSpec args(Iterable<?> args) { throw new UnsupportedOperationException() }

    @Override void exec() {
        def blankLines = getBlankLines()
        if (blankLines) println()
        super.setArgs(getArgs())
        beforeExec()
        super.exec()
        afterExec()
        if (blankLines) println()
    }

    protected void beforeExec() {}
    protected void afterExec() {}

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
