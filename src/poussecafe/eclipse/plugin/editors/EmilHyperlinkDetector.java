package poussecafe.eclipse.plugin.editors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poussecafe.eclipse.plugin.actions.OpenEmilEditorAction;
import poussecafe.eclipse.plugin.actions.OpenJavaEditorAction;
import poussecafe.eclipse.plugin.builder.ResourceSource;
import poussecafe.source.Source;
import poussecafe.source.emil.parser.EmilParser.AggregateRootConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.AggregateRootContext;
import poussecafe.source.emil.parser.EmilParser.ConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.ConsumptionsContext;
import poussecafe.source.emil.parser.EmilParser.EventContext;
import poussecafe.source.emil.parser.EmilParser.EventProductionContext;
import poussecafe.source.emil.parser.EmilParser.EventProductionsContext;
import poussecafe.source.emil.parser.EmilParser.FactoryConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.FactoryListenerContext;
import poussecafe.source.emil.parser.EmilParser.HeaderContext;
import poussecafe.source.emil.parser.EmilParser.MessageConsumptionsContext;
import poussecafe.source.emil.parser.EmilParser.MultipleMessageConsumptionsItemContext;
import poussecafe.source.emil.parser.EmilParser.ProcessConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.ProcessContext;
import poussecafe.source.emil.parser.EmilParser.QualifiedNameContext;
import poussecafe.source.emil.parser.EmilParser.RepositoryConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.SingleMessageConsumptionContext;
import poussecafe.source.generation.NamingConventions;
import poussecafe.source.model.Aggregate;
import poussecafe.source.model.Command;
import poussecafe.source.model.DomainEvent;
import poussecafe.source.model.Hooks;
import poussecafe.source.model.MessageListenerContainerType;
import poussecafe.source.model.ProcessModel;
import poussecafe.source.model.Runner;
import poussecafe.source.model.SourceModel;

public class EmilHyperlinkDetector extends AbstractHyperlinkDetector {

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
        tryAddLink(processName.getSymbol(), process.map(ProcessModel::source), Optional::of);
    }

    private SourceModel modelOrElseThrow() {
        return editor.getPousseCafeProject().model().orElseThrow();
    }

    private void tryAddLink(
            Token node,
            Optional<Source> sourceContainer,
            Function<IType, Optional<IMember>> memberExtractor) {
        var linkRegion = region(node);
        if(sourceContainer.isPresent()
                && new RegionQueries(linkRegion).contains(region)) {
            var source = (ResourceSource) sourceContainer.get();
            source.connect(editor.getPousseCafeProject().getJavaProject());
            var type = source.sourceType();
            var member = memberExtractor.apply(type);
            if(member.isPresent()
                    && member.get().exists()) {
                logger.debug("Found link with region {}..{}", linkRegion.getOffset(), linkRegion.getOffset() + linkRegion.getLength() - 1);
                var openJavaEditor = new OpenJavaEditorAction.Builder()
                        .site(editor.getSite())
                        .member(member.get())
                        .build();
                links.add(new ActionHyperlink.Builder()
                        .region(linkRegion)
                        .name(member.get().getElementName())
                        .action(openJavaEditor)
                        .build());
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
            tryAddLink(commandName.getSymbol(), command.map(Command::source), Optional::of);
            tryAddLinks(commandName.getText(), commandConsumption.messageConsumptions());
        }
    }

    private void tryAddLinks(String messageTypeName, MessageConsumptionsContext messageConsumptions) {
        if(messageConsumptions.multipleMessageConsumptions() != null) {
            for(MultipleMessageConsumptionsItemContext item : messageConsumptions.multipleMessageConsumptions().multipleMessageConsumptionsItem()) {
                tryAddLinks(messageTypeName, item);
            }
        } else if(messageConsumptions.singleMessageConsumption() != null) {
            tryAddLinks(messageTypeName, messageConsumptions.singleMessageConsumption());
        }
    }

    private void tryAddLinks(String messageTypeName, MultipleMessageConsumptionsItemContext item) {
        tryAddLinks(messageTypeName, item.singleMessageConsumption());
    }

    private void tryAddLinks(String messageTypeName, SingleMessageConsumptionContext singleMessageConsumption) {
        if(singleMessageConsumption.factoryConsumption() != null) {
            tryAddLinks(messageTypeName, singleMessageConsumption.factoryConsumption());
        } else if(singleMessageConsumption.aggregateRootConsumption() != null) {
            tryAddLinks(messageTypeName, singleMessageConsumption.aggregateRootConsumption());
        } else if(singleMessageConsumption.repositoryConsumption() != null) {
            tryAddLinks(messageTypeName, singleMessageConsumption.repositoryConsumption());
        } else if(singleMessageConsumption.processConsumption() != null) {
            tryAddLinks(singleMessageConsumption.processConsumption());
        }
    }

    private void tryAddLinks(String messageTypeName, FactoryConsumptionContext factoryConsumption) {
        tryAddLinks(messageTypeName, factoryConsumption.factoryListener());
        tryAddLinks(factoryConsumption.aggregateRoot());
        if(factoryConsumption.aggregateRoot() != null) {
            tryAddLinkListener(
                    factoryConsumption.aggregateRoot().qualifiedRootName,
                    factoryConsumption.aggregateRoot().simpleRootName,
                    hookNameToken(factoryConsumption, "onAdd"),
                    null,
                    factoryConsumption.aggregateRoot().simpleRootName != null ? MessageListenerContainerType.STANDALONE_ROOT : MessageListenerContainerType.INNER_ROOT);
            tryAddLinks(factoryConsumption.eventProductions());
        }
    }

    private Token hookNameToken(ParserRuleContext rule, String hookName) {
        for(int i = 0; i < rule.getChildCount(); ++i) {
            var child = rule.getChild(i);
            if(child instanceof TerminalNode) {
                var terminalNode = (TerminalNode) child;
                if(terminalNode.getText().equals(hookName)) {
                    return terminalNode.getSymbol();
                }
            }
        }
        throw new IllegalArgumentException("Hook token " + hookName + " not found");
    }

    private void tryAddLinks(String messageTypeName, FactoryListenerContext factoryListener) {
        if(factoryListener.qualifiedName() != null) {
            tryAddLinksOfAggregateContainer(factoryListener.qualifiedName());
        } else {
            tryAddLinkStandaloneComponent(
                    factoryListener.simpleFactoryName,
                    NamingConventions::aggregateNameFromSimpleFactoryName,
                    Aggregate::standaloneFactorySource);
        }

        tryAddLinkListener(
                factoryListener.qualifiedFactoryName,
                factoryListener.simpleFactoryName,
                factoryListener.listenerName,
                messageTypeName,
                factoryListener.simpleFactoryName != null ? MessageListenerContainerType.STANDALONE_FACTORY : MessageListenerContainerType.INNER_FACTORY);
    }

    private void tryAddLinksOfAggregateContainer(QualifiedNameContext qualifiedName) {
        var aggregateNameNode = qualifiedName.NAME(0);
        var source = aggregateContainer(qualifiedName);
        if(source.isPresent()) {
            tryAddLink(aggregateNameNode.getSymbol(), source.get(), Optional::of);

            var typeNameNode = qualifiedName.NAME(1);
            tryAddLink(typeNameNode.getSymbol(), source.get(), unit -> Optional.of(unit.getType(typeNameNode.getText())));
        }
    }

    private void tryAddLinkStandaloneComponent(
            Token simpleName,
            UnaryOperator<String> aggregateNameProvider,
            Function<Aggregate, Optional<Source>> sourceProvider) {
        var className = simpleName.getText();
        var aggregateName = aggregateNameProvider.apply(className);
        var aggregate = modelOrElseThrow().aggregate(aggregateName);
        if(aggregate.isPresent()) {
            tryAddLink(simpleName, sourceProvider.apply(aggregate.get()),
                    Optional::of);
        }
    }

    private void tryAddLinkListener(
            QualifiedNameContext containerQualifiedName,
            Token containerSimpleName,
            Token listenerName,
            String messageTypeName,
            MessageListenerContainerType containerType) {

        var listenerContainer = listenerContainer(
                containerQualifiedName,
                containerSimpleName,
                containerType);
        var listenerExtractor = listenerExtractor(
                containerQualifiedName,
                listenerName.getText(),
                messageTypeName);
        tryAddLink(listenerName, listenerContainer, listenerExtractor);
    }

    private Optional<Source> listenerContainer(
            QualifiedNameContext qualifiedName,
            Token simpleName,
            MessageListenerContainerType containerType) {
        if(qualifiedName != null) {
            return aggregateContainer(qualifiedName).orElse(Optional.empty());
        } else {
            try {
                if(containerType == MessageListenerContainerType.STANDALONE_ROOT) {
                    var aggregateName = NamingConventions.aggregateNameFromSimpleRootName(simpleName.getText());
                    var aggregate = modelOrElseThrow().aggregate(aggregateName);
                    return aggregate.map(Aggregate::standaloneRootSource).orElse(Optional.empty());
                } else if(containerType == MessageListenerContainerType.STANDALONE_FACTORY) {
                    var aggregateName = NamingConventions.aggregateNameFromSimpleFactoryName(simpleName.getText());
                    var aggregate = modelOrElseThrow().aggregate(aggregateName);
                    return aggregate.map(Aggregate::standaloneFactorySource).orElse(Optional.empty());
                } else if(containerType == MessageListenerContainerType.STANDALONE_REPOSITORY) {
                    var aggregateName = NamingConventions.aggregateNameFromSimpleRepositoryName(simpleName.getText());
                    var aggregate = modelOrElseThrow().aggregate(aggregateName);
                    return aggregate.map(Aggregate::standaloneRepositorySource).orElse(Optional.empty());
                } else {
                    return Optional.empty();
                }
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
    }

    private Optional<Optional<Source>> aggregateContainer(QualifiedNameContext qualifiedName) {
        var aggregate = aggregate(qualifiedName);
        return aggregate.map(Aggregate::containerSource);
    }

    private Optional<Aggregate> aggregate(QualifiedNameContext qualifiedName) {
        var aggregateNameNode = qualifiedName.NAME(0);
        var aggregateName = aggregateNameNode.getText();
        return modelOrElseThrow().aggregate(aggregateName);
    }

    private Function<IType, Optional<IMember>> listenerExtractor(
            QualifiedNameContext qualifiedName,
            String methodName,
            String messageTypeName) {
        return type -> {
            IType listenerContainer;
            if(qualifiedName != null) {
                listenerContainer = type.getType(qualifiedName.name.getText());
            } else {
                listenerContainer = type;
            }

            if(listenerContainer.exists()) {
                IMethod method;
                if(HOOK_NAMES.contains(methodName)) {
                    method = listenerContainer.getMethod(methodName, new String[0]);
                } else {
                    method = listenerContainer.getMethod(methodName, new String[] { Signature.createTypeSignature(messageTypeName, false) });
                }
                return Optional.of(method);
            } else {
                return Optional.empty();
            }
        };
    }

    private static final Set<String> HOOK_NAMES = new HashSet<>();
    static {
        HOOK_NAMES.add(Hooks.ON_ADD_METHOD_NAME);
        HOOK_NAMES.add(Hooks.ON_DELETE_METHOD_NAME);
    }

    private void tryAddLinks(AggregateRootContext aggregateRoot) {
        if(aggregateRoot != null) {
            if(aggregateRoot.qualifiedName() != null) {
                tryAddLinksOfAggregateContainer(aggregateRoot.qualifiedName());
            } else {
                tryAddLinkStandaloneComponent(
                        aggregateRoot.simpleRootName,
                        NamingConventions::aggregateNameFromSimpleRootName,
                        Aggregate::standaloneRootSource);
            }
        }
    }

    private void tryAddLinks(EventProductionsContext eventProductions) {
        if(eventProductions != null) {
            for(EventProductionContext eventProduction : eventProductions.eventProduction()) {
                tryAddLinks(eventProduction);
            }
        }
    }

    private void tryAddLinks(EventProductionContext eventProduction) {
        tryAddLinkEvent(eventProduction.event());
        tryAddLinks(eventProduction.event().NAME().getText(), eventProduction.messageConsumptions());
    }

    private void tryAddLinkEvent(EventContext event) {
        var eventName = event.NAME();
        var domainEvent = modelOrElseThrow().event(eventName.getText());
        tryAddLink(eventName.getSymbol(), domainEvent.map(DomainEvent::source), Optional::of);
    }

    private void tryAddLinks(String messageTypeName, AggregateRootConsumptionContext aggregateRootConsumption) {
        tryAddLinks(aggregateRootConsumption.aggregateRoot());
        var aggregateName = aggregateName(aggregateRootConsumption.aggregateRoot());
        var listener = modelOrElseThrow().aggregateListeners(aggregateName)
                .stream()
                .filter(candidate -> candidate.methodName().equals(aggregateRootConsumption.listenerName.getText()))
                .filter(candidate -> candidate.consumedMessage().name().equals(messageTypeName))
                .findFirst();
        if(listener.isPresent()
                && listener.get().runnerClass().isPresent()) {
            var runner = modelOrElseThrow().runner(listener.get().runnerClass().orElseThrow());
            tryAddLink(aggregateRootConsumption.runnerName,
                    runner.map(Runner::runnerSource),
                    Optional::of);
        }
        tryAddLinkListener(
                aggregateRootConsumption.aggregateRoot().qualifiedRootName,
                aggregateRootConsumption.aggregateRoot().simpleRootName,
                aggregateRootConsumption.listenerName,
                messageTypeName,
                aggregateRootConsumption.aggregateRoot().simpleRootName != null ? MessageListenerContainerType.STANDALONE_ROOT : MessageListenerContainerType.INNER_ROOT);
        tryAddLinks(aggregateRootConsumption.eventProductions());
    }

    private String aggregateName(AggregateRootContext aggregateRoot) {
        if(aggregateRoot.qualifiedRootName != null) {
            return aggregateRoot.qualifiedRootName.qualifier.getText();
        } else {
            return NamingConventions.aggregateNameFromSimpleRootName(aggregateRoot.simpleRootName.getText());
        }
    }

    private void tryAddLinks(String messageTypeName, RepositoryConsumptionContext repositoryConsumption) {
        tryAddLinksOfName(repositoryConsumption);
        tryAddLinkListener(
                repositoryConsumption.qualifiedRepositoryName,
                repositoryConsumption.simpleRepositoryName,
                repositoryConsumption.listenerName,
                messageTypeName,
                repositoryConsumption.simpleRepositoryName != null ? MessageListenerContainerType.STANDALONE_REPOSITORY : MessageListenerContainerType.INNER_REPOSITORY);
        if(repositoryConsumption.aggregateRoot() != null) {
            tryAddLinks(repositoryConsumption.aggregateRoot());
            tryAddLinkListener(
                    repositoryConsumption.aggregateRoot().qualifiedRootName,
                    repositoryConsumption.aggregateRoot().simpleRootName,
                    hookNameToken(repositoryConsumption, "onDelete"),
                    null,
                    repositoryConsumption.aggregateRoot().simpleRootName != null ? MessageListenerContainerType.STANDALONE_ROOT : MessageListenerContainerType.INNER_ROOT);
            tryAddLinks(repositoryConsumption.eventProductions());
        }
    }

    private void tryAddLinksOfName(RepositoryConsumptionContext repositoryConsumption) {
        if(repositoryConsumption.qualifiedName() != null) {
            tryAddLinksOfAggregateContainer(repositoryConsumption.qualifiedName());
        } else {
            tryAddLinkStandaloneComponent(
                    repositoryConsumption.simpleRepositoryName,
                    NamingConventions::aggregateNameFromSimpleRepositoryName,
                    Aggregate::standaloneRepositorySource);
        }
    }

    private void tryAddLinks(ProcessConsumptionContext processConsumption) {
        var processNameToken = processConsumption.NAME();
        var linkRegion = region(processNameToken.getSymbol());
        if(new RegionQueries(linkRegion).contains(region)) {
            logger.debug("Found link with region {}..{}", linkRegion.getOffset(), linkRegion.getOffset() + linkRegion.getLength() - 1);
            var processName = processNameToken.getText();
            var openEmilEditor = new OpenEmilEditorAction.Builder()
                    .processName(processName)
                    .project(editor.getPousseCafeProject())
                    .workbenchWindow(editor.getSite().getWorkbenchWindow())
                    .build();
            var link = new ActionHyperlink.Builder()
                    .region(linkRegion)
                    .action(openEmilEditor)
                    .name(processName)
                    .build();
            links.add(link);
        }
    }

    private void tryAddLinksOfEventConsumption(ConsumptionContext consumption) {
        var eventConsumption = consumption.eventConsumption();
        if(eventConsumption != null) {
            tryAddLinkEvent(eventConsumption.event());
            tryAddLinks(eventConsumption.event().NAME().getText(), eventConsumption.messageConsumptions());
        }
    }

    private IRegion region(Token token) {
        return new org.eclipse.jface.text.Region(token.getStartIndex(),
                token.getStopIndex() - token.getStartIndex() + 1);
    }

    public EmilHyperlinkDetector(EmilEditor editor) {
        this.editor = editor;
    }
}
