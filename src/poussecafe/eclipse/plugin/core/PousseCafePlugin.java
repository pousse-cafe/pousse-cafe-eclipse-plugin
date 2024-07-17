package poussecafe.eclipse.plugin.core;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import poussecafe.eclipse.plugin.preferences.PreferenceInitializer;

public class PousseCafePlugin extends AbstractUIPlugin {

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        new PreferenceInitializer().initializeDefaultPreferences();
    }
}
