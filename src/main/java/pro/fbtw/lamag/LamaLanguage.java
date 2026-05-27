package pro.fbtw.lamag;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import pro.fbtw.lamag.nodes.LamaProgramRootNode;
import pro.fbtw.lamag.parser.LamaSourceParser;
import pro.fbtw.lamag.runtime.LamaContext;
import pro.fbtw.lamag.runtime.LamaProgram;

@TruffleLanguage.Registration(
        id = LamaLanguage.ID,
        name = "Lama",
        defaultMimeType = LamaLanguage.MIME_TYPE,
        characterMimeTypes = LamaLanguage.MIME_TYPE,
        contextPolicy = ContextPolicy.SHARED,
        fileTypeDetectors = LamaFileDetector.class,
        website = "https://github.com/PLTools/Lama")
public final class LamaLanguage extends TruffleLanguage<LamaContext> {
    public static final String ID = "lama";
    public static final String MIME_TYPE = "application/x-lama";

    @Override
    protected LamaContext createContext(Env env) {
        return new LamaContext(env);
    }

    @Override
    protected boolean patchContext(LamaContext context, Env newEnv) {
        context.patchEnv(newEnv);
        return true;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) {
        Source source = request.getSource();
        LamaProgram program = LamaSourceParser.parse(this, source);
        RootNode root = new LamaProgramRootNode(this, new FrameDescriptor(), program);
        return root.getCallTarget();
    }
}
