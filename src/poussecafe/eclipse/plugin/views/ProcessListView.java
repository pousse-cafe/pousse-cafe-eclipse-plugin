package poussecafe.eclipse.plugin.views;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import javax.inject.Inject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.IPackagesViewPart;
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
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import poussecafe.eclipse.plugin.builder.PousseCafeNature;
import poussecafe.eclipse.plugin.core.PousseCafeCore;
import poussecafe.eclipse.plugin.core.PousseCafeProject;
import poussecafe.source.emil.EmilExporter;
import poussecafe.source.model.ProcessModel;

public class ProcessListView extends ViewPart {

    public static final String ID = "poussecafe.eclipse.plugin.views.ProcessListView";

    @Inject
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

    private ISelectionListener selectionListener = (part, selection) -> {
        if(part instanceof IPackagesViewPart
                && selection instanceof TreeSelection
                && !selection.isEmpty()) {
            var packageSelection = (TreeSelection) selection;
            if(packageSelection.getFirstElement() instanceof IJavaProject) {
                var javaProject = (IJavaProject) packageSelection.getFirstElement();
                setProject(javaProject);
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

                var exporter = new EmilExporter.Builder()
                        .model(currentProject.model().orElseThrow())
                        .processName(Optional.of(processName))
                        .build();
                String emil = exporter.toEmil();

                try {
                    var emilFileName = emilFileName(processName);
                    var emilFile = createTempFile(emilFileName);
                    emilFile.refreshLocal(IResource.DEPTH_ZERO, null);
                    var buffer = new ByteArrayInputStream(emil.getBytes());
                    if(emilFile.exists()) {
                        emilFile.setContents(buffer, IResource.FORCE, null);
                    } else {
                        emilFile.create(buffer, false, null);
                    }

                    var workbenchWindow = getSite().getWorkbenchWindow();
                    var editorRegistry = workbenchWindow.getWorkbench().getEditorRegistry();
                    var editorDescriptor = editorRegistry.getDefaultEditor(emilFileName);
                    workbenchWindow.getActivePage().openEditor(new FileEditorInput(emilFile), editorDescriptor.getId());
                } catch (CoreException e) {
                    Platform.getLog(getClass()).error("Unable to write EMIL file", e);
                }
            }
        };
        viewer.addDoubleClickListener(event -> doubleClickAction.run());
    }

    private String emilFileName(String processName) {
        return processName + ".emil";
    }

    private IFile createTempFile(String fileName) throws CoreException {
        var project = currentProject.getJavaProject().getProject();
        var tempFolder = project.getFolder(PLUGIN_TEMP_FOLDER);
        tempFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
        if(!tempFolder.exists()) {
            tempFolder.create(false, true, null);
        }
        return tempFolder.getFile(fileName);
    }

    private static final String PLUGIN_TEMP_FOLDER = ".pousse-cafe";

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
