package poussecafe.eclipse.plugin.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class Resources {

    public static boolean isJavaSourceFile(IResource resource) {
        var extension = resource.getFileExtension();
        return resource instanceof IFile
                && extension != null
                && extension.equals("java");
    }

    private Resources() {

    }
}
