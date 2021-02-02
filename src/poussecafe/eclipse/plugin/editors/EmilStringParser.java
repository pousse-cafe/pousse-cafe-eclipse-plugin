package poussecafe.eclipse.plugin.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.eclipse.jface.text.Position;
import poussecafe.source.emil.parser.EmilLexer;
import poussecafe.source.emil.parser.EmilParser;
import poussecafe.source.emil.parser.EmilParser.ProcessContext;

public class EmilStringParser {

    public EmilStringParser(String emilText) {
        var stream = CharStreams.fromString(emilText);
        var lexer = new EmilLexer(stream);
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
        lexer.addErrorListener(errorListener);

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        EmilParser parser = new EmilParser(tokenStream);
        parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
        parser.addErrorListener(errorListener);

        tree = parser.process();
    }

    private ProcessContext tree;

    public ProcessContext tree() {
        return tree;
    }

    private ANTLRErrorListener errorListener = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            Position position = null;
            if(offendingSymbol instanceof Token) {
                var token = (Token) offendingSymbol;
                position = new Position(token.getStartIndex(), token.getStopIndex() - token.getStartIndex() + 1);
            }
            var error = new Error();
            error.position = position;
            error.message = msg;
            error.line = line;
            errors.add(error);
        }
    };

    public static class Error {

        private Position position;

        public Position position() {
            return position;
        }

        private String message;

        public String message() {
            return message;
        }

        private int line;

        public int line() {
            return line;
        }

        private Error() {

        }
    }

    private List<Error> errors = new ArrayList<>();

    public List<Error> errors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
