package poussecafe.eclipse.plugin.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import poussecafe.eclipse.plugin.builder.JdtClassResolver;
import poussecafe.eclipse.plugin.builder.PousseCafeBuilder;
import poussecafe.eclipse.plugin.properties.PousseCafeProjectPropertyPage;
import poussecafe.source.analysis.ClassResolver;
import poussecafe.source.model.Model;

import static java.util.Objects.requireNonNull;

public class PousseCafeProject implements IAdaptable {

    PousseCafeProject(IJavaProject project) {
        requireNonNull(project);
        this.project = project;
    }

    private IJavaProject project;

    public IJavaProject getJavaProject() {
        return project;
    }

    public void addListener(ChangeListener listener) {
        synchronized(this) {
            listeners.add(listener);
        }

        if(model().isPresent()) {
            listener.consume(this);
        } else {
            triggerPousseCafeBuilder();
        }
    }

    private List<ChangeListener> listeners = new ArrayList<>();

    private Model model;

    private synchronized void triggerPousseCafeBuilder() {
        if(!buildInProgress) {
            var job = Job.create("Build Pousse-Café project " + project.getProject().getName(),
                    this::buildPousseCafeProject);
            job.schedule();
            buildInProgress = true;
        }
    }

    private boolean buildInProgress;

    private void buildPousseCafeProject(IProgressMonitor monitor) throws CoreException {
        project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, PousseCafeBuilder.BUILDER_ID, null, monitor);
    }

    @FunctionalInterface
    public static interface ChangeListener {

        void consume(PousseCafeProject project);
    }

    public synchronized void removeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    public void refresh(Model model) {
        synchronized(this) {
            requireNonNull(model);
            this.model = model;
            buildInProgress = false;
        }

        for(ChangeListener listener : listeners) {
            listener.consume(this);
        }
    }

    public synchronized Optional<Model> model() {
        return Optional.ofNullable(model);
    }

    public IFile createTempFile(String fileName) throws CoreException {
        var tempFolder = project.getJavaProject().getProject().getFolder(PLUGIN_TEMP_FOLDER);
        tempFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
        if(!tempFolder.exists()) {
            tempFolder.create(false, true, null);
        }
        var tempFile = tempFolder.getFile(fileName);
        tempFile.refreshLocal(IResource.DEPTH_ZERO, null);
        return tempFile;
    }

    private static final String PLUGIN_TEMP_FOLDER = ".pousse-cafe";

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if(adapter.isAssignableFrom(IResource.class)) {
            return (T) project.getProject();
        }
        return null;
    }

    public String getBasePackage() {
        return getProperty(PousseCafeProjectPropertyPage.BASE_PACKAGE_PROPERTY_NAME,
                PousseCafeProjectPropertyPage.DEFAULT_BASE_PACKAGE);
    }

    private String getProperty(QualifiedName name, String defaultValue) {
        var resource = project.getProject();
        try {
            return resource.getPersistentProperty(name);
        } catch (CoreException e) {
            return defaultValue;
        }
    }

    public Path getSourceFolder() {
        var relativeSourceFolder = getProperty(PousseCafeProjectPropertyPage.SOURCE_FOLDER_PROPERTY_NAME,
                PousseCafeProjectPropertyPage.DEFAULT_SOURCE_FOLDER);
        return Path.of(project.getProject().getRawLocation().toOSString(), relativeSourceFolder);
    }

    public ClassResolver buildClassResolver() {
        return new JdtClassResolver(getJavaProject());
    }
}
