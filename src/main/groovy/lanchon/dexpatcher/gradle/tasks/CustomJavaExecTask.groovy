package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.process.JavaExecSpec

@CompileStatic
class CustomJavaExecTask extends JavaExec {

    public boolean blankLines

    @Input boolean deleteOutputs = true

    @Input List<Object> extraArgs = new ArrayList()

    List<String> getExtraArgs() {
        extraArgs as List<String>
    }

    void setExtraArgs(Iterable<?> extraArgs) {
        this.extraArgs = new ArrayList(Arrays.asList(extraArgs))
    }

    void extraArgs(Object... extraArgs) {
        this.extraArgs.addAll(extraArgs)
    }

    @Override List<String> getArgs() {
        return new ArrayList<String>(getExtraArgs());
    }

    @Override JavaExec setArgs(Iterable<?> args) { throw new UnsupportedOperationException() }
    @Override JavaExec args(Object... args) { throw new UnsupportedOperationException() }
    @Override JavaExecSpec args(Iterable<?> args) { throw new UnsupportedOperationException() }

    @Override void exec() {
        super.setArgs(getArgs())
        if (blankLines) println()
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
