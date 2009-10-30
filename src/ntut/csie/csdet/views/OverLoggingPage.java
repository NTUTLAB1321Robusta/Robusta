package ntut.csie.csdet.views;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.JDomUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

/**
 * OverLogging Setting頁面
 * @author Shiau
 */
public class OverLoggingPage extends APropertyPage {	
	//Detect Logging的Rule
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();

	//Exception即使轉型仍繼續偵測的button
	private Button detectTransExBtn;
	//是否要捕捉log4j的button
	private Button log4jBtn;
	//是否要捕捉java.util.logging的button
	private Button javaUtillogBtn;
	//打開extraRuleDialog的按鈕
	private Button extraRuleBtn;
	
	//放code template的區域
	private StyledText callerTemplate;
	//放code template的區域
	private StyledText calleeTemplate;

	//CalleeTamplate和CallerTamplate的內容 和 字型風格
	TemplateText calleeText = new TemplateText("", false);
	TemplateText callerText = new TemplateText("", false);

	//CalleeTamplate和CallerTamplate的內容
	String callee, calleeOrg, calleeTrans;
	String callerHead, callerOrg, callerTrans, callerTail;
		
	public OverLoggingPage(Composite composite, CSPropertyPage page) {
		super(composite, page);

		//TODO callee前半部throws RuntimeException，要改成Throw FileNotFoundException
		/// CalleeTemplate程式碼的內容 ///
		//文字前半段
		callee = "public void A() throws RuntimeException {\n" +
				 		"\ttry {\n" +
				 		"\t// Do Something\n" +
				 		"\t} catch (FileNotFoundException e) {\n" +
				 		"\t\tlogger.info(e);	//OverLogging\n";

		//文字後半段(選項打勾前)
		calleeOrg = "\t\tthrow e;\n" +
						   "\t}\n" +
				 		   "}";

		//文字後半段(選項打勾後)
		calleeTrans = "\t\tthrow new RuntimeException(e);	//Transform Exception Type\n" +
				 			 "\t}\n" +
				 			 "}";

		/// CallerTemplate程式碼的內容 ///
		//文字前段
		callerHead = "public void B() {\n" +
							"\ttry {\n" +
							"\t\tA();\t\t\t//call method A\n";

		//文字中段(選項打勾前)
		callerOrg = "\t} catch (FileNotFoundException e) {\n";

		//文字中段(選項打勾後)
		callerTrans = "\t} catch (RuntimeException e) { //Catch Transform Exception Type\n";

		//文字後段
		callerTail = "\t\tlogger.info(e);\t//use log\n" +
							"\t}\n" +
							"}";

		//加入頁面的內容
		addFirstSection(composite);
	}
	
	/**
	 * 加入頁面的內容
	 * @param overLoggingPage
	 */
	private void addFirstSection(final Composite overLoggingPage){
		Document docJDom = JDomUtil.readXMLFile();
		String exSet = "";
		String log4jSet = "";
		String javaLogSet = "";
		if (docJDom != null){
			//從XML裡讀出之前的設定
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.OverLoggingTag) != null) {
				Element rule = root.getChild(JDomUtil.OverLoggingTag).getChild("rule");
				log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
				javaLogSet = rule.getAttribute(JDomUtil.java_Logger).getValue();

				//從XML取得外部Library
				Element libRule = root.getChild(JDomUtil.OverLoggingTag).getChild("librule");
				List<Attribute> libRuleList = libRule.getAttributes();

				Element exrule = root.getChild(JDomUtil.OverLoggingTag).getChild("exrule");
				exSet = exrule.getAttribute(JDomUtil.trans_Exception).getValue();
				
				//把使用者所儲存的Library設定存到Map資料裡
				for (int i=0; i < libRuleList.size(); i++) {
					//把EH_STAR取代為符號"*"
					String temp = libRuleList.get(i).getQualifiedName().replace("EH_STAR", "*");
					libMap.put(temp,libRuleList.get(i).getValue().equals("Y"));
				}
			}
		}

		/// 偵測條件Label ///
		final Label detectSettingsLabel = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel.setText("偵測條件：");
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//是否即使轉型仍偵測的按鈕
		detectTransExBtn = new Button(overLoggingPage, SWT.CHECK);
		detectTransExBtn.setText("Excpetion轉型後繼續偵測");
		detectTransExBtn.setLocation(detectSettingsLabel.getLocation().x+10, getBoundsPoint(detectSettingsLabel).y + 5);
		detectTransExBtn.pack();
		detectTransExBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont(overLoggingPage.getDisplay());
			}
		});
		if (exSet.equals("Y"))
			detectTransExBtn.setSelection(true);

		/// Customize定義偵測條件 Label ///
		final Label detectSettingsLabel2 = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel2.setText("自行定義偵測條件:");
		detectSettingsLabel2.setLocation(getBoundsPoint(detectTransExBtn).x + 85, 11);
		detectSettingsLabel2.pack();
		//是否偵測Log4j的按鈕
		log4jBtn = new Button(overLoggingPage, SWT.CHECK);
		log4jBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getBoundsPoint(detectSettingsLabel2).y + 5);
		log4jBtn.setText("Detect using org.apache.log4j");
		log4jBtn.pack();
		if(log4jSet.equals("Y"))
			log4jBtn.setSelection(true);
		//是否偵測JavaUtillog的按鈕
		javaUtillogBtn = new Button(overLoggingPage, SWT.CHECK);
		javaUtillogBtn.setText("Detect using java.util.logging.Logger");
		javaUtillogBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getBoundsPoint(log4jBtn).y + 5);
		javaUtillogBtn.pack();
		if(javaLogSet.equals("Y"))
			javaUtillogBtn.setSelection(true);
		//Customize Rule Button
		extraRuleBtn = new Button(overLoggingPage, SWT.NONE);
		extraRuleBtn.setText("開啟");
		extraRuleBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getBoundsPoint(javaUtillogBtn).y + 5);
		extraRuleBtn.pack();
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});

		/// 分隔線 ///
		final Label separateLabel1 = new Label(overLoggingPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getBoundsPoint(detectTransExBtn).x+70, 5);
		separateLabel1.setSize(1, getBoundsPoint(extraRuleBtn).y - 5);
		final Label separateLabel2 = new Label(overLoggingPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		separateLabel2.setLocation(5, this.getBoundsPoint(extraRuleBtn).y+5);
		separateLabel2.setSize(getBoundsPoint(javaUtillogBtn).x -5, 1);

		/// Template ///
		final Label callerLabel = new Label(overLoggingPage, SWT.NONE);
		callerLabel.setText("Call Chain Example:");
		callerLabel.setLocation(detectSettingsLabel.getLocation().x, getBoundsPoint(separateLabel2).y + 5);
		callerLabel.pack();
		Font templateFont = new Font(overLoggingPage.getDisplay(),"Courier New",9,SWT.NORMAL);		
		//Callee Template
		calleeTemplate = new StyledText(overLoggingPage, SWT.BORDER);
		calleeTemplate.setFont(templateFont);
		calleeTemplate.setBounds(detectSettingsLabel.getLocation().x, getBoundsPoint(callerLabel).y+5, 485, 132);
		calleeTemplate.setEditable(false);
		//Caller Template
		callerTemplate = new StyledText(overLoggingPage, SWT.BORDER);
		callerTemplate.setFont(templateFont);
		callerTemplate.setBounds(detectSettingsLabel.getLocation().x, getBoundsPoint(calleeTemplate).y+10, 485, 132);
		callerTemplate.setEditable(false);

		//分隔線與Template等長(取最長的)
		if (getBoundsPoint(separateLabel2).x < 485)
			separateLabel2.setSize(485, 1);
		else {
			calleeTemplate.setSize(getBoundsPoint(separateLabel2).x, 132);
			callerTemplate.setSize(getBoundsPoint(separateLabel2).x, 132);
		}

		//調整Text的文字
		adjustText();
		//調整程式碼的顏色
		adjustFont(overLoggingPage.getDisplay());
	}
	
	/**
	 * 調整Text的文字
	 */
	private void adjustText()
	{
		/// CalleeTemplate的字型風格的位置範圍 ///
		String calleeTemp = "";

		calleeTemp += callee;
		if(!detectTransExBtn.getSelection())
			calleeTemp += calleeOrg;
		else
			calleeTemp += calleeTrans;

		//設定Template的內容
		calleeTemplate.setText(calleeTemp);
		calleeText.setTemplateText(calleeTemp, false);
		
		/// CallerTemplate的字型風格的位置範圍 ///
		String callerTemp = "";

		callerTemp += callerHead;
		if(!detectTransExBtn.getSelection())
			callerTemp += callerOrg;
		else
			callerTemp += callerTrans;
		callerTemp += callerTail;

		//設定Template的內容
		callerTemplate.setText(callerTemp);
		callerText.setTemplateText(callerTemp, false);
	}
	
	/**
	 * 將程式碼中的文字標上顏色
	 */
	private void adjustFont(Display display) {
		/// 設置CalleeTemplate的字型風格的位置範圍 ///
		calleeText.setTemplateStyle(display, 0);
		//把字型的風格和風格的範圍套用在CalleeTemplate上
		calleeTemplate.setStyleRanges(calleeText.getLocationArray(), calleeText.getStyleArrray());

		/// 設置CalleeTemplate的字型風格的位置範圍 ///
		callerText.setTemplateStyle(display, 0);
		//把字型的風格和風格的範圍套用在CallerTemplate上
		callerTemplate.setStyleRanges(callerText.getLocationArray(), callerText.getStyleArrray());
	}

	/**
	 * 儲存使用者設定
	 */
	@Override
	public boolean storeSettings() {
		//取的XML的root
		Element root = JDomUtil.createXMLContent();

		//建立OverLogging的tag
		Element overLogging = new Element(JDomUtil.OverLoggingTag);

		Element rule = new Element("rule");
		//假如log4j有被勾選起來
		if (log4jBtn.getSelection())
			rule.setAttribute(JDomUtil.apache_log4j,"Y");
		else
			rule.setAttribute(JDomUtil.apache_log4j,"N");	

		//假如java.util.logging.Logger有被勾選起來
		if (javaUtillogBtn.getSelection())
			rule.setAttribute(JDomUtil.java_Logger,"Y");	
		else
			rule.setAttribute(JDomUtil.java_Logger,"N");

		//把使用者自訂的Rule存入XML
		Element libRule = new Element("librule");
		Iterator<String> libIt = libMap.keySet().iterator();
		while(libIt.hasNext()) {
			String temp = libIt.next();
			//若有出現*取代為EH_STAR(jdom不能記"*"這個字元)
			String libName = temp.replace("*", "EH_STAR");
			if (libMap.get(temp))
				libRule.setAttribute(libName,"Y");
			else
				libRule.setAttribute(libName,"N");
		}

		Element exRule = new Element("exrule");
		//把使用者設定的
		if (detectTransExBtn.getSelection())
			exRule.setAttribute(JDomUtil.trans_Exception,"Y");
		else
			exRule.setAttribute(JDomUtil.trans_Exception,"N");	

		//將新建的tag加進去
		overLogging.addContent(rule);
		overLogging.addContent(libRule);
		overLogging.addContent(exRule);

		//將OverLogging Tag加入至XML中
		if (root.getChild(JDomUtil.OverLoggingTag) != null)
			root.removeChild(JDomUtil.OverLoggingTag);
		root.addContent(overLogging);

		//將檔案寫回
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
