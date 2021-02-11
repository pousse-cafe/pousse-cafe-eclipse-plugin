package poussecafe.eclipse.plugin.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.IType;
import poussecafe.source.analysis.ClassName;
import poussecafe.source.analysis.ResolvedClass;
import poussecafe.source.validation.ClassPathExplorer;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

public class JdtClassPathExplorer implements ClassPathExplorer {

    @Override
    public Set<ResolvedClass> getSubTypesOf(ClassName superTypeName) {
        var resolvedClass = classResolver.loadClass(superTypeName).map(JdtResolvedClass.class::cast);
        if(resolvedClass.isPresent()) {
            var subTypes = new HashMap<String, List<IType>>();
            for(IType resolvedType : resolvedClass.get().types()) {
                var hierarchy = classResolver.typeHierarchies().newTypeHierarchy(resolvedType);
                for(IType resolvedTypeSubType : hierarchy.getSubtypes(resolvedType)) {
                    var resolvedTypeSubTypes = subTypes.computeIfAbsent(resolvedTypeSubType.getFullyQualifiedName(),
                            name -> new ArrayList<>());
                    resolvedTypeSubTypes.add(resolvedTypeSubType);
                }
            }
            return subTypes.values().stream()
                    .map(types -> classResolver.resolve(types))
                    .collect(toSet());
        } else {
            return emptySet();
        }
    }

    private JdtClassResolver classResolver;

    public JdtClassPathExplorer(JdtClassResolver classResolver) {
        requireNonNull(classResolver);
        this.classResolver = classResolver;
    }
}
