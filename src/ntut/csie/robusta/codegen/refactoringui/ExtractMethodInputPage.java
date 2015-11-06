package ntut.csie.robusta.codegen.refactoringui;

import ntut.csie.robusta.codegen.refactoring.TEFBExtractMethodRefactoring;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * provide a user interface to allow user select what method will be extracted
 * @author Min, Shiau
 */
public class ExtractMethodInputPage extends UserInputWizardPage {

	private static Logger logger = LoggerFactory.getLogger(ExtractMethodInputPage.class);

	private Text newMethodText;
	//Public、Protected、private RadioButton
	private Button publicRadBtn;
	private Button protectedRadBtn;
	private Button privateRadBtn;
	//e.printStack、java.logger RadioButton
	private Button printRadBtn;
	private Button loggerRadBtn;
	private JavaSourceViewer fSignaturePreview;
	private Document fSignaturePreviewDocument;
	
	private IMethod existingMethod = null;
	
	public ExtractMethodInputPage(String name) {
		super(name);
		fSignaturePreviewDocument= new Document();
	}
	
	@Override
	public void createControl(Composite parent) {	
		//Define UI
		Composite composite= new Composite(parent, SWT.NONE);		
		composite.setLayout(new GridLayout(3, false));
		setControl(composite);

		/* New Method Text */
		createLabel(composite, "&Method Name:");
		newMethodText = createText(composite, SWT.BORDER, "extracted");
		new Label(composite, SWT.NONE);

		/* New Method Modifier */
		createLabel(composite, "&Access Modifiers:");
		Composite modifierGroup = createLayoutComposite(composite, 3);
		publicRadBtn = createRadioButton(modifierGroup, "public");
		protectedRadBtn = createRadioButton(modifierGroup, "protected");
		privateRadBtn = createRadioButton(modifierGroup, "private");

		/* New Method Handler */
		createLabel(composite, "&Catch Handler:");
		Composite handlerGroup = createLayoutComposite(composite, 2);
		printRadBtn = createRadioButton(handlerGroup, "e.printStackTrace();");
		loggerRadBtn = createRadioButton(handlerGroup, "java.util.logging.Logger");

		Composite previewComposite = createLayoutComposite(composite, 1);
		createSignaturePreview(previewComposite);
		//set control action
		addControlListener();

		//initialize
		privateRadBtn.setSelection(true);
		printRadBtn.setSelection(true);

		handleInputChange();
	}

	
	private void createSignaturePreview(Composite composite) {
		Label previewLabel= new Label(composite, SWT.NONE);
		previewLabel.setText(RefactoringMessages.ExtractMethodInputPage_signature_preview);

		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		fSignaturePreview = new JavaSourceViewer(composite, null, null, false, SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP /*| SWT.BORDER*/, store);
		fSignaturePreview.configure(new JavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
		fSignaturePreview.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		fSignaturePreview.getTextWidget().setBackground(composite.getBackground());
		fSignaturePreview.setDocument(fSignaturePreviewDocument);
		fSignaturePreview.setEditable(false);

		//Layouting problems with wrapped text: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=9866
		Control signaturePreviewControl= fSignaturePreview.getControl();
		PixelConverter pixelConverter= new PixelConverter(signaturePreviewControl);
		GridData gdata= new GridData(GridData.FILL_BOTH);
		gdata.widthHint= pixelConverter.convertWidthInCharsToPixels(50);
		gdata.heightHint= pixelConverter.convertHeightInCharsToPixels(2);
		signaturePreviewControl.setLayoutData(gdata);
	}
	
	private void updatePreview() {
		if (fSignaturePreview == null)
			return;

		int top= fSignaturePreview.getTextWidget().getTopPixel();
		String signature;
		try {
			signature= getEMRefactoring().getSignature();
		} catch (IllegalArgumentException e) {
			signature= ""; //$NON-NLS-1$
		}
		fSignaturePreviewDocument.set(signature);
		fSignaturePreview.getTextWidget().setTopPixel(top);
	}
	
	private void setGroupSetting(boolean isTrue) {
		newMethodText.setEnabled(!isTrue);
		printRadBtn.setEnabled(!isTrue);
		loggerRadBtn.setEnabled(!isTrue);
		publicRadBtn.setEnabled(!isTrue);
		protectedRadBtn.setEnabled(!isTrue);
		privateRadBtn.setEnabled(!isTrue);
	}
	
	private void handleInputChange(){
		RefactoringStatus status = new RefactoringStatus();
		TEFBExtractMethodRefactoring refactoring = getEMRefactoring();
		
		status.merge(refactoring.setNewMethodName(newMethodText.getText()));

		if (publicRadBtn.getSelection())
			status.merge(refactoring.setNewMethodModifierType(publicRadBtn.getText()));
		else if (protectedRadBtn.getSelection())
			status.merge(refactoring.setNewMethodModifierType(protectedRadBtn.getText()));  			
		else
			status.merge(refactoring.setNewMethodModifierType(privateRadBtn.getText()));
		
		if (printRadBtn.getSelection())
			status.merge(refactoring.setNewMethodLogType(printRadBtn.getText()));  			
		else
			status.merge(refactoring.setNewMethodLogType(loggerRadBtn.getText()));
		

		setPageComplete(!status.hasError());
		int severity = status.getSeverity();
		String message = status.getMessageMatchingSeverity(severity);

		if(severity >= RefactoringStatus.INFO) {
			fSignaturePreviewDocument.set("");
			setMessage(message,RefactoringStatus.ERROR);
		} else {
			updatePreview();
			setMessage("", NONE);
		}	
	}

	private TEFBExtractMethodRefactoring getEMRefactoring(){
		return (TEFBExtractMethodRefactoring) getRefactoring();
	}
	
	private void createLabel(Composite composite, String name) {
		Label handlerLabel = new Label(composite, SWT.NONE);
		handlerLabel.setLayoutData(new GridData());
		handlerLabel.setText(name);
	}

	private Button createRadioButton(Composite parent, String name) {
		Button protectedRadBtn= new Button(parent, SWT.RADIO);
		protectedRadBtn.setText(name);
		protectedRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleInputChange();
			}
		});
		return protectedRadBtn;
	}
	
	private Composite createLayoutComposite(Composite parent, int columnNumber) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		GridLayout layout= new GridLayout();
		layout.numColumns= columnNumber;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		return composite;
	}
	
	private Text createText(Composite composite, int style, String defaultText) {
		Text text = new Text(composite, style);
		text.setText(defaultText);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	private Button createButton(Composite composite, int style, String name) {
		Button button = new Button(composite, style);
		button.setLayoutData(new GridData());
		button.setText(name);
		return button;
	}
	
	private void addControlListener() {
		//if content has been modified, save content in CarelessCleanupRefactoring
		newMethodText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}
		});
	}
}