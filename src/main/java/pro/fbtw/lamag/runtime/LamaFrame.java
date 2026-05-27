package pro.fbtw.lamag.runtime;

import pro.fbtw.lamag.LamaException;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LamaFrame {
    private final LamaFrame parent;
    private final Map<String, LamaCell> locals = new LinkedHashMap<>();

    public LamaFrame(LamaFrame parent) {
        this.parent = parent;
    }

    public LamaFrame parent() {
        return parent;
    }

    public LamaCell define(String name, Object value) {
        LamaCell cell = new LamaCell(value);
        locals.put(name, cell);
        return cell;
    }

    public boolean isDefinedLocally(String name) {
        return locals.containsKey(name);
    }

    public Object read(String name) {
        return resolve(name).get();
    }

    public void write(String name, Object value) {
        resolve(name).set(value);
    }

    public LamaCell resolve(String name) {
        LamaCell cell = locals.get(name);
        if (cell != null) {
            return cell;
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        throw LamaException.error("undefined variable: " + name);
    }
}
