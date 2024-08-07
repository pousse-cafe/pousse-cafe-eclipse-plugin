package poussecafe.eclipse.plugin.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;

import poussecafe.eclipse.plugin.builder.JdtClassResolver;
import poussecafe.eclipse.plugin.builder.PousseCafeBuilder;
import poussecafe.eclipse.plugin.builder.PousseCafeNature;
import poussecafe.eclipse.plugin.preferences.PreferenceConstants;
import poussecafe.eclipse.plugin.properties.PousseCafeProjectPropertyPage;
import poussecafe.source.analysis.ClassResolver;
import poussecafe.source.model.SourceModel;

import static java.util.Objects.requireNonNull;

public class PousseCafeProject implements IAdaptable {

    public static boolean isPousseCafeProjectCompilationUnit(IFile file) {
        ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(file);
        if(compilationUnit == null) {
            return false;
        } else {
            var type = compilationUnit.findPrimaryType();
            if(type == null) {
                return false;
            } else {
                var javaProject = type.getJavaProject();
                return PousseCafeNature.isPousseCafeProject(javaProject);
            }
        }
    }

    PousseCafeProject(IJavaProject project) {
        requireNonNull(project);
        if(!PousseCafeNature.isPousseCafeProject(project)) {
            throw new IllegalArgumentException("Not a Pousse-Café project");
        }
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
            triggerInitialBuild();
        }
    }

    private List<ChangeListener> listeners = new ArrayList<>();

    private SourceModel model;

    public synchronized void triggerInitialBuild() {
        if(!initialBuildTriggered) {
            var job = Job.create("Initial build of " + project.getProject().getName(),
                    this::buildPousseCafeProject);
            job.schedule();
            initialBuildTriggered = true;
        }
    }

    private boolean initialBuildTriggered;

    private void buildPousseCafeProject(IProgressMonitor monitor) throws CoreException {
        var args = new HashMap<String, String>();
        args.put(PousseCafeBuilder.TRY_INCREMENTAL_FIRST_ARG, "true");
        project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, PousseCafeBuilder.BUILDER_ID, args, monitor);
    }

    @FunctionalInterface
    public static interface ChangeListener {

        void consume(PousseCafeProject project);
    }

    public synchronized void removeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    public void refresh(SourceModel model) {
        synchronized(this) {
            requireNonNull(model);
            this.model = model;
        }

        for(ChangeListener listener : listeners) {
            listener.consume(this);
        }
    }

    public synchronized Optional<SourceModel> model() {
        return Optional.ofNullable(model);
    }

    public IFile createTempFile(String fileName) throws CoreException {
        var tempFolder = createPousseCafeTempFolder();
        var tempFile = tempFolder.getFile(fileName);
        tempFile.refreshLocal(IResource.DEPTH_ZERO, null);
        return tempFile;
    }

    private IFolder createPousseCafeTempFolder() throws CoreException {
        var tempFolder = project.getJavaProject().getProject().getFolder(PLUGIN_TEMP_FOLDER);
        tempFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
        if(!tempFolder.exists()) {
            tempFolder.create(false, true, null);
        }
        return tempFolder;
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
            String value = resource.getPersistentProperty(name);
            if(value == null) {
                return defaultValue;
            } else {
                return value;
            }
        } catch (CoreException e) {
            return defaultValue;
        }
    }

    public Path getSourceFolder() {
        var relativeSourceFolder = getProperty(PousseCafeProjectPropertyPage.SOURCE_FOLDER_PROPERTY_NAME,
                PousseCafeProjectPropertyPage.DEFAULT_SOURCE_FOLDER);
        return Path.of(project.getProject().getLocation().toOSString(), relativeSourceFolder);
    }

    public ClassResolver buildClassResolver() {
        return new JdtClassResolver(getJavaProject());
    }

    public boolean usesInternalStorage() {
        var value = getProperty(PousseCafeProjectPropertyPage.USES_INTERNAL_STORAGE_PROPERTY_NAME,
                PousseCafeProjectPropertyPage.DEFAULT_USES_INTERNAL_STORAGE);
        return Boolean.parseBoolean(value);
    }

    public boolean usesSpringMongoStorage() {
        var value = getProperty(PousseCafeProjectPropertyPage.USES_SPRING_MONGO_STORAGE_PROPERTY_NAME,
                PousseCafeProjectPropertyPage.DEFAULT_USES_SPRING_MONGO_STORAGE);
        return Boolean.parseBoolean(value);
    }

    public boolean usesSpringJpaStorage() {
        var value = getProperty(PousseCafeProjectPropertyPage.USES_SPRING_JPA_STORAGE_PROPERTY_NAME,
                PousseCafeProjectPropertyPage.DEFAULT_USES_SPRING_JPA_STORAGE);
        return Boolean.parseBoolean(value);
    }

    public IFile builderStateFile() throws CoreException {
        var tempFolder = createPousseCafeTempFolder();
        var file = tempFolder.getFile("builderState.dat");
        file.refreshLocal(IResource.DEPTH_ZERO, null);
        return file;
    }

    public boolean hasProblems() throws CoreException {
        var problems = project.getProject().findMarkers(PousseCafeBuilder.MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        return problems != null && problems.length > 0;
    }

    public Path getDocumentationFolder() throws CoreException {
        var tempFolder = createPousseCafeTempFolder();
        var folder = tempFolder.getFolder("doc");
        folder.refreshLocal(IResource.DEPTH_ZERO, null);
        if(!folder.exists()) {
            folder.create(false, true, null);
        }
        return Path.of(folder.getLocation().toOSString());
    }

    public String getDomain() {
        return getProperty(PousseCafeProjectPropertyPage.DOMAIN_PROPERTY_NAME,
                PousseCafeProjectPropertyPage.DEFAULT_DOMAIN);
    }

    public Browser documentationBrowser() {
        var store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
                String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));
        var browser = store.getString(PreferenceConstants.BROWSER_FOR_DOCUMENTATION);
        if(PreferenceConstants.BROWSER_FOR_DOCUMENTATION_ECLIPSE.equals(browser)) {
            return Browser.ECLIPSE;
        } else {
            return Browser.EXTERNAL;
        }
    }
}
