package ntut.csie.csdet.refactor.ui;

import ntut.csie.csdet.refactor.RetryRefactoring;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryInputPage extends UserInputWizardPage {
	private static Logger logger = LoggerFactory.getLogger(RetryInputPage.class);
	
	//retry的變數名稱
	private Text retryText;
	//最大retry次數	
	private Text maxNum;
	//最大retry次數的變數名稱
	private Text maxAttempt;	
	//attempt的變數名稱
	private Text attempt;
	//填寫要throw的Exception type	
	private Text exNameField;
	//使用者所選擇的Exception Type
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
		attempLable.setText("&Attempt variable name");

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
		maxNum.setText("2");
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
		//假如內容被更改的話,將資訊存到RetryRefactoring
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
		// 預設拋出RuntimeException
		exNameField.setText("RuntimeException");
		
		//Browse Button 用來呼叫Selection Dialog
		final Button browseButton= new Button(composite, SWT.PUSH);
		browseButton.setText("&Browse...");
		GridData data= new GridData();
		data.horizontalAlignment= GridData.END;
		browseButton.setLayoutData(data);
		
		//假如內容被更改的話,將資訊存到RetryRefactoring
		exNameField.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}			
		});
		
		//被按下的時候去開啟Selection Dialog
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
	 * 假入Text中的東西有改變時要處理
	 */
	private void handleInputChange(){	
		RefactoringStatus status = new RefactoringStatus();
		RetryRefactoring refactoring = getRetryRefactoring();
		status.merge(refactoring.setAttemptVariable(attempt.getText()));
		status.merge(refactoring.setMaxAttemptVariable(maxAttempt.getText()));
		status.merge(refactoring.setMaxAttemptNum(maxNum.getText()));
		status.merge(refactoring.setRetryVariable(retryText.getText()));
		status.merge(refactoring.setExceptionName(exNameField.getText()));
		
		//假如要Throw的exception沒有import進來的話,可利用保留的type來import		
		refactoring.setExType(exType);
		//先確認有沒有error的情形
		setPageComplete(!status.hasError());
		int severity = status.getSeverity();
		String message = status.getMessageMatchingSeverity(severity);
		if(severity >= RefactoringStatus.INFO){
			//有Error的情形就把他設定進來
			setMessage(message,RefactoringStatus.WARNING);
		}else{
			setMessage("",NONE);
		}		
	}
	
	
	/**
	 * 填寫Exception Type的Text UI設置 
	 */
	private Text createNameField(Composite result) {
		Text field= new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return field;
	}
	
	/**
	 * 取得Refactoring的物件型態
	 * @return
	 */
	private RetryRefactoring getRetryRefactoring(){
		return (RetryRefactoring) getRefactoring();
	}
	
	
	/**
	 * 跳出Dialog讓使用者選擇要Throw的Class
	 * @return
	 */
	private IType selectExType(){

		try {
			//取得存在getRethrowExRefactoring中的project
			IJavaProject project = getRetryRefactoring().getProject();	
			//TODO 需要用到的部分
			//透過Eclipse 所提供的Dialog來找尋專案中所有的class or library......等等
			IType type = project.findType("java.lang.Exception");
			IJavaSearchScope scope = SearchEngine.createHierarchyScope(type);
			SelectionStatusDialog dialog = (SelectionStatusDialog) JavaUI.createTypeDialog(getShell(), getContainer(), scope, IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false);
			dialog.setTitle("Choose Exception type");
			dialog.setMessage("Choose the Exception type  to Rethrow:");
			if(dialog.open() == Window.OK){
				//按下ok後回傳使用者所選擇的
				return (IType)dialog.getFirstResult();
			}
		} catch (JavaModelException e) {			
			logger.error("[Refactor][Get Selection Dialog Error] EXCEPTION ",e);
		}
		return null;
	}
}
