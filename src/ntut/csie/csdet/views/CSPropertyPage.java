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

	//儲存每一個page
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
		
		//將每個分頁加入
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
	 * 砍xml file
	 */
	private void deleteXMLFile(){
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		File file = new File(path);
		if(file.exists())
			file.delete();
	}

	/**
	 * 按下ok的時候去抓取每個Tab Page的資料
	 * 然後將他儲存下來
	 */
	public boolean performOk() {
		//每次都先將xml檔砍掉,這樣code寫起來比較少
		deleteXMLFile();
		for(int i=0;i<tabPages.size();i++){
			tabPages.get(i).storeSettings();
		}		
		return true;
	}
}
