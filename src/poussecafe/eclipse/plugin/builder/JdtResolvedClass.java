package poussecafe.eclipse.plugin.builder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import poussecafe.source.analysis.ClassResolver;
import poussecafe.source.analysis.Name;
import poussecafe.source.analysis.ResolvedClass;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class JdtResolvedClass implements ResolvedClass {

    @Override
    public Optional<ResolvedClass> declaringClass() {
        if(type.getFullyQualifiedName().charAt('$') != -1) {
            return Optional.of(resolver.declaringClass(type));
        } else {
            return Optional.empty();
        }
    }

    private IType type;

    @Override
    public List<ResolvedClass> innerClasses() {
        try {
            return Arrays.stream(type.getTypes())
                    .filter(this::isClass)
                    .map(this::newResolvedClass)
                    .collect(toList());
        } catch (JavaModelException e) {
            logError("Unable to extract inner classes", e);
            return emptyList();
        }
    }

    private void logError(String message, Exception e) {
        Platform.getLog(getClass()).error(message, e);
    }

    private boolean isClass(IType candidate) {
        try {
            return candidate.isClass() || candidate.isInterface();
        } catch (JavaModelException e) {
            logError("Unable to extract inner classes", e);
            return false;
        }
    }

    private JdtResolvedClass newResolvedClass(IType type) {
        return new JdtResolvedClass.Builder()
                .resolver(resolver)
                .type(type)
                .build();
    }

    @Override
    public boolean instanceOf(String name) throws ClassNotFoundException {
        var consideredType = resolver.loadClass(name);
        return instanceOf(consideredType);
    }

    private boolean instanceOf(JdtResolvedClass consideredType) {
        try {
            if(consideredType.name().equals(name())) {
                return true;
            } else {
                var hierarchy = type.newSupertypeHierarchy(null);
                return hierarchy.contains(consideredType.type);
            }
        } catch (JavaModelException e) {
            logError("Failure with " + consideredType.type, e);
            return false;
        }
    }

    @Override
    public Name name() {
        return new Name(type.getFullyQualifiedName('.'));
    }

    @Override
    public ClassResolver resolver() {
        return resolver;
    }

    private JdtClassResolver resolver;

    @Override
    public Optional<Object> staticFieldValue(String constantName) {
        return Optional.ofNullable(getConstant(type.getField(constantName)));
    }

    private Object getConstant(IField field) {
        try {
            return field.getConstant();
        } catch (JavaModelException e) {
            return null;
        }
    }

    public static class Builder {

        public JdtResolvedClass build() {
            return resolvedClass;
        }

        private JdtResolvedClass resolvedClass = new JdtResolvedClass();

        public Builder resolver(JdtClassResolver resolver) {
            resolvedClass.resolver = resolver;
            return this;
        }

        public Builder type(IType type) {
            resolvedClass.type = type;
            return this;
        }
    }

    private JdtResolvedClass() {

    }
}
