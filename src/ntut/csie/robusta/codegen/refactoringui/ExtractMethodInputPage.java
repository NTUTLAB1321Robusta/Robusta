package ntut.csie.robusta.codegen.refactoringui;

import ntut.csie.robusta.codegen.refactoring.ExtractMethodRefactoring;

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
 * 提供一個介面給user, 讓user可以選擇要Extract什麼樣的Method
 * @author Min, Shiau
 */
public class ExtractMethodInputPage extends UserInputWizardPage {

	private static Logger logger = LoggerFactory.getLogger(ExtractMethodInputPage.class);

	//Extract Method的變數名稱
	private Text newMethodText;
	//Public、Protected、private三擇一的RadioButton
	private Button publicRadBtn;
	private Button protectedRadBtn;
	private Button privateRadBtn;
	//e.printStack、java.logger二擇一的RadioButton
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
		//設定Control動作
		addControlListener();

		//初始動作
		privateRadBtn.setSelection(true);
		printRadBtn.setSelection(true);

		//使用者未更改Text內容，而直接按確定，會執行此行。否則Text內容會抓不到。
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
	
	private void updatePreview(String text) {
		if (fSignaturePreview == null)
			return;

		if (text.length() == 0)
			text= "someMethodName";			 //$NON-NLS-1$

		int top= fSignaturePreview.getTextWidget().getTopPixel();
		String signature;
		try {
			signature= getEMRefactoring().getSignature(text);
		} catch (IllegalArgumentException e) {
			signature= ""; //$NON-NLS-1$
		}
		fSignaturePreviewDocument.set(signature);
		fSignaturePreview.getTextWidget().setTopPixel(top);
	}
	
	/**
	 * 是否使用既有的Method設定 (否則使用新的Method設定)
	 */
	private void setGroupSetting(boolean isTrue) {
		//若新增新的Method
		newMethodText.setEnabled(!isTrue);
		printRadBtn.setEnabled(!isTrue);
		loggerRadBtn.setEnabled(!isTrue);
		publicRadBtn.setEnabled(!isTrue);
		protectedRadBtn.setEnabled(!isTrue);
		privateRadBtn.setEnabled(!isTrue);
	}
	
	/**
	 * 假入Text中的東西有改變時要處理
	 */
	private void handleInputChange(){
		RefactoringStatus status = new RefactoringStatus();
		ExtractMethodRefactoring refactoring = getEMRefactoring();
		
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
		

		//先確認有沒有error的情形
		setPageComplete(!status.hasError());
		int severity = status.getSeverity();
		String message = status.getMessageMatchingSeverity(severity);

		if(severity >= RefactoringStatus.INFO) {
			//有Error的情形就把他設定進來
			fSignaturePreviewDocument.set("");
			setMessage(message,RefactoringStatus.ERROR);
		} else {
			updatePreview(newMethodText.getText());
			setMessage("", NONE);
		}	
	}

	/**
	 * 取得Refactoring的物件型態
	 * @return
	 */
	private ExtractMethodRefactoring getEMRefactoring(){
		return (ExtractMethodRefactoring) getRefactoring();
	}
	
	/**
	 * 建立Label
	 * @param composite
	 * @param name
	 */
	private void createLabel(Composite composite, String name) {
		Label handlerLabel = new Label(composite, SWT.NONE);
		handlerLabel.setLayoutData(new GridData());
		handlerLabel.setText(name);
	}

	/**
	 * 建立Radio Button
	 * @param parent
	 * @param name
	 * @return
	 */
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
	
	/**
	 * 建立有Layout的Composite
	 * @param parent
	 * @param columnNumber Column個數
	 * @return
	 */
	private Composite createLayoutComposite(Composite parent, int columnNumber) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		GridLayout layout= new GridLayout();
		layout.numColumns= columnNumber;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		return composite;
	}
	
	/**
	 * 建立Text
	 * @param composite
	 * @param style
	 * @param defaultText
	 * @return 
	 */
	private Text createText(Composite composite, int style, String defaultText) {
		Text text = new Text(composite, style);
		text.setText(defaultText);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	/**
	 * 建立Button
	 * @param composite
	 * @return 
	 */
	private Button createButton(Composite composite, int style, String name) {
		Button button = new Button(composite, style);
		button.setLayoutData(new GridData());
		button.setText(name);
		return button;
	}
	
	/**
	 * 將Control元件添加動作
	 */
	private void addControlListener() {
		//假如內容被更改的話,將資訊存到CarelessCleanupRefactoring
		newMethodText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}
		});
	}
}