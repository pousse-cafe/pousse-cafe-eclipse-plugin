package poussecafe.eclipse.plugin.editors;

import org.eclipse.jface.text.rules.Token;

public class EmilToken extends Token {

    public EmilToken(int offset, int length, Object data) {
        super(data);
        this.offset = offset;
        this.length = length;
    }

    public int length() {
        return length;
    }

    private int length;

    public int offset() {
        return offset;
    }

    private int offset;

    public int endInclusive() {
        return offset + length - 1;
    }
}
