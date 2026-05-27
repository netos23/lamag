package pro.fbtw.lamag.parser;

import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import pro.fbtw.lamag.LamaException;
import pro.fbtw.lamag.LamaLanguage;
import pro.fbtw.lamag.runtime.LamaProgram;

public final class LamaSourceParser {
    private LamaSourceParser() {
    }

    public static LamaProgram parse(LamaLanguage language, Source source) {
        LamaLexer lexer = new LamaLexer(CharStreams.fromString(source.getCharacters().toString()));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener(source.getName()));

        LamaParser parser = new LamaParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener(source.getName()));

        LamaAstBuilder builder = new LamaAstBuilder(language);
        return builder.build(parser.program());
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        private final String sourceName;

        private ThrowingErrorListener(String sourceName) {
            this.sourceName = sourceName;
        }

        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException e) {
            throw LamaException.error(sourceName + ":" + line + ":" + (charPositionInLine + 1) + ": " + msg);
        }
    }
}
