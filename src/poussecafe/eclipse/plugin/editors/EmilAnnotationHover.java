package poussecafe.eclipse.plugin.editors;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.DefaultAnnotationHover;

public class EmilAnnotationHover extends DefaultAnnotationHover {

    @Override
    protected boolean isIncluded(Annotation annotation) {
        return !isQuickDiff(annotation);
    }

    private boolean isQuickDiff(Annotation annotation) {
        return annotation.getType().startsWith("org.eclipse.ui.workbench.texteditor.quickdiff");
    }
}
