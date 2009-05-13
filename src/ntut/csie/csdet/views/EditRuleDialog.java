package ntut.csie.csdet.views;

import ntut.csie.rleht.common.ImageManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * 讓使用者修改Rule的Dialog
 */
public class EditRuleDialog  extends Dialog{
	//給予使用者修改的Text
	private Text tempText;
	//上一層所選擇修改的Rule名稱
	private String ruleName;
	//上一層儲存Rule資料
	private Table ruleTable;
	//警告圖示和文字
	private Label warningLabel;
	private Label picLabel;

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
				//若有修改把警告標語消掉
				picLabel.setVisible(false);
				warningLabel.setVisible(false);
			}
		});
		tempText.setBounds(10, 41, 317, 25);

		final Label ruleLabel = new Label(container, SWT.NONE);
		ruleLabel.setBounds(10, 10, 317, 25);

		warningLabel = new Label(container, SWT.NONE);
		warningLabel.setBounds(32, 87, 85, 12);
		warningLabel.setVisible(false);
		warningLabel.setText("Library已存在");

		picLabel = new Label(container, SWT.NONE);
		picLabel.setBounds(10, 87, 16, 15);
		picLabel.setVisible(false);
		picLabel.setImage(ImageManager.getInstance().get("warning"));
		
		picLabel.setVisible(false);
		warningLabel.setVisible(false);
		
		tempText.setText(ruleName);
		ruleLabel.setText("原Rule: " + ruleName);
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
	protected void okPressed()
	{
		String temp = tempText.getText().trim();
		if (temp.length() > 0)
		{
			boolean isWarning = false;
			//看Library的Name有沒有重複
			for(int i=0;i<ruleTable.getItemCount();i++){
				//若重複就顯示警告訊息
				if(tempText.getText().equals(ruleTable.getItem(i).getText()))
					isWarning = true;
			}
			//若重複就出現警告訊息，否則修改Key
			if (isWarning){
				picLabel.setVisible(true);
				warningLabel.setVisible(true);
			//沒有重複就修改
			}else{
				ruleTable.getItem(ruleTable.getSelectionIndex()).setText(tempText.getText());
				super.okPressed();
			}
		}
	}
	@Override
	protected void cancelPressed()
	{
		super.cancelPressed();
	}

	/**
	 * Return the initial size of the dialog
	 */
	@Override
	protected Point getInitialSize()
	{
		return new Point(345, 180);
	}
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Edit Rules Dialog");
	}

}
