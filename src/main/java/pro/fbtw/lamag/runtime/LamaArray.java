package pro.fbtw.lamag.runtime;

import pro.fbtw.lamag.LamaException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LamaArray {
    private final List<Object> values;

    public LamaArray(List<Object> values) {
        this.values = new ArrayList<>(values);
    }

    public static LamaArray filled(int size, Object value) {
        return new LamaArray(new ArrayList<>(Collections.nCopies(size, value)));
    }

    public int size() {
        return values.size();
    }

    public Object get(long index) {
        int i = checkedIndex(index);
        return values.get(i);
    }

    public void set(long index, Object value) {
        int i = checkedIndex(index);
        values.set(i, value);
    }

    public List<Object> values() {
        return values;
    }

    private int checkedIndex(long index) {
        if (index < 0 || index >= values.size()) {
            throw LamaException.error("array index out of bounds: " + index);
        }
        return (int) index;
    }
}
