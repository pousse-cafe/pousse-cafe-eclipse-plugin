package poussecafe.eclipse.plugin.editors;

public class EmilPartition {

    public EmilPartition(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    private int offset;

    public int offset() {
        return offset;
    }

    private int length;

    public int length() {
        return length;
    }
}
