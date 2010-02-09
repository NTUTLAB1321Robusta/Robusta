package ntut.csie.csdet.refactor.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ntut.csie.csdet.refactor.CarelessCleanUpRefactor;

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
 * ���Ѥ@�Ӥ�����user, ��user�i�H��ܭnExtract����˪�Method
 * @author Min, Shiau
 */
public class ExtractCleanUpMethodInputPage extends UserInputWizardPage {

	private static Logger logger = LoggerFactory.getLogger(ExtractCleanUpMethodInputPage.class);

	//Extract Method���ܼƦW��
	private Text newMethodText;
	private Text existMethodText;
	//Public�BProtected�Bprivate�T�ܤ@��RadioButton
	private Button publicRadBtn;
	private Button protectedRadBtn;
	private Button privateRadBtn;
	//e.printStack�Bjava.logger�G�ܤ@��RadioButton
	private Button printRadBtn;
	private Button loggerRadBtn;
	//Exist Method Check Button
	private Button existMethodBtn;
	//Exist Method Browse Button
	private Button browseBtn;
	
	private IMethod existingMethod = null;
	
	public ExtractCleanUpMethodInputPage(String name) {
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

		//�]�wControl�ʧ@
		addControlListener();

		//��l�ʧ@
		privateRadBtn.setSelection(true);
		printRadBtn.setSelection(true);
		browseBtn.setEnabled(false);
		existMethodText.setEnabled(false);

		//�ϥΪ̥����Text���e�A�Ӫ������T�w�A�|���榹��C�_�hText���e�|�줣��C
		handleInputChange();
	}

	/**
	 * �O�_�ϥάJ����Method�]�w (�_�h�ϥηs��Method�]�w)
	 */
	private void setGroupSetting(boolean isTrue) {
		//�Y�ϥάJ����Method
		browseBtn.setEnabled(isTrue);
		existMethodText.setEnabled(isTrue);

		//�Y�s�W�s��Method
		newMethodText.setEnabled(!isTrue);
		printRadBtn.setEnabled(!isTrue);
		loggerRadBtn.setEnabled(!isTrue);
		publicRadBtn.setEnabled(!isTrue);
		protectedRadBtn.setEnabled(!isTrue);
		privateRadBtn.setEnabled(!isTrue);
	}
	
	/**
	 * ���JText�����F�観���ܮɭn�B�z
	 */
	private void handleInputChange(){
		RefactoringStatus status = new RefactoringStatus();
		CarelessCleanUpRefactor refactoring = getCCURefactoring();

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

		//���T�{���S��error������
		setPageComplete(!status.hasError());
		int severity = status.getSeverity();
		String message = status.getMessageMatchingSeverity(severity);

		if(severity >= RefactoringStatus.INFO) {
			//��Error�����δN��L�]�w�i��
			setMessage(message,RefactoringStatus.WARNING);
		} else {
			setMessage("", NONE);
		}	
	}

	/**
	 * ���oRefactoring�����󫬺A
	 * @return
	 */
	private CarelessCleanUpRefactor getCCURefactoring(){
		return (CarelessCleanUpRefactor) getRefactoring();
	}

	/**
	 * ���XDialog���ϥΪ̿�ܤw�s�b��Method
	 * @return
	 */
	private IMethod selectExistingMethod(){

		//�z�LEclipse�Ҵ��Ѫ�Dialog�ӧ�M�M�פ��Ҧ���Method
		try {

			//���o�s�bgetRethrowExRefactoring����project
			IJavaProject project = getCCURefactoring().getProject();

			//�M��Ҧ��s�b��Method
			List<IJavaElement> methodList = searchProjectMethods(project);

			//�M��M�פ��w�s�b��Method��JMethodSelectionDialog����
			ExistingMethodSelectionDialog dialog = new ExistingMethodSelectionDialog(getShell(), getCCURefactoring().getCurrentMethodNode());
			dialog.setElements(methodList.toArray(new IJavaElement[methodList.size()]));
			dialog.setTitle("Choose Existing Method");
			dialog.setMessage("Choose Existing Method to Close Resource:");

			if(dialog.open() == Window.OK){
				//���Uok��^�ǨϥΪ̩ҿ�ܪ�
				return (IMethod)dialog.getFirstResult();
			}
		} catch (JavaModelException e) {			
			logger.error("[Refactor][Get Selection Dialog Error] EXCEPTION ",e);
		}
		return null;
	}

	/**
	 * �M��Project�����Ҧ�Method
	 * @param project
	 * @return
	 * @throws JavaModelException
	 */
	private List<IJavaElement> searchProjectMethods(IJavaProject project)
			throws JavaModelException {

		//���oProject����CompilationUnit
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

		//���oCompilationUnit����Method
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
	 * �إ�Label
	 * @param composite
	 * @param name
	 */
	private void createLabel(Composite composite, String name) {
		Label handlerLabel = new Label(composite, SWT.NONE);
		handlerLabel.setLayoutData(new GridData());
		handlerLabel.setText(name);
	}

	/**
	 * �إ�Radio Button
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
	 * �إߦ�Layout��Composite
	 * @param parent
	 * @param columnNumber Column�Ӽ�
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
	 * �إ�Text
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
	 * �إ�Button
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
	 * �NControl����K�[�ʧ@
	 */
	private void addControlListener() {
		//���p���e�Q��諸��,�N��T�s��CarelessCleanUpRefactoring
		newMethodText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}
		});
		//�ϥΪ̫��UExistMethod�A�N�����s�����/�����
		existMethodBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (existMethodBtn.getSelection())
					setGroupSetting(true);
				else
					setGroupSetting(false);

				handleInputChange();
			}
		});
		//�ϥΪ̫��UBrowseButton�A���XMethodSelectionDialog
		browseBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//���XDialog
				existingMethod = selectExistingMethod();
				if (existingMethod == null)
					return;

				//��ܨϥΪ̿��Method��T
				IType className = (IType) existingMethod.getParent();
				String path = className.getFullyQualifiedName() + "." + existingMethod.getElementName();
				existMethodText.setText(path);

				handleInputChange();
			}
		});
	}
}
