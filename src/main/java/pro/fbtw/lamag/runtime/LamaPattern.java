package pro.fbtw.lamag.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface LamaPattern {
    boolean match(Object value, Map<String, Object> bindings);

    default boolean bind(Object value, LamaFrame frame) {
        Map<String, Object> bindings = new LinkedHashMap<>();
        if (!match(value, bindings)) {
            return false;
        }
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            frame.define(entry.getKey(), entry.getValue());
        }
        return true;
    }

    static LamaPattern wildcard() {
        return (value, bindings) -> true;
    }

    static LamaPattern named(String name, LamaPattern pattern) {
        return (value, bindings) -> {
            if (!pattern.match(value, bindings)) {
                return false;
            }
            bindings.put(name, value);
            return true;
        };
    }

    static LamaPattern constant(long constant) {
        return (value, bindings) -> value instanceof Long && ((Long) value) == constant;
    }

    static LamaPattern string(String constant) {
        return (value, bindings) -> value instanceof String && Objects.equals(value, constant);
    }

    static LamaPattern sexp(String tag, List<LamaPattern> fields) {
        return (value, bindings) -> {
            if (!(value instanceof LamaSexp)) {
                return false;
            }
            LamaSexp sexp = (LamaSexp) value;
            if (!sexp.tag().equals(tag) || sexp.size() != fields.size()) {
                return false;
            }
            Object[] values = sexp.fields();
            return matchAll(fields, values, bindings);
        };
    }

    static LamaPattern array(List<LamaPattern> elements) {
        return (value, bindings) -> {
            if (!(value instanceof LamaArray)) {
                return false;
            }
            LamaArray array = (LamaArray) value;
            if (array.size() != elements.size()) {
                return false;
            }
            return matchAll(elements, array.values().toArray(), bindings);
        };
    }

    static LamaPattern boxed() {
        return (value, bindings) -> value instanceof String || value instanceof LamaArray || value instanceof LamaSexp || value instanceof LamaCallable;
    }

    static LamaPattern unboxed() {
        return (value, bindings) -> value instanceof Long;
    }

    static LamaPattern stringTag() {
        return (value, bindings) -> value instanceof String;
    }

    static LamaPattern sexpTag() {
        return (value, bindings) -> value instanceof LamaSexp;
    }

    static LamaPattern arrayTag() {
        return (value, bindings) -> value instanceof LamaArray;
    }

    static LamaPattern closureTag() {
        return (value, bindings) -> value instanceof LamaCallable;
    }

    static LamaPattern list(List<LamaPattern> elements) {
        LamaPattern result = constant(0);
        for (int i = elements.size() - 1; i >= 0; i--) {
            result = sexp("cons", List.of(elements.get(i), result));
        }
        return result;
    }

    private static boolean matchAll(List<LamaPattern> patterns, Object[] values, Map<String, Object> bindings) {
        Map<String, Object> snapshot = new LinkedHashMap<>(bindings);
        for (int i = 0; i < patterns.size(); i++) {
            if (!patterns.get(i).match(values[i], bindings)) {
                bindings.clear();
                bindings.putAll(snapshot);
                return false;
            }
        }
        return true;
    }
}
