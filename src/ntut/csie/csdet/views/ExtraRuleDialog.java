package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.TreeMap;
import ntut.csie.rleht.common.ImageManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * 讓user設定要偵測試的Dummy Handler
 */
public class ExtraRuleDialog extends Dialog{
	//顯示Rule資訊的Table
	private Table displayTable;
	//Dialog上方的Text
	private Text tempText;
	//修改Item的按鈕
	private Button editBtn;
	//存放Library或Statement的Rule資料
	private TreeMap<String, Boolean> ruleMap;
	
	/**
	 * Create the dialog
	 * @param parent
	 * @param libMap
	 */
	public ExtraRuleDialog(Shell parent,TreeMap<String, Boolean> libMap){
		super(parent);
		this.ruleMap = libMap;
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
		
		//顯示Table
		displayTable = new Table(container, SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);
		displayTable.setFont(new Font(this.getShell().getDisplay(),"Arial", 11,SWT.NONE));
		final GridData gd_testList = new GridData(SWT.FILL, SWT.FILL, true, true);
		displayTable.setBounds(10, 66, 243, 150);
		displayTable.setLayoutData(gd_testList);

		//如果選擇的displayTable的Item把其名稱顯示在Text上
		displayTable.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event e){
				int selectionIndex = displayTable.getSelectionIndex();
				//防止一開Dialog就先勾選checkbox,出現index=-1的情況
				if(selectionIndex >= 0){
					//把選擇的Item其Library名稱顯示在Text上
					editBtn.setEnabled(true);
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});

		//警告圖示和文字
		final Label picLabel = new Label(container, SWT.NONE);
		picLabel.setBounds(10, 222, 16, 15);
		picLabel.setVisible(false);
		picLabel.setImage(ImageManager.getInstance().get("warning"));
		final Label warningLabel = new Label(container, SWT.NONE);
		warningLabel.setText("Library已存在");
		warningLabel.setVisible(false);
		warningLabel.setBounds(32, 222, 85, 12);
		
		//新增按鈕
		Button addBtn = new Button(container, SWT.NONE);
		addBtn.setBounds(259, 36, 68, 22);
		addBtn.setText("Add");
		addBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				boolean isWarning = addRule();
				//若重複就顯示警告訊息
				if (isWarning){
					picLabel.setVisible(true);
					warningLabel.setVisible(true);
				}
			}
		});

		//使用都輸入Text
		tempText = new Text(container, SWT.BORDER);
		tempText.setFont(new Font(parent.getDisplay(), "Courier New",12,SWT.NORMAL));
		tempText.setBounds(10, 38, 243, 22);
		tempText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				//若Text一改變就把警告訊息消掉
				picLabel.setVisible(false);
				warningLabel.setVisible(false);
			}
		});

		//刪除按鈕
		final Button removeButton = new Button(container, SWT.NONE);
		removeButton.setBounds(259, 64, 68, 22);
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				//Table不為空的且有選到Library 就把選擇的Library給刪掉
				if (displayTable.getItemCount() != 0 && displayTable.getSelectionIndex()!=-1)
					displayTable.remove(displayTable.getSelectionIndex());
			}
		});
		removeButton.setText("Remove");

		//全選按鈕
		final Button selectBtn = new Button(container, SWT.NONE);
		selectBtn.setText("Select All");
		selectBtn.setBounds(259, 120, 68, 22);
		selectBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				//選擇全部的Item
				for (int i=0;i<displayTable.getItemCount();i++)
				{
					TableItem item = displayTable.getItem(i);
					item.setChecked(true);
				}
			}
		});
		
		//全取消按鈕
		final Button clearBtn = new Button(container, SWT.NONE);
		clearBtn.setBounds(259, 148, 68, 22);
		clearBtn.setText("Deselect All");
		clearBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				//取消全部的Item
				for (int i=0;i<displayTable.getItemCount();i++)
				{
					TableItem item = displayTable.getItem(i);
					item.setChecked(false);
				}
			}
		});
		
		Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setBounds(10, 10, 97, 22);
		//依是否為library或statement來改變不同的範例
		nameLabel.setText("偵測外部條件: ");

		//修改的按鈕
		editBtn = new Button(container, SWT.NONE);
		editBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int selectionIndex = displayTable.getSelectionIndex();
				if (selectionIndex >= 0)
				{
					boolean isWarning = false;
					//看Library的Name有沒有重複
					for(int i=0;i<displayTable.getItemCount();i++){
						//若重複就顯示警告訊息
						if(tempText.getText().equals(displayTable.getItem(i).getText()))
							isWarning = true;
					}
					//若重複就出現警告訊息，否則修改Key
					if (isWarning){
						picLabel.setVisible(true);
						warningLabel.setVisible(true);
					}
					else
						displayTable.getItem(selectionIndex).setText(tempText.getText());
				}
			}
		});
		editBtn.setBounds(259, 92, 68, 22);
		editBtn.setText("Edit");
		editBtn.setEnabled(false);

		//說明視窗
		final Button explainBtn = new Button(container, SWT.NONE);
		explainBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//跳出說明的Dialog
				MessageDialog.openInformation(
						new Shell(),
						"說明",
						"偵測方法: Library名稱 + . + Statement名稱 (eg. 'java.lang.String.toString')\n\n" +
						"偵測Library: Library名稱 + .* (eg. 'java.lang.String.*')\n\n" +
						"偵測Statement: *. + Statement名稱 (eg. '*.toString')\n");
			}
		});
		explainBtn.setText("說明");
		explainBtn.setImage(ImageManager.getInstance().get("processinginst"));
		explainBtn.setBounds(259, 225, 68, 22);

		//將該所有的偵測Library資料顯示在List
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
		//增加Rule
		addRule();
		//清除掉原本的Rule
		ruleMap.clear();
		//先將列表的item取出來
		TableItem[] temp = displayTable.getItems();
		//去traverse整個table看item的Text和是否被勾選到
		for(int i=0;i<temp.length;i++){
			ruleMap.put(temp[i].getText(),temp[i].getChecked());
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
		//顯示Dialog標題
		newShell.setText("Extra Rules Dialog");
	}

	/**
	 * 增加Rule資料
	 */
	private boolean addRule() {
		boolean isWarning = false;
		//刪除Text前後空格部份
		String temp = tempText.getText().trim();

		if (tempText.getText().length() != 0)
		{			
			//若沒有"."表示為Method，自行幫使用者加"*."
			if (!temp.contains("."))
				temp = "*." + temp;

			boolean isExist = false;
			//看Library的Name有沒有重複
			for(int i=0;i<displayTable.getItemCount();i++)
			{
				if(temp.equals(displayTable.getItem(i).getText()))
					isExist = true;
			}
			//沒有重複就加入新的Library
			if (!isExist){
				TableItem item = new TableItem(displayTable,SWT.NONE);
				item.setText(temp);
				item.setChecked(true);
				tempText.setText("");
			}else{
				tempText.setText(temp);
				isWarning = true;
			}
		}
		return isWarning;
	}
	/**
	 * 取得設定偵測的Library資料
	 */
	public TreeMap<String, Boolean> getLibMap()
	{
		return ruleMap;
	}
	/**
	 * 將所有偵測案例顯示在table中
	 */
	private void setInput(){
		Iterator<String> libIt = ruleMap.keySet().iterator();
		while(libIt.hasNext()){
			String temp = libIt.next();
			TableItem item = new TableItem(displayTable,SWT.NONE);
			item.setText(temp);
			item.setChecked(ruleMap.get(temp));
		}
	}
}
