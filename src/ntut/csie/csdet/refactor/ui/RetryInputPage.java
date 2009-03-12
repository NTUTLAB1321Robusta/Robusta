package ntut.csie.csdet.refactor.ui;

import ntut.csie.csdet.refactor.RetryRefactoring;

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

public class RetryInputPage extends UserInputWizardPage {

	//retry���ܼƦW��
	private Text retryText;
	//�̤jretry����	
	private Text maxNum;
	//�̤jretry���ƪ��ܼƦW��
	private Text maxAttempt;	
	//attempt���ܼƦW��
	private Text attempt;
	//��g�nthrow��Exception type	
	private Text exNameField;
	//�ϥΪ̩ҿ�ܪ�Exception Type
	private IType exType;
	
	public RetryInputPage(String name) {
		super(name);
	}

	@Override
	public void createControl(Composite parent) {		
		Composite result= new Composite(parent, SWT.NONE);		
		setControl(result);

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		final Label attempLable = new Label(result, SWT.NONE);
		attempLable.setText("&Attempt varilbe name");

		attempt = new Text(result, SWT.BORDER);
		attempt.setText("attempt");
		attempt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		attempt.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}			
		});

		final Label maxAttempLabel = new Label(result, SWT.NONE);
		maxAttempLabel.setText("&Max Attempt variable Name");

		maxAttempt = new Text(result, SWT.BORDER);
		maxAttempt.setText("maxAttempt");
		maxAttempt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		maxAttempt.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}			
		});
		
		
		final Label maxNumLabel = new Label(result, SWT.NONE);
		maxNumLabel.setText("&Max Attempt Number");

		maxNum = new Text(result, SWT.BORDER);
		maxNum.setText("3");
		maxNum.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		maxNum.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}			
		});

		final Label retryLabel = new Label(result, SWT.NONE);
		retryLabel.setText("&Retry varialbe");

		retryText = new Text(result, SWT.BORDER);
		retryText.setText("retry");
		final GridData gd_retryText = new GridData(SWT.FILL, SWT.CENTER, true, false);
		retryText.setLayoutData(gd_retryText);
		//���p���e�Q��諸��,�N��T�s��RetryRefactoring
		retryText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}			
		});
		
		
		Label label= new Label(result, SWT.NONE);
		label.setText("&Retry Exception Type:");

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
		
		//Browse Button �ΨөI�sSelection Dialog
		final Button browseButton= new Button(composite, SWT.PUSH);
		browseButton.setText("&Browse...");
		GridData data= new GridData();
		data.horizontalAlignment= GridData.END;
		browseButton.setLayoutData(data);
		
		//���p���e�Q��諸��,�N��T�s��RetryRefactoring
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
	 * ���JText�����F�観���ܮɭn�B�z
	 */
	private void handleInputChange(){	
		RefactoringStatus status = new RefactoringStatus();
		RetryRefactoring refactoring = getRetryRefactoring();
		status.merge(refactoring.setAttemptVariable(attempt.getText()));
		status.merge(refactoring.setMaxAttemptVariable(maxAttempt.getText()));
		status.merge(refactoring.setMaxAttemptNum(maxNum.getText()));
		status.merge(refactoring.setRetryVariable(retryText.getText()));
		status.merge(refactoring.setExceptionName(exNameField.getText()));
		
		//���p�nThrow��exception�S��import�i�Ӫ���,�i�Q�ΫO�d��type��import		
		refactoring.setExType(exType);
		//TODO �O�_����k�P�w�ϥΪ̩ҿ�ܪ��Oexception type??
		//���T�{���S��error������
		setPageComplete(!status.hasError());
		int severity = status.getSeverity();
		String message = status.getMessageMatchingSeverity(severity);
		if(severity >= RefactoringStatus.INFO){
			//��Error�����δN��L�]�w�i��
			setMessage(message,RefactoringStatus.WARNING);
		}else{
			setMessage("",NONE);
		}		
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
	 * ���oRefactoring�����󫬺A
	 * @return
	 */
	private RetryRefactoring getRetryRefactoring(){
		return (RetryRefactoring) getRefactoring();
	}
	
	/**
	 * ���XDialog���ϥΪ̿�ܭnThrow��Class
	 * @return
	 */
	private IType selectExType(){
		//���o�s�bgetRethrowExRefactoring����project
		IJavaProject project = getRetryRefactoring().getProject();
		
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
			//logger.error("[Refactor][Get Selection Dialog Error] EXCEPTION ",e);
		}
		return null;
	}
}
