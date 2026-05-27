package pro.fbtw.lamag.nodes;

import com.oracle.truffle.api.nodes.Node;
import pro.fbtw.lamag.runtime.LamaFrame;

public abstract class LamaDeclarationNode extends Node {
    public abstract void executeDeclaration(LamaFrame env);
}
