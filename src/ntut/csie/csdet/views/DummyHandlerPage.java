package ntut.csie.csdet.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
import org.jdom.Document;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * 讓user定義一些簡單的detect rule
 * @author chewei
 */
public class DummyHandlerPage extends APropertyPage{
	// 放code template的區域
	private StyledText templateArea;
	// 放code template的區域
	// 是否要捕捉System.out.println() and print()的按鈕
	private Button sysoBtn;
	// 是否要捕捉e.printStackTrace的button
	private Button eprintBtn;
	// 是否要捕捉log4j的button
	private Button log4jBtn;
	// 是否要捕捉java.util.logging的button
	private Button javaUtillogBtn;
	//
	StyleRange[] sampleStyles = new StyleRange[9];
	// code template前半部內容
	private String mainText;
	//　code template的結尾"}"
	private String endText;
	// system.out.println的button的字串
	private String sysoText;
	// e.print的button的字串
	private String eprintText;
	// log4j的button的字串
	private String log4jText;
	// java.util.logging的字串
	private String javaUtillogText;
	// 打開extraLibDialog的按鈕
	private Button extraLibBtn;
	// 打開extraStDialog的按鈕
	private Button extraStBtn;

	// Library Data
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();
	// Statement Data
	private TreeMap<String, Boolean> stMap = new TreeMap<String, Boolean>();
	
	public DummyHandlerPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		//程式碼的內容
		mainText =		"try {   \n" +
						"    // code in here\n" +
						"} catch (Exception e) { \n";
		eprintText = 	"    e.printStackTrace();\n";
		endText =		"}";
		sysoText =		"    System.out.println(e);\n" +
						"    System.out.print(e);\n";
		log4jText =		"    // using log4j\n" +
						"    logger.info(e.getMessage()"+ ");\n";
		javaUtillogText =		"    // using java.util.logging.Logger \n" +
						"    java_logger.info(e.getMessage()"+ "); \n";

		//加入頁面的內容
		addFirstSection(composite);
	}
	
	private void addFirstSection(final Composite dummyHandlerPage){
		Document docJDom = JDomUtil.readXMLFile();
		String eprint = "";
		String setting = "";
		String log4jSet = "";
		String javaLogSet = "";
		if(docJDom != null){
			//從XML裡讀出之前的設定
			Element root = docJDom.getRootElement();
			Element rule = root.getChild(JDomUtil.DummyHandlerTag).getChild("rule");
			eprint = rule.getAttribute(JDomUtil.eprintstacktrace).getValue();
			setting = rule.getAttribute(JDomUtil.systemoutprint).getValue();
			log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			javaLogSet = rule.getAttribute(JDomUtil.java_Logger).getValue();

			//從XML取得外部Library
			Element libRule = root.getChild(JDomUtil.DummyHandlerTag).getChild("librule");
			List<Attribute> libRuleList = libRule.getAttributes();
			//從XML取得外部Statement
			Element stRule = root.getChild(JDomUtil.DummyHandlerTag).getChild("strule");
			List<Attribute> stRuleList = stRule.getAttributes();
			
			//把使用者所儲存的Library設定存到Map資料裡
			for (int i=0;i<libRuleList.size();i++)
				libMap.put(libRuleList.get(i).getQualifiedName(),libRuleList.get(i).getValue().equals("Y"));
			//把使用者所儲存的Statement設定存到Map資料裡
			for (int i=0;i<stRuleList.size();i++)
				stMap.put(stRuleList.get(i).getQualifiedName(),stRuleList.get(i).getValue().equals("Y"));
		}

		final Label detectSettingsLabel = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel.setText("偵測條件(打勾偵測,不打勾不偵測):");
		detectSettingsLabel.setBounds(10, 10, 210, 12);
		
		final Label detectSettingsLabel2 = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel2.setText("偵測外部Statement條件:");
		detectSettingsLabel2.setBounds(251, 11, 210, 12);
		
		extraLibBtn = new Button(dummyHandlerPage, SWT.NONE);
		extraLibBtn.setText("開啟");
		extraLibBtn.setBounds(251, 80, 94, 22);
		extraLibBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),true,libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});
		
		final Label detectSettingsLabel3 = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel3.setBounds(251, 62, 210, 12);
		detectSettingsLabel3.setText("偵測外部Library條件:");
		
		extraStBtn = new Button(dummyHandlerPage, SWT.NONE);
		extraStBtn.setText("開啟");
		extraStBtn.setBounds(251, 29, 94, 22);
		extraStBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),false,stMap);
				dialog.open();
				stMap = dialog.getLibMap();
			}
		});
		
		
		eprintBtn = new Button(dummyHandlerPage, SWT.CHECK);
		eprintBtn.setText("e.printStackTrace();");
		eprintBtn.setBounds(20, 28, 123, 16);
		eprintBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		if(eprint.equals("Y")) {
			eprintBtn.setSelection(true);
		}
		
		sysoBtn = new Button(dummyHandlerPage, SWT.CHECK);
		sysoBtn.setText("System.out.print();");
		sysoBtn.setBounds(20, 50, 123, 16);
		sysoBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		if(setting.equals("Y")){
			sysoBtn.setSelection(true);
		}

		log4jBtn = new Button(dummyHandlerPage, SWT.CHECK);
		log4jBtn.setBounds(20, 72, 159, 16);
		log4jBtn.setText("Detect using org.apache.log4j");
		log4jBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		if(log4jSet.equals("Y")){
			log4jBtn.setSelection(true);
		}

		javaUtillogBtn = new Button(dummyHandlerPage, SWT.CHECK);
		javaUtillogBtn.setText("Detect using java.util.logging.Logger");
		javaUtillogBtn.setBounds(20, 94, 194, 16);
		javaUtillogBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		if(javaLogSet.equals("Y")) {
			javaUtillogBtn.setSelection(true);
		}
		
		final Label codeTemplateLabel = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel.setText("偵測範例:");
		codeTemplateLabel.setBounds(10, 134, 96, 12);

		final Label label1 = new Label(dummyHandlerPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		label1.setBounds(10, 116, 472, 12);
		
		final Label label2 = new Label(dummyHandlerPage, SWT.VERTICAL | SWT.SEPARATOR);
		label2.setBounds(240, 5, 5, 111);
		label2.setText("Label");

		templateArea = new StyledText(dummyHandlerPage, SWT.BORDER);
		Font font = new Font(dummyHandlerPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setBounds(10, 151, 458, 263);
		templateArea.setEditable(false);

		//載入預定的字型、顏色
		addSampleStyle(dummyHandlerPage.getDisplay());

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
		String temp = mainText;
		
		if (eprintBtn.getSelection())
			temp += eprintText;
		if(sysoBtn.getSelection())
			temp += sysoText;
		if (log4jBtn.getSelection())
			temp += log4jText;
		if (javaUtillogBtn.getSelection())
			temp += javaUtillogText;
		temp += endText;

		templateArea.setText(temp);
	}

	/**
	 * 將程式碼中的可能會用到的字型、顏色先行輸入
	 * @param display
	 */
	private void addSampleStyle(Display display) {
		//Try
		sampleStyles[0] = new StyleRange();
		sampleStyles[0].fontStyle = SWT.BOLD;
		sampleStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// 註解
		sampleStyles[1] = new StyleRange();
		sampleStyles[1].fontStyle = SWT.ITALIC;
		sampleStyles[1].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		//catch
		sampleStyles[2] = new StyleRange();
		sampleStyles[2].fontStyle = SWT.BOLD;
		sampleStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//out
		sampleStyles[3] = new StyleRange();
		sampleStyles[3].fontStyle = SWT.ITALIC;
		sampleStyles[3].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// out
		sampleStyles[4] = new StyleRange();
		sampleStyles[4].fontStyle = SWT.ITALIC;
		sampleStyles[4].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// 註解
		sampleStyles[5] = new StyleRange();
		sampleStyles[5].fontStyle = SWT.ITALIC;
		sampleStyles[5].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);		
		// log4j
		sampleStyles[6] = new StyleRange();
		sampleStyles[6].fontStyle = SWT.ITALIC;
		sampleStyles[6].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// 註解
		sampleStyles[7] = new StyleRange();
		sampleStyles[7].fontStyle = SWT.ITALIC;
		sampleStyles[7].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);	
		// java.util.logging.Logger
		sampleStyles[8] = new StyleRange();
		sampleStyles[8].fontStyle = SWT.ITALIC;
		sampleStyles[8].foreground = display.getSystemColor(SWT.COLOR_BLUE);
	}
	
	/**
	 * 將程式碼中的Try ,catch,out標上顏色
	 */
	private void adjustFont(){
		//目前文字長度
		int textLength = mainText.length();

		//(styles和ranges)需要配置多少空間
		int spaceSize = 6;
		if (sysoBtn.getSelection())
			spaceSize+=4;
		if (log4jBtn.getSelection())
			spaceSize+=4;
		if (javaUtillogBtn.getSelection())
			spaceSize+=4;

		//ranges為字型風格的位置範圍，根據spaceSize來決定需要多少空間
		int[] ranges = new int[spaceSize];
		//字型的風格，根據spaceSize來決定需要多少空間
		StyleRange[] styles = new StyleRange[spaceSize/2];

		//ranges和styles的index
		int range_i=0;
		int style_i=0;

		//本文(try catch)文字的對應位置(兩個一組{起始位置,個數})
		int[] main = new int[] {0,3,13,15,31,5};
		//把本文(try catch)文字的字型風格和對應的位置存入
		for (int i=0;i<3;i++)
			styles[style_i++] = sampleStyles[i];
		for (int i=0;i<6;i++)
			ranges[range_i++] = main[i];

		//如果e.printStack選項被選中
		if (eprintBtn.getSelection())
			textLength += eprintText.length();
		//如果SystemOut選項被選中
		if (sysoBtn.getSelection())
		{
			//SystemOut文字的對應位置(相對位置+目前位章的長度)
			int[] syso = new int[] {11 + textLength,3,38 + textLength,3};
			//把本文SystemOut文字的字型風格和對應的位置存入
			for (int i=0;i<4;i++)
				ranges[range_i++] = syso[i];
			for (int i=3;i<5;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += sysoText.length();
		}
		//如果Log4j選項被選中
		if (log4jBtn.getSelection())
		{
			//Log4J文字的對應位置(相對位置+目前位章的長度)
			int[] log4j = new int[] {4+textLength,14,23+textLength,6,};
			//把本文Log4j文字的字型風格和對應的位置存入
			for (int i=0;i<4;i++)
				ranges[range_i++] = log4j[i];			
			for (int i=5;i<7;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += log4jText.length();
		}
		//如果JavaUtillog選項被選中
		if (javaUtillogBtn.getSelection())
		{
			//javaUtillog文字的對應位置(相對位置+目前位章的長度)
			int[] javaUtillog = new int[] {4 + textLength,33,43 + textLength,11};
			//把本文JavaUtillog文字的字型風格和對應的位置存入
			for (int i=0;i<4;i++)
				ranges[range_i++] = javaUtillog[i];
			for (int i=7;i<9;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += javaUtillogText.length();
		}

		//把字型的風格和風格的範圍套用在Template上
		templateArea.setStyleRanges(ranges, styles);
	}

	@Override
	public boolean storeSettings() {

		//取的xml的root
		Element root = JDomUtil.createXMLContent();
		//建立dummyhandler的tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//假如e.printStackTrace有被勾選起來
		if (eprintBtn.getSelection()){
			rule.setAttribute(JDomUtil.eprintstacktrace,"Y");
		}else{
			rule.setAttribute(JDomUtil.eprintstacktrace,"N");		
		}
		//假如system.out.println有被勾選起來
		if(sysoBtn.getSelection()){
			rule.setAttribute(JDomUtil.systemoutprint,"Y");	
		}else{
			rule.setAttribute(JDomUtil.systemoutprint,"N");
		}
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
		//假如dummy handler有新的rule可以加在這裡
		//假如有額外新增的Library rule
		Element libRule = new Element("librule");
		Iterator<String> libIt = libMap.keySet().iterator();
		while(libIt.hasNext()){
			String temp = libIt.next();
			if (libMap.get(temp))
				libRule.setAttribute(temp,"Y");
			else
				libRule.setAttribute(temp,"N");
		}
		//假如有額外新增的Library rule
		Element stRule = new Element("strule");
		Iterator<String> stIt = stMap.keySet().iterator();
		while(stIt.hasNext()){
			String temp = stIt.next();
			if (stMap.get(temp))
				stRule.setAttribute(temp,"Y");
			else
				stRule.setAttribute(temp,"N");
		}
		//將新建的tag加進去
		dummyHandler.addContent(rule);
		dummyHandler.addContent(libRule);
		dummyHandler.addContent(stRule);
		root.addContent(dummyHandler);
		//將檔案寫回
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
