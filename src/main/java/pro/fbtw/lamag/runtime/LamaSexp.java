package pro.fbtw.lamag.runtime;

import pro.fbtw.lamag.LamaException;

import java.util.Arrays;

public final class LamaSexp {
    private final String tag;
    private final Object[] fields;

    public LamaSexp(String tag, Object[] fields) {
        this.tag = tag;
        this.fields = fields.clone();
    }

    public String tag() {
        return tag;
    }

    public int size() {
        return fields.length;
    }

    public Object get(long index) {
        if (index < 0 || index >= fields.length) {
            throw LamaException.error("S-expression index out of bounds: " + index);
        }
        return fields[(int) index];
    }

    public Object[] fields() {
        return fields.clone();
    }

    @Override
    public String toString() {
        return tag + Arrays.toString(fields);
    }
}
