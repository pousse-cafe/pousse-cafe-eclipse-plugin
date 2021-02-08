package poussecafe.eclipse.plugin.hyperlink;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poussecafe.eclipse.plugin.actions.OpenJavaEditorAction;
import poussecafe.eclipse.plugin.builder.ResourceSource;
import poussecafe.eclipse.plugin.core.JavaNameResolver;
import poussecafe.eclipse.plugin.core.PousseCafeCore;
import poussecafe.eclipse.plugin.editors.ActionHyperlink;
import poussecafe.source.analysis.CompilationUnitResolver;
import poussecafe.source.analysis.Name;
import poussecafe.source.model.MessageListener;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;


public class MessageListenerHyperlinkDetector extends AbstractHyperlinkDetector {

    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        try {
            IMethod methodForRegion = findMethodForRegion(region);
            if(methodForRegion == null) {
                return noResult();
            } else {
                return methodLinks(region, methodForRegion);
            }
        } catch (JavaModelException e) {
            logger.error("Unable to detect hyperlinks", e);
            return noResult();
        }
    }

    private IHyperlink[] noResult() {
        return arrayOrNull(emptyList());
    }

    private IHyperlink[] arrayOrNull(List<IHyperlink> links) {
        if(links.isEmpty()) {
            return null; // NOSONAR - empty arrays are invalid here
        } else {
            var array = new IHyperlink[links.size()];
            return links.toArray(array);
        }
    }

    private IHyperlink[] methodLinks(IRegion target, IMethod method) throws JavaModelException {
        var links = new ArrayList<IHyperlink>();
        IJavaProject javaProject = method.getJavaProject();
        var pousseCafeProject = PousseCafeCore.getProject(javaProject);

        if(isMessageListener(method)
                && targetIsConsumedMessageName(target, method)) {
            var messageName = consumedMessageName(method);
            var producers = pousseCafeProject.model().orElseThrow().messageListeners().stream()
                    .filter(listener -> listener.producedEvents().stream()
                        .anyMatch(producedEvent -> producedEvent.message().name().equals(messageName)))
                    .collect(toList());
            links.addAll(buildLinksToListeners(
                        region(method),
                        javaProject,
                        producers,
                        "Producer"));
        }

        IAnnotation producedEvent = findProducesEventAnnotationForTarget(target, method);
        if(producedEvent != null) {
            var producedEventMemberValuePair = producedEvent.getMemberValuePairs()[0];
            if(producedEventMemberValuePair.getValueKind() == IMemberValuePair.K_CLASS) {
                var messageTypeName = (String) producedEventMemberValuePair.getValue();
                var messageName = new Name(messageTypeName).simple();
                var consumers = pousseCafeProject.model().orElseThrow().messageListeners().stream()
                        .filter(listener -> listener.consumedMessage().name().equals(messageName))
                        .collect(toList());
                links.addAll(buildLinksToListeners(
                        region(method),
                        javaProject,
                        consumers,
                        "Consumer"));
            }
        }
        return arrayOrNull(links);
    }

    private boolean targetIsConsumedMessageName(IRegion region, IMethod methodForRegion) throws JavaModelException {
        return methodForRegion.getParameters().length == 1
                && rangeContainsRegion(methodForRegion.getParameters()[0].getSourceRange(), region);
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    private IMethod findMethodForRegion(IRegion region) throws JavaModelException {
        ITextEditor editor = getAdapter(ITextEditor.class);
        IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
        IFile file = input.getFile();
        ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(file);
        IType primaryType = compilationUnit.findPrimaryType();
        return findMethodForRegion(primaryType, region);
    }

    private IMethod findMethodForRegion(IType type, IRegion region) throws JavaModelException {
        for(IType innerType : type.getTypes()) {
            var typeRange = innerType.getSourceRange();
            if(SourceRange.isAvailable(typeRange)
                    && rangeContainsRegion(typeRange, region)) {
                return findMethodForRegion(innerType, region);
            }
        }
        for(IMethod method : type.getMethods()) {
            var methodRange = method.getSourceRange();
            if(SourceRange.isAvailable(methodRange)
                    && rangeContainsRegion(methodRange, region)) {
                return method;
            }
        }
        return null;
    }

    private boolean rangeContainsRegion(ISourceRange methodRange,
            IRegion region) {
        return methodRange.getOffset() <= region.getOffset()
                && endOf(methodRange) >= endOf(region);
    }

    private int endOf(IRegion region) {
        return region.getOffset() + region.getLength();
    }

    private int endOf(ISourceRange range) {
        return range.getOffset() + range.getLength();
    }

    private boolean isMessageListener(IMethod method) throws JavaModelException {
        IType declaringType = method.getDeclaringType();
        for(IAnnotation annotation : method.getAnnotations()) {
            var annotationTypeName = annotation.getElementName();
            var resolvedAnnotationTypeName = JavaNameResolver.resolve(declaringType,
                    annotationTypeName);
            if(CompilationUnitResolver.MESSAGE_LISTENER_ANNOTATION_CLASS.equals(resolvedAnnotationTypeName)) {
                return true;
            }
        }
        return false;
    }

    private String consumedMessageName(IMethod method) {
        return JavaNameResolver.simpleName(method.getParameterTypes()[0]);
    }

    private IRegion region(IMethod methodForRegion) throws JavaModelException {
        var sourceRange = methodForRegion.getParameters()[0].getSourceRange();
        return new Region(sourceRange.getOffset(), sourceRange.getLength());
    }

    private List<IHyperlink> buildLinksToListeners(
            IRegion region,
            IJavaProject project,
            List<MessageListener> listeners,
            String listenerType) throws JavaModelException {
        var links = new ArrayList<IHyperlink>(listeners.size());
        ITextEditor editor = getAdapter(ITextEditor.class);
        for(MessageListener listener : listeners) {
            var source = (ResourceSource) listener.source();
            source.connect(project);
            ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(source.file());
            IType containerType = compilationUnit.findPrimaryType();
            IMethod listenerMethod = locateListenerMethod(containerType, listener);
            if(listenerMethod != null) {
                var action = new OpenJavaEditorAction.Builder()
                        .member(listenerMethod)
                        .site(editor.getEditorSite())
                        .build();
                links.add(new ActionHyperlink.Builder()
                        .action(action)
                        .name(listenerType + " " + listener.id())
                        .region(region)
                        .build());
            }
        }
        return links;
    }

    private IMethod locateListenerMethod(IType type, MessageListener listener) throws JavaModelException {
        for(IMethod method : type.getMethods()) {
            if(isMessageListener(method, listener)) {
                return method;
            }
        }
        for(IType innerType : type.getTypes()) {
            var method = locateListenerMethod(innerType, listener);
            if(method != null) {
                return method;
            }
        }
        return null;
    }

    private boolean isMessageListener(IMethod method, MessageListener listener) throws JavaModelException {
        return isMessageListener(method)
                && method.getParameterTypes().length == 1
                && listener.consumedMessage().name().equals(consumedMessageName(method));
    }

    private IAnnotation findProducesEventAnnotationForTarget(IRegion target, IMethod method) throws JavaModelException {
        IType declaringType = method.getDeclaringType();
        for(IAnnotation annotation : method.getAnnotations()) {
            var annotationTypeName = annotation.getElementName();
            var resolvedAnnotationTypeName = JavaNameResolver.resolve(declaringType,
                    annotationTypeName);
            if(CompilationUnitResolver.PRODUCES_EVENT_ANNOTATION_CLASS.equals(resolvedAnnotationTypeName)
                    && rangeContainsRegion(annotation.getSourceRange(), target)) {
                return annotation;
            }
        }
        return null;
    }
}
