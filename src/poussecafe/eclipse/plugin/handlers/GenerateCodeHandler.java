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
import poussecafe.eclipse.plugin.editors.EmilEditor;
import poussecafe.source.analysis.ClassLoaderClassResolver;
import poussecafe.source.emil.parser.TreeAnalyzer;
import poussecafe.source.emil.parser.TreeParser;
import poussecafe.source.generation.CoreCodeGenerator;

public class GenerateCodeHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        var activeEditor = window.getActivePage().getActiveEditor();
        if(activeEditor instanceof EmilEditor) {
            var activeEmilEditor = (EmilEditor) activeEditor;
            if(!activeEmilEditor.isDirty()) {
                var tree = TreeParser.parseString(activeEmilEditor.getContent());
                if(tree.isValid()) {
                    var project = activeEmilEditor.getPousseCafeProject();
                    var analyzer = new TreeAnalyzer.Builder()
                            .tree(tree)
                            .basePackage(project.getBasePackage())
                            .build();
                    analyzer.analyze();
                    var model = analyzer.model();

                    var generatorBuilder = new CoreCodeGenerator.Builder()
                            .sourceDirectory(project.getSourceFolder())
                            .classResolver(new ClassLoaderClassResolver());
                    var currentModel = project.model();
                    if(currentModel.isPresent()) {
                        generatorBuilder.currentModel(currentModel.get());
                    }

                    generatorBuilder.preferencesContext(InstanceScope.INSTANCE);

                    var generator = generatorBuilder.build();
                    generator.generate(model);

                    try {
                        project.getJavaProject().getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
                    } catch (CoreException e) {
                        e.printStackTrace();
                    }

                    MessageDialog.openInformation(window.getShell(), "Pousse-Café code generator", "Code successfully generated.");
                }
            } else {
                MessageDialog.openInformation(window.getShell(), "Pousse-Café code generator", "Save editor's content first.");
            }
        }
        return null;
    }
}
