package ntut.csie.csdet.refactor.ui;

import ntut.csie.csdet.refactor.RethrowExRefactoring;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.jdt.core.search.SearchEngine;
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
 * provide a user interface where user can select what exception type will be rethrown
 * @author chewei
 */
public class RethrowExInputPage extends UserInputWizardPage {
	private static Logger logger = LoggerFactory.getLogger(RethrowExInputPage.class);
	
	//input the Exception type to throw
	private Text exNameField;
	//exception Type selected by user
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
		//RuntimeException is default exception type
		exNameField.setText("RuntimeException");
		
		//use browse button to pop up selection dialog
		final Button browseButton= new Button(composite, SWT.PUSH);
		browseButton.setText("&Browse...");
		GridData data= new GridData();
		data.horizontalAlignment= GridData.END;
		browseButton.setLayoutData(data);
		//if content has been modified, save information in RethrowExRefactoring
		exNameField.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleInputChange();				
			}			
		});
		
		//press browse button to pop up selection dialog
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
	 * get Refactoring object type
	 * @return
	 */
	private RethrowExRefactoring getRethrowExRefactoring(){
		return (RethrowExRefactoring) getRefactoring();
	}
	
	/**
	 * create text UI to edit exception type 
	 */
	private Text createNameField(Composite result) {
		Text field= new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return field;
	}
	
	/**
	 * if there is something change in text, we should handle it 
	 */
	private void handleInputChange(){	
		RefactoringStatus status = new RefactoringStatus();
		RethrowExRefactoring refactoring = getRethrowExRefactoring();
		status.merge(refactoring.setExceptionType(exNameField.getText()));
		//if exception type which will be thrown has not been imported, use retain exception type to replace it    
		refactoring.setUserSelectingExceptionType(exType);
		//check whether there is an error or not
		setPageComplete(!status.hasError());
		int severity = status.getSeverity();
		String message = status.getMessageMatchingSeverity(severity);
		if(severity >= RefactoringStatus.INFO){
			setMessage(message,RefactoringStatus.WARNING);
		}else{
			setMessage("",NONE);
		}		
	}
	
	/**
	 * pop up dialog that allows user to select a class to throw exception
	 * @return
	 */
	private IType selectExType(){
		//get project exist in getRethrowExRefactoring
		IJavaProject project = getRethrowExRefactoring().getProject();
		
		//through out Dialog find out all class or library......etc in project
		try {
			//set the boundary of exception type 
			IType runtimeExceptionType = project.findType("java.lang.RuntimeException");

			ITypeHierarchy hierarchy = runtimeExceptionType.newTypeHierarchy(project, new NullProgressMonitor());
			
			IType[] types = hierarchy.getAllSubtypes(runtimeExceptionType);
			IType[] allTypes = new IType[types.length+1];
			allTypes[0] = runtimeExceptionType;
			System.arraycopy(types, 0, allTypes, 1, types.length);
			
			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(allTypes , false);

			SelectionStatusDialog dialog = (SelectionStatusDialog) 
					JavaUI.createTypeDialog (getShell(), getContainer(), scope,
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false);
			
			dialog.setTitle("Choose Exception type");
			dialog.setMessage("Choose the Exception type to Rethrow:");
			if(dialog.open() == Window.OK){
				//press ok and return all user selection
				return (IType)dialog.getFirstResult();
			}
		} catch (JavaModelException e) {			
			logger.error("[Refactor][Get Selection Dialog Error] EXCEPTION ",e);
		}
		return null;
	}
}
