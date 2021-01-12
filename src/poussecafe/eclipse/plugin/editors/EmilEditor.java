package poussecafe.eclipse.plugin.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmilEditor extends TextEditor {

    public EmilEditor() {
        setSourceViewerConfiguration(new EmilConfiguration(this));
        setDocumentProvider(new FileDocumentProvider());
    }

    public Style style() {
        if(style == null) {
            style = new Style();
        }
        return style;
    }

    private Style style;

    @Override
    public void dispose() {
        clearMarkers();
        if(style != null) {
            style.dispose();
            style = null;
        }
        super.dispose();
    }

    public void clearMarkers() {
        try {
            var file = getEditorInput().getAdapter(IFile.class);
            file.deleteMarkers(EMIL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            logger.error("Unable to clear markers", e);
        }
    }

    private static final String EMIL_PROBLEM_MARKER = "poussecafe.eclipse.plugin.emilProblem";

    private Logger logger = LoggerFactory.getLogger(getClass());

    public void addMarker(String message, int lineNumber, int severity, Position position) {
        try {
            var file = getEditorInput().getAdapter(IFile.class);
            IMarker marker = file.createMarker(EMIL_PROBLEM_MARKER);
            marker.setAttribute(IMarker.MESSAGE, message);
            marker.setAttribute(IMarker.SEVERITY, severity);
            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
            if(position != null) {
                marker.setAttribute(IMarker.CHAR_START, position.getOffset());
                marker.setAttribute(IMarker.CHAR_END, position.getOffset() + position.getLength());
            }
        } catch (CoreException e) {
            logger.error("Unable to add marker", e);
        }
    }
}
