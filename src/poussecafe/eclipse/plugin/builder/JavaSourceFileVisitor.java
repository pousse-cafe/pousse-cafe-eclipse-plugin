package poussecafe.eclipse.plugin.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.jdt.core.IJavaProject;

public abstract class JavaSourceFileVisitor implements IResourceVisitor {

    @Override
    public boolean visit(IResource resource) {
        if(Resources.isJavaSourceFile(resource)
                && Resources.isHealthy(resource)) {
            IFile file = (IFile) resource;
            var source = new ResourceSource(file);
            source.connect(javaProject);
            visit(source);
        }
        return true;
    }

    private IJavaProject javaProject;

    protected abstract void visit(ResourceSource source);

    protected JavaSourceFileVisitor(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }
}
