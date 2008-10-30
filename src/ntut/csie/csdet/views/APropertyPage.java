package ntut.csie.csdet.views;

import org.eclipse.swt.widgets.Composite;

public abstract class APropertyPage {
	protected CSPropertyPage page;
	
	public APropertyPage(Composite composite,CSPropertyPage page){
		this.page = page;
	}
	
	abstract public boolean storeSettings();
	
	protected void setVaild(boolean valid){
		page.setValid(valid);
	}
}
