package poussecafe.eclipse.plugin.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public class PousseCafeProjectPropertyPage extends PropertyPage {

    private static final String POUSSE_CAFE_PROPERTY_QUALIFIER = "poussecafe";

    private static final String BASE_PACKAGE_TITLE = "Base package:";

    public static final QualifiedName BASE_PACKAGE_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "basePackage");

    public static final String DEFAULT_BASE_PACKAGE = "default.base.package";

    private static final String SOURCE_FOLDER_TITLE = "Source folder:";

    public static final QualifiedName SOURCE_FOLDER_PROPERTY_NAME = new QualifiedName(POUSSE_CAFE_PROPERTY_QUALIFIER, "sourceFolder");

    public static final String DEFAULT_SOURCE_FOLDER = "src/";

    private static final int TEXT_FIELD_WIDTH = 50;

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

        addSingleSection();

        return pageRoot;
    }

    private Composite pageRoot;

    private void addSingleSection() {
        createDefaultComposite();

        basePackageText = addField(BASE_PACKAGE_TITLE, BASE_PACKAGE_PROPERTY_NAME, DEFAULT_BASE_PACKAGE);
        sourceFolderText = addField(SOURCE_FOLDER_TITLE, SOURCE_FOLDER_PROPERTY_NAME, DEFAULT_SOURCE_FOLDER);
    }

    private void createDefaultComposite() {
        fieldsComposite = new Composite(pageRoot, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        fieldsComposite.setLayout(layout);

        GridData data = new GridData();
        data.verticalAlignment = GridData.FILL;
        data.horizontalAlignment = GridData.FILL;
        fieldsComposite.setLayoutData(data);
    }

    private Composite fieldsComposite;

    private Text basePackageText;

    private Text sourceFolderText;

    private Text addField(String title, QualifiedName propertyName, String defaultValue) {
        var label = new Label(fieldsComposite, SWT.NULL);
        label.setText(title);

        var field = new Text(fieldsComposite, SWT.SINGLE | SWT.BORDER);
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

    @Override
    protected void performDefaults() {
        super.performDefaults();
        basePackageText.setText(DEFAULT_BASE_PACKAGE);
        sourceFolderText.setText(DEFAULT_SOURCE_FOLDER);
    }

    @Override
    public boolean performOk() {
        try {
            getResource().setPersistentProperty(BASE_PACKAGE_PROPERTY_NAME, basePackageText.getText());
            getResource().setPersistentProperty(SOURCE_FOLDER_PROPERTY_NAME, sourceFolderText.getText());
        } catch (CoreException e) {
            return false;
        }
        return true;
    }

}