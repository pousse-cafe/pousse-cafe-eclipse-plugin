package poussecafe.eclipse.plugin.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;


public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        var store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
                String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));

        store.setDefault(PreferenceConstants.BROWSER_FOR_DOCUMENTATION, 1);
        PreferenceConverter.setDefault(store, PreferenceConstants.EMIL_KEYWORD_COLOR, PreferenceConstants.DEFAULT_EMIL_KEYWORD_COLOR);
        PreferenceConverter.setDefault(store, PreferenceConstants.EMIL_EVENT_COLOR, PreferenceConstants.DEFAULT_EMIL_EVENT_COLOR);
        PreferenceConverter.setDefault(store, PreferenceConstants.EMIL_COMMAND_COLOR, PreferenceConstants.DEFAULT_EMIL_COMMAND_COLOR);
        PreferenceConverter.setDefault(store, PreferenceConstants.EMIL_EXTERNAL_COLOR, PreferenceConstants.DEFAULT_EMIL_EXTERNAL_COLOR);
        PreferenceConverter.setDefault(store, PreferenceConstants.EMIL_NAME_COLOR, PreferenceConstants.DEFAULT_EMIL_NAME_COLOR);
        PreferenceConverter.setDefault(store, PreferenceConstants.EMIL_PROCESS_COLOR, PreferenceConstants.DEFAULT_EMIL_PROCESS_COLOR);
        PreferenceConverter.setDefault(store, PreferenceConstants.EMIL_MULTIPLICITY_COLOR, PreferenceConstants.DEFAULT_EMIL_MULTIPLICITY_COLOR);
    }

}
