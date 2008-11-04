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
		final Composite dummyHandlerPage = new Composite(tabFolder, SWT.NONE);
		APropertyPage page = new DummyHandlerPage(dummyHandlerPage,this);
		dummyHandlerTabItem.setControl(dummyHandlerPage);
		tabPages.add(page);
		
		
//		final TabItem nestedPageTabItem = new TabItem(tabFolder,SWT.NONE);
//		nestedPageTabItem.setText("Nested Try Block");
//		final Composite nestedPage = new Composite(tabFolder, SWT.NONE);
//		page = new NestedTryBlockPage(nestedPage,this);
//		nestedPageTabItem.setControl(nestedPage);
//		tabPages.add(page);
	}
	
	/**
	 * ��xml file
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
		//�C�������Nxml�ɬ屼,�o��code�g�_�Ӥ����
		deleteXMLFile();
		for(int i=0;i<tabPages.size();i++){
			tabPages.get(i).storeSettings();
		}		
		return true;
	}
}
