package ntut.csie.csdet.report.ui;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.views.EditRuleDialog;
import ntut.csie.rleht.common.ImageManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

public class FilterDialog extends Dialog {

	private TreeMap<String, Boolean> filterMap = new TreeMap<String, Boolean>();
	private boolean isAllPackage;
	private Button AllRadBtn;
	private Button selectRadBtn;
	private Composite filterComposite;
	private Button editBtn;
	private Composite btnComposite;
	private Table displayTable;
	private Text tempText;
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	public FilterDialog(Shell parentShell) {
		super(parentShell);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(resource.getString("filter.title"));		
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(403, 366);
	}

	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);

		///偵測全部Project的Button///
		AllRadBtn = new Button(container, SWT.RADIO);
		AllRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				setFilterRuleBtn(false);
			}
		});
		AllRadBtn.setLocation(10, 10);
		AllRadBtn.setText(resource.getString("detect.all"));
		AllRadBtn.pack();

		///選擇Package的Button///
		selectRadBtn = new Button(container, SWT.RADIO);
		selectRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				setFilterRuleBtn(true);
			}
		});
		selectRadBtn.setLocation(10, 30);
		selectRadBtn.setText(resource.getString("detect.select.package"));
		selectRadBtn.pack();

		///Filter相關的Composite///
		filterComposite = new Composite(container, SWT.NONE);
		filterComposite.setBounds(41, 54, 343, 233);

		///顯示文字的Label///
		final Label nameLabel = new Label(filterComposite, SWT.NONE);
		nameLabel.setLocation(7, 10);
		nameLabel.setText(resource.getString("filter.rule"));
		nameLabel.pack();

		///錯誤圖示與錯誤訊息///
		final Label warningLabel = new Label(filterComposite, SWT.NONE);
		warningLabel.setLocation(32, 212);
		warningLabel.setVisible(false);
		warningLabel.setText(resource.getString("filter.exist"));
		warningLabel.pack();
		final Label picLabel = new Label(filterComposite, SWT.NONE);
		picLabel.setLocation(10, 212);
		picLabel.setVisible(false);
		picLabel.setImage(ImageManager.getInstance().get("warning"));
		picLabel.pack();

		///給予User輸入Rule的Text///
		tempText = new Text(filterComposite, SWT.BORDER);
		tempText.setBounds(10, 28, 243, 22);
		tempText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				//若Text一改變就把警告訊息消掉
				setWarning(warningLabel, picLabel, false);
			}
		});

		///顯示Rule的Table///
		displayTable = new Table(filterComposite, SWT.FULL_SELECTION | SWT.CHECK | SWT.MULTI | SWT.BORDER);
		displayTable.setFont(new Font(this.getShell().getDisplay(),"Arial", 11,SWT.NONE));
		displayTable.setBounds(10, 56, 243, 150);
		final GridData gd_testList = new GridData(SWT.FILL, SWT.FILL, true, true);
		displayTable.setLayoutData(gd_testList);
		//如果選擇的displayTable的Item把其名稱顯示在Text上
		displayTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				int selectionIndex = displayTable.getSelectionIndex();
				//防止一開Dialog就先勾選checkbox,出現index=-1的情況
				if(selectionIndex >= 0) {
					//把選擇的Item其Library名稱顯示在Text上
					editBtn.setEnabled(true);
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});
		//若在displayTable的Item上點選兩下，跳出修改視窗
		displayTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				int selectionIndex = displayTable.getSelectionIndex();
				if (selectionIndex >= 0) {
					String temp = displayTable.getItem(selectionIndex).getText();
					//呼叫修改Dialog
					EditRuleDialog dialog = new EditRuleDialog(new Shell(),temp,displayTable);
					dialog.open();
					//依Edit結果修改使用都所選取的Item
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});

		///Button的Composite///
		btnComposite = new Composite(filterComposite, SWT.NONE);
		btnComposite.setBounds(259, 28, 68, 199);

		///增加Rule的Button///
		final Button addBtn = new Button(btnComposite, SWT.NONE);
		addBtn.setBounds(0, 0, 68, 22);
		addBtn.setText(resource.getString("add"));
		addBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				boolean isWarning = addRule();
				//若重複就顯示警告訊息
				if (isWarning)
					setWarning(warningLabel, picLabel, true);
			}
		});

		///刪除Rule的Button///
		final Button removeButton = new Button(btnComposite, SWT.NONE);
		removeButton.setBounds(0, 28, 68, 22);
		removeButton.setText(resource.getString("remove"));
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//Table不為空的且有選到Library 就把選擇的Library給刪掉
				if (displayTable.getItemCount() != 0 && displayTable.getSelectionIndex()!=-1) {
					displayTable.remove(displayTable.getSelectionIndices());
					//刪除時把Text清除
					tempText.setText("");
				}
			}
		});

		///修改的Button///
		editBtn = new Button(btnComposite, SWT.NONE);
		editBtn.setEnabled(false);
		editBtn.setBounds(0, 56, 68, 22);
		editBtn.setText(resource.getString("edit"));
		editBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int selectionIndex = displayTable.getSelectionIndex();
				if (selectionIndex >= 0) {
					String temp = displayTable.getItem(selectionIndex).getText();
					//呼叫修改Dialog
					EditRuleDialog dialog = new EditRuleDialog(new Shell(),temp,displayTable);
					dialog.open();
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});

		///選擇全部Item的Button///
		final Button selectBtn = new Button(btnComposite, SWT.NONE);
		selectBtn.setBounds(0, 84, 68, 22);
		selectBtn.setText(resource.getString("select.all"));
		selectBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//選擇全部的Item
				for (int i=0;i<displayTable.getItemCount();i++) {
					TableItem item = displayTable.getItem(i);
					item.setChecked(true);
				}
			}
		});

		///全部不選擇的Button///
		final Button clearBtn = new Button(btnComposite, SWT.NONE);
		clearBtn.setBounds(0, 112, 68, 22);
		clearBtn.setText(resource.getString("deselect.all"));
		clearBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//取消全部的Item
				for (int i=0;i<displayTable.getItemCount();i++) {
					TableItem item = displayTable.getItem(i);
					item.setChecked(false);
				}
			}
		});
		
		///說明視窗///
		final Button explainBtn = new Button(btnComposite, SWT.NONE);
		explainBtn.setBounds(0, 140,68, 22);
		explainBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//跳出說明的Dialog
				MessageDialog.openInformation(
						new Shell(),
						resource.getString("caption"),
						resource.getString("help.package.description"));
			}
		});
		explainBtn.setText(resource.getString("help"));
		explainBtn.setImage(ImageManager.getInstance().get("help"));
		

		//從XML中取得之前User的設定
		getFilterSettings();

		//將所有偵測案例顯示在table中
		setInput();

		//若之前設定的Select在開始時就選擇Select
		if (!isAllPackage)
			selectRadBtn.setSelection(true);

		return parent;
	}

	/**
	 * 從XML中取得之前User的設定
	 */
	private void getFilterSettings() {
		Document docJDom = JDomUtil.readXMLFile();
		if(docJDom != null) {
			//從XML裡讀出之前的設定
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.EHSmellFilterTaq) != null)
			{
				Element filter = root.getChild(JDomUtil.EHSmellFilterTaq).getChild("filter");
				isAllPackage = Boolean.valueOf(filter.getAttribute("IsAllPackage").getValue());

				List<Attribute> libRuleList = filter.getAttributes();
				//把使用者所儲存的Filter規則設定存到Map資料裡
				for (int i=0;i<libRuleList.size();i++) {
					if (libRuleList.get(i).getQualifiedName() == "IsAllPackage")
						continue;

					//把EH_STAR取代為符號"*"
					String temp = toUnNormalize(libRuleList.get(i).getQualifiedName());
					filterMap.put(temp, libRuleList.get(i).getValue().equals("true"));
				}
			} else {
				isAllPackage = true;
			}
		} else {
			isAllPackage = true;
		}
	}
	
	/**
	 * 儲存設定至XML
	 */
	private void storeSettings() {
		//取的XML的root
		Element root = JDomUtil.createXMLContent();
		//建立DummyHandler的tag		
		Element smellFilter = new Element(JDomUtil.EHSmellFilterTaq);
		Element filter = new Element("filter");

		//儲存是否偵測全部的Package
		filter.setAttribute("IsAllPackage", Boolean.toString(AllRadBtn.getSelection()));

		smellFilter.addContent(filter);

		//若之前已儲存Filter Rule則刪除
		if (root.getChild(JDomUtil.EHSmellFilterTaq) != null)
			root.removeChild(JDomUtil.EHSmellFilterTaq);

		TableItem[] temp = displayTable.getItems();
		//去traverse整個table看item的Text和是否被勾選到
		for(int i=0;i<temp.length;i++) {
			String libName = toNormalize(temp[i].getText());
			filter.setAttribute(libName, String.valueOf(temp[i].getChecked()));
		}

		root.addContent(smellFilter);

		//將檔案寫回
		String path = JDomUtil.getWorkspace() +File.separator +"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}

	/**
	 * 將特殊文字正規化
	 * @param libName
	 * @return
	 */
	private String toNormalize(String libName) {
		libName = libName.replace("*", JDomUtil.EH_Star);
		libName = libName.replace("[", JDomUtil.EH_Left);
		libName = libName.replace("]", JDomUtil.EH_Right);
		return libName;
	}
	
	/**
	 * 將正規化的文字反譯回來
	 * @param libName
	 * @return
	 */
	private String toUnNormalize(String libName) {
		libName = libName.replace(JDomUtil.EH_Star, "*");
		libName = libName.replace(JDomUtil.EH_Left, "[");
		libName = libName.replace(JDomUtil.EH_Right, "]");
		return libName;
	}
	
	/**
	 * 是否將Filter Composite的按鍵設為可使用
	 * @param isEnable
	 */
	private void setFilterRuleBtn(boolean isEnable)
	{
		Control[] fcChild = filterComposite.getChildren();
		for (Control control: fcChild)
			control.setEnabled(isEnable);

		Control[] bcChild = btnComposite.getChildren();
		for (Control control: bcChild)
			control.setEnabled(isEnable);
			
		editBtn.setEnabled(false);
	}
	
	/**
	 * 增加Rule資料
	 */
	private boolean addRule() {
		boolean isWarning = false;
		//刪除Text前後空格部份
		String temp = tempText.getText().trim();

		if (tempText.getText().length() != 0) {
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
	 * 設定Warning的顯示與否
	 * @param warningLabel
	 * @param picLabel
	 */
	private void setWarning(final Label warningLabel, final Label picLabel, boolean isWarning) {
		picLabel.setVisible(isWarning);
		warningLabel.setVisible(isWarning);
	}
	
	/**
	 * 將所有偵測案例顯示在table中
	 */
	private void setInput(){
		Iterator<String> filterIt = filterMap.keySet().iterator();
		while(filterIt.hasNext()){
			String temp = filterIt.next();
			TableItem item = new TableItem(displayTable,SWT.NONE);
			item.setText(temp);
			item.setChecked(filterMap.get(temp));
		}
	}

	/**
	 * store dates
	 */
	protected void okPressed() {
		//儲存設定至XML
		storeSettings();

		super.okPressed();
	}
}
