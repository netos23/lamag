package pro.fbtw.lamag.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import pro.fbtw.lamag.LamaLanguage;
import pro.fbtw.lamag.runtime.LamaContext;
import pro.fbtw.lamag.runtime.LamaFrame;
import pro.fbtw.lamag.runtime.LamaProgram;

public final class LamaProgramRootNode extends RootNode {
    @Children private final LamaDeclarationNode[] declarations;
    @Child private LamaExpressionNode body;

    public LamaProgramRootNode(LamaLanguage language, FrameDescriptor frameDescriptor, LamaProgram program) {
        super(language, frameDescriptor);
        this.declarations = program.declarations();
        this.body = program.body();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeWithoutFrame();
    }

    private Object executeWithoutFrame() {
        LamaContext context = LamaContext.get(this);
        LamaFrame env = context.globalFrame();
        for (LamaDeclarationNode declaration : declarations) {
            declaration.executeDeclaration(env);
        }
        body.executeGeneric(env);
        return 0L;
    }
}
