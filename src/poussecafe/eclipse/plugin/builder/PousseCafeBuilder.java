package poussecafe.eclipse.plugin.builder;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
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

    @Inject
    private ILog logger;

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        validateProject(monitor);
        return null;
    }

    protected void validateProject(final IProgressMonitor monitor) throws CoreException {
        IProject project = getProject();
        validator = new Validator(new JdtClassResolver(JavaCore.create(project)));
        files.clear();
        try {
            project.accept(new ResourceVisitor());
        } catch (CoreException e) {
            logger.error("Unable to validate project", e);
        }
        refreshMarkers();
    }

    private Validator validator;

    class ResourceVisitor implements IResourceVisitor {
        @Override
        public boolean visit(IResource resource) {
            if(isJavaSourceFile(resource)) {
                IFile file = (IFile) resource;
                var source = new ResourceSource.Builder()
                        .project(getProject())
                        .file(file)
                        .build();
                files.put(source.id(), file);
                validator.includeSource(source);
            }
            return true;
        }

        private boolean isJavaSourceFile(IResource resource) {
            return resource instanceof IFile && resource.getName().endsWith(".java");
        }
    }

    private Map<String, IFile> files = new HashMap<>();

    private void refreshMarkers() {
        validator.validate();
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
