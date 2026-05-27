package pro.fbtw.lamag.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import pro.fbtw.lamag.LamaException;
import pro.fbtw.lamag.LamaLanguage;
import pro.fbtw.lamag.runtime.LamaFrame;
import pro.fbtw.lamag.runtime.LamaFunction;
import pro.fbtw.lamag.runtime.LamaPattern;

public final class LamaFunctionRootNode extends RootNode {
    private final String name;
    private final LamaPattern[] parameters;
    @Child private LamaExpressionNode body;

    public LamaFunctionRootNode(LamaLanguage language, String name, LamaPattern[] parameters, LamaExpressionNode body) {
        super(language, new FrameDescriptor());
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        LamaFunction function = (LamaFunction) args[0];
        LamaFrame local = new LamaFrame(function.closure());
        for (int i = 0; i < parameters.length; i++) {
            Object value = args[i + 1];
            if (!parameters[i].bind(value, local)) {
                throw LamaException.error("argument " + (i + 1) + " does not match parameter pattern in " + name);
            }
        }
        return body.executeGeneric(frame, local);
    }
}
