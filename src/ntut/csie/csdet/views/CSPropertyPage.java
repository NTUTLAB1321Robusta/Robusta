package ntut.csie.csdet.views;

import java.io.File;
import java.util.ArrayList;

import ntut.csie.csdet.preference.JDomUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class CSPropertyPage extends org.eclipse.ui.dialogs.PropertyPage{

	//�x�s�C�@��page
	private ArrayList<APropertyPage> tabPages;

	public CSPropertyPage(){
		super();
		tabPages = new ArrayList<APropertyPage>();
		noDefaultAndApplyButton();
	}
	
	
	@Override
	protected Control createContents(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		
		final TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//�N�C�Ӥ����[�J
		addPage(tabFolder);
		
		return composite;
	}
	
	private void addPage(TabFolder tabFolder){
		//add Dummy Handler Page
		final TabItem dummyHandlerTabItem = new TabItem(tabFolder, SWT.NONE);
		dummyHandlerTabItem.setText("Dummy Handler");
		final Composite dummyHandlerComposite = new Composite(tabFolder, SWT.NONE);
		APropertyPage dummyHandlerPage = new DummyHandlerPage(dummyHandlerComposite,this);
		dummyHandlerTabItem.setControl(dummyHandlerComposite);
		tabPages.add(dummyHandlerPage);
		
		//add OverLogging Page
		final TabItem overLoggingTabItem = new TabItem(tabFolder, SWT.NONE);
		overLoggingTabItem.setText("OverLogging");
		final Composite overLoggingComposite = new Composite(tabFolder, SWT.NONE);
		APropertyPage overLoggingPage = new OverLoggingPage(overLoggingComposite,this);
		overLoggingTabItem.setControl(overLoggingComposite);
		tabPages.add(overLoggingPage);
		
		//add CarelessCleanUp Page
		final TabItem carelessCleanUpTabItem=new TabItem(tabFolder,SWT.NONE);
		carelessCleanUpTabItem.setText("Careless CleanUp");
		final Composite carelessCleanUpPage=new Composite(tabFolder,SWT.NONE);
		APropertyPage cleanUpPage = new CarelessCleanUpPage(carelessCleanUpPage,this);
		carelessCleanUpTabItem.setControl(carelessCleanUpPage);
		tabPages.add(cleanUpPage);
	}
	
	/**
	 * �屼XML file (�ثe���ϥ�)
	 */
	private void deleteXMLFile(){
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		File file = new File(path);
		if(file.exists())
			file.delete();
	}

	/**
	 * ���Uok���ɭԥh����C��Tab Page�����
	 * �M��N�L�x�s�U��
	 */
	public boolean performOk() {
//		//�C�������Nxml�ɬ屼,�o��code�g�_�Ӥ����
//		deleteXMLFile();
		for(int i=0;i<tabPages.size();i++){
			tabPages.get(i).storeSettings();
		}		
		return true;
	}
}
