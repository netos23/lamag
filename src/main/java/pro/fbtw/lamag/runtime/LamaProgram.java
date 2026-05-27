package pro.fbtw.lamag.runtime;

import pro.fbtw.lamag.nodes.LamaDeclarationNode;
import pro.fbtw.lamag.nodes.LamaExpressionNode;

public final class LamaProgram {
    private final LamaDeclarationNode[] declarations;
    private final LamaExpressionNode body;

    public LamaProgram(LamaDeclarationNode[] declarations, LamaExpressionNode body) {
        this.declarations = declarations;
        this.body = body;
    }

    public LamaDeclarationNode[] declarations() {
        return declarations;
    }

    public LamaExpressionNode body() {
        return body;
    }
}
