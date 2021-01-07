package poussecafe.eclipse.plugin.builder;

import java.util.Arrays;
import java.util.Set;
import poussecafe.source.analysis.Name;
import poussecafe.source.analysis.ResolvedClass;
import poussecafe.source.validation.ClassPathExplorer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

public class JdtClassPathExplorer implements ClassPathExplorer {

    @Override
    public Set<ResolvedClass> getSubTypesOf(Name superTypeName) {
        var resolvedClass = (JdtResolvedClass) classResolver.loadClass(superTypeName).orElseThrow();
        var hierarchy = classResolver.typeHierarchies().newTypeHierarchy(resolvedClass.type());
        return Arrays.stream(hierarchy.getAllTypes())
                .filter(type -> type != resolvedClass.type())
                .map(type -> classResolver.resolve(type))
                .collect(toSet());
    }

    private JdtClassResolver classResolver;

    public JdtClassPathExplorer(JdtClassResolver classResolver) {
        requireNonNull(classResolver);
        this.classResolver = classResolver;
    }
}
