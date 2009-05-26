package ntut.csie.csdet.views;

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
 * ���ϥΪ̭ק�Rule��Dialog
 */
public class EditRuleDialog  extends Dialog{
	//�����ϥΪ̭ק諸Text
	private Text tempText;
	//�W�@�h�ҿ�ܭק諸Rule�W��
	private String ruleName;
	//�W�@�h�x�sRule���
	private Table ruleTable;
	//ĵ�i�ϥܩM��r
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
				//�Y���ק��ĵ�i�лy����
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
	protected void okPressed()
	{
		String temp = tempText.getText().trim();
		if (temp.length() > 0)
		{
			//�Y�S��"."��ܬ�Method�A�ۦ����ϥΪ̥["*."
			if (!temp.contains("."))
				temp = "*." + temp;
			
			boolean isWarning = false;
			//��Library��Name���S������
			for(int i=0;i<ruleTable.getItemCount();i++){
				//�Y���ƴN���ĵ�i�T��
				if(temp.equals(ruleTable.getItem(i).getText()))
					isWarning = true;
			}
			//�Y���ƴN�X�{ĵ�i�T���A�_�h�ק�Key
			if (isWarning){
				tempText.setText(temp);
				showWarningText("Rule�w�s�b");
			//�S�����ƴN�ק�
			}else{
				ruleTable.getItem(ruleTable.getSelectionIndex()).setText(temp);
				super.okPressed();
			}
		}
		else
			showWarningText("Rule���o����");
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
		return new Point(345, 130);
	}
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Edit Rules Dialog");
	}
	/**
	 * ���ĵ�i�лy�����
	 */
	protected void showWarningText(String warningInf)
	{
		warningLabel.setText(warningInf);
		picLabel.setVisible(true);
		warningLabel.setVisible(true);
	}

}
