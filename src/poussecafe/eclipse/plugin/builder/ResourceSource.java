package poussecafe.eclipse.plugin.builder;

import java.io.Serializable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import poussecafe.source.Source;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("serial")
public class ResourceSource extends Source implements Serializable {

    public IType sourceType() {
        return type;
    }

    private transient IType type;

    private void connectedOrThrow() {
        if(!isConnected()) {
            throw new IllegalStateException("Connect resource to a project");
        }
    }

    public boolean isConnected() {
        return type != null;
    }

    public boolean hasFile() {
        connectedOrThrow();
        return file != null;
    }

    @Override
    public void configure(ASTParser parser) {
        parser.setSource(resourceCompilationUnit());
    }

    public ICompilationUnit resourceCompilationUnit() {
        connectedOrThrow();
        if(compilationUnit == null) {

        }
        return compilationUnit;
    }

    private transient ICompilationUnit compilationUnit;

    public IFile file() {
        connectedOrThrow();
        return file;
    }

    private transient IFile file;

    public ResourceSource(IFile file) {
        super(sourceId(file));
        var project = JavaCore.create(file.getProject());

        relativePath = file.getProjectRelativePath().toString();
        connect(project);
    }

    public static String sourceId(IFile file) {
        return file.getProjectRelativePath().toString();
    }

    private String relativePath;

    public ResourceSource(IType type) {
        super(type.getFullyQualifiedName());

        typeName = type.getFullyQualifiedName();
        connect(type.getJavaProject());
    }

    private String typeName;

    @Override
    public void connect(Object project) {
        requireNonNull(project);
        var javaProject = (IJavaProject) project;

        if(relativePath != null) {
            IPath path = new Path(relativePath);
            file = javaProject.getProject().getFile(path);
            if(file == null
                    || !file.isAccessible()) {
                file = null;
            } else {
                compilationUnit = (ICompilationUnit) JavaCore.create(file, javaProject);
                if(compilationUnit != null) {
                    type = compilationUnit.findPrimaryType();
                    if(type != null) {
                        typeName = type.getFullyQualifiedName();
                    }
                }
            }
        }

        if(type == null) {
            relativePath = null;
            try {
                type = javaProject.findType(typeName);
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    ResourceSource() {

    }
}
