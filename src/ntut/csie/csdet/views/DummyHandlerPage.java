package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;

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
 * allow user to define manual detect rule
 * @author chewei
 */
public class DummyHandlerPage extends APropertyPage{
	// the area contains code template
	private StyledText templateArea;
	// button to invoke catching System.out.println() and print() these method invocation 
	private Button sysoBtn;
	// button to invoke catching e.printStackTrace this method invocation 
	private Button eprintBtn;
	// button to invoke catching log4j this method invocation
	private Button log4jBtn;
	// button to invoke catching java.util.logging this method invocation
	private Button javaUtillogBtn;
	// default template's font style
	StyleRange[] sampleStyles = new StyleRange[9];
	// First half of code template
	private String mainText;
	//　After half of code template ex. "}"
	private String endText;
	// text of sysoBtn
	private String sysoText;
	// text of eprintBtn
	private String eprintText;
	// text of log4jBtn
	private String log4jText;
	// text of javaUtillogBtn
	private String javaUtillogText;
	// button to pup up extraRuleDialog
	private Button extraRuleBtn;
	// Library Data
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();
	//access configure stored in XML 
	private SmellSettings smellSettings;

	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	public DummyHandlerPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings) {
		super(composite,page);
		//prepare text for each button and template
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
		//add page content
		addFirstSection(composite);
	}
	
	private void addFirstSection(final Composite dummyHandlerPage) {
		libMap = smellSettings.getSmellPatterns(SmellSettings.SMELL_DUMMYHANDLER);
		/// default detection rule  ///
		final Label detectSettingsLabel = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel.setText(resource.getString("detect.rule"));
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//button to detect e.printStackTrace invocation
		eprintBtn = new Button(dummyHandlerPage, SWT.CHECK);
		eprintBtn.setText(resource.getString("print.stack.trace"));
		eprintBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(detectSettingsLabel).y+5);
		eprintBtn.pack();
		eprintBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//change font style
				adjustText();
				adjustFont();
			}
		});
		eprintBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace));
		
		//button to detect System.out.print invocation
		sysoBtn = new Button(dummyHandlerPage, SWT.CHECK);
		sysoBtn.setText(resource.getString("system.out.print"));
		sysoBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(eprintBtn).y+5);
		sysoBtn.pack();
		sysoBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//change font style
				adjustText();
				adjustFont();
			}
		});
		sysoBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint));
		
		//button to detect Log4j invocation
		log4jBtn = new Button(dummyHandlerPage, SWT.CHECK);
		log4jBtn.setText(resource.getString("detect.log4j"));
		log4jBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(sysoBtn).y+5);
		log4jBtn.pack();
		log4jBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//change font style
				adjustText();
				adjustFont();
			}
		});
		log4jBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j));
		
		//button to detect JavaUtillog invocation
		javaUtillogBtn = new Button(dummyHandlerPage, SWT.CHECK);
		javaUtillogBtn.setText(resource.getString("detect.logger"));
		javaUtillogBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(log4jBtn).y+5);
		javaUtillogBtn.pack();
		javaUtillogBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//change font style
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

		//set separator bar 
		final Label separateLabel1 = new Label(dummyHandlerPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getLowerRightCoordinate(javaUtillogBtn).x+28, 5);
		separateLabel1.setSize(1, getLowerRightCoordinate(javaUtillogBtn).y-5);
		final Label separateLabel2 = new Label(dummyHandlerPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		separateLabel2.setLocation(10, getLowerRightCoordinate(javaUtillogBtn).y+5);
		separateLabel2.setSize(getLowerRightCoordinate(detectSettingsLabel2).x, 1);

		//set template label 
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

		//set separator bar and template label the same length
		if (getLowerRightCoordinate(separateLabel2).x < 458)
			separateLabel2.setSize(458, 1);
		else
			templateArea.setSize(getLowerRightCoordinate(separateLabel2).x, 263);

		//apply default text style
		addSampleStyle(dummyHandlerPage.getDisplay());

		adjustText();

		adjustFont();
	}
	
	/**
	 * adjust template text
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
	 * pre-load font style which will be use in program
	 * @param display
	 */
	private void addSampleStyle(Display display) {
		//Try
		sampleStyles[0] = new StyleRange();
		sampleStyles[0].fontStyle = SWT.BOLD;
		sampleStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// comment
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
		// comment
		sampleStyles[5] = new StyleRange();
		sampleStyles[5].fontStyle = SWT.ITALIC;
		sampleStyles[5].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);		
		// log4j
		sampleStyles[6] = new StyleRange();
		sampleStyles[6].fontStyle = SWT.ITALIC;
		sampleStyles[6].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// comment
		sampleStyles[7] = new StyleRange();
		sampleStyles[7].fontStyle = SWT.ITALIC;
		sampleStyles[7].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);	
		// java.util.logging.Logger
		sampleStyles[8] = new StyleRange();
		sampleStyles[8].fontStyle = SWT.ITALIC;
		sampleStyles[8].foreground = display.getSystemColor(SWT.COLOR_BLUE);
	}
	
	/**
	 * set Try, catch and out colored
	 */
	private void adjustFont() {
		int textLength = mainText.length();

		//allocate space for style and range 
		int spaceSize = 6;
		if (sysoBtn.getSelection())
			spaceSize+=4;
		if (log4jBtn.getSelection())
			spaceSize+=4;
		if (javaUtillogBtn.getSelection())
			spaceSize+=4;

		//ranges is boundary of font location position
		int[] ranges = new int[spaceSize];
		//styles is font style 
		StyleRange[] styles = new StyleRange[spaceSize/2];

		//set index of ranges and styles 
		int range_i=0;
		int style_i=0;

		//try statement and catch statement corresponding position(these two statement is pair{start position, start position,.... , amount})
		int[] main = new int[] {0,3,13,15,31,5};
		//save font style correspond to try statement and catch statement position
		for (int i=0;i<3;i++)
			styles[style_i++] = sampleStyles[i];
		for (int i=0;i<6;i++)
			ranges[range_i++] = main[i];

		if (eprintBtn.getSelection())
			textLength += eprintText.length();
		
		if (sysoBtn.getSelection()) {
			//SystemOut statement corresponding position(relative position + current statement length)
			int[] syso = new int[] {11 + textLength,3,38 + textLength,3};
			//save font style correspond to SystemOut statement position
			for (int i=0;i<4;i++)
				ranges[range_i++] = syso[i];
			for (int i=3;i<5;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += sysoText.length();
		}
		
		if (log4jBtn.getSelection()) {
			//Log4J statement corresponding position(relative position + current statement length)
			int[] log4j = new int[] {4+textLength,14,23+textLength,6,};
			//save font style correspond to Log4j statement position
			for (int i=0;i<4;i++)
				ranges[range_i++] = log4j[i];			
			for (int i=5;i<7;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += log4jText.length();
		}

		if (javaUtillogBtn.getSelection()) {
			//javaUtillog statement corresponding position(relative position + current statement length)
			int[] javaUtillog = new int[] {4 + textLength,33,43 + textLength,11};
			//save font style correspond to javaUtillog statement position
			for (int i=0;i<4;i++)
				ranges[range_i++] = javaUtillog[i];
			for (int i=7;i<9;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += javaUtillogText.length();
		}

		//apply style on template
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
		
		// save user define rule
		Iterator<String> userDefinedCodeIterator = libMap.keySet().iterator();
		while(userDefinedCodeIterator.hasNext()) {
			String key = userDefinedCodeIterator.next();
			smellSettings.addDummyHandlerPattern(key, libMap.get(key));
		}

		// save smell configure back to XML
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}
}
