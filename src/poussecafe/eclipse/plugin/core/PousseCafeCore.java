package poussecafe.eclipse.plugin.core;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

public class PousseCafeCore {

    public static synchronized PousseCafeProject getProject(IJavaElement element) {
        return PROJECTS.computeIfAbsent(element.getJavaProject(), PousseCafeProject::new);
    }

    public static synchronized PousseCafeProject getProject(IJavaProject project) {
        return PROJECTS.computeIfAbsent(project, PousseCafeProject::new);
    }

    private static final Map<IJavaProject, PousseCafeProject> PROJECTS = new HashMap<>();

    private PousseCafeCore() {

    }
}
