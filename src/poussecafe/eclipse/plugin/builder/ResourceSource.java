package poussecafe.eclipse.plugin.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import poussecafe.source.Source;

import static java.util.Objects.requireNonNull;

public class ResourceSource implements Source {

    @Override
    public void configure(ASTParser parser) {
        parser.setSource(compilationUnit);
    }

    private ICompilationUnit compilationUnit;

    @Override
    public String id() {
        return id;
    }

    private String id;

    public static class Builder {

        public ResourceSource build() {
            requireNonNull(file);
            source.id = file.getFullPath().toString();

            requireNonNull(project);
            source.compilationUnit = (ICompilationUnit) JavaCore.create(file, project);

            return source;
        }

        public Builder file(IFile file) {
            this.file = file;
            return this;
        }

        private IFile file;

        private ResourceSource source = new ResourceSource();

        public Builder project(IJavaProject project) {
            this.project = project;
            return this;
        }

        private IJavaProject project;
    }

    private ResourceSource() {

    }
}
