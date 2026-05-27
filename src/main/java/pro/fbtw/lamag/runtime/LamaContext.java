package pro.fbtw.lamag.runtime;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.nodes.Node;
import pro.fbtw.lamag.LamaException;
import pro.fbtw.lamag.LamaLanguage;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public final class LamaContext {
    private static final ContextReference<LamaContext> REFERENCE = ContextReference.create(LamaLanguage.class);

    private Env env;
    private Scanner input;
    private PrintWriter output;
    private final LamaFrame globalFrame = new LamaFrame(null);
    private final Random random = new Random(0);

    public LamaContext(Env env) {
        patchEnv(env);
        installBuiltins();
    }

    public static LamaContext get(Node node) {
        return REFERENCE.get(node);
    }

    public void patchEnv(Env newEnv) {
        this.env = newEnv;
        this.input = new Scanner(newEnv.in());
        this.output = new PrintWriter(newEnv.out(), true);
    }

    public Env env() {
        return env;
    }

    public PrintWriter output() {
        return output;
    }

    public LamaFrame globalFrame() {
        return globalFrame;
    }

    private void installBuiltins() {
        defineBuiltin("read", 0, args -> {
            output.print("> ");
            output.flush();
            if (!input.hasNextLong()) {
                return 0L;
            }
            return input.nextLong();
        });
        defineBuiltin("write", 1, args -> {
            output.println(LamaValues.asLong(args[0]));
            return 0L;
        });
        defineBuiltin("length", 1, args -> LamaValues.length(args[0]));
        defineBuiltin("string", 1, args -> LamaValues.print(args[0]));
        defineBuiltin("stringcat", 1, args -> LamaValues.print(args[0]));
        defineBuiltin("makeArray", 1, args -> LamaArray.filled(Math.toIntExact(LamaValues.asLong(args[0])), 0L));
        defineBuiltin("array", -1, args -> new LamaArray(List.of(args)));
        defineBuiltin("fst", 1, args -> LamaValues.element(args[0], 0L));
        defineBuiltin("snd", 1, args -> LamaValues.element(args[0], 1L));
        defineBuiltin("hd", 1, args -> LamaValues.element(args[0], 0L));
        defineBuiltin("tl", 1, args -> LamaValues.element(args[0], 1L));
        defineBuiltin("compare", 2, args -> LamaValues.compare(args[0], args[1]));
        defineBuiltin("flatCompare", 2, args -> LamaValues.compare(args[0], args[1]));
        defineBuiltin("clone", 1, args -> cloneValue(args[0]));
        defineBuiltin("random", 1, args -> {
            long bound = LamaValues.asLong(args[0]);
            if (bound <= 0) {
                throw LamaException.error("invalid random bound: " + bound);
            }
            return (long) random.nextInt(Math.toIntExact(bound));
        });
        defineBuiltin("time", 0, args -> System.nanoTime() / 1000L);
        defineBuiltin("assert", 1, args -> {
            if (LamaValues.truth(args[0]) == 0) {
                throw LamaException.error("assertion failed");
            }
            return 0L;
        });
        defineBuiltin("failure", 1, args -> {
            throw LamaException.error(LamaValues.print(args[0]));
        });
        defineBuiltin("substring", 3, args -> {
            String text = LamaValues.asString(args[0]);
            int start = Math.toIntExact(LamaValues.asLong(args[1]));
            int length = Math.toIntExact(LamaValues.asLong(args[2]));
            return text.substring(start, Math.min(text.length(), start + length));
        });
        defineBuiltin("makeString", 1, args -> {
            int length = Math.toIntExact(LamaValues.asLong(args[0]));
            StringBuilder builder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                builder.append('\0');
            }
            return builder.toString();
        });
        defineBuiltin("tagHash", 1, args -> LamaValues.tagHash(LamaValues.asString(args[0])));
        defineBuiltin("kindOf", 1, args -> (long) kindOf(args[0]));
        defineBuiltin("compareTags", 2, args -> compareTags(args[0], args[1]));
        defineBuiltin("sprintf", -1, args -> format(args));
        defineBuiltin("printf", -1, args -> {
            String result = format(args);
            output.print(result);
            output.flush();
            return 0L;
        });
        globalFrame.define("sysargs", new LamaArray(List.of()));
    }

    private void defineBuiltin(String name, int arity, java.util.function.Function<Object[], Object> implementation) {
        if (!globalFrame.isDefinedLocally(name)) {
            globalFrame.define(name, new LamaBuiltinFunction(name, arity, implementation));
        }
    }

    private static Object cloneValue(Object value) {
        if (value instanceof LamaArray) {
            return new LamaArray(((LamaArray) value).values());
        }
        return value;
    }

    private static int kindOf(Object value) {
        if (value instanceof Long) {
            return 0;
        }
        if (value instanceof String) {
            return 1;
        }
        if (value instanceof LamaArray) {
            return 2;
        }
        if (value instanceof LamaSexp) {
            return 3;
        }
        if (value instanceof LamaCallable) {
            return 4;
        }
        return -1;
    }

    private static long compareTags(Object left, Object right) {
        if (left instanceof LamaSexp && right instanceof LamaSexp) {
            return LamaValues.tagHash(((LamaSexp) left).tag()) - LamaValues.tagHash(((LamaSexp) right).tag());
        }
        throw LamaException.error("not a sexp in compareTags");
    }

    private static String format(Object[] args) {
        if (args.length == 0) {
            return "";
        }
        String format = LamaValues.asString(args[0]);
        List<Object> values = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            Object arg = args[i];
            values.add(arg instanceof Long ? arg : LamaValues.print(arg));
        }
        return String.format(format, values.toArray());
    }
}
