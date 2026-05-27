package pro.fbtw.lamag;

import com.oracle.truffle.api.nodes.Node;

public final class LamaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private LamaException(String message) {
        super(message);
    }

    public static LamaException error(String message) {
        return new LamaException(message);
    }

    public static LamaException typeError(Node node, String message) {
        return new LamaException(message);
    }
}
