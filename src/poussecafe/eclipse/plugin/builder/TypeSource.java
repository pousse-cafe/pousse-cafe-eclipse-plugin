package poussecafe.eclipse.plugin.builder;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTParser;
import poussecafe.source.Source;

@SuppressWarnings("serial")
public class TypeSource extends Source {

    @Override
    protected void configure(ASTParser parser) {
        throw new UnsupportedOperationException();
    }

    public TypeSource(IType type) {
        super(type.getElementName());
    }
}
