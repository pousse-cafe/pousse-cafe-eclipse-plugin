package poussecafe.eclipse.plugin.actions;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.FileEditorInput;
import poussecafe.eclipse.plugin.core.PousseCafeProject;
import poussecafe.source.emil.EmilExporter;

import static java.util.Objects.requireNonNull;

public class OpenEmilEditorAction extends Action {

    @Override
    public void run() {
        var model = project.model();
        if(model.isPresent()) {
            var exporter = new EmilExporter.Builder()
                    .model(model.get())
                    .processName(Optional.of(processName))
                    .build();
            String emil = exporter.toEmil();

            try {
                var emilFileName = emilFileName(processName);
                var emilFile = project.createTempFile(emilFileName);

                var buffer = new ByteArrayInputStream(emil.getBytes());
                if(emilFile.exists()) {
                    emilFile.setContents(buffer, IResource.FORCE, null);
                } else {
                    emilFile.create(buffer, false, null);
                }

                var editorRegistry = workbenchWindow.getWorkbench().getEditorRegistry();
                var editorDescriptor = editorRegistry.getDefaultEditor(emilFileName);
                workbenchWindow.getActivePage().openEditor(new FileEditorInput(emilFile), editorDescriptor.getId());
            } catch (CoreException e) {
                Platform.getLog(getClass()).error("Unable to write EMIL file", e);
            }
        }
    }

    private PousseCafeProject project;

    private String processName;

    private String emilFileName(String processName) {
        return processName + ".emil";
    }

    private IWorkbenchWindow workbenchWindow;

    public static class Builder {

        public OpenEmilEditorAction build() {
            requireNonNull(action.project);
            requireNonNull(action.processName);
            requireNonNull(action.workbenchWindow);
            return action;
        }

        private OpenEmilEditorAction action = new OpenEmilEditorAction();

        public Builder project(PousseCafeProject project) {
            action.project = project;
            return this;
        }

        public Builder processName(String processName) {
            action.processName = processName;
            return this;
        }

        public Builder workbenchWindow(IWorkbenchWindow workbenchWindow) {
            action.workbenchWindow = workbenchWindow;
            return this;
        }
    }

    private OpenEmilEditorAction() {

    }
}
