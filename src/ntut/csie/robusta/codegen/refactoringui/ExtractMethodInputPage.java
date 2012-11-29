package ntut.csie.robusta.codegen.refactoringui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ntut.csie.csdet.refactor.ui.ExistingMethodSelectionDialog;
import ntut.csie.robusta.codegen.refactoring.ExtractMethodRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.window.Window;
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
	private Text existMethodText;
	//Public、Protected、private三擇一的RadioButton
	private Button publicRadBtn;
	private Button protectedRadBtn;
	private Button privateRadBtn;
	//e.printStack、java.logger二擇一的RadioButton
	private Button printRadBtn;
	private Button loggerRadBtn;
	//Exist Method Check Button
	private Button existMethodBtn;
	//Exist Method Browse Button
	private Button browseBtn;
	
	private IMethod existingMethod = null;
	
	public ExtractMethodInputPage(String name) {
		super(name);		
	}
	
	@Override
	public void createControl(Composite parent) {	
		//Define UI
		Composite composite= new Composite(parent, SWT.NONE);		
		composite.setLayout(new GridLayout(3, false));
		setControl(composite);

		/* New Method Text */
		createLabel(composite, "&Method Name:");
		newMethodText = createText(composite, SWT.BORDER, "close");
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

		/* ExistingMethod */
		existMethodBtn = createButton(composite, SWT.CHECK, "Existing Method:");
		existMethodText = createText(composite, SWT.BORDER | SWT.READ_ONLY, "");
		browseBtn = createButton(composite, SWT.NONE, "Browse...");

		//設定Control動作
		addControlListener();

		//初始動作
		privateRadBtn.setSelection(true);
		printRadBtn.setSelection(true);
		browseBtn.setEnabled(false);
		existMethodText.setEnabled(false);

		//使用者未更改Text內容，而直接按確定，會執行此行。否則Text內容會抓不到。
		handleInputChange();
	}

	/**
	 * 是否使用既有的Method設定 (否則使用新的Method設定)
	 */
	private void setGroupSetting(boolean isTrue) {
		//若使用既有的Method
		browseBtn.setEnabled(isTrue);
		existMethodText.setEnabled(isTrue);

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

		if (existMethodBtn.getSelection()) {
			status.merge(refactoring.setIsRefactoringMethodExist(true));

			status.merge(refactoring.setExistingMethod(existingMethod));

		} else {
			status.merge(refactoring.setIsRefactoringMethodExist(false));

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
		}

		//先確認有沒有error的情形
		setPageComplete(!status.hasError());
		int severity = status.getSeverity();
		String message = status.getMessageMatchingSeverity(severity);

		if(severity >= RefactoringStatus.INFO) {
			//有Error的情形就把他設定進來
			setMessage(message,RefactoringStatus.WARNING);
		} else {
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
	 * 跳出Dialog讓使用者選擇已存在的Method
	 * @return
	 */
	private IMethod selectExistingMethod(){

		//透過Eclipse所提供的Dialog來找尋專案中所有的Method
		try {

			//取得存在getRethrowExRefactoring中的project
			IJavaProject project = getEMRefactoring().getProject();

			//尋找所有存在的Method
			List<IJavaElement> methodList = searchProjectMethods(project);

			//尋找專案中已存在的Method放入MethodSelectionDialog之中
			ExistingMethodSelectionDialog dialog = new ExistingMethodSelectionDialog(getShell(), getEMRefactoring().getCurrentMethodNode());
			dialog.setElements(methodList.toArray(new IJavaElement[methodList.size()]));
			dialog.setTitle("Choose Existing Method");
			dialog.setMessage("Choose Existing Method to Close Resource:");

			if(dialog.open() == Window.OK){
				//按下ok後回傳使用者所選擇的
				return (IMethod)dialog.getFirstResult();
			}
		} catch (JavaModelException e) {			
			logger.error("[Refactor][Get Selection Dialog Error] EXCEPTION ",e);
		}
		return null;
	}

	/**
	 * 尋找Project中的所有Method
	 * @param project
	 * @return
	 * @throws JavaModelException
	 */
	private List<IJavaElement> searchProjectMethods(IJavaProject project)
			throws JavaModelException {

		//取得Project中的CompilationUnit
		List<ICompilationUnit> compilationUnitList = new ArrayList<ICompilationUnit>();
		for (IJavaElement element : project.getChildren()) {
			if (!element.getElementName().endsWith(".jar")) {
				IPackageFragmentRoot root = (IPackageFragmentRoot) element;
				for (IJavaElement element2 : root.getChildren()) {
					IPackageFragment pk = (IPackageFragment) element2;
					compilationUnitList.addAll(Arrays.asList(pk.getCompilationUnits()));
				}
			}
		}

		//取得CompilationUnit中的Method
		List<IJavaElement> methodList = new ArrayList<IJavaElement>();
		for (ICompilationUnit icu : compilationUnitList) {
			for (IJavaElement element : icu.getChildren()) {
				if (element.getElementType() == IJavaElement.TYPE) {
					IType type = (IType) element;
					methodList.addAll(Arrays.asList(type.getMethods()));
				}
			}
		}

		return methodList;
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
		//假如內容被更改的話,將資訊存到CarelessCleanUpRefactoring
		newMethodText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}
		});
		//使用者按下ExistMethod，將部分群組顯示/不顯示
		existMethodBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (existMethodBtn.getSelection())
					setGroupSetting(true);
				else
					setGroupSetting(false);

				handleInputChange();
			}
		});
		//使用者按下BrowseButton，跳出MethodSelectionDialog
		browseBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//跳出Dialog
				existingMethod = selectExistingMethod();
				if (existingMethod == null)
					return;

				//顯示使用者選擇Method資訊
				IType className = (IType) existingMethod.getParent();
				String path = className.getFullyQualifiedName() + "." + existingMethod.getElementName();
				existMethodText.setText(path);

				handleInputChange();
			}
		});
	}
}
