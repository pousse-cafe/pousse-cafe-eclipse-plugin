package poussecafe.eclipse.plugin.editors;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;

public class ColorManager {

    public ColorManager() {
        preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
                String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));
    }

    private ScopedPreferenceStore preferenceStore;

    private ResourceMap<RGB, Color> map = new ResourceMap<>(ColorManager::buildColor);

    public void dispose() {
        map.dispose();
    }

    public Color getColor(RGB rgb) {
        return map.get(rgb);
    }

    private static Color buildColor(RGB rgb) {
        return new Color(Display.getCurrent(), rgb);
    }

    public Color getColor(String id) {
        var rgbColor = PreferenceConverter.getColor(preferenceStore, id);
        return getColor(rgbColor);
    }
}
