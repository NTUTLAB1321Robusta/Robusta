package ntut.csie.csdet.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class NestedTryBlockPage extends APropertyPage{
	private Button systemoutprinteButton;
	
	
	public NestedTryBlockPage(Composite composite, CSPropertyPage page) {
		super(composite, page);
		
		addFirstSection(composite);
	}

	private void addFirstSection(Composite composite){
	    systemoutprinteButton = new Button(composite, SWT.CHECK);
		systemoutprinteButton.setText("System.out.print();");
		systemoutprinteButton.setBounds(20, 28, 123, 16);
	}
	
	
	@Override
	public boolean storeSettings() {
		if(systemoutprinteButton.getSelection()){
			System.out.println("Page 2 select");
		}else{
			System.out.println("Page 2 no select");
		}
			
		
		return true;
	}

}
