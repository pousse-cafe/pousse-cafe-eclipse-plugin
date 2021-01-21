package poussecafe.eclipse.plugin.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class EmilConfiguration extends TextSourceViewerConfiguration {

    public EmilConfiguration(EmilEditor editor) {
        super(EditorsUI.getPreferenceStore());
        this.editor = editor;
    }

    private EmilEditor editor;

    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = new PresentationReconciler();

        DefaultDamagerRepairer defaultDamagerRepairer = new DefaultDamagerRepairer(new EmilTokenScanner(editor));
        reconciler.setDamager(defaultDamagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(defaultDamagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);

        return reconciler;
    }

    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new EmilAnnotationHover();
    }

    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
        IHyperlinkDetector[] detectors = super.getHyperlinkDetectors(sourceViewer);
        var linkDetector = new EmilHyperlinkDetector(editor);
        if(detectors == null) {
            return new IHyperlinkDetector[] { linkDetector };
        } else {
            var newDetectors = new IHyperlinkDetector[detectors.length + 1];
            System.arraycopy(detectors, 0, newDetectors, 0, detectors.length);
            newDetectors[newDetectors.length - 1] = linkDetector;
            return newDetectors;
        }
    }
}
