package poussecafe.eclipse.plugin.handlers;

import javax.inject.Inject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import poussecafe.eclipse.plugin.builder.PousseCafeNature;

public class AddRemovePousseCafeNatureHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if(selection instanceof IStructuredSelection) {
            var it = ProjectSelectionIterator.iterate((IStructuredSelection) selection);
            while(it.hasNext()) {
                IProject project = it.next();
                try {
                    toggleNature(project);
                } catch (CoreException e) {
                    logger.error("Failed to toggle nature");
                    throw new ExecutionException("Failed to toggle nature", e);
                }
            }
        }
        return null;
    }

    private void toggleNature(IProject project) throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();

        for (int i = 0; i < natures.length; ++i) {
            if(PousseCafeNature.NATURE_ID.equals(natures[i])) {
                // Remove the nature
                String[] newNatures = new String[natures.length - 1];
                System.arraycopy(natures, 0, newNatures, 0, i);
                System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
                description.setNatureIds(newNatures);
                project.setDescription(description, null);
                return;
            }
        }

        // Add the nature
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = PousseCafeNature.NATURE_ID;
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
    }

    @Inject
    private ILog logger;
}