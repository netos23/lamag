package pro.fbtw.lamag.runtime;

import java.util.List;
import java.util.Objects;

/**
 * A pattern binds its variables directly into the target {@link LamaFrame} as it
 * matches. There is no intermediate bindings map and no rollback on partial
 * match: every caller (case branches, function parameters, let-bindings) matches
 * into a fresh frame that is discarded when the match fails, so leftover
 * bindings from a failed attempt are never observable. Avoiding the per-match
 * map allocation is essential for pattern-heavy code such as the list-based
 * sorts in the performance suite.
 */
public interface LamaPattern {
    boolean match(Object value, LamaFrame frame);

    default boolean bind(Object value, LamaFrame frame) {
        return match(value, frame);
    }

    static LamaPattern wildcard() {
        return (value, frame) -> true;
    }

    static LamaPattern named(String name, LamaPattern pattern) {
        return (value, frame) -> {
            if (!pattern.match(value, frame)) {
                return false;
            }
            frame.define(name, value);
            return true;
        };
    }

    static LamaPattern constant(long constant) {
        return (value, frame) -> value instanceof Long && ((Long) value) == constant;
    }

    static LamaPattern string(String constant) {
        return (value, frame) -> LamaValues.isString(value) && Objects.equals(LamaValues.asString(value), constant);
    }

    static LamaPattern sexp(String tag, List<LamaPattern> fields) {
        LamaPattern[] patterns = fields.toArray(new LamaPattern[0]);
        return (value, frame) -> {
            if (!(value instanceof LamaSexp)) {
                return false;
            }
            LamaSexp sexp = (LamaSexp) value;
            if (sexp.size() != patterns.length || !sexp.tag().equals(tag)) {
                return false;
            }
            Object[] values = sexp.fields();
            for (int i = 0; i < patterns.length; i++) {
                if (!patterns[i].match(values[i], frame)) {
                    return false;
                }
            }
            return true;
        };
    }

    static LamaPattern array(List<LamaPattern> elements) {
        LamaPattern[] patterns = elements.toArray(new LamaPattern[0]);
        return (value, frame) -> {
            if (!(value instanceof LamaArray)) {
                return false;
            }
            LamaArray array = (LamaArray) value;
            if (array.size() != patterns.length) {
                return false;
            }
            for (int i = 0; i < patterns.length; i++) {
                if (!patterns[i].match(array.get(i), frame)) {
                    return false;
                }
            }
            return true;
        };
    }

    static LamaPattern boxed() {
        return (value, frame) -> LamaValues.isString(value) || value instanceof LamaArray || value instanceof LamaSexp || value instanceof LamaCallable;
    }

    static LamaPattern unboxed() {
        return (value, frame) -> value instanceof Long;
    }

    static LamaPattern stringTag() {
        return (value, frame) -> LamaValues.isString(value);
    }

    static LamaPattern sexpTag() {
        return (value, frame) -> value instanceof LamaSexp;
    }

    static LamaPattern arrayTag() {
        return (value, frame) -> value instanceof LamaArray;
    }

    static LamaPattern closureTag() {
        return (value, frame) -> value instanceof LamaCallable;
    }

    static LamaPattern list(List<LamaPattern> elements) {
        LamaPattern result = constant(0);
        for (int i = elements.size() - 1; i >= 0; i--) {
            result = sexp("cons", List.of(elements.get(i), result));
        }
        return result;
    }
}
