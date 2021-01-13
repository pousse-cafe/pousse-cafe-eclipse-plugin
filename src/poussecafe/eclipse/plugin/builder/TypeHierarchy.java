package poussecafe.eclipse.plugin.builder;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import static java.util.Objects.requireNonNull;

public class TypeHierarchy {

    public TypeHierarchy(ITypeHierarchy typeHierarchy) {
        requireNonNull(typeHierarchy);
        this.typeHierarchy = typeHierarchy;
        buildQualfiedNamesIndex();
    }

    private ITypeHierarchy typeHierarchy;

    public boolean contains(IType type) {
        return qualifiedNamesIndex().contains(type.getFullyQualifiedName());
    }

    private Set<String> qualifiedNamesIndex() {
        if(qualifiedNamesIndex == null) {
            buildQualfiedNamesIndex();
        }
        return qualifiedNamesIndex;
    }

    private Set<String> qualifiedNamesIndex;

    private void buildQualfiedNamesIndex() {
        qualifiedNamesIndex = new HashSet<>();
        for(IType type : typeHierarchy.getAllTypes()) {
            qualifiedNamesIndex.add(type.getFullyQualifiedName());
        }
    }

    public IType[] getAllTypes() {
        return typeHierarchy.getAllTypes();
    }

    public IType[] getSubTypes(IType type) {
        return typeHierarchy.getSubtypes(type);
    }
}
