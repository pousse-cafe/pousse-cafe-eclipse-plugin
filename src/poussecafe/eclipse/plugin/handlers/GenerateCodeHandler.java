package poussecafe.eclipse.plugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import poussecafe.eclipse.plugin.core.PousseCafeProject;
import poussecafe.eclipse.plugin.editors.EmilEditor;
import poussecafe.source.emil.parser.Tree;
import poussecafe.source.emil.parser.TreeAnalyzer;
import poussecafe.source.emil.parser.TreeParser;
import poussecafe.source.generation.CoreCodeGenerator;
import poussecafe.source.model.Model;

public class GenerateCodeHandler extends AbstractHandler {

    private static final String DIALOG_TITLE = "Pousse-Café code generator";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        var activeEditor = window.getActivePage().getActiveEditor();
        if(activeEditor instanceof EmilEditor) {
            var activeEmilEditor = (EmilEditor) activeEditor;
            if(!activeEmilEditor.isDirty()) {
                var tree = TreeParser.parseString(activeEmilEditor.getContent());
                if(tree.isValid()) {
                    generateCode(activeEmilEditor.getPousseCafeProject(), tree);
                    MessageDialog.openInformation(window.getShell(), DIALOG_TITLE, "Code successfully generated.");
                } else {
                    MessageDialog.openError(window.getShell(), DIALOG_TITLE, "Cannot generated code, invalid EMIL.");
                }
            } else {
                MessageDialog.openInformation(window.getShell(), DIALOG_TITLE, "Save editor's content first.");
            }
        }
        return null;
    }

    private void generateCode(PousseCafeProject project, Tree tree) {
        var newModel = buildNewModel(project, tree);
        updateCode(project, newModel);
        refreshResources(project);
    }

    private Model buildNewModel(PousseCafeProject project, Tree tree) {
        var analyzer = new TreeAnalyzer.Builder()
                .tree(tree)
                .basePackage(project.getBasePackage())
                .build();
        analyzer.analyze();
        return analyzer.model();
    }

    private void updateCode(PousseCafeProject project, Model newModel) {
        var generatorBuilder = new CoreCodeGenerator.Builder()
                .sourceDirectory(project.getSourceFolder())
                .classResolver(project.buildClassResolver());
        generatorBuilder.currentModel(project.model().orElseThrow());
        generatorBuilder.preferencesContext(InstanceScope.INSTANCE);
        var generator = generatorBuilder.build();
        generator.generate(newModel);
    }

    private void refreshResources(PousseCafeProject project) {
        try {
            project.getJavaProject().getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}
