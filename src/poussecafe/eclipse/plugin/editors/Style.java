package poussecafe.eclipse.plugin.editors;

public class Style {

    public ColorManager colorManager() {
        return colorManager;
    }

    private ColorManager colorManager = new ColorManager();

    public FontManager fontManager() {
        return fontManager;
    }

    private FontManager fontManager = new FontManager();

    public void dispose() {
        colorManager.dispose();
        fontManager.dispose();
    }
}
