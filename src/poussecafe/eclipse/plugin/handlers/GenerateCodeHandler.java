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
import poussecafe.source.generation.internal.InternalStorageAdaptersCodeGenerator;
import poussecafe.source.model.Aggregate;
import poussecafe.source.model.Model;
import poussecafe.spring.jpa.storage.codegeneration.JpaStorageAdaptersCodeGenerator;
import poussecafe.spring.mongo.storage.codegeneration.MongoStorageAdaptersCodeGenerator;

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
                    var project = activeEmilEditor.getPousseCafeProject();
                    if(project.model().isEmpty()) {
                        MessageDialog.openWarning(window.getShell(), DIALOG_TITLE, "Build the project first or wait for current build to finish.");
                    } else {
                        generateCode(window, project, tree);
                        MessageDialog.openInformation(window.getShell(), DIALOG_TITLE, "Code successfully generated.");
                    }
                } else {
                    MessageDialog.openError(window.getShell(), DIALOG_TITLE, "Cannot generated code, invalid EMIL.");
                }
            } else {
                MessageDialog.openInformation(window.getShell(), DIALOG_TITLE, "Save editor's content first.");
            }
        }
        return null;
    }

    private void generateCode(IWorkbenchWindow window, PousseCafeProject project, Tree tree) {
        try {
            window.getShell().setEnabled(false);
            var newModel = buildNewModel(project, tree);
            updateCode(project, newModel);
            refreshResources(project);
        } finally {
            window.getShell().setEnabled(true);
        }
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

        if(project.usesInternalStorage()) {
            var internalGeneratorBuilder = new InternalStorageAdaptersCodeGenerator.Builder()
                    .sourceDirectory(project.getSourceFolder());
            internalGeneratorBuilder.preferencesContext(InstanceScope.INSTANCE);
            for(Aggregate aggregate : newModel.aggregates()) {
                internalGeneratorBuilder.build().generate(aggregate);
            }
        }

        if(project.usesSpringMongoStorage()) {
            var internalGeneratorBuilder = new MongoStorageAdaptersCodeGenerator.Builder()
                    .sourceDirectory(project.getSourceFolder());
            internalGeneratorBuilder.preferencesContext(InstanceScope.INSTANCE);
            for(Aggregate aggregate : newModel.aggregates()) {
                internalGeneratorBuilder.build().generate(aggregate);
            }
        }

        if(project.usesSpringJpaStorage()) {
            var internalGeneratorBuilder = new JpaStorageAdaptersCodeGenerator.Builder()
                    .sourceDirectory(project.getSourceFolder());
            internalGeneratorBuilder.preferencesContext(InstanceScope.INSTANCE);
            for(Aggregate aggregate : newModel.aggregates()) {
                internalGeneratorBuilder.build().generate(aggregate);
            }
        }
    }

    private void refreshResources(PousseCafeProject project) {
        try {
            project.getJavaProject().getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}
