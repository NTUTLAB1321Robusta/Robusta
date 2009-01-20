package ntut.csie.csdet.refactor.ui;

import ntut.csie.csdet.refactor.RethrowExRefactoring;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
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
import org.eclipse.ui.dialogs.SelectionStatusDialog;

/**
 * ���Ѥ@�Ӥ�����user,��user�i�H��ܭnRethrow����˪�Excpetion type
 * @author chewei
 */

public class RethrowExInputPage extends UserInputWizardPage {

	//��g�nthrow��Excpetion type
	private Text exNameField;
	//�ϥΪ̩ҿ�ܪ�Exception Type
	private IType exType;
	
	public RethrowExInputPage(String name) {
		super(name);		
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		
		setControl(result);

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		Label label= new Label(result, SWT.NONE);
		label.setText("&Declaring class:");

		Composite composite= new Composite(result, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		exNameField = createNameField(composite);		
		exNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// �w�]�ߥXRuntimeException
		exNameField.setText("RuntimeException");
		
		//Browse Button
		final Button browseButton= new Button(composite, SWT.PUSH);
		browseButton.setText("&Browse...");
		GridData data= new GridData();
		data.horizontalAlignment= GridData.END;
		browseButton.setLayoutData(data);
		//���p���e�Q��諸��,�N��T�s��RethrowExRefactoring
		exNameField.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}			
		});
		
		//�Q���U���ɭԥh�}��Selection Dialog
		browseButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent event) {
				exType= selectExType();	
				
				//System.out.println("�iDeclaringClass Type�j====>"+exType.getFullyQualifiedName());
				if (exType == null)
					return;
				exNameField.setText(exType.getElementName());				
			}
		});
		exNameField.setFocus();
		exNameField.selectAll();
		
		handleInputChange();
		
	}
	
	/**
	 * ���oRefactoring�����󫬺A
	 * @return
	 */
	private RethrowExRefactoring getRethrowExRefactoring(){
		return (RethrowExRefactoring) getRefactoring();
	}
	
	/**
	 * ��gException Type��Text UI�]�m 
	 */
	private Text createNameField(Composite result) {
		Text field= new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return field;
	}
	
	/**
	 * ���JText�����F�観���ܮɭn�B�z
	 */
	private void handleInputChange(){	
		RethrowExRefactoring refactoring = getRethrowExRefactoring();
		refactoring.setExceptionName(exNameField.getText());
		//���p�nThrow��exception�S��import�i�Ӫ���,�i�Q�ΫO�d��type��import
		refactoring.setExType(exType);
		//TODO ���p�Ů�S��Ϊ�user�Ҷ�g���F�観���D,��������
		setMessage("Hello",RefactoringStatus.WARNING);
	}
	
	/**
	 * ���XDialog���ϥΪ̿�ܭnThrow��Class
	 * @return
	 */
	private IType selectExType(){
		//���o�s�bgetRethrowExRefactoring����project
		IJavaProject project = getRethrowExRefactoring().getProject();
		
		IJavaElement[] elements = new IJavaElement[] {project};
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements);
		//�z�LEclipse �Ҵ��Ѫ�Dialog�ӧ�M�M�פ��Ҧ���class or library......����
		try {
			SelectionStatusDialog dialog = (SelectionStatusDialog) JavaUI.createTypeDialog(getShell(), getContainer(), scope, IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false);
			dialog.setTitle("Choose Exception type");
			dialog.setMessage("Choose the Exception type  to Rethrow:");
			if(dialog.open() == Window.OK){
				//���Uok��^�ǨϥΪ̩ҿ�ܪ�
				return (IType)dialog.getFirstResult();
			}
		} catch (JavaModelException e) {			
			e.printStackTrace();
		}
		return null;
	}
}
