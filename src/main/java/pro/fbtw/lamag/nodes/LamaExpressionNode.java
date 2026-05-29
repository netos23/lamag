package pro.fbtw.lamag.nodes;

import com.oracle.truffle.api.nodes.Node;
import pro.fbtw.lamag.runtime.LamaFrame;

public abstract class LamaExpressionNode extends Node {
    public abstract Object executeGeneric(LamaFrame env);
}
