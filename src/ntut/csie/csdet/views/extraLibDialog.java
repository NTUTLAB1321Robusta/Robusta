package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.TreeMap;

import ntut.csie.rleht.common.ImageManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class extraLibDialog extends Dialog {

	Image infoImage;
	private Table displayTable;
	private Text tempText;
	private TreeMap<String, Boolean> libMap;
	
	/**
	 * Create the dialog
	 * @param parent
	 * @param libMap
	 */
	public extraLibDialog(Shell parent,TreeMap<String, Boolean> libMap) {
		super(parent);
		this.libMap = libMap;
	}

	/**
	 * Create contents of the dialog
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);
		
		tempText = new Text(container, SWT.BORDER);
		tempText.setFont(new Font(parent.getDisplay(), "Courier New",12,SWT.NORMAL));
		tempText.setBounds(10, 38, 243, 22);
		
		displayTable = new Table(container, SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);
		displayTable.setFont(new Font(this.getShell().getDisplay(),"Arial", 11,SWT.NONE));
		final GridData gd_testList = new GridData(SWT.FILL, SWT.FILL, true, true);
		displayTable.setBounds(10, 66, 243, 150);
		displayTable.setLayoutData(gd_testList);		
		
		//ĵ�i�ϥܩM��r
		final Label picLabel = new Label(container, SWT.NONE);
		picLabel.setBounds(10, 222, 16, 15);
		picLabel.setVisible(false);
		picLabel.setImage(ImageManager.getInstance().get("warning"));
		final Label warningLabel = new Label(container, SWT.NONE);
		warningLabel.setText("Library�w�s�b");
		warningLabel.setVisible(false);
		warningLabel.setBounds(32, 222, 85, 12);
		
		final Button addButton = new Button(container, SWT.NONE);
		addButton.setBounds(259, 36, 68, 22);
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (tempText.getText().length() != 0)
				{
					boolean isExist = false;
					//��Library��Name���S������
					for(int i=0;i<displayTable.getItemCount();i++)
					{
						if(tempText.getText().equals(displayTable.getItem(i).getText()))
							isExist = true;
					}
					//�S�����ƴN�[�J�s��Library
					if (!isExist)
					{
						TableItem item = new TableItem(displayTable,SWT.NONE);
						item.setText(tempText.getText());
						item.setChecked(true);
						tempText.setText("");
					}
					//�Y���ƴN���ĵ�i�T��
					else
					{
						picLabel.setVisible(true);
						warningLabel.setVisible(true);
					}
				}
			}
		});
		tempText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				//�YText�@���ܴN��ĵ�i�T������
				picLabel.setVisible(false);
				warningLabel.setVisible(false);
			}
		});

		final Button removeButton = new Button(container, SWT.NONE);
		removeButton.setBounds(259, 64, 68, 22);
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				//Table�����Ū��B�����Library �N���ܪ�Library���R��
				if (displayTable.getItemCount() != 0 && displayTable.getSelectionIndex()!=-1)
					displayTable.remove(displayTable.getSelectionIndex());
			}
		});
		removeButton.setText("Remove");

		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setBounds(10, 10, 210, 22);
		nameLabel.setText("�����~������:");

		//�N�өҦ�������Library�����ܦbList
		setInput();

		return container;
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void okPressed()
	{
		libMap.clear();
		
		//���N�C��item���X��
		TableItem[] temp = displayTable.getItems();
		//�htraverse���table��item��Text�M�O�_�Q�Ŀ��
		for(int i=0;i<temp.length;i++){
			libMap.put(temp[i].getText(),temp[i].getChecked());
		}
		super.okPressed();
	}

	@Override
	protected Point getInitialSize()
	{
		return new Point(350, 325);
	}
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Extra Library Dialogs");
	}

	
	/**
	 * ���o�]�w������Library���
	 */
	public TreeMap<String, Boolean> getLibMap()
	{
		return libMap;
	}

	/**
	 * �N�Ҧ������ר���ܦbtable��
	 */
	private void setInput(){
		
		Iterator<String> libIt = libMap.keySet().iterator();
		while(libIt.hasNext()){
			String temp = libIt.next();
			TableItem item = new TableItem(displayTable,SWT.NONE);
			item.setText(temp);
			item.setChecked(libMap.get(temp));
		}
	}
}