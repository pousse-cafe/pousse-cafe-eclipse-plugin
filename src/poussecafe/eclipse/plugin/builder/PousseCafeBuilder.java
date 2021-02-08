package poussecafe.eclipse.plugin.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poussecafe.eclipse.plugin.core.PousseCafeCore;
import poussecafe.source.SourceScanner;
import poussecafe.source.analysis.SourceModelBuilderVisitor;
import poussecafe.source.analysis.TypeResolvingCompilationUnitVisitor;
import poussecafe.source.validation.ValidationMessage;
import poussecafe.source.validation.ValidationMessageType;
import poussecafe.source.validation.ValidationModelBuilderVisitor;
import poussecafe.source.validation.Validator;

public class PousseCafeBuilder extends IncrementalProjectBuilder {

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        deleteProjectPousseCafeMarkers();
        clearBuilderState();
    }

    private void clearBuilderState() {
        scanner = null;
        files.clear();
        latestBuilderState = null;
        var project = PousseCafeCore.getProject(javaProject());
        try {
            var stateFile = project.builderStateFile();
            if(stateFile.exists()) {
                stateFile.delete(true, null);
            }
        } catch (CoreException e) {
            platformLogger.error("Unable to delete state file", e);
        }
    }

    private void deleteProjectPousseCafeMarkers() {
        try {
            getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            platformLogger.error("Unable to delete markers", e);
        }
    }

    public static final String MARKER_TYPE = "poussecafe.eclipse.plugin.pousseCafeProblem";

    private ILog platformLogger = Platform.getLog(getClass());

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("Pousse-Café build: " + getProject().getName(), 4);
        initBuilder();

        if(isIncrementalBuild(kind, args)) {
            monitor.subTask("Pousse-Café build: incremental...");
            incrementalBuild();
        } else {
            monitor.subTask("Pousse-Café build: full...");
            fullBuild(monitor);
        }
        persistBuilderState();
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

    private void initBuilder() {
        if(scanner == null) {
            classResolver = new JdtClassResolver(javaProject());
            validationVisitor = new ValidationModelBuilderVisitor();
            sourceModelVisitor = new SourceModelBuilderVisitor();
            scanner = new SourceScanner(new TypeResolvingCompilationUnitVisitor.Builder()
                    .withClassResolver(classResolver)
                    .withVisitor(sourceModelVisitor)
                    .withVisitor(validationVisitor)
                    .build());
            tryLoadPersistedState();
        }
    }

    private void tryLoadPersistedState() {
        try {
            long start = System.currentTimeMillis();
            logger.debug("Trying to load persited state");
            var project = PousseCafeCore.getProject(javaProject());
            var stateFile = project.builderStateFile();
            if(stateFile.exists()) {
                latestBuilderState = BuilderState.deserialize(stateFile);
                for(String file : latestBuilderState.files()) {
                    files.put(file, getProject().getFile(file));
                }
                sourceModelVisitor.loadSerializedState(latestBuilderState.getSourceModelVisitorState());
                validationVisitor.loadSerializedState(latestBuilderState.getValidationModelVisitorState());
            }
            long end = System.currentTimeMillis();
            logger.debug("Successfully loaded persisted state in {} ms", (end - start));
        } catch (Exception e) {
            platformLogger.info("Could not deserialize latest build state", e);
            latestBuilderState = null;
        }
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    private BuilderState latestBuilderState;

    private boolean isIncrementalBuild(int kind, Map<String, String> args) {
        boolean tryIncrementalFirst = tryIncrementalFirst(args);
        return latestBuilderState != null
                && ((kind == FULL_BUILD
                        && tryIncrementalFirst)
                    || kind == INCREMENTAL_BUILD
                    || kind == AUTO_BUILD);
    }

    private boolean tryIncrementalFirst(Map<String, String> args) {
        return args != null
                && args.get(TRY_INCREMENTAL_FIRST_ARG) != null
                && Boolean.parseBoolean(args.get(TRY_INCREMENTAL_FIRST_ARG));
    }

    public static final String TRY_INCREMENTAL_FIRST_ARG = "tryIncrementalFirst";

    private void incrementalBuild() {
        long start = System.currentTimeMillis();
        var delta = getDelta(getProject());
        var updatedResources = relevantDeltas(delta);
        for(IResourceDelta source: updatedResources) {
            var resource = new ResourceSource((IFile) source.getResource());
            resource.connect(javaProject());
            if(source.getKind() == IResourceDelta.ADDED
                    || source.getKind() == IResourceDelta.CHANGED) {
                includeFile(resource);
            } else {
                scanner.forget(resource.id());
                files.remove(resource.id());
            }
        }
        long end = System.currentTimeMillis();
        logger.info("Scanned delta in {} ms", (end - start));
    }

    private void fullBuild(IProgressMonitor monitor) {
        long start = System.currentTimeMillis();
        try {
            getProject().accept(new ResourceVisitor(monitor));
        } catch (CoreException e) {
            platformLogger.error("Unable to validate project", e);
        }
        long end = System.currentTimeMillis();
        logger.info("Scanned project in {} ms", (end - start));
    }

    private List<IResourceDelta> relevantDeltas(IResourceDelta delta) {
        var updatedResources = new ArrayList<IResourceDelta>();
        addRelevantDeltas(updatedResources, delta);
        return updatedResources;
    }

    private void addRelevantDeltas(List<IResourceDelta> resources, IResourceDelta delta) {
        if(delta != null) {
            var resource = delta.getResource();
            if(Resources.isJavaSourceFile(resource)) {
                resources.add(delta);
            } else {
                for(IResourceDelta child : delta.getAffectedChildren()) {
                    addRelevantDeltas(resources, child);
                }
            }
        }
    }

    private JdtClassResolver classResolver;

    private IJavaProject javaProject() {
        if(javaProject == null) {
            javaProject = JavaCore.create(getProject());
        }
        return javaProject;
    }

    private IJavaProject javaProject;

    private ValidationModelBuilderVisitor validationVisitor;

    private SourceModelBuilderVisitor sourceModelVisitor;

    private SourceScanner scanner;

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
            includeFile(source);
        }
    }

    private void includeFile(ResourceSource source) {
        files.put(source.id(), source.file());
        try {
            scanner.includeSource(source);
        } catch (Exception e) {
            platformLogger.error("Error while scanning " + source.id(), e);
        }
    }

    private void persistBuilderState() {
        latestBuilderState = new BuilderState();
        latestBuilderState.addFiles(files);
        latestBuilderState.setSourceModelVisitorState(sourceModelVisitor.getSerializableState());
        latestBuilderState.setValidationModelVisitorState(validationVisitor.getSerializableState());

        var project = PousseCafeCore.getProject(javaProject());
        try {
            var stateFile = project.builderStateFile();
            latestBuilderState.serialize(stateFile);
        } catch (Exception e) {
            platformLogger.info("Could not serialize latest build state", e);
            latestBuilderState = null;
        }
    }

    private void validateProject() {
        validator = new Validator(validationVisitor.buildModel(),
                classResolver,
                Optional.of(new JdtClassPathExplorer(classResolver)));
        validator.validate();
    }

    private Validator validator;

    private void refreshPousseCafeProject() {
        var pousseCafeProject = PousseCafeCore.getProject(javaProject);
        pousseCafeProject.refresh(sourceModelVisitor.buildModel());
    }

    private void refreshMarkers() {
        deleteProjectPousseCafeMarkers();
        var result = validator.result();
        for(ValidationMessage message : result.messages()) {
            var location = message.location();
            var file = files.get(location.source().id());
            if(file != null) {
                addMarker(file, message.message(), location.line(), severity(message.type()));
            } else {
                platformLogger.error("Unknown file " + location.source().id());
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
            platformLogger.error("Unable to add marker", e);
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
