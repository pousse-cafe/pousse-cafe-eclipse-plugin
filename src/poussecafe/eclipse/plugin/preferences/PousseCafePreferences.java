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
        var scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
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
        addField(
            new ColorFieldEditor(
                PreferenceConstants.EMIL_KEYWORD_COLOR,
                "EMIL &keyword color",
                getFieldEditorParent()
            )
        );
        addField(
            new ColorFieldEditor(
                PreferenceConstants.EMIL_EVENT_COLOR,
                "EMIL &event color",
                getFieldEditorParent()
            )
        );
        addField(
            new ColorFieldEditor(
                PreferenceConstants.EMIL_COMMAND_COLOR,
                "EMIL &command color",
                getFieldEditorParent()
            )
        );
        addField(
            new ColorFieldEditor(
                PreferenceConstants.EMIL_EXTERNAL_COLOR,
                "EMIL &external system color",
                getFieldEditorParent()
            )
        );
        addField(
            new ColorFieldEditor(
                PreferenceConstants.EMIL_NAME_COLOR,
                "EMIL component &name color",
                getFieldEditorParent()
            )
        );
        addField(
            new ColorFieldEditor(
                PreferenceConstants.EMIL_PROCESS_COLOR,
                "EMIL &process name color",
                getFieldEditorParent()
            )
        );
        addField(
            new ColorFieldEditor(
                PreferenceConstants.EMIL_MULTIPLICITY_COLOR,
                "EMIL &process name color",
                getFieldEditorParent()
            )
        );
    }

    @Override
    public void init(IWorkbench workbench) {
        // Nothing to do
    }
}
