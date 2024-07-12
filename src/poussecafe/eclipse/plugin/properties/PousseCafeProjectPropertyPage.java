package poussecafe.eclipse.plugin.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import poussecafe.eclipse.plugin.core.Browser;

public class PousseCafeProjectPropertyPage extends PropertyPage {

    private static final String POUSSE_CAFE_PROPERTY_QUALIFIER = "poussecafe";

    private static final String BASE_PACKAGE_TITLE = "Base package:";

    public static final QualifiedName BASE_PACKAGE_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "basePackage");

    public static final String DEFAULT_BASE_PACKAGE = "default.base.package";

    private static final String SOURCE_FOLDER_TITLE = "Source folder:";

    public static final QualifiedName SOURCE_FOLDER_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "sourceFolder");

    public static final String DEFAULT_SOURCE_FOLDER = "src/";

    public static final QualifiedName DOMAIN_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "domain");

    public static final String DEFAULT_DOMAIN = "My Domain";

    private static final int TEXT_FIELD_WIDTH = 50;

    public static final QualifiedName USES_INTERNAL_STORAGE_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "usesInternalStorage");

    public static final String DEFAULT_USES_INTERNAL_STORAGE = "true";

    public static final QualifiedName USES_SPRING_MONGO_STORAGE_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "usesSpringMongoStorage");

    public static final String DEFAULT_USES_SPRING_MONGO_STORAGE = "false";

    public static final QualifiedName USES_SPRING_JPA_STORAGE_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "usesSpringJpaStorage");

    public static final String DEFAULT_USES_SPRING_JPA_STORAGE = "false";

    public static final QualifiedName OPEN_IN_EXTERNAL_BROWSER_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "openInExternalBrowser");

    public static final String DEFAULT_OPEN_IN_EXTERNAL_BROWSER = Integer.toString(Browser.ECLIPSE.ordinal());

    public PousseCafeProjectPropertyPage() {
        super();
    }

    @Override
    protected Control createContents(Composite parent) {
        pageRoot = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        pageRoot.setLayout(layout);
        GridData data = new GridData(GridData.FILL);
        data.grabExcessHorizontalSpace = true;
        pageRoot.setLayoutData(data);

        addGeneralSection();
        addSeparator();
        addDocumentationSection();
        addSeparator();
        addStorageSection();

        return pageRoot;
    }

    private Composite pageRoot;

    private void addGeneralSection() {
        var fieldsComposite = createTwoColumnsComposite();
        basePackageText = addField(fieldsComposite, BASE_PACKAGE_TITLE, BASE_PACKAGE_PROPERTY_NAME, DEFAULT_BASE_PACKAGE);
        sourceFolderText = addField(fieldsComposite, SOURCE_FOLDER_TITLE, SOURCE_FOLDER_PROPERTY_NAME, DEFAULT_SOURCE_FOLDER);
    }

    private Composite createTwoColumnsComposite() {
        var composite = new Composite(pageRoot, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        composite.setLayout(layout);

        GridData data = new GridData();
        data.verticalAlignment = GridData.FILL;
        data.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(data);

        return composite;
    }

    private Text basePackageText;

    private Text sourceFolderText;

    private Text addField(Composite parent, String title, QualifiedName propertyName, String defaultValue) {
        var label = new Label(parent, SWT.NULL);
        label.setText(title);

        var field = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData();
        gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
        field.setLayoutData(gd);

        try {
            String value = getResource().getPersistentProperty(propertyName);
            field.setText((value != null) ? value : defaultValue);
        } catch (CoreException e) {
            field.setText(defaultValue);
        }

        return field;
    }

    private IResource getResource() {
        var element = getElement();
        if(element instanceof IProject) {
            return (IResource) element;
        } else {
            return getElement().getAdapter(IProject.class);
        }
    }

    private void addSeparator() {
        Label separator = new Label(pageRoot, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        separator.setLayoutData(gridData);
    }

    private void addDocumentationSection() {
        var fieldsComposite = createTwoColumnsComposite();
        domainText = addField(fieldsComposite, "Domain name:", DOMAIN_PROPERTY_NAME, DEFAULT_DOMAIN);
        openInExternalBrowser = addSelect(fieldsComposite, "Browser for documentation",
                OPEN_IN_EXTERNAL_BROWSER_PROPERTY_NAME,
                0,
                new String[] { "Eclipse preferences", "Internal", "External" });
    }

    private Text domainText;

    private Combo addSelect(Composite parent, String title, QualifiedName propertyName, int defaultValue, String[] items) {
        var label = new Label(parent, SWT.NULL);
        label.setText(title);

        var field = new Combo(parent, SWT.READ_ONLY);
        var gd = new GridData();
        gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
        field.setLayoutData(gd);

        field.setItems(items);

        try {
            int value = Integer.parseInt(getResource().getPersistentProperty(propertyName));
            field.select(value);
        } catch (Exception e) {
            field.select(defaultValue);
        }

        return field;
    }

    private Combo openInExternalBrowser;

    private void addStorageSection() {
        var storageComposite = createTwoColumnsComposite();
        usesInternalStorage = addCheckbox(storageComposite, "Uses internal storage",
                USES_INTERNAL_STORAGE_PROPERTY_NAME,
                Boolean.parseBoolean(DEFAULT_USES_INTERNAL_STORAGE));
        usesSpringMongoStorage = addCheckbox(storageComposite, "Uses Spring Mongo storage",
                USES_SPRING_MONGO_STORAGE_PROPERTY_NAME,
                Boolean.parseBoolean(DEFAULT_USES_SPRING_MONGO_STORAGE));
        usesSpringJpaStorage = addCheckbox(storageComposite, "Uses Spring JPA storage",
                USES_SPRING_JPA_STORAGE_PROPERTY_NAME,
                Boolean.parseBoolean(DEFAULT_USES_SPRING_JPA_STORAGE));
    }

    private Button usesInternalStorage;

    private Button usesSpringMongoStorage;

    private Button usesSpringJpaStorage;

    private Button addCheckbox(Composite parent, String title, QualifiedName propertyName, boolean defaultValue) {
        var label = new Label(parent, SWT.NULL);
        label.setText(title);

        var field = new Button(parent, SWT.CHECK);
        GridData gd = new GridData();
        gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
        field.setLayoutData(gd);

        try {
            String value = getResource().getPersistentProperty(propertyName);
            field.setSelection((value != null) ? Boolean.parseBoolean(value) : defaultValue);
        } catch (CoreException e) {
            field.setSelection(defaultValue);
        }

        return field;
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        basePackageText.setText(DEFAULT_BASE_PACKAGE);
        sourceFolderText.setText(DEFAULT_SOURCE_FOLDER);

        domainText.setText(DEFAULT_DOMAIN);
        openInExternalBrowser.select(0);

        usesInternalStorage.setSelection(Boolean.parseBoolean(DEFAULT_USES_INTERNAL_STORAGE));
        usesSpringMongoStorage.setSelection(Boolean.parseBoolean(DEFAULT_USES_SPRING_MONGO_STORAGE));
        usesSpringJpaStorage.setSelection(Boolean.parseBoolean(DEFAULT_USES_SPRING_JPA_STORAGE));
    }

    @Override
    public boolean performOk() {
        try {
            getResource().setPersistentProperty(BASE_PACKAGE_PROPERTY_NAME, basePackageText.getText());
            getResource().setPersistentProperty(SOURCE_FOLDER_PROPERTY_NAME, sourceFolderText.getText());

            getResource().setPersistentProperty(DOMAIN_PROPERTY_NAME, domainText.getText());
            getResource().setPersistentProperty(OPEN_IN_EXTERNAL_BROWSER_PROPERTY_NAME,
                    Integer.toString(openInExternalBrowser.getSelectionIndex()));

            getResource().setPersistentProperty(USES_INTERNAL_STORAGE_PROPERTY_NAME,
                    Boolean.toString(usesInternalStorage.getSelection()));
            getResource().setPersistentProperty(USES_SPRING_MONGO_STORAGE_PROPERTY_NAME,
                    Boolean.toString(usesSpringMongoStorage.getSelection()));
            getResource().setPersistentProperty(USES_SPRING_JPA_STORAGE_PROPERTY_NAME,
                    Boolean.toString(usesSpringJpaStorage.getSelection()));
        } catch (CoreException e) {
            return false;
        }
        return true;
    }

}