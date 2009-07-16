package ntut.csie.csdet.views;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.JDomUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
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
 *
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
	//CalleeTamplate的內容
	private String calleeText;
	//CallerTamplate的內容
	private String callerHead;
	private String callerEnd;
	//Exception不轉型、Exception有轉型
	private String callerEx, callerTransEx;
	//預設的CalleeTemplate的字型風格
	private StyleRange[] calleeStyles = new StyleRange[9];
	//預設的CallerTemplate的字型風格
	private StyleRange[] callerStyles = new StyleRange[7];

	public OverLoggingPage(Composite composite, CSPropertyPage page) {
		super(composite, page);
		//CalleeTemplate程式碼的內容
		calleeText = "public void A() throws FileNotFoundException {\n" +
		 			 "\ttry {\n" +
		 			 "\t\t// Do Something\n" +
		 			 "\t} catch (FileNotFoundException e) {\n" +
					 "\t\tlogger.info(e);	//OverLogging\n" +
					 "\t\tthrow e;\n" +
					 "\t}\n" +
					 "}";

		//CallerTemplate程式碼的內容
		callerHead = "public void B() {\n" +
					 "\ttry {\n" +
					 "\t\tA();\t\t\t//call method A\n";
		callerEx = "\t} catch (FileNotFoundException e) {\n";
		callerTransEx = "\t} catch (IOException e) { //Catch Homogeneous Excpetion\n";
		callerEnd =	"\t\tlogger.info(e);\n" +
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
		if(docJDom != null){
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
				exSet = exrule.getAttribute(JDomUtil.transException).getValue();
				
				//把使用者所儲存的Library設定存到Map資料裡
				for (int i=0;i<libRuleList.size();i++)
				{
					//把EH_STAR取代為符號"*"
					String temp = libRuleList.get(i).getQualifiedName().replace("EH_STAR", "*");
					libMap.put(temp,libRuleList.get(i).getValue().equals("Y"));
				}
			}
		}

		final Label detectSettingsLabel = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel.setText("偵測條件:");
		detectSettingsLabel.setBounds(10, 10, 210, 12);
		
		final Label detectSettingsLabel2 = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel2.setText("自行定義偵測條件:");
		detectSettingsLabel2.setBounds(251, 11, 210, 12);
		
		extraRuleBtn = new Button(overLoggingPage, SWT.NONE);
		extraRuleBtn.setText("開啟");
		extraRuleBtn.setBounds(251, 73, 94, 22);
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});

		/// 是否即使轉型仍偵測的按鈕 ///
		detectTransExBtn = new Button(overLoggingPage, SWT.CHECK);
		detectTransExBtn.setText("Excpetion轉型後繼續偵測");
		detectTransExBtn.setBounds(10, 29, 159, 16);
		detectTransExBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		if (exSet.equals("Y"))
			detectTransExBtn.setSelection(true);

		/// 是否偵測Log4j的按鈕 ///
		log4jBtn = new Button(overLoggingPage, SWT.CHECK);
		log4jBtn.setBounds(251, 29, 159, 16);
		log4jBtn.setText("Detect using org.apache.log4j");
		if(log4jSet.equals("Y"))
			log4jBtn.setSelection(true);

		/// 是否偵測JavaUtillog的按鈕 ///
		javaUtillogBtn = new Button(overLoggingPage, SWT.CHECK);
		javaUtillogBtn.setText("Detect using java.util.logging.Logger");
		javaUtillogBtn.setBounds(251, 51, 194, 16);
		if(javaLogSet.equals("Y")) {
			javaUtillogBtn.setSelection(true);
		}

		final Label callerLabel = new Label(overLoggingPage, SWT.NONE);
		callerLabel.setText("Call Chain Example:");
		callerLabel.setBounds(10, 119, 96, 12);
		
		final Label label1 = new Label(overLoggingPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		label1.setBounds(10, 101, 472, 12);
		
		final Label label2 = new Label(overLoggingPage, SWT.VERTICAL | SWT.SEPARATOR);
		label2.setBounds(240, 5, 5, 92);
		label2.setText("Label");

		Font templateFont = new Font(overLoggingPage.getDisplay(),"Courier New",9,SWT.NORMAL);		
		/// Callee Template ///
		calleeTemplate = new StyledText(overLoggingPage, SWT.BORDER);
		calleeTemplate.setFont(templateFont);
		calleeTemplate.setBounds(10, 137, 485, 132);
		calleeTemplate.setEditable(false);
		/// Caller Template ///
		callerTemplate = new StyledText(overLoggingPage, SWT.BORDER);
		callerTemplate.setFont(templateFont);
		callerTemplate.setBounds(10, 282, 485, 132);
		callerTemplate.setEditable(false);

		//載入預定的字型、顏色
		addSampleStyle(overLoggingPage.getDisplay());

		//調整Text的文字
		adjustText();

		//調整程式碼的顏色
		adjustFont();
	}
	
	/**
	 * 調整Text的文字
	 */
	private void adjustText()
	{
		String callerText = callerHead;
		//因為是否按下決定caller template的文字內容
		if(detectTransExBtn.getSelection())
			callerText += callerTransEx;
		else
			callerText += callerEx;
		callerText += callerEnd;

		//設定Template的內容
		callerTemplate.setText(callerText);
		calleeTemplate.setText(calleeText);
	}

	/**
	 * 將程式碼中的可能會用到的字型、顏色先行輸入
	 * @param display
	 */
	private void addSampleStyle(Display display) {
		/// CalleeTemplate Styles ///
		//public void
		calleeStyles[0] = new StyleRange();
		calleeStyles[0].fontStyle = SWT.BOLD;
		calleeStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//throws
		calleeStyles[1] = new StyleRange();
		calleeStyles[1].fontStyle = SWT.BOLD;
		calleeStyles[1].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//try
		calleeStyles[2] = new StyleRange();
		calleeStyles[2].fontStyle = SWT.BOLD;
		calleeStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// 註解
		calleeStyles[3] = new StyleRange();
		calleeStyles[3].fontStyle = SWT.ITALIC;
		calleeStyles[3].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		//catch
		calleeStyles[4] = new StyleRange();
		calleeStyles[4].fontStyle = SWT.BOLD;
		calleeStyles[4].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//Exception
		calleeStyles[5] = new StyleRange();
		calleeStyles[5].fontStyle = SWT.BOLD;
		calleeStyles[5].foreground = display.getSystemColor(SWT.DEFAULT);
		//log
		calleeStyles[6] = new StyleRange();
		calleeStyles[6].fontStyle = SWT.ITALIC;
		calleeStyles[6].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// 註解
		calleeStyles[7] = new StyleRange();
		calleeStyles[7].fontStyle = SWT.ITALIC;
		calleeStyles[7].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// throw
		calleeStyles[8] = new StyleRange();
		calleeStyles[8].fontStyle = SWT.BOLD;
		calleeStyles[8].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);

		/// CallerTemplate Styles ///
		//public void
		callerStyles[0] = new StyleRange();
		callerStyles[0].fontStyle = SWT.BOLD;
		callerStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//try
		callerStyles[1] = new StyleRange();
		callerStyles[1].fontStyle = SWT.BOLD;
		callerStyles[1].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// 註解
		callerStyles[2] = new StyleRange();
		callerStyles[2].fontStyle = SWT.ITALIC;
		callerStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		//catch
		callerStyles[3] = new StyleRange();
		callerStyles[3].fontStyle = SWT.BOLD;
		callerStyles[3].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//Exception
		callerStyles[4] = new StyleRange();
		callerStyles[4].fontStyle = SWT.BOLD;
		callerStyles[4].foreground = display.getSystemColor(SWT.DEFAULT);
		// 註解
		callerStyles[5] = new StyleRange();
		callerStyles[5].fontStyle = SWT.ITALIC;
		callerStyles[5].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		//log
		callerStyles[6] = new StyleRange();
		callerStyles[6].fontStyle = SWT.ITALIC;
		callerStyles[6].foreground = display.getSystemColor(SWT.COLOR_BLUE);
	}
	
	/**
	 * 將程式碼中的Try ,catch,out標上顏色
	 */
	private void adjustFont(){
		//CalleeTemplate的字型風格的位置範圍
		int[] calleeRange = new int[] {0,11,16,6,48,3,56,15,75,5,82,21,111,6,127,13,143,5};
		//把字型的風格和風格的範圍套用在CalleeTemplate上
		calleeTemplate.setStyleRanges(calleeRange, calleeStyles);

		//Caller Template上半部字型風格的位置範圍
		int[] upper = new int[] {0,11,19,3,34,15,53,5};
		//Caller Template下半部字型風格的位置範圍
		int[] lower;

		//目前文字長度
		int textLength;
		//如果e.printStack選項被選中
		if (detectTransExBtn.getSelection()) {
			textLength = callerHead.length() + callerTransEx.length();
			lower = new int[] {60,11,77,29,textLength+2,6};
		} else {
			textLength = callerHead.length() + callerEx.length();
			//若沒有選少一行注解
			lower = new int[] {60,21,textLength+2,6};
		}

		//ranges為字型風格的位置範圍，根據spaceSize來決定需要多少空間
		int[] callerRanges = new int [upper.length + lower.length];

		//ranges的index
		int range_i = 0;
		//配置callerRanges字型風格的位置範圍
		for (int i = 0; i < upper.length; i++)
			callerRanges[range_i++] = upper[i];
		for (int i = 0; i < lower.length; i++)
			callerRanges[range_i++] = lower[i];

		int size = callerRanges.length / 2;
		//字型的風格，根據spaceSize來決定需要多少空間
		StyleRange[] styles = new StyleRange[size];
		//把本文(try catch)文字的字型風格和對應的位置存入
		for (int i = 0, style_i = 0; i < 7; i++) {
			//若沒有選exceptionBtn，因少一行注解，所以省略一個風格
			if (size == 6 && i == 5)
				continue;
			styles[style_i++] = callerStyles[i];
		}
		//把字型的風格和風格的範圍套用在Template上
		callerTemplate.setStyleRanges(callerRanges, styles);
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
		if(log4jBtn.getSelection()){
			rule.setAttribute(JDomUtil.apache_log4j,"Y");
		}else{
			rule.setAttribute(JDomUtil.apache_log4j,"N");	
		}
		//假如java.util.logging.Logger有被勾選起來
		if(javaUtillogBtn.getSelection()){
			rule.setAttribute(JDomUtil.java_Logger,"Y");	
		}else{
			rule.setAttribute(JDomUtil.java_Logger,"N");
		}

		//把使用者自訂的Rule存入XML
		Element libRule = new Element("librule");
		Iterator<String> libIt = libMap.keySet().iterator();
		while(libIt.hasNext()){
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
		if(detectTransExBtn.getSelection()){
			exRule.setAttribute(JDomUtil.transException,"Y");
		}else{
			exRule.setAttribute(JDomUtil.transException,"N");	
		}

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
