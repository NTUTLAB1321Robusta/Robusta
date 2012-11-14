package ntut.csie.csdet.views;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class APropertyPage {
	protected CSPropertyPage page;
	
	public APropertyPage(Composite composite,CSPropertyPage page){
		this.page = page;
	}
	
	abstract public boolean storeSettings();
	
	protected void setVaild(boolean valid){
		page.setValid(valid);
	}

	/**
	 * 取得Control的右下角座標
	 * @param control
	 * @return			右下角座標
	 */
	protected Point getLowerRightCoordinate(Control control) {
		if (control == null) return new Point(0,0);

		return new Point(control.getBounds().x + control.getBounds().width ,
						 control.getBounds().y + control.getBounds().height);
	}
}
