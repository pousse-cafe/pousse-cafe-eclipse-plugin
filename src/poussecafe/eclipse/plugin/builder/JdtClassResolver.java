package poussecafe.eclipse.plugin.builder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poussecafe.eclipse.plugin.core.JavaNameResolver;
import poussecafe.eclipse.plugin.core.JavaSearchEngine;
import poussecafe.source.analysis.ClassName;
import poussecafe.source.analysis.ClassResolver;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class JdtClassResolver extends ClassResolver {

    @Override
    protected JdtResolvedClass loadClass(String name) throws ClassNotFoundException {
        var types = loadType(name);
        if(types.isEmpty()) {
            throw new ClassNotFoundException(name);
        } else {
            return resolve(types);
        }
    }

    List<IType> loadType(String name) throws ClassNotFoundException {
        try {
            var type = project.findType(name);
            if(type == null) {
                return searchType(new ClassName(name));
            } else {
                return asList(type);
            }
        } catch (JavaModelException e) {
            throw new ClassNotFoundException("Error while searching for type " + name, e);
        }
    }

    private List<IType> searchType(ClassName typeName) {
        return searchResults.computeIfAbsent(typeName, this::doSearchType);
    }

    private Map<ClassName, List<IType>> searchResults = new HashMap<>();

    private List<IType> doSearchType(ClassName typeName) {
        logger.debug("Searching for type {}", typeName);
        return searchEngine.searchType(typeName);
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    public JdtResolvedClass resolve(Collection<IType> types) {
        return new JdtResolvedClass.Builder()
                .resolver(this)
                .types(types)
                .build();
    }

    private IJavaProject project;

    public JdtClassResolver(IJavaProject project) {
        requireNonNull(project);
        this.project = project;

        searchEngine = new JavaSearchEngine();
    }

    private TypeHierarchies typeHierarchies = new TypeHierarchies();

    public TypeHierarchies typeHierarchies() {
        return typeHierarchies;
    }

    private JavaSearchEngine searchEngine;

    public boolean instanceOf(JdtResolvedClass subject,
            JdtResolvedClass supertype) {
        var relation = relation(subject, supertype);
        Boolean value = instanceOfRelations.get(relation);
        if(value != null) {
            return value;
        } else {
            value = computeInstanceOf(subject, supertype);
            instanceOfRelations.put(relation, value);
            return value;
        }
    }

    private Map<String, Boolean> instanceOfRelations = new HashMap<>();

    private String relation(JdtResolvedClass subject, JdtResolvedClass supertype) {
        return subject.name().toString() + "+" + supertype.name().toString();
    }

    private Boolean computeInstanceOf(JdtResolvedClass subject, JdtResolvedClass supertype) {
        if(subject.isInterface() && !supertype.isInterface()) {
            return false;
        }
        if(supertype.name().equals(subject.name())) {
            return true;
        } else {
            try {
                for(IType type : subject.types()) {
                    if(computeInstanceOf(type, supertype)) {
                        return true;
                    }
                }
                return false;
            } catch (JavaModelException e) {
                return false;
            }
        }
    }

    private boolean computeInstanceOf(IType type, JdtResolvedClass supertype) throws JavaModelException {
        if(supertype.isInterface()) {
            for(String superinterfaceSignature : type.getSuperInterfaceTypeSignatures()) {
                var superinterfaceName = JavaNameResolver.resolveSignature(type,
                        superinterfaceSignature);
                var superinterface = loadClass(new ClassName(superinterfaceName))
                        .map(JdtResolvedClass.class::cast);
                if(superinterface.isPresent()
                        && instanceOf(superinterface.get(), supertype)) {
                    return true;
                }
            }
        }

        var superclassSignature = type.getSuperclassTypeSignature();
        if(superclassSignature != null
                && !superclassSignature.equals("java.lang.Object")) {
            var superclassName = JavaNameResolver.resolveSignature(type, superclassSignature);
            var superclass = loadClass(new ClassName(superclassName)).map(JdtResolvedClass.class::cast);
            if(superclass.isPresent()
                    && instanceOf(superclass.get(), supertype)) {
                return true;
            }
        }

        return false;
    }
}
