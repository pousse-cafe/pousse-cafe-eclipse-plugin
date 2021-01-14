package poussecafe.eclipse.plugin.builder;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

public class TypeHierarchies {

    public ITypeHierarchy newSupertypeHierarchy(IType type) {
        return supertypeHierarchyCache.computeIfAbsent(type, this::supertypeHierarchy);
    }

    private Map<IType, ITypeHierarchy> supertypeHierarchyCache = new HashMap<>();

    private ITypeHierarchy supertypeHierarchy(IType type) {
        try {
            return type.newSupertypeHierarchy(null);
        } catch (JavaModelException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public ITypeHierarchy newTypeHierarchy(IType type) {
        return typeHierarchyCache.computeIfAbsent(type, this::typeHierarchy);
    }

    private Map<IType, ITypeHierarchy> typeHierarchyCache = new HashMap<>();

    private ITypeHierarchy typeHierarchy(IType type) {
        try {
            return type.newTypeHierarchy(null);
        } catch (JavaModelException e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
