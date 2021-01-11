package poussecafe.eclipse.plugin.editors;

import org.eclipse.ui.editors.text.TextEditor;

public class EmilEditor extends TextEditor {

    public EmilEditor() {
        setSourceViewerConfiguration(new EmilConfiguration(style()));
    }

    private Style style() {
        if(style == null) {
            style = new Style();
        }
        return style;
    }

    private Style style;

    @Override
    public void dispose() {
        if(style != null) {
            style.dispose();
            style = null;
        }
        super.dispose();
    }
}
