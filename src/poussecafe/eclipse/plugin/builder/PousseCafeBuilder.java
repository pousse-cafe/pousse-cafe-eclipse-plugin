package poussecafe.eclipse.plugin.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import poussecafe.eclipse.plugin.core.PousseCafeCore;
import poussecafe.source.SourceModelBuilder;
import poussecafe.source.validation.ValidationMessage;
import poussecafe.source.validation.ValidationMessageType;
import poussecafe.source.validation.Validator;

public class PousseCafeBuilder extends IncrementalProjectBuilder {

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        deleteProjectPousseCafeMarkers();
    }

    private void deleteProjectPousseCafeMarkers() {
        try {
            getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            logger.error("Unable to delete markers", e);
        }
    }

    private static final String MARKER_TYPE = "poussecafe.eclipse.plugin.pousseCafeProblem";

    private ILog logger = Platform.getLog(getClass());

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        if(kind == INCREMENTAL_BUILD
                || kind == AUTO_BUILD) {
            var delta = getDelta(getProject());
            if(!isJavaSourceDelta(delta)) {
                return new IProject[0];
            }
        }

        monitor.beginTask("Pousse-Café build: " + getProject().getName(), 4);

        monitor.subTask("Pousse-Café build: walking project...");
        buildModels(monitor);
        monitor.worked(1);

        monitor.subTask("Pousse-Café build: validating...");
        validateProject();
        monitor.worked(2);

        monitor.subTask("Pousse-Café build: refreshing model...");
        refreshPousseCafeProject();
        monitor.worked(3);

        monitor.subTask("Pousse-Café build: refreshing markers...");
        refreshMarkers();
        monitor.worked(4);

        return new IProject[0];
    }

    private boolean isJavaSourceDelta(IResourceDelta delta) {
        var resource = delta.getResource();
        if(Resources.isJavaSourceFile(resource)) {
            return true;
        } else {
            for(IResourceDelta child : delta.getAffectedChildren()) {
                if(isJavaSourceDelta(child)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void buildModels(IProgressMonitor monitor) {
        var classResolver = new JdtClassResolver(javaProject());
        validator = new Validator(classResolver,
                Optional.of(new JdtClassPathExplorer(classResolver)));
        modelBuilder = new SourceModelBuilder(classResolver);

        files.clear();
        try {
            getProject().accept(new ResourceVisitor(monitor));
        } catch (CoreException e) {
            logger.error("Unable to validate project", e);
        }
    }

    private IJavaProject javaProject() {
        if(javaProject == null) {
            javaProject = JavaCore.create(getProject());
        }
        return javaProject;
    }

    private IJavaProject javaProject;

    private Validator validator;

    private SourceModelBuilder modelBuilder;

    private Map<String, IFile> files = new HashMap<>();

    private class ResourceVisitor extends JavaSourceFileVisitor {

        ResourceVisitor(IProgressMonitor monitor) {
            super(javaProject());
            this.monitor = monitor;
        }

        private IProgressMonitor monitor;

        @Override
        protected void visit(ResourceSource source) {
            if(monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            monitor.subTask("Pousse-Café build: scanning " + source.id());
            files.put(source.id(), source.file());
            try {
                validator.includeSource(source);
            } catch (Exception e) {
                logger.error("Validator skips resource " + source.id(), e);
            }
            try {
                modelBuilder.includeSource(source);
            } catch (Exception e) {
                logger.error("Model builder skips resource " + source.id(), e);
            }
        }
    }

    private void validateProject() {
        validator.validate();
    }

    private void refreshPousseCafeProject() {
        var pousseCafeProject = PousseCafeCore.getProject(javaProject);
        pousseCafeProject.refresh(modelBuilder.build());
    }

    private void refreshMarkers() {
        deleteProjectPousseCafeMarkers();
        var result = validator.result();
        for(ValidationMessage message : result.messages()) {
            var location = message.location();
            var file = files.get(location.sourceFile().id());
            if(file != null) {
                addMarker(file, message.message(), location.line(), severity(message.type()));
            } else {
                logger.error("Unknown file " + location.sourceFile().id());
            }
        }
    }

    private void addMarker(IFile file, String message, int lineNumber, int severity) {
        try {
            IMarker marker = file.createMarker(MARKER_TYPE);
            marker.setAttribute(IMarker.MESSAGE, message);
            marker.setAttribute(IMarker.SEVERITY, severity);
            if(lineNumber == -1) {
                lineNumber = 1;
            }
            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        } catch (CoreException e) {
            logger.error("Unable to add marker", e);
        }
    }

    private int severity(ValidationMessageType type) {
        if(type == ValidationMessageType.ERROR) {
            return IMarker.SEVERITY_ERROR;
        } else if(type == ValidationMessageType.WARNING) {
            return IMarker.SEVERITY_WARNING;
        } else {
            return IMarker.SEVERITY_INFO;
        }
    }

    public static final String BUILDER_ID = "poussecafe.eclipse.plugin.pousseCafeBuilder";
}
