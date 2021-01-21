package poussecafe.eclipse.plugin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import poussecafe.eclipse.plugin.builder.PousseCafeBuilder;
import poussecafe.source.model.Model;

import static java.util.Objects.requireNonNull;

public class PousseCafeProject {

    PousseCafeProject(IJavaProject project) {
        requireNonNull(project);
        this.project = project;
    }

    private IJavaProject project;

    public IJavaProject getJavaProject() {
        return project;
    }

    public synchronized void addListener(ChangeListener listener) {
        listeners.add(listener);
        if(model != null) {
            listener.consume(this);
        } else {
            triggerPousseCafeBuilder();
        }
    }

    private List<ChangeListener> listeners = new ArrayList<>();

    private Model model;

    private void triggerPousseCafeBuilder() {
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

    public synchronized void refresh(Model model) {
        requireNonNull(model);
        this.model = model;

        buildInProgress = false;

        for(ChangeListener listener : listeners) {
            listener.consume(this);
        }
    }

    public synchronized Optional<Model> model() {
        return Optional.ofNullable(model);
    }
}
