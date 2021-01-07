package poussecafe.eclipse.plugin.builder;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class TypeHierarchies {

    public TypeHierarchy newSupertypeHierarchy(IType type) {
        return supertypeHierarchyCache.computeIfAbsent(type.getFullyQualifiedName(), name -> supertypeHierarchy(type));
    }

    private Map<String, TypeHierarchy> supertypeHierarchyCache = new HashMap<>();

    private TypeHierarchy supertypeHierarchy(IType type) {
        try {
            return new TypeHierarchy(type.newSupertypeHierarchy(null));
        } catch (JavaModelException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public TypeHierarchy newTypeHierarchy(IType type) {
        return typeHierarchyCache.computeIfAbsent(type.getFullyQualifiedName(), name -> typeHierarchy(type));
    }

    private Map<String, TypeHierarchy> typeHierarchyCache = new HashMap<>();

    private TypeHierarchy typeHierarchy(IType type) {
        try {
            return new TypeHierarchy(type.newTypeHierarchy(null));
        } catch (JavaModelException e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
