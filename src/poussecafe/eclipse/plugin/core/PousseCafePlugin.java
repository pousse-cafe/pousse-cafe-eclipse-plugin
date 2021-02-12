package poussecafe.eclipse.plugin.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class PousseCafePlugin extends AbstractUIPlugin {

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        for(IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages()) {
            for(IEditorReference editor : page.getEditorReferences()) {
                IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
                IFile file = input.getFile();
                if(PousseCafeProject.isPousseCafeProjectCompilationUnit(file)) {
                    var element = JavaCore.create(file);
                    var project = PousseCafeCore.getProject(element);
                    project.triggerInitialBuild();
                }
            }
        }
    }
}
