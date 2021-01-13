package poussecafe.eclipse.plugin.editors;

import static java.util.Objects.requireNonNull;

public class EmilTokenScannerInput {

    public EmilTokenScannerInput(int offset, String documentPart) {
        this.offset = offset;
        requireNonNull(documentPart);
        this.documentPart = documentPart;
    }

    private int offset;

    private String documentPart;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((documentPart == null) ? 0 : documentPart.hashCode());
        result = prime * result + offset;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        EmilTokenScannerInput other = (EmilTokenScannerInput) obj;
        if(documentPart == null) {
            if(other.documentPart != null) {
                return false;
            }
        } else if(!documentPart.equals(other.documentPart)) {
            return false;
        }
        if(offset != other.offset) {
            return false;
        }
        return true;
    }
}
