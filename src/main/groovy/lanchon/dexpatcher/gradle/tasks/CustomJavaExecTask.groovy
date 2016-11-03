package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.process.JavaExecSpec

@CompileStatic
class CustomJavaExecTask extends JavaExec {

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
        beforeExec()
        super.exec()
        afterExec()
    }

    protected void beforeExec() {}
    protected void afterExec() {}

}
