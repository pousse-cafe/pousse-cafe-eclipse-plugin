package poussecafe.eclipse.plugin.editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poussecafe.source.emil.parser.EmilParser.AggregateRootConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.CommandConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.ConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.ConsumptionsContext;
import poussecafe.source.emil.parser.EmilParser.EmptyConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.EventConsumptionContext;
import poussecafe.source.emil.parser.EmilParser.EventProductionContext;
import poussecafe.source.emil.parser.EmilParser.EventProductionsContext;
import poussecafe.source.emil.parser.EmilParser.ExternalContext;
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
import poussecafe.source.emil.parser.TreeParser;

import static java.util.stream.Collectors.toList;

public class EmilScanner implements ITokenScanner {

    @Override
    public void setRange(IDocument document, int offset, int length) {
        logger.debug("Scanning range {}..{}", offset, offset + length - 1);
        rangeOffset = offset;
        rangeLength = length;
        tokens.clear();
        try {
            var tree = TreeParser.parseString(document.get());
            computeTokens(tree.processContext());
        } catch (Exception e) {
            logger.debug("Unable to parse document", e);
        }
        tokenIterator = tokens.iterator();
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    private int rangeOffset;

    private int rangeLength;

    private List<EmilToken> tokens = new ArrayList<>();

    private void computeTokens(ProcessContext process) {
        computeHeaderTokens(process.header());
        computeConsumptionsTokens(process.consumptions());
        padIfNecessary(rangeOffset + rangeLength);
    }

    private void computeHeaderTokens(HeaderContext header) {
        computeToken(header.getStart(),  keywordTextAttribute());
    }

    private void computeToken(Token singleRuleToken, TextAttribute textAttribute) {
        computeToken(singleRuleToken, singleRuleToken, textAttribute);
    }

    private void computeToken(Token start, Token stop, TextAttribute textAttribute) {
        int offset = offset(start);
        int length = length(start, stop);
        if(isRegionInRange(offset, length)) {
            padIfNecessary(offset);
            tokens.add(new EmilToken(offset, length, textAttribute));
        }
    }

    private int offset(Token start) {
        return start.getStartIndex();
    }

    private int length(Token start, Token stop) {
        var startIndex = start.getStartIndex();
        var endIndex = stop.getStopIndex();
        return endIndex - startIndex + 1;
    }

    private boolean isRegionInRange(int offset, int length) {
        return offset >= rangeOffset && (offset + length) <= (rangeOffset + rangeLength);
    }

    private void padIfNecessary(int nextTokenOffset) {
        if(!tokens.isEmpty()
                && tokens.get(tokens.size() - 1).endInclusive() < nextTokenOffset - 1) {
            var offset = tokens.get(tokens.size() - 1).endInclusive() + 1;
            var end = nextTokenOffset - 1;
            var length = end - offset + 1;
            tokens.add(new EmilToken(offset, length, defaultTextAttribute()));
        }
    }

    private TextAttribute defaultTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.DEFAULT_FOREGROUND_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultTextFont());
    }

    private TextAttribute keywordTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.KEYWORD_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultBoldTextFont());
    }

    private Style style;

    private void computeConsumptionsTokens(ConsumptionsContext consumptions) {
        for(ConsumptionContext consumption : consumptions.consumption()) {
            if(consumption.eventConsumption() != null) {
                computeEventConsumptionTokens(consumption.eventConsumption());
            }
            if(consumption.commandConsumption() != null) {
                computeCommandConsumptionTokens(consumption.commandConsumption());
            }
        }
    }

    private void computeEventConsumptionTokens(EventConsumptionContext eventConsumption) {
        if(eventConsumption.external() != null) {
            computeExternalToken(eventConsumption.external());
        }
        computeToken(eventConsumption.event().NAME().getSymbol(), eventNameTextAttribute());
        computeMessageConsumptionsTokens(eventConsumption.messageConsumptions());
    }

    private void computeExternalToken(ExternalContext external) {
        computeToken(external.getStart(), external.getStop(), externalTextAttribute());
    }

    private TextAttribute externalTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.EXTERNAL_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultTextFont());
    }

    private TextAttribute eventNameTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.EVENT_NAME_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultTextFont());
    }

    private void computeMessageConsumptionsTokens(MessageConsumptionsContext messageConsumptions) {
        if(messageConsumptions.singleMessageConsumption() != null) {
            computeSingleMessageConsumptionTokens(messageConsumptions.singleMessageConsumption());
        } else if(messageConsumptions.multipleMessageConsumptions() != null) {
            var singleConsumptions =
                    messageConsumptions.multipleMessageConsumptions().multipleMessageConsumptionsItem().stream()
                    .map(MultipleMessageConsumptionsItemContext::singleMessageConsumption)
                    .collect(toList());
            for(SingleMessageConsumptionContext singleConsumption : singleConsumptions) {
                computeSingleMessageConsumptionTokens(singleConsumption);
            }
        }
    }

    private void computeSingleMessageConsumptionTokens(SingleMessageConsumptionContext singleMessageConsumption) {
        if(singleMessageConsumption.factoryConsumption() != null) {
            computeFactoryConsumptionTokens(singleMessageConsumption.factoryConsumption());
        } else if(singleMessageConsumption.repositoryConsumption() != null) {
            computeRepositoryConsumptionTokens(singleMessageConsumption.repositoryConsumption());
        } else if(singleMessageConsumption.aggregateRootConsumption() != null) {
            computeAggregateRootConsumptionTokens(singleMessageConsumption.aggregateRootConsumption());
        } else if(singleMessageConsumption.processConsumption() != null) {
            computeProcessConsumptionTokens(singleMessageConsumption.processConsumption());
        } else if(singleMessageConsumption.emptyConsumption() != null) {
            computeEmptyConsumptionTokens(singleMessageConsumption.emptyConsumption());
        }
    }

    private void computeFactoryConsumptionTokens(FactoryConsumptionContext factoryConsumption) {
        computeFactoryListenerTokens(factoryConsumption.factoryListener());
        if(factoryConsumption.aggregateRoot() != null
                && factoryConsumption.eventProductions() != null) {
            computeNameTokens(factoryConsumption.aggregateRoot().simpleRootName, factoryConsumption.aggregateRoot().qualifiedRootName);
            computeHookToken((TerminalNode) factoryConsumption.getChild(3));
            computeEventProductionsTokens(factoryConsumption.eventProductions());
        }
    }

    private void computeHookToken(TerminalNode hookName) {
        computeToken(hookName.getSymbol(), hookNameTextAttribute());
    }

    private TextAttribute hookNameTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.DEFAULT_FOREGROUND_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultItalicTextFont());
    }

    private void computeFactoryListenerTokens(FactoryListenerContext factoryListener) {
        var fLiteral = (TerminalNode) factoryListener.getChild(0);
        computeToken(fLiteral.getSymbol(), keywordTextAttribute());
        computeNameTokens(factoryListener.simpleFactoryName, factoryListener.qualifiedFactoryName);
        computeListenerNameToken(factoryListener.listenerName);
        if(factoryListener.optional != null) {
            computeToken(factoryListener.optional, multiplicitySymbolTextAttribute());
        }
        if(factoryListener.serveral != null) {
            computeToken(factoryListener.serveral, multiplicitySymbolTextAttribute());
        }
    }

    private void computeNameTokens(Token simpleName, QualifiedNameContext qualifiedName) {
        if(simpleName != null) {
            computeSimpleNameToken(simpleName);
        } else if(qualifiedName != null) {
            computeQualifiedNameToken(qualifiedName);
        }
    }

    private void computeSimpleNameToken(Token simpleFactoryName) {
        computeToken(simpleFactoryName, nameTextAttribute());
    }

    private TextAttribute nameTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.NAME_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultTextFont());
    }

    private void computeQualifiedNameToken(QualifiedNameContext qualifiedFactoryName) {
        computeToken(qualifiedFactoryName.qualifier, nameTextAttribute());
    }

    private void computeListenerNameToken(Token listenerName) {
        computeToken(listenerName, listenerNameTextAttribute());
    }

    private TextAttribute listenerNameTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.LISTENER_NAME_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultTextFont());
    }

    private TextAttribute multiplicitySymbolTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.MULTIPLICITY_SYMBOL_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultBoldTextFont());
    }

    private void computeEventProductionsTokens(EventProductionsContext eventProductions) {
        for(EventProductionContext production : eventProductions.eventProduction()) {
            computeEventProductionTokens(production);
        }
    }

    private void computeEventProductionTokens(EventProductionContext production) {
        computeToken(production.event().NAME().getSymbol(), eventNameTextAttribute());
        if(production.optional != null) {
            computeToken(production.optional, multiplicitySymbolTextAttribute());
        }
        computeMessageConsumptionsTokens(production.messageConsumptions());
    }

    private void computeRepositoryConsumptionTokens(RepositoryConsumptionContext repositoryConsumption) {
        var reLiteral = (TerminalNode) repositoryConsumption.getChild(0);
        computeToken(reLiteral.getSymbol(), keywordTextAttribute());
        computeNameTokens(repositoryConsumption.simpleRepositoryName, repositoryConsumption.qualifiedRepositoryName);
        computeListenerNameToken(repositoryConsumption.listenerName);
        if(repositoryConsumption.aggregateRoot() != null
                && repositoryConsumption.eventProductions() != null) {
            computeNameTokens(repositoryConsumption.aggregateRoot().simpleRootName, repositoryConsumption.aggregateRoot().qualifiedRootName);
            computeHookToken(onDeleteHookToken(repositoryConsumption));
            computeEventProductionsTokens(repositoryConsumption.eventProductions());
        }
    }

    private TerminalNode onDeleteHookToken(RepositoryConsumptionContext repositoryConsumption) {
        for(int i = 0; i < repositoryConsumption.getChildCount(); ++i) {
            var child = repositoryConsumption.getChild(i);
            if(child instanceof TerminalNode) {
                var terminalNode = (TerminalNode) child;
                if(terminalNode.getText().equals("onDelete")) {
                    return terminalNode;
                }
            }
        }
        throw new IllegalArgumentException("onDelete hook not found");
    }

    private void computeAggregateRootConsumptionTokens(AggregateRootConsumptionContext aggregateRootConsumption) {
        var ruLiteral = (TerminalNode) aggregateRootConsumption.getChild(0);
        computeToken(ruLiteral.getSymbol(), keywordTextAttribute());
        computeNameTokens(aggregateRootConsumption.aggregateRoot().simpleRootName, aggregateRootConsumption.aggregateRoot().qualifiedRootName);
        computeListenerNameToken(aggregateRootConsumption.listenerName);
        if(aggregateRootConsumption.eventProductions() != null) {
            computeEventProductionsTokens(aggregateRootConsumption.eventProductions());
        }
    }

    private void computeProcessConsumptionTokens(ProcessConsumptionContext processConsumption) {
        var pLiteral = (TerminalNode) processConsumption.getChild(0);
        computeToken(pLiteral.getSymbol(), keywordTextAttribute());
        computeToken(processConsumption.processName, processNameTextAttribute());
    }

    private TextAttribute processNameTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.PROCESS_NAME_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultTextFont());
    }

    private void computeEmptyConsumptionTokens(EmptyConsumptionContext emptyConsumption) {
        if(emptyConsumption.external() != null) {
            computeExternalToken(emptyConsumption.external());
        }
    }

    private void computeCommandConsumptionTokens(CommandConsumptionContext commandConsumption) {
        computeToken(commandConsumption.command().NAME().getSymbol(), commandNameTextAttribute());
        computeMessageConsumptionsTokens(commandConsumption.messageConsumptions());
    }

    private TextAttribute commandNameTextAttribute() {
        var foregroundColor = style.colorManager().getColor(Style.COMMAND_NAME_COLOR);
        var backgroundColor = style.colorManager().getColor(Style.DEFAULT_BACKGROUND_COLOR);
        return new TextAttribute(foregroundColor, backgroundColor, 0, style.defaultTextFont());
    }

    private Iterator<EmilToken> tokenIterator;

    @Override
    public IToken nextToken() {
        if(tokenIterator.hasNext()) {
            nextToken = tokenIterator.next();
            logToken(nextToken);
            return nextToken;
        } else {
            nextToken = null;
            return org.eclipse.jface.text.rules.Token.EOF;
        }
    }

    private EmilToken nextToken;

    private void logToken(EmilToken token) {
        if(logger.isDebugEnabled()) {
            logger.debug("Issuing token (o:{}, l:{}, data:{})", token.offset(), token.length(), toString(token.getData()));
        }
    }

    private String toString(Object data) {
        if(data instanceof TextAttribute) {
            var textAttribute = (TextAttribute) data;
            var builder = new StringBuilder();
            builder.append("[");
            builder.append(textAttribute.getForeground());
            builder.append(",");
            builder.append(textAttribute.getBackground());
            builder.append(",");
            builder.append(textAttribute.getFont());
            builder.append("]");
            return builder.toString();
        } else {
            return "";
        }
    }

    @Override
    public int getTokenLength() {
        if(nextToken == null) {
            throw new IllegalStateException("EOF");
        }
        return nextToken.length();
    }

    @Override
    public int getTokenOffset() {
        if(nextToken == null) {
            throw new IllegalStateException("EOF");
        }
        return nextToken.offset();
    }

    public EmilScanner(Style style) {
        this.style = style;
    }
}
