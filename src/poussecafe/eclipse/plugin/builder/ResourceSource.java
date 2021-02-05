package poussecafe.eclipse.plugin.builder;

import java.io.Serializable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import poussecafe.source.Source;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("serial")
public class ResourceSource extends Source implements Serializable {

    @Override
    public void configure(ASTParser parser) {
        parser.setSource(resourceCompilationUnit());
    }

    public ICompilationUnit resourceCompilationUnit() {
        connectedOrThrow();
        if(compilationUnit == null) {
            compilationUnit = (ICompilationUnit) JavaCore.create(file(), project);
        }
        return compilationUnit;
    }

    private void connectedOrThrow() {
        if(project == null) {
            throw new IllegalStateException("Connect resource to a project");
        }
    }

    private transient ICompilationUnit compilationUnit;

    public IFile file() {
        connectedOrThrow();
        if(file == null) {
            IPath path = new Path(id());
            file = project.getProject().getFile(path);
        }
        return file;
    }

    private transient IFile file;

    @Override
    public void connect(Object project) {
        requireNonNull(project);
        this.project = (IJavaProject) project;
    }

    private transient IJavaProject project;

    public ResourceSource(IFile file) {
        super(file.getProjectRelativePath().toString());
    }

    ResourceSource() {

    }
}
