package poussecafe.eclipse.plugin.views;

import javax.inject.Inject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import poussecafe.eclipse.plugin.actions.OpenEmilEditorAction;
import poussecafe.eclipse.plugin.builder.PousseCafeNature;
import poussecafe.eclipse.plugin.core.PousseCafeCore;
import poussecafe.eclipse.plugin.core.PousseCafeProject;
import poussecafe.source.model.ProcessModel;

public class ProcessListView extends ViewPart {

    public static final String ID = "poussecafe.eclipse.plugin.views.ProcessListView";

    @Inject // NOSONAR - No constructor injection possible with Eclipse
    IWorkbench workbench;

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        clearList();
        viewer.setLabelProvider(new ViewLabelProvider());

        getSite().setSelectionProvider(viewer);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);

        registerDoubleClickAction();
        fillActionBar();
    }

    private TableViewer viewer;

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void dispose() {
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
        super.dispose();
    }

    class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public String getColumnText(Object obj, int index) {
            return getText(obj);
        }

        @Override
        public Image getColumnImage(Object obj, int index) {
            return getImage(obj);
        }

        @Override
        public Image getImage(Object obj) {
            return workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
        }
    }

    private ILog logger = Platform.getLog(getClass());

    private ISelectionListener selectionListener = (part, selection) -> {
        if(selection instanceof TreeSelection
                && !selection.isEmpty()) {
            var treeSelection = (TreeSelection) selection;
            var maybeProjectSelection = treeSelection.getFirstElement();
            if(maybeProjectSelection instanceof IJavaProject) {
                var javaProject = (IJavaProject) maybeProjectSelection;
                setProject(javaProject);
            } else if(maybeProjectSelection instanceof IProject) {
                var projectSelection = (IProject) maybeProjectSelection;
                try {
                    if(projectSelection.hasNature(JavaCore.NATURE_ID)) {
                        var javaProject = JavaCore.create(projectSelection);
                        setProject(javaProject);
                    }
                } catch (CoreException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    };

    private void setProject(IJavaProject javaProject) {
        if(PousseCafeNature.isPousseCafeProject(javaProject)) {
            var newProject = PousseCafeCore.getProject(javaProject);
            if(currentProject != newProject) {
                clearList();
                if(currentProject != null) {
                    currentProject.removeListener(projectListener);
                }
                currentProject = newProject;
                currentProject.addListener(projectListener);
            }
        }
    }

    private PousseCafeProject currentProject;

    private void clearList() {
        viewer.setInput(new String[] {});
    }

    private ProjectListener projectListener = new ProjectListener();

    private class ProjectListener implements PousseCafeProject.ChangeListener {
        @Override
        public void consume(PousseCafeProject project) {
            if(project.equals(currentProject)) {
                refreshProcessList();
            }
        }
    }

    private void refreshProcessList() {
        String[] processNames = currentProject.model().orElseThrow().processes().stream()
                .map(ProcessModel::simpleName)
                .sorted()
                .toArray(String[]::new);

        var uiJob = new UIJob("Refresh process list") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                viewer.setInput(processNames);
                return Status.OK_STATUS;
            }
        };
        uiJob.schedule();
    }

    private void registerDoubleClickAction() {
        var doubleClickAction = new Action() {
            @Override
            public void run() {
                IStructuredSelection selection = viewer.getStructuredSelection();
                String processName = (String) selection.getFirstElement();
                var openEmilEditor = new OpenEmilEditorAction.Builder()
                        .processName(processName)
                        .project(currentProject)
                        .workbenchWindow(getSite().getWorkbenchWindow())
                        .build();
                openEmilEditor.run();
            }
        };
        viewer.addDoubleClickListener(event -> doubleClickAction.run());
    }

    private void fillActionBar() {
        var toolBar = getViewSite().getActionBars().getToolBarManager();
        var refreshListAction = buildRefreshListAction();
        toolBar.add(refreshListAction);
    }

    private IAction buildRefreshListAction() {
        var action = new Action() {
            @Override
            public void run() {
                if(currentProject != null
                        && currentProject.model().isPresent()) {
                    refreshProcessList();
                }
            }
        };
        action.setText("Reload");
        action.setToolTipText("Reload process list");
        action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
        return action;
    }
}
