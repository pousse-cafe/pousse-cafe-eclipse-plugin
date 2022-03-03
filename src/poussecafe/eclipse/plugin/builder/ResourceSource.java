package poussecafe.eclipse.plugin.builder;

import java.io.Serializable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
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
        return type != null || source != null;
    }

    public boolean hasSource() {
        return compilationUnit != null || source != null;
    }

    @Override
    public void configure(ASTParser parser) {
        if(compilationUnit != null) {
            parser.setSource(resourceCompilationUnit());
        } else if(source != null) {
            parser.setSource(source.toCharArray());
        } else {
            throw new IllegalStateException("No source available");
        }
    }

    private ICompilationUnit resourceCompilationUnit() {
        connectedOrThrow();
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

    public ResourceSource(IClassFile classFile) {
        super(classFile.getElementName());

        classFileHandleIdentifier = classFile.getHandleIdentifier();
        connect(classFile.getJavaProject());
    }

    private String classFileHandleIdentifier;

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

        if(type == null && typeName != null) {
            relativePath = null;
            try {
                type = javaProject.findType(typeName);
            } catch (Exception e) {
                // Do nothing
            }
        }

        if(type == null && classFileHandleIdentifier != null) {
            relativePath = null;
            try {
                var classFile = (IClassFile) JavaCore.create(classFileHandleIdentifier);
                type = classFile.findPrimaryType();
                source = classFile.getSource();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    private String source;

    ResourceSource() {

    }
}
