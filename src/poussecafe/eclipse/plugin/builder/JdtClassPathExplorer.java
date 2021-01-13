package poussecafe.eclipse.plugin.builder;

import java.util.Arrays;
import java.util.Set;
import org.eclipse.jdt.core.IType;
import poussecafe.source.analysis.Name;
import poussecafe.source.analysis.ResolvedClass;
import poussecafe.source.validation.ClassPathExplorer;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

public class JdtClassPathExplorer implements ClassPathExplorer {

    @Override
    public Set<ResolvedClass> getSubTypesOf(Name superTypeName) {
        var resolvedClass = classResolver.loadClass(superTypeName).map(JdtResolvedClass.class::cast);
        if(resolvedClass.isPresent()) {
        IType resolvedType = resolvedClass.get().type();
        var hierarchy = classResolver.typeHierarchies().newTypeHierarchy(resolvedType);
        return Arrays.stream(hierarchy.getAllTypes())
                .filter(type -> type != resolvedType)
                .map(type -> classResolver.resolve(type))
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
