package poussecafe.eclipse.plugin.builder;

import java.util.Optional;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
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

    private TypeHierarchies typeHierarchies = new TypeHierarchies();

    public TypeHierarchies typeHierarchies() {
        return typeHierarchies;
    }
}
