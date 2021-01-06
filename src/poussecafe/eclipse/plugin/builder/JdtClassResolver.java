package poussecafe.eclipse.plugin.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import poussecafe.source.analysis.ClassResolver;

import static java.util.Objects.requireNonNull;

public class JdtClassResolver extends ClassResolver {

    @Override
    protected JdtResolvedClass loadClass(String name) throws ClassNotFoundException {
        try {
            IType type = project.findType(name);
            if(type == null) {
                throw new ClassNotFoundException();
            } else {
                return resolve(type);
            }
        } catch (JavaModelException e) {
            throw new ClassNotFoundException("Unable to find type " + name, e);
        }
    }

    public JdtResolvedClass resolve(IType type) {
        return new JdtResolvedClass.Builder()
                .resolver(this)
                .type(type)
                .build();
    }

    private IJavaProject project;

    public Optional<JdtResolvedClass> declaringClass(IType type) {
        try {
            String fullyQualifiedName = type.getFullyQualifiedName();
            int declaringClassNameEnd = declaringClassNameEnd(fullyQualifiedName);
            if(declaringClassNameEnd == -1) {
                return Optional.empty();
            } else {
                return Optional.of(loadClass(fullyQualifiedName.substring(0, declaringClassNameEnd)));
            }
        } catch (ClassNotFoundException e) {
            Platform.getLog(getClass()).error("Unable to get declaring class of " + type, e);
            return Optional.empty();
        }
    }

    private int declaringClassNameEnd(String fullyQualifiedName) {
        for(int i = fullyQualifiedName.length() - 1; i >= 0; --i) {
            if(fullyQualifiedName.charAt(i) == '$') {
                return i;
            }
        }
        return -1;
    }

    public JdtClassResolver(IJavaProject project) {
        requireNonNull(project);
        this.project = project;
    }

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
