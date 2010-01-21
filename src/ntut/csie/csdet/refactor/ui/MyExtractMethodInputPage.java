package ntut.csie.csdet.refactor.ui;

import ntut.csie.csdet.refactor.CarelessCleanUpRefactor;

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
 * 提供一個介面給user,讓user可以選擇要Extract什麼樣的method
 * @author Min
 */
public class MyExtractMethodInputPage extends UserInputWizardPage {
	
	private static Logger logger = LoggerFactory.getLogger(MyExtractMethodInputPage.class);

	
	//Extract Method的變數名稱
	private Text methodName;
	//radio button of public,protected,private,print,logger
	private Button publicRadBtn;
	private Button protectedRadBtn;
	private Button privateRadBtn;
	private Button printRadBtn;
	private Button loggerRadBtn;	
	
	public MyExtractMethodInputPage(String name) {
		super(name);		
	}
	
	@Override
	public void createControl(Composite parent) {	
		//define UI
		Composite result= new Composite(parent, SWT.NONE);		
		setControl(result);

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		
		final Label nameLable = new Label(result, SWT.NONE);
		nameLable.setText("&Method Name:");
		
		methodName=new Text(result,SWT.BORDER);
		methodName.setText("close");
		methodName.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
		//假如內容被更改的話,將資訊存到CarelessCleanUpRefactoring
		methodName.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}			
		});

		Label accessLabel = new Label(result, SWT.NONE);
		accessLabel.setText("&Access Modifiers:");
		
		Composite group= new Composite(result, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 3; layout.marginWidth= 0;
		group.setLayout(layout);

		publicRadBtn= new Button(group, SWT.RADIO);
		publicRadBtn.setText("public");
		publicRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleInputChange();
			}
		});
		
		protectedRadBtn= new Button(group, SWT.RADIO);
		protectedRadBtn.setText("protected");
		protectedRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleInputChange();
			}
		});
		
		privateRadBtn= new Button(group, SWT.RADIO);
		privateRadBtn.setText("private");
		privateRadBtn.setSelection(true);
		privateRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleInputChange();
			}
		});
				
		Label handlerLabel = new Label(result, SWT.NONE);
		handlerLabel.setText("&Catch Handler:");
		
		Composite handler= new Composite(result, SWT.NONE);
		handler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 2; 	layout.marginWidth= 0;
		handler.setLayout(layout);
		
		printRadBtn= new Button(handler, SWT.RADIO);
		printRadBtn.setText("e.printStackTrace();");
		printRadBtn.setSelection(true);
		printRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleInputChange();
			}
		});
		
		loggerRadBtn= new Button(handler, SWT.RADIO);
		loggerRadBtn.setText("java.util.logging.Logger");
		loggerRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleInputChange();
			}
		});
		
		//使用者未更改Text內容，而直接按確定，會執行此行。否則Text內容會抓不到。
		handleInputChange();
	}
	private void handleInputChange(){
		RefactoringStatus status= new RefactoringStatus();
		CarelessCleanUpRefactor refactoring=getCCURefactoring();
		status.merge(refactoring.setMethodName(methodName.getText()));  
		
		if(publicRadBtn.getSelection()){
			status.merge(refactoring.setModifierType(publicRadBtn.getText()));
		}else if(protectedRadBtn.getSelection()){
			status.merge(refactoring.setModifierType(protectedRadBtn.getText()));  			
		}else{
			status.merge(refactoring.setModifierType(privateRadBtn.getText()));  			
		}
		
		if(printRadBtn.getSelection()){
			status.merge(refactoring.setLogType(printRadBtn.getText()));  			
		}else{
			status.merge(refactoring.setLogType(loggerRadBtn.getText()));
		}
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
	 * 取得Refactoring的物件型態
	 * @return
	 */
	private CarelessCleanUpRefactor getCCURefactoring(){
		return (CarelessCleanUpRefactor) getRefactoring();
	}
	
}
