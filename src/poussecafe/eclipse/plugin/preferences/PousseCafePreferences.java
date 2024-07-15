package poussecafe.eclipse.plugin.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;

public class PousseCafePreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public PousseCafePreferences() {
        super(GRID);
        ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
                String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));
        setPreferenceStore(scopedPreferenceStore);
        setDescription("Preferences shared by all Pousse-Café projects");
    }

    public void createFieldEditors() {
        addField(
            new ComboFieldEditor(
                PreferenceConstants.BROWSER_FOR_DOCUMENTATION,
                "&Browser for documentation",
                new String[][] {
                    {"Eclipse preferences", PreferenceConstants.BROWSER_FOR_DOCUMENTATION_ECLIPSE},
                    {"External",            PreferenceConstants.BROWSER_FOR_DOCUMENTATION_EXTERNAL},
                },
                getFieldEditorParent()
            )
        );
    }

    @Override
    public void init(IWorkbench workbench) {
        // Nothing to do
    }
}
