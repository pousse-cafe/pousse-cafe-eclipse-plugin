package poussecafe.eclipse.plugin.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import poussecafe.source.analysis.ClassResolver;
import poussecafe.source.analysis.Name;

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
                return searchType(new Name(name));
            } else {
                return asList(type);
            }
        } catch (JavaModelException e) {
            throw new ClassNotFoundException("Error while searching for type " + name, e);
        }
    }

    private List<IType> searchType(Name typeName) {
        return searchResults.computeIfAbsent(typeName, this::doSearchType);
    }

    private Map<Name, List<IType>> searchResults = new HashMap<>();

    private List<IType> doSearchType(Name typeName) {
        var requestor = new SearchRequestor();
        try {
            searchEngine.searchAllTypeNames(
                    typeName.qualifier().toCharArray(),
                    SearchPattern.R_EXACT_MATCH,
                    typeName.simple().toCharArray(),
                    SearchPattern.R_EXACT_MATCH,
                    IJavaSearchConstants.CLASS_AND_INTERFACE,
                    SearchEngine.createWorkspaceScope(),
                    requestor,
                    IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH,
                    null);
        } catch (JavaModelException e) {
            Platform.getLog(getClass()).error("Unable to search for types", e);
        }
        return requestor.foundTypes;
    }

    private static class SearchRequestor extends TypeNameMatchRequestor {
        @Override
        public void acceptTypeNameMatch(TypeNameMatch match) {
            foundTypes.add(match.getType());
        }

        private List<IType> foundTypes = new ArrayList<>();
    }

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

        searchEngine = new SearchEngine();
    }

    private TypeHierarchies typeHierarchies = new TypeHierarchies();

    public TypeHierarchies typeHierarchies() {
        return typeHierarchies;
    }

    private SearchEngine searchEngine;
}
