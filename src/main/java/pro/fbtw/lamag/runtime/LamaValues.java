package pro.fbtw.lamag.runtime;

import pro.fbtw.lamag.LamaException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LamaValues {
    private static final String TAG_CHARS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'";

    private LamaValues() {
    }

    public static long asLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        throw LamaException.error("integer value expected, got " + typeName(value));
    }

    public static int truth(Object value) {
        return asLong(value) != 0 ? 1 : 0;
    }

    public static String asString(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        throw LamaException.error("string value expected, got " + typeName(value));
    }

    public static Object element(Object value, Object indexValue) {
        long index = asLong(indexValue);
        if (value instanceof LamaArray) {
            return ((LamaArray) value).get(index);
        }
        if (value instanceof LamaSexp) {
            return ((LamaSexp) value).get(index);
        }
        if (value instanceof String) {
            String text = (String) value;
            if (index < 0 || index >= text.length()) {
                throw LamaException.error("string index out of bounds: " + index);
            }
            return (long) text.charAt((int) index);
        }
        throw LamaException.error("aggregate value expected, got " + typeName(value));
    }

    public static void setElement(Object value, Object indexValue, Object newValue) {
        long index = asLong(indexValue);
        if (value instanceof LamaArray) {
            ((LamaArray) value).set(index, newValue);
            return;
        }
        throw LamaException.error("mutable array expected, got " + typeName(value));
    }

    public static long length(Object value) {
        if (value instanceof LamaArray) {
            return ((LamaArray) value).size();
        }
        if (value instanceof LamaSexp) {
            return ((LamaSexp) value).size();
        }
        if (value instanceof String) {
            return ((String) value).length();
        }
        throw LamaException.error("boxed value expected in length, got " + typeName(value));
    }

    public static LamaSexp cons(Object head, Object tail) {
        return new LamaSexp("cons", new Object[]{head, tail});
    }

    public static Object list(List<Object> values) {
        Object result = 0L;
        for (int i = values.size() - 1; i >= 0; i--) {
            result = cons(values.get(i), result);
        }
        return result;
    }

    public static long compare(Object left, Object right) {
        int rank = Integer.compare(rank(left), rank(right));
        if (rank != 0) {
            return rank;
        }
        if (left instanceof Long) {
            return Long.compare((Long) left, (Long) right);
        }
        if (left instanceof String) {
            return ((String) left).compareTo((String) right);
        }
        if (left instanceof LamaArray) {
            return compareArrays((LamaArray) left, (LamaArray) right);
        }
        if (left instanceof LamaSexp) {
            return compareSexps((LamaSexp) left, (LamaSexp) right);
        }
        return left == right ? 0 : Integer.compare(System.identityHashCode(left), System.identityHashCode(right));
    }

    public static boolean equal(Object left, Object right) {
        return compare(left, right) == 0;
    }

    public static String print(Object value) {
        if (value instanceof Long || value instanceof String) {
            return String.valueOf(value);
        }
        if (value instanceof LamaArray) {
            List<String> parts = new ArrayList<>();
            for (Object item : ((LamaArray) value).values()) {
                parts.add(print(item));
            }
            return "[" + String.join(", ", parts) + "]";
        }
        if (value instanceof LamaSexp) {
            LamaSexp sexp = (LamaSexp) value;
            if (sexp.tag().equals("cons")) {
                String list = printList(sexp);
                if (list != null) {
                    return list;
                }
            }
            Object[] fields = sexp.fields();
            List<String> parts = new ArrayList<>();
            for (Object field : fields) {
                parts.add(print(field));
            }
            return fields.length == 0 ? sexp.tag() : sexp.tag() + "(" + String.join(", ", parts) + ")";
        }
        if (value instanceof LamaBuiltinFunction) {
            return ((LamaBuiltinFunction) value).toString();
        }
        if (value instanceof LamaFunction) {
            return ((LamaFunction) value).toString();
        }
        return Objects.toString(value);
    }

    public static String typeName(Object value) {
        if (value instanceof Long) {
            return "int";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof LamaArray) {
            return "array";
        }
        if (value instanceof LamaSexp) {
            return "sexp";
        }
        if (value instanceof LamaCallable) {
            return "function";
        }
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    public static long tagHash(String tag) {
        long hash = 0;
        int limit = Math.min(tag.length(), 10);
        for (int i = 0; i < limit; i++) {
            int pos = TAG_CHARS.indexOf(tag.charAt(i));
            if (pos < 0) {
                throw LamaException.error("tagHash: character not found: " + tag.charAt(i));
            }
            hash = (hash << 6) | pos;
        }
        return hash;
    }

    private static long compareArrays(LamaArray left, LamaArray right) {
        int min = Math.min(left.size(), right.size());
        for (int i = 0; i < min; i++) {
            long compare = compare(left.get(i), right.get(i));
            if (compare != 0) {
                return compare;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private static long compareSexps(LamaSexp left, LamaSexp right) {
        int tagCompare = left.tag().compareTo(right.tag());
        if (tagCompare != 0) {
            return tagCompare;
        }
        return compareArrays(new LamaArray(List.of(left.fields())), new LamaArray(List.of(right.fields())));
    }

    private static int rank(Object value) {
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
        return 5;
    }

    private static String printList(LamaSexp cons) {
        List<String> parts = new ArrayList<>();
        Object current = cons;
        while (current instanceof LamaSexp && ((LamaSexp) current).tag().equals("cons") && ((LamaSexp) current).size() == 2) {
            LamaSexp pair = (LamaSexp) current;
            Object[] fields = pair.fields();
            parts.add(print(fields[0]));
            current = fields[1];
        }
        if (current instanceof Long && ((Long) current) == 0L) {
            return "{" + String.join(", ", parts) + "}";
        }
        return null;
    }
}
