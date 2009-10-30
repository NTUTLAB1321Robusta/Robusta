package ntut.csie.csdet.views;

import java.io.File;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jdom.Document;
import org.jdom.Element;

/**
 * EH TOOL設定畫面
 * @author Shiau
 *
 */
public class SettingPage extends APropertyPage {
	
	private Composite mainPageComposite;
	private Table smellList;
	private StyledText templateArea;
	private Group templateGroup;
	private Composite selectComposite;
	private Button detAllBtn;
	private Button showWarningBtn;
	
	private boolean isDetAll = true;
	private boolean[] detSmellList;
	private boolean isShowWarning = false;
	
	private TemplateText[] tempText;
	private String[] descText;

	public SettingPage(Composite composite, CSPropertyPage page) {
		super(composite, page);
		
		mainPageComposite = composite;
		
		initailState();

		readSetting();

		buildPage(composite);
		
		setUserSetting();
		
		for (int i =0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++)
			tempText[i].setTemplateStyle(composite.getDisplay(), 0);

		smellList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				//取得Table裡的所有欄位
				TableItem[] allItems = smellList.getItems();
				//取得點選的Item
				int index = smellList.getSelectionIndex();
				changeTemplateText(index);
				changeTemplateSize();
			}
		});
	}

	private void initailState() {
		detSmellList = new boolean[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		for (int i =0; i < detSmellList.length; i++)
			detSmellList[i] = true;

		tempText = new TemplateText[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		String temp =	"try {\n" +
						"\t// code in here\n" +
						"} $catch (Exception e) {$\n" +
						"\n" +
						"$}$";
		tempText[0] = new TemplateText(temp, isShowWarning);
		temp =	"try {\n" +
				"\t// code in here\n" +
				"} catch (Exception e) {\n" +
				"\t// TODO Auto-generated catch block\n" +
				"$\te.printStackTrace();$\n" +
				"}";
		tempText[1] = new TemplateText(temp, isShowWarning);
		temp =	"try {\n" +
				"\tfos = new FileOutputStream(path);\n" +
				"} catch (IOException e) {\n" +
				"\tlogger.error(e.getMessage());\n" +
				"} finally {\n" +
				"\t$try {\n" +
				"\t\tfos.close();\n" +
				"\t} catch (IOException e) {\n" +
				"\t\te.printStackTrace();\n" +
				"\t}$\n" +
				"}";
		tempText[2] = new TemplateText(temp, isShowWarning);
		temp = 	"public static void main (String[] args) {\n" +
				"\t//Nothing...\n" +
				"\t$Model model = new Model();\n" +
				"\tView view = new View(model);\n" +
				"\tview.run();$\n" +
				"}";
		tempText[3] = new TemplateText(temp, isShowWarning);
		temp = "try {\n" +
		"\tInputStream is = file.openStream();\n" +
		"\t$is.close();$\n" +
		"} catch (IOException e) {\n" +
		"\tlogger.info(e.getMessage());\n" +
		"}";
		tempText[4] = new TemplateText(temp, isShowWarning);
		temp =	"public void A() throws FileNotFoundException {\n" +
		"\ttry {\n" +
		"\t\t// Do Something\n" +
		"\t} catch (FileNotFoundException e) {\n" +
		"\t\t$logger.info(e);$	//OverLogging\n" +
		"\t\tthrow e;\n" +
		"\t}\n" +
		"}\n" +
		"public void B() {\n" +
		"\ttry {\n" +
		"\t\tA();\t//call method A\n" +
		"\t} catch (FileNotFoundException e) {\n" +
		"\t\t$logger.info(e);$	//use log\n" +
		"\t}\n" +
		"}";
		tempText[5] = new TemplateText(temp, isShowWarning);
		
		descText = new String[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		descText[0] = "Ignroe Checked Exception Description";
		descText[1] = "Dummy Handler Decsription";
		descText[2] = "Nested Try Block Description";
		descText[3] = "Unprotected Main Program Description";
		descText[4] = "Careless CleanUp Description";
		descText[5] = "OverLogging Description";
	}

	/**
	 * 讀取使用者之前設定
	 */
	private void readSetting() {
		Document docJDom = JDomUtil.readXMLFile();

		if(docJDom != null) {
			//從XML裡讀出之前的設定
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.DetectSmellTag) != null) {
				Element rule = root.getChild(JDomUtil.DetectSmellTag).getChild("rule");
				isDetAll = rule.getAttribute(JDomUtil.detect_all).getValue().equals("Y");
				
				for (int i =0; i < detSmellList.length; i++) {
					detSmellList[i] =
					rule.getAttribute(RLMarkerAttribute.CS_TOTAL_TYPE[i]).getValue().equals("Y");
				}
			}
		}
	}

	/**
	 * 建立Page外觀View
	 * @param composite
	 */
	private void buildPage(Composite composite) {
		//是否要選擇性偵測的Button
		detAllBtn = new Button(composite, SWT.CHECK);
		detAllBtn.setText("Enable Project Set Smell");
		detAllBtn.setLocation(10, 5);
		detAllBtn.pack();
		detAllBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				boolean isSelect = detAllBtn.getSelection();
				setSelectEnable(!isSelect);
			}
		});

		//分隔線
		final Label label = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		label.setBounds(0, getBoundsPoint(detAllBtn).y + 5, 550, 1);

		selectComposite = new Composite(composite, SWT.NONE);
		selectComposite.setBounds(0, getBoundsPoint(detAllBtn).y + 5, 550, 402);

		final Label label1 = new Label(selectComposite, SWT.NONE);
		label1.setText("Detect EH Smell Type:");
		label1.setLocation(10, 5);
		label1.pack();

		//選擇EH Smell List
		smellList = new Table(selectComposite, SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION );
		smellList.setLocation(10, getBoundsPoint(label1).y + 5);
		smellList.setFont(new Font(composite.getDisplay(),"Arial", 11, SWT.NONE));
		smellList.setLinesVisible(true);
		smellList.setHeaderVisible(true);
		smellList.setItemCount(6);
		smellList.pack();

		final TableColumn smellColumn = new TableColumn(smellList, SWT.NONE);
		smellColumn.setText("EH Smell Type");
		smellColumn.setWidth(220);
		final TableColumn descColumn = new TableColumn(smellList, SWT.NONE);
		descColumn.setText("Description");

		for (int i =0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++) {
			TableItem item = smellList.getItem(i);
			item.setText(0, RLMarkerAttribute.CS_TOTAL_TYPE[i].replace('_', ' '));
			item.setFont(0, new Font(composite.getDisplay(),"Arial", 11, SWT.BOLD));			
			item.setText(1, descText[i]);
		}
		descColumn.pack();
		smellList.setSize(500 , smellList.getSize().y);
		
		//Template Group
		templateGroup = new Group(selectComposite, SWT.NONE);
		templateGroup.setText("Detial");
		templateGroup.setLocation(10, getBoundsPoint(smellList).y + 10);

		//是否要選擇性偵測的Button
		showWarningBtn = new Button(templateGroup, SWT.CHECK);
		showWarningBtn.setText("Show EH Smell Code");
		showWarningBtn.setLocation(10, 15);
		showWarningBtn.pack();
		showWarningBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				isShowWarning = showWarningBtn.getSelection();

				int index = smellList.getSelectionIndex();
				if (index != -1) {
					changeTemplateText(index);
				}
			}
		});
		
		//Template Text
		templateArea = new StyledText(templateGroup, SWT.BORDER | SWT.V_SCROLL);
		templateArea.setBounds(10, this.getBoundsPoint(showWarningBtn).y +5, 500, 170);
		templateArea.setText("");
		Font font = new Font(composite.getDisplay(),"Courier New", 14,SWT.NORMAL);		
		templateArea.setFont(font);

		templateGroup.pack();
	}

	private void setUserSetting() {
		this.detAllBtn.setSelection(isDetAll);
		setSelectEnable(!isDetAll);
		
		TableItem[] item = smellList.getItems();
		//去traverse整個table看item的Text和是否被勾選到
		for (int i=0; i < item.length; i++)
			item[i].setChecked(detSmellList[i]);
	}

	/**
	 * 是否將Select Composite的按鍵設為可使用
	 * @param isEnable
	 */
	private void setSelectEnable(boolean isEnable) {
		Control[] selectChild = selectComposite.getChildren();

		for (Control control: selectChild)
			control.setEnabled(isEnable);

		Control[] templateChild = this.templateGroup.getChildren();
		for (Control control: templateChild)
			control.setEnabled(isEnable);
	}
	
	private void changeTemplateText(int index) {
		if (index != -1) {
			templateArea.setText(tempText[index].getText());
			tempText[index].setShowWarning(isShowWarning);
			tempText[index].setTemplateStyle(mainPageComposite.getDisplay(), 0);
			templateArea.setStyleRanges(tempText[index].getLocationArray(), tempText[index].getStyleArrray());
		}
	}

	/**
	 * 
	 */
	private void changeTemplateSize() {
		templateArea.pack();
		Font font;
		if (templateArea.getBounds().height > templateGroup.getBounds().height)
			font = new Font(mainPageComposite.getDisplay(),"Courier New", 10,SWT.NORMAL);
		else
			font = new Font(mainPageComposite.getDisplay(),"Courier New", 14,SWT.NORMAL);	

		templateArea.setFont(font);
		templateArea.setBounds(10, this.getBoundsPoint(showWarningBtn).y +5, 500, 170);
	}

	@Override
	public boolean storeSettings() {
		//取的XML的root
		Element root = JDomUtil.createXMLContent();

		//建立Dummy Handler的Tag
		Element detectSmell = new Element(JDomUtil.DetectSmellTag);
		Element rule = new Element("rule");
		//假如e.printStackTrace有被勾選起來
		if (detAllBtn.getSelection())
			rule.setAttribute(JDomUtil.detect_all,"Y");
		else
			rule.setAttribute(JDomUtil.detect_all,"N");

		//
		//先將列表的item取出來
		TableItem[] item = smellList.getItems();
		//去traverse整個table看item的Text和是否被勾選到
		for (int i=0; i < item.length; i++) {
			if (item[i].getChecked())
				rule.setAttribute(RLMarkerAttribute.CS_TOTAL_TYPE[i], "Y");
			else
				rule.setAttribute(RLMarkerAttribute.CS_TOTAL_TYPE[i], "N");
		}

		//將新建的tag加進去
		detectSmell.addContent(rule);

		if (root.getChild(JDomUtil.DetectSmellTag) != null)
			root.removeChild(JDomUtil.DetectSmellTag);

		root.addContent(detectSmell);

		//將檔案寫回
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}

}
