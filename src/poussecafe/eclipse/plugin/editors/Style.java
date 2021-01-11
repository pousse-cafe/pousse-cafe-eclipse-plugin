package poussecafe.eclipse.plugin.editors;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class Style {

    public ColorManager colorManager() {
        return colorManager;
    }

    private ColorManager colorManager = new ColorManager();

    public static final RGB KEYWORD_COLOR = new RGB(127, 0, 85);

    public static final RGB DEFAULT_FOREGROUND_COLOR = new RGB(0, 0, 0);

    public static final RGB DEFAULT_BACKGROUND_COLOR = new RGB(255, 255, 255);

    public static final RGB EVENT_NAME_COLOR = new RGB(51, 105, 30);

    public static final RGB COMMAND_NAME_COLOR = new RGB(255, 87, 34);

    public static final RGB EXTERNAL_COLOR = new RGB(121, 85, 72);

    public static final RGB NAME_COLOR = new RGB(63, 81, 181);

    public static final RGB LISTENER_NAME_COLOR = DEFAULT_FOREGROUND_COLOR;

    public static final RGB PROCESS_NAME_COLOR = new RGB(121, 85, 72);

    public static final RGB MULTIPLICITY_SYMBOL_COLOR = new RGB(244, 67, 54);

    public void dispose() {
        colorManager.dispose();
        fonts.values().forEach(Font::dispose);
        fonts.clear();
    }

    private Map<Integer, Font> fonts = new HashMap<>();

    public Font defaultTextFont() {
        return JFaceResources.getTextFont();
    }

    public Font defaultBoldTextFont() {
        return textFont(SWT.BOLD);
    }

    public Font textFont(int style) {
        return fonts.computeIfAbsent(style, this::newTextFont);
    }

    private Font newTextFont(int style) {
        var descriptor = JFaceResources.getTextFontDescriptor();
        var boldDescriptor = descriptor.setStyle(style);
        return boldDescriptor.createFont(Display.getCurrent());
    }

    public Font defaultItalicTextFont() {
        return textFont(SWT.ITALIC);
    }
}
