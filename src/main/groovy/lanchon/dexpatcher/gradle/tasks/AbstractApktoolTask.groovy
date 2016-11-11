package lanchon.dexpatcher.gradle.tasks

import groovy.transform.CompileStatic
import lanchon.dexpatcher.gradle.Resolver

@CompileStatic
abstract class AbstractApktoolTask extends AbstractJavaExecTask {

    enum Verbosity {
        QUIET,
        NORMAL,
        VERBOSE
    }

    def verbosity

    AbstractApktoolTask() {
        main = 'brut.apktool.Main'
        blankLines = {
            switch (getVerbosity()) {
                case Verbosity.QUIET:
                    return false
                    break
                case Verbosity.NORMAL:
                case null:
                case Verbosity.VERBOSE:
                    return true
                    break
                default:
                    throw new AssertionError('Unexpected verbosity', null)
            }
        }
    }

    Verbosity getVerbosity() { Resolver.resolve(verbosity) as Verbosity }

    @Override List<String> getArgs() {
        ArrayList<String> args = new ArrayList()
        switch (getVerbosity()) {
            case Verbosity.QUIET:
                args.add('--quiet')
                break
            case Verbosity.NORMAL:
            case null:
                break
            case Verbosity.VERBOSE:
                args.add('--verbose')
                break
            default:
                throw new AssertionError('Unexpected verbosity', null)
        }
        return args;
    }

}
