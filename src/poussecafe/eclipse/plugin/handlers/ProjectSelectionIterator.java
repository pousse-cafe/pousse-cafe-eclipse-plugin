package poussecafe.eclipse.plugin.handlers;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;

public class ProjectSelectionIterator implements Iterator<IProject> {

    @Override
    public boolean hasNext() {
        return nextProject != null;
    }

    private Iterator<?> selectionIterator;

    private IProject nextProject;

    @Override
    public IProject next() {
        if(nextProject == null) {
            throw new NoSuchElementException();
        }
        IProject project = nextProject;
        moveToNext();
        return project;
    }

    public static ProjectSelectionIterator iterate(IStructuredSelection selection) {
        var iterator = new ProjectSelectionIterator();
        iterator.selectionIterator = selection.iterator();
        iterator.moveToNext();
        return iterator;
    }

    private void moveToNext() {
        nextProject = null;
        while(selectionIterator.hasNext() && nextProject == null) {
            Object element = selectionIterator.next();
            if(element instanceof IProject) {
                nextProject = (IProject) element;
            } else if(element instanceof IAdaptable) {
                nextProject = ((IAdaptable) element).getAdapter(IProject.class);
            }
        }
    }

    private ProjectSelectionIterator() {

    }
}
