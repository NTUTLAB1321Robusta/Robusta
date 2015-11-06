package ntut.csie.csdet.views;

import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.rleht.common.ImageManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * dialog allow user modify user defined rule
 */
public class EditRuleDialog  extends Dialog{
	private Text tempText;
	private String ruleName;
	private Table ruleTable;
	private Label warningLabel;
	private Label picLabel;

	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	/**
	 * Create the dialog
	 * @param parentShell
	 */

	public EditRuleDialog(Shell parentShell,String ruleName,Table displayTable)
	{
		super(parentShell);
		this.ruleName = ruleName;
		this.ruleTable = displayTable;
	}
	
	/**
	 * Create contents of the dialog
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);

		tempText = new Text(container, SWT.BORDER);
		tempText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				//if user modify dialog content, disable warring
				picLabel.setVisible(false);
				warningLabel.setVisible(false);
			}
		});
		tempText.setBounds(10, 10, 317, 25);
		tempText.setFont(new Font(this.getShell().getDisplay(),"Arial", 11,SWT.NONE));

		warningLabel = new Label(container, SWT.NONE);
		warningLabel.setBounds(32, 41, 85, 12);
		warningLabel.setVisible(false);

		picLabel = new Label(container, SWT.NONE);
		picLabel.setBounds(10, 41, 16, 15);
		picLabel.setVisible(false);
		picLabel.setImage(ImageManager.getInstance().get("warning"));
		
		picLabel.setVisible(false);
		warningLabel.setVisible(false);
		
		tempText.setText(ruleName);
		return container;
	}

	/**
	 * Create contents of the button bar
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		String temp = tempText.getText().trim();
		if (temp.length() > 0)
		{
			//if input doesn't contain ".", it means input is a method and append "*." to input automatically
			if (!temp.contains("."))
				temp = "*." + temp;
			
			boolean isWarning = false;
			//check whether there are duplicate library name 
			for(int i=0;i<ruleTable.getItemCount();i++){
				//if duplicate library name is true then pup up warning
				if(temp.equals(ruleTable.getItem(i).getText()))
					isWarning = true;
			}
			if (isWarning){
				tempText.setText(temp);
				showWarningText(resource.getString("rule.exist"));
			}else{
				ruleTable.getItem(ruleTable.getSelectionIndex()).setText(temp);
				super.okPressed();
			}
		}
		else
			showWarningText(resource.getString("empty"));
	}
	
	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}

	/**
	 * Return the initial size of the dialog
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(345, 130);
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(resource.getString("edit.rules.dialog.title"));
	}
	
	/**
	 * show modify warning
	 */
	protected void showWarningText(String warningInf) {
		warningLabel.setText(warningInf);
		picLabel.setVisible(true);
		warningLabel.setVisible(true);
	}

}
