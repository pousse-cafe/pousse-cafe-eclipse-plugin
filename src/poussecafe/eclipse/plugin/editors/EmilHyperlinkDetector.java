package poussecafe.eclipse.plugin.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import poussecafe.source.Source;
import poussecafe.source.emil.parser.EmilParser.ConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.ConsumptionsContext;
import poussecafe.source.emil.parser.EmilParser.HeaderContext;
import poussecafe.source.emil.parser.EmilParser.ProcessContext;
import poussecafe.source.model.Command;
import poussecafe.source.model.DomainEvent;
import poussecafe.source.model.Model;
import poussecafe.source.model.ProcessModel;

public class EmilHyperlinkDetector extends AbstractHyperlinkDetector {

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
            links = new ArrayList<>();
            this.region = region;
            findLinks();
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

    private EmilEditor editor;

    private ProcessContext tree;

    private List<IHyperlink> links;

    private IRegion region;

    private void findLinks() {
        tryAddLinksOfHeader(tree.header());
        tryAddLinksOfConsumptions(tree.consumptions());
    }

    private void tryAddLinksOfHeader(HeaderContext header) {
        var processName = header.NAME();
        var process = modelOrElseThrow().processes().stream()
                .filter(candidate -> candidate.simpleName().equals(processName.getText()))
                .findFirst();
        tryAddLink(processName, process.map(ProcessModel::source));
    }

    private Model modelOrElseThrow() {
        return editor.getPousseCafeProject().model().orElseThrow();
    }

    private void tryAddLink(TerminalNode node, Optional<Optional<Source>> sourceContainer) {
        var linkRegion = region(node);
        if(new RegionQueries(linkRegion).contains(region)) {
            if(sourceContainer.isPresent()
                    && sourceContainer.get().isPresent()) {
                var source = (ResourceSource) sourceContainer.get().orElseThrow();
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

    private void tryAddLinksOfConsumptions(ConsumptionsContext consumptions) {
        for(ConsumptionContext consumption : consumptions.consumption()) {
            tryAddLinksOfCommandConsumption(consumption);
            tryAddLinksOfEventConsumption(consumption);
        }
    }

    private void tryAddLinksOfCommandConsumption(ConsumptionContext consumption) {
        var commandConsumption = consumption.commandConsumption();
        if(commandConsumption != null) {
            var commandName = commandConsumption.command().NAME();
            var command = modelOrElseThrow().command(commandName.getText());
            tryAddLink(commandName, command.map(Command::source));
        }
    }

    private void tryAddLinksOfEventConsumption(ConsumptionContext consumption) {
        var eventConsumption = consumption.eventConsumption();
        if(eventConsumption != null) {
            var eventName = eventConsumption.event().NAME();
            var event = modelOrElseThrow().event(eventName.getText());
            tryAddLink(eventName, event.map(DomainEvent::source));
        }
    }

    private IRegion region(TerminalNode node) {
        return new org.eclipse.jface.text.Region(node.getSymbol().getStartIndex(),
                node.getSymbol().getStopIndex() - node.getSymbol().getStartIndex() + 1);
    }

    public EmilHyperlinkDetector(EmilEditor editor) {
        this.editor = editor;
        openAction = new OpenAction(editor.getSite());
    }
}
