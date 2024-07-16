package poussecafe.eclipse.plugin.editors;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

public class FontManager {

    private ResourceMap<Integer, Font> map = new ResourceMap<>(FontManager::buildFont);

    private static Font buildFont(int style) {
        var descriptor = JFaceResources.getTextFontDescriptor();
        var boldDescriptor = descriptor.setStyle(style);
        return boldDescriptor.createFont(Display.getCurrent());
    }

    public void dispose() {
        map.dispose();
    }

    public Font defaultBoldTextFont() {
        return textFont(SWT.BOLD);
    }

    public Font textFont(int style) {
        return map.get(style);
    }

    public Font defaultItalicTextFont() {
        return textFont(SWT.ITALIC);
    }
}
