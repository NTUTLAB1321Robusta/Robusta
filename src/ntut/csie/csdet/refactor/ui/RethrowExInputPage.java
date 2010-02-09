package ntut.csie.csdet.refactor.ui;

import ntut.csie.csdet.refactor.RethrowExRefactoring;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
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

/**
 * 提供一個介面給user,讓user可以選擇要Rethrow什麼樣的Exception type
 * @author chewei
 */
public class RethrowExInputPage extends UserInputWizardPage {
	private static Logger logger = LoggerFactory.getLogger(RethrowExInputPage.class);
	
	
	//填寫要throw的Excpetion type
	private Text exNameField;
	//使用者所選擇的Exception Type
	private IType exType;
	
	public RethrowExInputPage(String name) {
		super(name);		
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);		
		setControl(result);

		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		result.setLayout(layout);

		Label label= new Label(result, SWT.NONE);
		label.setText("&Rethrow Exception Type:");

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
		//假如內容被更改的話,將資訊存到RethrowExRefactoring
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
		
		//使用者未更改Text內容，而直接按確定，會執行此行。否則Text內容會抓不到。
		handleInputChange();		
	}
	
	/**
	 * 取得Refactoring的物件型態
	 * @return
	 */
	private RethrowExRefactoring getRethrowExRefactoring(){
		return (RethrowExRefactoring) getRefactoring();
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
	 * 假入Text中的東西有改變時要處理
	 */
	private void handleInputChange(){	
		RefactoringStatus status = new RefactoringStatus();
		RethrowExRefactoring refactoring = getRethrowExRefactoring();
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
	 * 跳出Dialog讓使用者選擇要Throw的Class
	 * @return
	 */
	private IType selectExType(){
		//取得存在getRethrowExRefactoring中的project
		IJavaProject project = getRethrowExRefactoring().getProject();
		
//		IJavaElement[] elements = new IJavaElement[] {project};
//		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements);
		//透過Eclipse 所提供的Dialog來找尋專案中所有的class or library......等等
		try {	
			//尋找所有可以拋出的例外類型
			IType type = project.findType("java.lang.Exception");
			
			//會找到全WorkSpace的java.lang.Exception
//			IJavaSearchScope scope = SearchEngine.createHierarchyScope(type);

			ITypeHierarchy hierarchy = type.newTypeHierarchy(project, new NullProgressMonitor());
			IType[] types = hierarchy.getAllTypes();

			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(types , false);

			SelectionStatusDialog dialog = (SelectionStatusDialog) 
					JavaUI.createTypeDialog (getShell(), getContainer(), scope,
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false);
			
			dialog.setTitle("Choose Exception type");
			dialog.setMessage("Choose the Exception type to Rethrow:");
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
