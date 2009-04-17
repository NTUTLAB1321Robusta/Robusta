package ntut.csie.csdet.views;

import java.io.File;

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
import org.jdom.Document;
import org.jdom.Element;

/**
 * 讓user定義一些簡單的detect rule
 * @author chewei
 */
public class DummyHandlerPage extends APropertyPage{
	// 放code template的區域
	private StyledText templateArea;
	// 放code template的區域
	private StyledText templateArea2;
	// 是否要捕捉System.out.println() and print()的按鈕
	private Button systemoutprintlnButton;
	// 是否要捕捉log4j的button
	private Button log4jButton;
	// 是否要捕捉java.util.logging的button
	private Button javaUtillogBtn;
	//
	StyleRange[] sampleStyles1 = new StyleRange[5];
	StyleRange[] sampleStyles2 = new StyleRange[7];
	// code template前半部內容
	private String mainText;
	//　code template的結尾"}"
	private String endText;
	// code template
	private String printText;
	// log4j的button的字串
	private String logText;
	// java.util.logging的字串
	private String javaText;
	
	public DummyHandlerPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		//程式碼的內容
		mainText =		"try {   \n" +
						"    // code in here\n" +
						"} catch (Exception e) { \n" +
						"    e.printStackTrace();\n";
		endText =		"}";
		printText =		"    System.out.println(e);\n" +
						"    System.out.print(e);\n";
		logText =		"    // using log4j\n" +
						"    logger.info(e.getMessage()"+ ");\n";
		javaText =		"    // using java.util.logging.Logger \n" +
						"    java_logger.info(e.getMessage()"+ "); \n";
		//加入頁面的內容
		addFirstSection(composite);
	}
	
	private void addFirstSection(final Composite dummyHandlerPage){
		final Label detectSettingsLabel1 = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel1.setText("偵測條件 (預設只偵測e.printStackTrace())");
		detectSettingsLabel1.setBounds(10, 10, 210, 12);
		
		final Label detectSettingsLabel2 = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel2.setBounds(228, 10, 77, 12);
		detectSettingsLabel2.setText("");
		
		Document docJDom = JDomUtil.readXMLFile();
		String setting = "";
		String log4jSet = "";
		String javaLogSet = "";
		if(docJDom != null){
			Element root = docJDom.getRootElement();
			Element rule = root.getChild(JDomUtil.DummyHandlerTag).getChild("rule");
			setting = rule.getAttribute(JDomUtil.systemoutprint).getValue();
			log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			javaLogSet = rule.getAttribute(JDomUtil.java_Logger).getValue();
		}
		
		systemoutprintlnButton = new Button(dummyHandlerPage, SWT.CHECK);
		systemoutprintlnButton.setText("System.out.print();");
		systemoutprintlnButton.setBounds(20, 28, 202, 16);
		systemoutprintlnButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		if(setting.equals("Y")){
			systemoutprintlnButton.setSelection(true);
		}

		log4jButton = new Button(dummyHandlerPage, SWT.CHECK);
		log4jButton.setBounds(238, 28, 202, 16);
		log4jButton.setText("Detect using org.apache.log4j");
		log4jButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		if(log4jSet.equals("Y")){
			log4jButton.setSelection(true);
		}

		javaUtillogBtn = new Button(dummyHandlerPage, SWT.CHECK);
		javaUtillogBtn.setText("Detect using java.util.logging.Logger");
		javaUtillogBtn.setBounds(238, 48, 202, 16);
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
		
		final Label codeTemplateLabel1 = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel1.setText("偵測Dummy handler的範例:");
		codeTemplateLabel1.setBounds(10, 82, 155, 12);
		
		final Label codeTemplateLabel2 = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel2.setBounds(10, 232, 270, 12);
		codeTemplateLabel2.setText("偵測有Logger機制則不視為Dummy handler的範例:");

		final Label label1 = new Label(dummyHandlerPage, SWT.SHADOW_IN | SWT.HORIZONTAL | SWT.SEPARATOR);
		label1.setBounds(10, 218, 464, 12);
		final Label label3 = new Label(dummyHandlerPage, SWT.CENTER | SWT.SEPARATOR | SWT.HORIZONTAL);
		label3.setAlignment(SWT.CENTER);
		label3.setBounds(0, 68, 484, 12);

		templateArea = new StyledText(dummyHandlerPage, SWT.BORDER);
		Font font = new Font(dummyHandlerPage.getDisplay(),"Courier New",9,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setBounds(10, 100, 464, 115);
		templateArea.setEditable(false);
		
		templateArea2 = new StyledText(dummyHandlerPage, SWT.BORDER);
		templateArea2.setFont(font);
		templateArea2.setBounds(10, 250, 464, 148);
		templateArea2.setEditable(false);

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
		if(systemoutprintlnButton.getSelection())
			templateArea.setText(mainText + printText + endText);
		else
			templateArea.setText(mainText + endText);
		if (log4jButton.getSelection() && javaUtillogBtn.getSelection())
			templateArea2.setText(mainText + logText + javaText + endText);
		else if (log4jButton.getSelection() && !javaUtillogBtn.getSelection())
			templateArea2.setText(mainText + logText + endText);
		else if (!log4jButton.getSelection() && javaUtillogBtn.getSelection())
			templateArea2.setText(mainText + javaText + endText);
		else
			templateArea2.setText(mainText + endText);
	}

	/**
	 * 將程式碼中的可能會用到的字型、顏色先行輸入
	 * @param display
	 */
	private void addSampleStyle(Display display) {
		//Try
		sampleStyles1[0] = sampleStyles2[0] = new StyleRange();
		sampleStyles1[0].fontStyle = sampleStyles2[0].fontStyle  = SWT.BOLD;
		sampleStyles1[0].foreground = sampleStyles2[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// 註解
		sampleStyles1[1] = sampleStyles2[1] =  new StyleRange();
		sampleStyles1[1].fontStyle = sampleStyles2[1].fontStyle = SWT.ITALIC;
		sampleStyles1[1].foreground = sampleStyles2[1].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		//catch
		sampleStyles1[2] = sampleStyles2[2] = new StyleRange();
		sampleStyles1[2].fontStyle = sampleStyles2[2].fontStyle = SWT.BOLD;
		sampleStyles1[2].foreground = sampleStyles2[2].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//out
		sampleStyles1[3] = new StyleRange();
		sampleStyles1[3].fontStyle = SWT.ITALIC;
		sampleStyles1[3].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// out
		sampleStyles1[4] = new StyleRange();
		sampleStyles1[4].fontStyle = SWT.ITALIC;
		sampleStyles1[4].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// 註解
		sampleStyles2[3] = new StyleRange();
		sampleStyles2[3].fontStyle = SWT.ITALIC;
		sampleStyles2[3].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);		
		// log4j
		sampleStyles2[4] = new StyleRange();
		sampleStyles2[4].fontStyle = SWT.ITALIC;
		sampleStyles2[4].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// 註解
		sampleStyles2[5] = new StyleRange();
		sampleStyles2[5].fontStyle = SWT.ITALIC;
		sampleStyles2[5].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);	
		// java.util.logging.Logger
		sampleStyles2[6] = new StyleRange();
		sampleStyles2[6].fontStyle = SWT.ITALIC;
		sampleStyles2[6].foreground = display.getSystemColor(SWT.COLOR_BLUE);
	}
	
	/**
	 * 將程式碼中的Try ,catch,out標上顏色
	 */
	private void adjustFont(){
		int[] ranges1;
		int[] ranges2;
		
		//SystemOutPrint按鈕有無按下而調整顏色位置(起始位置,幾個字元)兩個一組
		if (systemoutprintlnButton.getSelection())
			ranges1 = new int[] {0,3,13,15,31,5,90,3,117,3};
		else
			ranges1 = new int[] {0,3,13,15,31,5};

		//因log4j和javaUtilog按鈕有無按下而調整顏色位置
		if (log4jButton.getSelection() && javaUtillogBtn.getSelection())
			ranges2 = new int[] {0,3,13,15,31,5,83,14,102,6,135,33,174,11};
		else if (log4jButton.getSelection() && !javaUtillogBtn.getSelection())
			ranges2 = new int[] {0,3,13,15,31,5,83,14,102,6};
		else if (!log4jButton.getSelection() && javaUtillogBtn.getSelection())
			ranges2 = new int[] {0,3,13,15,31,5,83,33,122,11};
		else
			ranges2 = new int[] {0,3,13,15,31,5};
		
		//把預設字型風格載入到要使用的字型風格(配合字數而改變)
		StyleRange[] styles1 = new StyleRange[ranges1.length/2];
		for (int i=0;i<(ranges1.length/2);i++)
			styles1[i] = sampleStyles1[i];

		StyleRange[] styles2 = new StyleRange[ranges2.length/2];
		for (int i=0;i<(ranges2.length/2);i++)
			styles2[i] = sampleStyles2[i];		

		templateArea.setStyleRanges(ranges1, styles1);
		templateArea2.setStyleRanges(ranges2, styles2);
	}

	@Override
	public boolean storeSettings() {

		//取的xml的root
		Element root = JDomUtil.createXMLContent();
		//建立dummyhandler的tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//假如system.out.println有被勾選起來
		if(systemoutprintlnButton.getSelection()){
			rule.setAttribute(JDomUtil.systemoutprint,"Y");	
		}else{
			rule.setAttribute(JDomUtil.systemoutprint,"N");
		}
		//假如log4j有被勾選起來
		if(log4jButton.getSelection()){
			rule.setAttribute(JDomUtil.apache_log4j,"Y");	
		}else{
			rule.setAttribute(JDomUtil.apache_log4j,"N");	
		}
		// 假如java.util.logging.Logger有被勾選起來
		if(javaUtillogBtn.getSelection()){
			rule.setAttribute(JDomUtil.java_Logger,"Y");	
		}else{
			rule.setAttribute(JDomUtil.java_Logger,"N");
		}
		//假如dummy handler有新的rule可以加在這裡
		
		//將新建的tag加進去
		dummyHandler.addContent(rule);
		root.addContent(dummyHandler);
		//將檔案寫回
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
