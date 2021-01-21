package poussecafe.eclipse.plugin.editors;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poussecafe.eclipse.plugin.builder.ResourceSource;
import poussecafe.source.emil.parser.EmilParser.ProcessContext;

public class EmilHyperlinkDetector extends AbstractHyperlinkDetector {

    public EmilHyperlinkDetector(EmilEditor editor) {
        this.editor = editor;
        openAction = new OpenAction(editor.getSite());
    }

    private EmilEditor editor;

    private OpenAction openAction;

    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        logger.debug("Detecting links at {}", region.getOffset());

        IEditorInput editorInput = editor.getEditorInput();

        var optionalModel = editor.getPousseCafeProject().model();
        if(optionalModel.isEmpty()) {
            return null; // NOSONAR
        }

        IDocumentProvider documentProvider = editor.getDocumentProvider();
        IDocument document = documentProvider.getDocument(editorInput);
        var parser = new EmilStringParser(document.get());
        if(parser.errors().isEmpty()) {
            tree = parser.tree();
            var links = new ArrayList<IHyperlink>();
            findLinks(links, new RegionQueries(region));
            if(links.isEmpty()) {
                logger.debug("Found 0 links");
                return null; // NOSONAR
            } else {
                var linksArray = new IHyperlink[links.size()];
                return links.toArray(linksArray);
            }
        } else {
            return null; // NOSONAR
        }
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ProcessContext tree;

    private void findLinks(List<IHyperlink> links, RegionQueries region) {
        var processName = tree.header().NAME();
        var process = editor.getPousseCafeProject().model().orElseThrow().processes().stream()
                .filter(candidate -> candidate.simpleName().equals(processName.getText()))
                .findFirst();
        if(process.isPresent()) {
            var linkRegion = region(processName);
            if(new RegionQueries(linkRegion).contains(region)) {
                var source = (ResourceSource) process.get().source();
                var compilationUnit = source.compilationUnit();
                logger.debug("Found link with region {}..{}", linkRegion.getOffset(), linkRegion.getOffset() + linkRegion.getLength() - 1);
                var link = new CompilationUnitHyperlink.Builder()
                        .region(linkRegion)
                        .action(openAction)
                        .compilationUnit(compilationUnit)
                        .build();
                links.add(link);
            }
        }
    }

    private IRegion region(TerminalNode node) {
        return new org.eclipse.jface.text.Region(node.getSymbol().getStartIndex(),
                node.getSymbol().getStopIndex() - node.getSymbol().getStartIndex() + 1);
    }
}
