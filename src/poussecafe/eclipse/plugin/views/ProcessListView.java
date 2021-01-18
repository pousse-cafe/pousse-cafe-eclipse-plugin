package poussecafe.eclipse.plugin.views;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import javax.inject.Inject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poussecafe.eclipse.plugin.builder.JdtClassResolver;
import poussecafe.eclipse.plugin.builder.PousseCafeNature;
import poussecafe.source.SourceModelBuilder;
import poussecafe.source.emil.EmilExporter;
import poussecafe.source.model.Model;
import poussecafe.source.model.ProcessModel;

public class ProcessListView extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "poussecafe.eclipse.plugin.views.ProcessListView";

    @Inject
    IWorkbench workbench;

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(new String[] {});
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
                refreshProcessList(javaProject, false);
            }
        }
    };

    private void refreshProcessList(IJavaProject javaProject, boolean forceRefresh) {
        try {
            if(!forceRefresh && javaProject.equals(currentProject)) {
                return;
            }
            logger.debug("Trying to refresh process list...");
            currentProject = javaProject;
            IProject project = currentProject.getProject();
            if(project.hasNature(PousseCafeNature.NATURE_ID)) {
                viewer.setInput(new String[] {});
                var job = new UIJob("Refresh processList") {
                    @Override
                    public IStatus runInUIThread(IProgressMonitor monitor) {
                        resetViewerInput(currentProject);
                        return Status.OK_STATUS;
                    }
                };
                job.schedule();
            }
        } catch (CoreException e) {
            Platform.getLog(getClass()).error("Unable to refresh process list", e);
        }
    }

    private void resetViewerInput(IJavaProject javaProject) {
        IProject project = currentProject.getProject();
        logger.debug("Extract process list from project {}", project.getName());
        var classResolver = new JdtClassResolver(javaProject);

        var modelBuilder = new SourceModelBuilder(classResolver);
        try {
            project.accept(new ModelBuildingResourceVisitor(javaProject, modelBuilder));
            sourceModel = modelBuilder.build();

            var processNames = sourceModel.processes().stream()
                    .map(ProcessModel::simpleName)
                    .sorted()
                    .toArray(String[]::new);
            viewer.setInput(processNames);
        } catch (CoreException e) {
            Platform.getLog(getClass()).error("Unable to visit project " + project.getName(), e);
        }
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    private IJavaProject currentProject;

    private Model sourceModel;

    private void registerDoubleClickAction() {
        var doubleClickAction = new Action() {
            @Override
            public void run() {
                IStructuredSelection selection = viewer.getStructuredSelection();
                String processName = (String) selection.getFirstElement();

                var exporter = new EmilExporter.Builder()
                        .model(sourceModel)
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
        var project = currentProject.getProject();
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
                if(currentProject != null) {
                    refreshProcessList(currentProject, true);
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
