package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;

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

/**
 * 讓user定義一些簡單的detect rule
 * @author chewei
 */
public class DummyHandlerPage extends APropertyPage{
	// 放code template的區域
	private StyledText templateArea;
	// 是否要捕捉System.out.println() and print()的按鈕
	private Button sysoBtn;
	// 是否要捕捉e.printStackTrace的button
	private Button eprintBtn;
	// 是否要捕捉log4j的button
	private Button log4jBtn;
	// 是否要捕捉java.util.logging的button
	private Button javaUtillogBtn;
	// 預設的template的字型風格
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
	// 打開extraRuleDialog的按鈕
	private Button extraRuleBtn;
	// Library Data
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();
	// 負責處理讀寫XML
	private SmellSettings smellSettings;

	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	public DummyHandlerPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings) {
		super(composite,page);
		//程式碼的內容
		mainText =			"try {   \n" +
							"    // code in here\n" +
							"} catch (Exception e) { \n";
		eprintText = 		"    e.printStackTrace();\n";
		endText =			"}";
		sysoText =			"    System.out.println(e);\n" +
							"    System.out.print(e);\n";
		log4jText =			"    // using log4j\n" +
							"    logger.info(e.getMessage()"+ ");\n";
		javaUtillogText =	"    // using java.util.logging.Logger \n" +
							"    java_logger.info(e.getMessage()"+ "); \n";

		this.smellSettings = smellSettings;
		//加入頁面的內容
		addFirstSection(composite);
	}
	
	private void addFirstSection(final Composite dummyHandlerPage) {
		libMap = smellSettings.getSemllPatterns(SmellSettings.SMELL_DUMMYHANDLER);
		/// 預設偵測條件  ///
		final Label detectSettingsLabel = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel.setText(resource.getString("detect.rule"));
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//是否偵測e.printStackTrace的按鈕
		eprintBtn = new Button(dummyHandlerPage, SWT.CHECK);
		eprintBtn.setText(resource.getString("print.stack.trace"));
		eprintBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(detectSettingsLabel).y+5);
		eprintBtn.pack();
		eprintBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		eprintBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace));
		
		//是否偵測System.out.print的按鈕
		sysoBtn = new Button(dummyHandlerPage, SWT.CHECK);
		sysoBtn.setText(resource.getString("system.out.print"));
		sysoBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(eprintBtn).y+5);
		sysoBtn.pack();
		sysoBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		sysoBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint));
		
		//是否偵測Log4j的按鈕
		log4jBtn = new Button(dummyHandlerPage, SWT.CHECK);
		log4jBtn.setText(resource.getString("detect.log4j"));
		log4jBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(sysoBtn).y+5);
		log4jBtn.pack();
		log4jBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		log4jBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j));
		
		//是否偵測JavaUtillog的按鈕
		javaUtillogBtn = new Button(dummyHandlerPage, SWT.CHECK);
		javaUtillogBtn.setText(resource.getString("detect.logger"));
		javaUtillogBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(log4jBtn).y+5);
		javaUtillogBtn.pack();
		javaUtillogBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		javaUtillogBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger));

		/// Customize Rule ///
		final Label detectSettingsLabel2 = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel2.setText(resource.getString("customize.rule"));
		detectSettingsLabel2.setLocation(getLowerRightCoordinate(javaUtillogBtn).x+43, 10);
		detectSettingsLabel2.pack();
		//Customize Rule Button
		extraRuleBtn = new Button(dummyHandlerPage, SWT.NONE);
		extraRuleBtn.setText(resource.getString("extra.rule"));
		extraRuleBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getLowerRightCoordinate(detectSettingsLabel2).y+5);
		extraRuleBtn.pack();
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});

		/// 分隔線 ///
		final Label separateLabel1 = new Label(dummyHandlerPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getLowerRightCoordinate(javaUtillogBtn).x+28, 5);
		separateLabel1.setSize(1, getLowerRightCoordinate(javaUtillogBtn).y-5);
		final Label separateLabel2 = new Label(dummyHandlerPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		separateLabel2.setLocation(10, getLowerRightCoordinate(javaUtillogBtn).y+5);
		separateLabel2.setSize(getLowerRightCoordinate(detectSettingsLabel2).x, 1);

		/// Template ///
		final Label codeTemplateLabel = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel.setText(resource.getString("detect.example"));
		codeTemplateLabel.setLocation(10, getLowerRightCoordinate(separateLabel2).y+10);
		codeTemplateLabel.pack();
		//Detect Template
		templateArea = new StyledText(dummyHandlerPage, SWT.BORDER);
		Font font = new Font(dummyHandlerPage.getDisplay(),"Courier New", 14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setLocation(10, getLowerRightCoordinate(codeTemplateLabel).y+5);
		templateArea.setSize(458, 263);
		templateArea.setEditable(false);

		//分隔線與Template等長(取最長的)
		if (getLowerRightCoordinate(separateLabel2).x < 458)
			separateLabel2.setSize(458, 1);
		else
			templateArea.setSize(getLowerRightCoordinate(separateLabel2).x, 263);

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
	private void adjustText() {
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
		// catch
		sampleStyles[2] = new StyleRange();
		sampleStyles[2].fontStyle = SWT.BOLD;
		sampleStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// out
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
	private void adjustFont() {
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
		if (sysoBtn.getSelection()) {
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
		if (log4jBtn.getSelection()) {
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
		if (javaUtillogBtn.getSelection()) {
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
		smellSettings.removePatterns(SmellSettings.SMELL_DUMMYHANDLER);
		smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrint);
		smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrintln);
		smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		
		if(eprintBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		if(sysoBtn.getSelection()) {
			smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
			smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		}
		if(log4jBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		if(javaUtillogBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		
		// 存入使用者自訂Rule
		Iterator<String> userDefinedCodeIterator = libMap.keySet().iterator();
		while(userDefinedCodeIterator.hasNext()) {
			String key = userDefinedCodeIterator.next();
			smellSettings.addDummyHandlerPattern(key, libMap.get(key));
		}

		// 將檔案寫回
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}
}
