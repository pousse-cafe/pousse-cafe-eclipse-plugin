package poussecafe.eclipse.plugin.views;

import org.eclipse.jdt.core.IJavaProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poussecafe.eclipse.plugin.builder.JavaSourceFileVisitor;
import poussecafe.eclipse.plugin.builder.ResourceSource;
import poussecafe.source.SourceModelBuilder;

import static java.util.Objects.requireNonNull;

public class ModelBuildingResourceVisitor extends JavaSourceFileVisitor {

    public ModelBuildingResourceVisitor(IJavaProject javaProject, SourceModelBuilder builder) {
        super(javaProject);
        requireNonNull(builder);
        this.builder = builder;
    }

    private SourceModelBuilder builder;

    @Override
    protected void visit(ResourceSource source) {
        try {
            builder.includeSource(source);
        } catch (Exception e) {
            logger.debug("Skipping source {}", source.id(), e);
        }
    }

    private Logger logger = LoggerFactory.getLogger(getClass());
}