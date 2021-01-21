package poussecafe.eclipse.plugin.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import poussecafe.source.Source;
import poussecafe.source.analysis.ClassResolver;
import poussecafe.source.analysis.Name;
import poussecafe.source.analysis.ResolvedClass;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class JdtResolvedClass implements ResolvedClass {

    @Override
    public Optional<ResolvedClass> declaringClass() {
        if(isInnerClass()) {
            var declaringClassesTypes = declaringTypes(jdtName);
            if(declaringClassesTypes.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(resolver.resolve(declaringClassesTypes));
            }
        } else {
            return Optional.empty();
        }
    }

    private boolean isInnerClass() {
        return jdtName.indexOf(JDT_INNER_CLASS_SEPARATOR) != -1;
    }

    private static final char JDT_INNER_CLASS_SEPARATOR = '$';

    private List<IType> declaringTypes(String fullyQualifiedName) {
        try {
            int declaringClassNameEnd = declaringClassNameEnd(fullyQualifiedName);
            return resolver.loadType(fullyQualifiedName.substring(0, declaringClassNameEnd));
        } catch (ClassNotFoundException e) {
            Platform.getLog(getClass()).error("Unable to get declaring class of " + fullyQualifiedName, e);
            return emptyList();
        }
    }

    private int declaringClassNameEnd(String fullyQualifiedName) {
        for(int i = fullyQualifiedName.length() - 1; i >= 0; --i) {
            if(fullyQualifiedName.charAt(i) == JDT_INNER_CLASS_SEPARATOR) {
                return i;
            }
        }
        return -1;
    }

    private Set<IType> types = new HashSet<>();

    public Set<IType> types() {
        return types;
    }

    @Override
    public List<ResolvedClass> innerClasses() {
        var innerTypes = new HashMap<String, List<IType>>();
        for(IType type : types) {
            try {
                var typeInnerTypes = Arrays.stream(type.getTypes())
                        .filter(this::isResolvableType)
                        .collect(toList());
                for(IType innerType : typeInnerTypes) {
                    var innerTypeTypes = innerTypes.computeIfAbsent(innerType.getFullyQualifiedName(),
                            name -> new ArrayList<>());
                    innerTypeTypes.add(innerType);
                }
            } catch (JavaModelException e) {
                logError("Unable to extract inner classes", e);
            }
        }
        return innerTypes.values().stream()
                .map(innerTypesClasses -> resolver.resolve(innerTypesClasses))
                .collect(toList());
    }

    private void logError(String message, Exception e) {
        Platform.getLog(getClass()).error(message, e);
    }

    private boolean isResolvableType(IType candidate) {
        try {
            return candidate.isClass() || candidate.isInterface() || candidate.isEnum();
        } catch (JavaModelException e) {
            logError("Unable to extract inner classes", e);
            return false;
        }
    }

    @Override
    public boolean instanceOf(String name) throws ClassNotFoundException {
        var consideredType = resolver.loadClass(name);
        return instanceOf(consideredType);
    }

    private boolean instanceOf(JdtResolvedClass superclass) {
        if(superclass.name().equals(name())) {
            return true;
        } else {
            for(IType superclassType : superclass.types()) {
                var supertypeHierarchy = resolver.typeHierarchies().newTypeHierarchy(superclassType);
                for(IType type : types) {
                    if(supertypeHierarchy.contains(type)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public Name name() {
        return javaName;
    }

    private String jdtName;

    private Name javaName;

    @Override
    public ClassResolver resolver() {
        return resolver;
    }

    private JdtClassResolver resolver;

    @Override
    public Optional<Object> staticFieldValue(String constantName) {
        Set<Object> values = types.stream()
                .map(type -> type.getField(constantName))
                .map(this::getConstant)
                .collect(toSet());
        if(values.size() > 1) {
            throw new IllegalStateException("Conflicting values for constant " + constantName);
        } else if(values.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(values.iterator().next());
        }
    }

    private Object getConstant(IField field) {
        try {
            return field.getConstant();
        } catch (JavaModelException e) {
            return null;
        }
    }

    @Override
    public Optional<Source> source() {
        Optional<IFile> file = types.stream()
                .filter(type -> type.getResource() != null)
                .map(IType::getResource)
                .filter(resource -> resource instanceof IFile)
                .map(IFile.class::cast)
                .findFirst();
        if(file.isPresent()) {
            return Optional.of(new ResourceSource.Builder()
                    .project(JavaCore.create(file.get().getProject()))
                    .file(file.get())
                    .build());
        } else {
            return Optional.empty();
        }
    }

    public static class Builder {

        public JdtResolvedClass build() {
            requireNonNull(resolvedClass.resolver);
            if(resolvedClass.types.isEmpty()) {
                throw new IllegalStateException("Resolved class must have at least one linked type");
            }

            Set<String> javaNames = resolvedClass.types.stream()
                    .map(type -> type.getFullyQualifiedName('.'))
                    .collect(toSet());
            if(javaNames.size() > 1) {
                throw new IllegalStateException("Conflicting java names");
            }
            resolvedClass.javaName = new Name(javaNames.iterator().next());

            Set<String> jdtNames = resolvedClass.types.stream()
                    .map(IType::getFullyQualifiedName)
                    .collect(toSet());
            if(jdtNames.size() > 1) {
                throw new IllegalStateException("Conflicting JDT names");
            }
            resolvedClass.jdtName = jdtNames.iterator().next();

            return resolvedClass;
        }

        private JdtResolvedClass resolvedClass = new JdtResolvedClass();

        public Builder resolver(JdtClassResolver resolver) {
            resolvedClass.resolver = resolver;
            return this;
        }

        public Builder types(Collection<IType> types) {
            resolvedClass.types.clear();
            resolvedClass.types.addAll(types);
            return this;
        }
    }

    private JdtResolvedClass() {

    }
}
