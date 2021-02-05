package poussecafe.eclipse.plugin.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class Resources {

    public static boolean isJavaSourceFile(IResource resource) {
        var extension = resource.getFileExtension();
        return resource instanceof IFile
                && extension != null
                && extension.equals("java");
    }

    public static boolean isHealthy(IResource resource) {
        try {
            var markers = resource.findMarkers("org.eclipse.jdt.core.problem", true, IResource.DEPTH_INFINITE);
            return markers.length == 0;
        } catch (CoreException e) {
            return false;
        }
    }

    private Resources() {

    }
}
