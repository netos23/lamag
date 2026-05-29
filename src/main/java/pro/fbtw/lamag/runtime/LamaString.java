package pro.fbtw.lamag.runtime;

import pro.fbtw.lamag.LamaException;

import java.util.Arrays;

/**
 * A Lama string value. Unlike a Java {@link String}, Lama strings are mutable
 * fixed-length arrays of characters (e.g. {@code s[i] := s[i] + 2}), so they are
 * backed by a {@code char[]} we can update in place. Reading {@code s[i]} yields
 * the character code as an integer.
 */
public final class LamaString {
    private final char[] chars;

    public LamaString(String value) {
        this.chars = value.toCharArray();
    }

    public LamaString(char[] chars) {
        this.chars = chars;
    }

    public static LamaString filled(int length, char fill) {
        char[] chars = new char[length];
        Arrays.fill(chars, fill);
        return new LamaString(chars);
    }

    public int length() {
        return chars.length;
    }

    public long get(long index) {
        return chars[checkedIndex(index)];
    }

    public void set(long index, long value) {
        chars[checkedIndex(index)] = (char) value;
    }

    public String value() {
        return new String(chars);
    }

    @Override
    public String toString() {
        return new String(chars);
    }

    private int checkedIndex(long index) {
        if (index < 0 || index >= chars.length) {
            throw LamaException.error("string index out of bounds: " + index);
        }
        return (int) index;
    }
}
