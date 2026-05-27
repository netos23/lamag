package pro.fbtw.lamag.runtime;

public final class LamaCell {
    private Object value;

    public LamaCell(Object value) {
        this.value = value;
    }

    public Object get() {
        return value;
    }

    public void set(Object value) {
        this.value = value;
    }
}
