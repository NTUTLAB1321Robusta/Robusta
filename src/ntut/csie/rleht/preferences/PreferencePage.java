package ntut.csie.rleht.preferences;

import java.util.Locale;
import java.util.ResourceBundle;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import ntut.csie.rleht.RLEHTPlugin;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	public PreferencePage() {
		super(GRID);
		setPreferenceStore(RLEHTPlugin.getDefault().getPreferenceStore());
		setDescription(resource.getString("filter.description"));
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		addField(new StringFieldEditor(PreferenceConstants.P_RLCHPF_STRING, resource.getString("filter.text"), getFieldEditorParent()));
	}

	@Override
	public boolean performOk() {
		return super.performOk();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}