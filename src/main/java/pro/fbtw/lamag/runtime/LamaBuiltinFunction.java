package pro.fbtw.lamag.runtime;

import pro.fbtw.lamag.LamaException;

import java.util.function.Function;

public final class LamaBuiltinFunction implements LamaCallable {
    private final String name;
    private final int arity;
    private final Function<Object[], Object> implementation;

    public LamaBuiltinFunction(String name, int arity, Function<Object[], Object> implementation) {
        this.name = name;
        this.arity = arity;
        this.implementation = implementation;
    }

    public String name() {
        return name;
    }

    @Override
    public Object call(Object[] arguments) {
        if (arity >= 0 && arguments.length != arity) {
            throw LamaException.error("function " + name + " expects " + arity + " arguments, got " + arguments.length);
        }
        return implementation.apply(arguments);
    }

    @Override
    public String toString() {
        return "<builtin " + name + ">";
    }
}
