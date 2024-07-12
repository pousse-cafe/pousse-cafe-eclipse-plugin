package poussecafe.eclipse.plugin.handlers;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.handlers.HandlerUtil;
import poussecafe.doc.PousseCafeDocGenerationConfiguration;
import poussecafe.doc.PousseCafeDocGenerator;
import poussecafe.eclipse.plugin.builder.PousseCafeNature;
import poussecafe.eclipse.plugin.core.Browser;
import poussecafe.eclipse.plugin.core.PousseCafeCore;
import poussecafe.eclipse.plugin.core.PousseCafeProject;
import poussecafe.source.model.SourceModel;

public class GenerateDocHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if(selection instanceof IStructuredSelection) {
            var it = ProjectSelectionIterator.iterate((IStructuredSelection) selection);
            while(it.hasNext()) {
                var project = it.next();
                if(PousseCafeNature.isPousseCafeProject(project)) {
                    var pousseCafeProject = PousseCafeCore.getProject(JavaCore.create(project));
                    generateDocAndOpen(pousseCafeProject);
                }
            }
        }
        return null;
    }

    private boolean generateDocAndOpen(PousseCafeProject pousseCafeProject) {
        var sourceModel = pousseCafeProject.model();
        if(sourceModel.isPresent()) {
            try {
                generateDoc(pousseCafeProject, sourceModel);
                openDocInBrowser(pousseCafeProject);
                return true;
            } catch (CoreException e) {
                Platform.getLog(getClass()).error("Error while generating doc", e);
            }
        }
        return false;
    }

    private void generateDoc(PousseCafeProject pousseCafeProject, Optional<SourceModel> sourceModel) throws CoreException {
        var generator = PousseCafeDocGenerator.builder()
                .configuration(PousseCafeDocGenerationConfiguration.builder()
                        .domainName(pousseCafeProject.getDomain())
                        .outputDirectory(pousseCafeProject.getDocumentationFolder().toString())
                        .version(LocalDateTime.now().toString())
                        .pdfFileName("project.pdf")
                        .build())
                .model(sourceModel.orElseThrow())
                .build();
        generator.generate();
    }

    private void openDocInBrowser(PousseCafeProject pousseCafeProject) {
        try {
            var url = new URL("file://" + pousseCafeProject.getDocumentationFolder().resolve("index.html").toString());
            var documentationBrowser = pousseCafeProject.documentationBrowser();
            var browserId = pousseCafeProject.getJavaProject().getProject().getName() + "PousseCafeDocBrowserId";
            if(documentationBrowser == Browser.EXTERNAL) {
                PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
            } else if(documentationBrowser == Browser.INTERNAL) {
                int style = IWorkbenchBrowserSupport.AS_EDITOR;
                var browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(style, browserId, "index.html", "Pousse-Café Documentation");
                browser.openURL(url);
            } else if(documentationBrowser == Browser.ECLIPSE) {
                var browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(browserId);
                browser.openURL(url);
            } else {
                throw new IllegalArgumentException("Unsupported documentation browser " + documentationBrowser);
            }
        } catch (Exception e) {
            Platform.getLog(getClass()).error("Error while generating doc", e);
        }
    }
}
