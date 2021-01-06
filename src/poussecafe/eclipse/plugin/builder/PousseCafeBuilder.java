package poussecafe.eclipse.plugin.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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
        monitor.beginTask("Validating " + getProject().getName(), 3);
        validateProject(monitor);
        return new IProject[0];
    }

    protected void validateProject(final IProgressMonitor monitor) throws CoreException {
        monitor.subTask("Walking project...");
        var classResolver = new JdtClassResolver(javaProject());
        validator = new Validator(classResolver,
                Optional.of(new JdtClassPathExplorer(classResolver)));
        files.clear();
        try {
            getProject().accept(new ResourceVisitor(monitor));
        } catch (CoreException e) {
            logger.error("Unable to validate project", e);
        }
        monitor.worked(1);

        monitor.subTask("Validating...");
        validator.validate();
        monitor.worked(2);

        monitor.subTask("Refreshing markers...");
        refreshMarkers();
        monitor.worked(3);
    }

    private Validator validator;

    private IJavaProject javaProject() {
        if(javaProject == null) {
            javaProject = JavaCore.create(getProject());
        }
        return javaProject;
    }

    private IJavaProject javaProject;

    class ResourceVisitor implements IResourceVisitor {

        ResourceVisitor(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        private IProgressMonitor monitor;

        @Override
        public boolean visit(IResource resource) {
            if(isJavaSourceFile(resource)) {
                IFile file = (IFile) resource;
                var source = new ResourceSource.Builder()
                        .project(javaProject())
                        .file(file)
                        .build();
                monitor.subTask("Validation: scanning " + source.id());
                files.put(source.id(), file);
                try {
                    validator.includeSource(source);
                } catch (Exception e) {
                    logger.error("Skipping resource " + source.id(), e);
                }
            }
            return true;
        }

        private boolean isJavaSourceFile(IResource resource) {
            var extension = resource.getFileExtension();
            return resource instanceof IFile
                    && extension != null
                    && extension.equals("java");
        }
    }

    private Map<String, IFile> files = new HashMap<>();

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
