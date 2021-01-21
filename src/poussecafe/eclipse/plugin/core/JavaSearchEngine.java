package poussecafe.eclipse.plugin.core;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import poussecafe.source.analysis.Name;

public class JavaSearchEngine {

    public List<IType> searchType(Name typeName) {
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

    public List<IType> searchTypeInProject(String simpleTypeName, IJavaProject project) {
        var requestor = new SearchRequestor();
        try {
            searchEngine.searchAllTypeNames(
                    null,
                    SearchPattern.R_EXACT_MATCH,
                    simpleTypeName.toCharArray(),
                    SearchPattern.R_EXACT_MATCH,
                    IJavaSearchConstants.CLASS_AND_INTERFACE,
                    SearchEngine.createJavaSearchScope(new IJavaElement[] {project}),
                    requestor,
                    IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH,
                    null);
        } catch (JavaModelException e) {
            Platform.getLog(getClass()).error("Unable to search for types", e);
        }
        return requestor.foundTypes;
    }

    private SearchEngine searchEngine = new SearchEngine();
}
