package pro.fbtw.lamag.runtime;

import com.oracle.truffle.api.CallTarget;
import pro.fbtw.lamag.LamaException;

public final class LamaFunction implements LamaCallable {
    private final String name;
    private final int arity;
    private final LamaFrame closure;
    private final CallTarget callTarget;

    public LamaFunction(String name, int arity, LamaFrame closure, CallTarget callTarget) {
        this.name = name;
        this.arity = arity;
        this.closure = closure;
        this.callTarget = callTarget;
    }

    public String name() {
        return name;
    }

    public LamaFrame closure() {
        return closure;
    }

    @Override
    public Object call(Object[] arguments) {
        if (arguments.length != arity) {
            throw LamaException.error("function " + name + " expects " + arity + " arguments, got " + arguments.length);
        }
        Object[] frameArguments = new Object[arguments.length + 1];
        frameArguments[0] = this;
        System.arraycopy(arguments, 0, frameArguments, 1, arguments.length);
        return callTarget.call(frameArguments);
    }

    @Override
    public String toString() {
        return "<fun " + name + "/" + arity + ">";
    }
}
