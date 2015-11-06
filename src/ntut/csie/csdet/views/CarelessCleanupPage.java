package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

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

public class CarelessCleanupPage  extends APropertyPage {
	private StyledText templateArea;
	private Button isOnlyDetectingInTryStatement;
	StyleRange[] beforeSampleStyles = new StyleRange[8];
	StyleRange[] afterSampleStyles = new StyleRange[8];
	private String beforeText;
	private String afterText;
	private Button extraRuleBtn;
	private TreeMap<String, Boolean> userDefinedCode = new TreeMap<String, Boolean>();
	private SmellSettings smellSettings;

	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public CarelessCleanupPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings){
		super(composite,page);
		//Context of TextBox, when only detected in try statement
		beforeText = "InputStream in = new FileInputStream(inputFile);\n" +
				"OutputStream out = new FileOutputStream(outputFile);\n" +
				"try {\n" +
				"    // some declared exceptions here\n" +
				"    out.close(); // will be detected\n" +
				"} catch (Exception e) {\n" +
				"    throw e;\n" +
				"}\n" +
				"in.close(); // will not be detected";

		//Context of TextBox, when detected whole area
		afterText =	"InputStream in = new FileInputStream(inputFile);\n" +
				"OutputStream out = new FileOutputStream(outputFile);\n" +
				"try {\n" +
				"    // some declared exceptions here\n" +
				"    out.close(); // will be detected\n" +
				"} catch (Exception e) {\n" +
				"    throw e;\n" +
				"}\n" +
				"in.close(); // will be detected";

		this.smellSettings = smellSettings;
		//add page content
		addFirstSection(composite);
	}

	/**
	 * add page content
	 */
	private void addFirstSection(final Composite CarelessCleanupPage){
		userDefinedCode = smellSettings.getSmellPatterns(SmellSettings.SMELL_CARELESSCLEANUP);

		final Label detectSettingsLabel = new Label(CarelessCleanupPage, SWT.NONE);
		detectSettingsLabel.setText(resource.getString("detect.rule"));
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//Release Method Button
		isOnlyDetectingInTryStatement = new Button(CarelessCleanupPage, SWT.CHECK);
		isOnlyDetectingInTryStatement.setText(resource.getString("detect.out.of.try.statement"));
		isOnlyDetectingInTryStatement.setLocation(
				detectSettingsLabel.getLocation().x + 10,
				getLowerRightCoordinate(detectSettingsLabel).y + 5);
		isOnlyDetectingInTryStatement.pack();
		isOnlyDetectingInTryStatement.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e1){
				//press button to change text and text color
				adjustText();
				adjustFont();
			}
		});

		//Customize Rule
		final Label detectSettingsLabel2 = new Label(CarelessCleanupPage, SWT.NONE);
		detectSettingsLabel2.setText(resource.getString("customize.rule"));
		detectSettingsLabel2.setLocation(getLowerRightCoordinate(isOnlyDetectingInTryStatement).x+80, 10);
		detectSettingsLabel2.pack();
		//Open Dialog Button
		extraRuleBtn = new Button(CarelessCleanupPage, SWT.NONE);
		extraRuleBtn.setText(resource.getString("extra.rule"));
		extraRuleBtn.setLocation(detectSettingsLabel2.getLocation().x+5, getLowerRightCoordinate(detectSettingsLabel2).y+5);
		extraRuleBtn.pack();
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),userDefinedCode);
				dialog.open();
				userDefinedCode = dialog.getLibMap();
			}
		});
		//checked the check box to apply detect rule and change text and text color
		isOnlyDetectingInTryStatement.setSelection(
			smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT));

		//separator bar
		final Label separateLabel1 = new Label(CarelessCleanupPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getLowerRightCoordinate(isOnlyDetectingInTryStatement).x+60, 5);
		separateLabel1.setSize(1, getLowerRightCoordinate(isOnlyDetectingInTryStatement).y+5);
		final Label separateLabel2 = new Label(CarelessCleanupPage,SWT.SEPARATOR | SWT.HORIZONTAL);
		separateLabel2.setLocation(10, getLowerRightCoordinate(extraRuleBtn).y+10);
		separateLabel2.setSize(getLowerRightCoordinate(detectSettingsLabel2).x-10, 1);
		
		//Template Label
		final Label detBeforeLbl = new Label(CarelessCleanupPage, SWT.NONE);
		detBeforeLbl.setText(resource.getString("detect.example"));
		detBeforeLbl.setLocation(10, getLowerRightCoordinate(separateLabel2).y+10);
		detBeforeLbl.pack();
		//TextBox
		templateArea = new StyledText(CarelessCleanupPage, SWT.BORDER);
		Font font = new Font(CarelessCleanupPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setLocation(10, getLowerRightCoordinate(detBeforeLbl).y+5);
		int templateAreaWidth = 620;
		int templateAreaHeight = 230;
		templateArea.setSize(templateAreaWidth, templateAreaHeight);
		templateArea.setEditable(false);
		templateArea.setText(beforeText);
		
		//set separator bar and template label the same length
		if (getLowerRightCoordinate(separateLabel2).x < templateAreaWidth)
			separateLabel2.setSize(templateAreaWidth, 1);
		else
			templateArea.setSize(getLowerRightCoordinate(separateLabel2).x, templateAreaHeight);
		
		//apply default text style
		addBeforeSampleStyle(CarelessCleanupPage.getDisplay());	
		addAfterSampleStyle(CarelessCleanupPage.getDisplay());
		
		adjustText();

		adjustFont();
	}
	
	/**
	 * adjust text on TextBox
	 */
	private void adjustText() {
		if (isOnlyDetectingInTryStatement.getSelection()) {
			templateArea.setText(afterText);
		}
		else {
			templateArea.setText(beforeText);
		}
	}
	
	/**
	 * pre-load text style which will be use in program(Try only)   
	 */
	private void addBeforeSampleStyle(Display display){
		beforeSampleStyles[0] = createBoldMagentaStyleRange(display); // key word new
		beforeSampleStyles[1] = createBoldMagentaStyleRange(display); // key word new
		beforeSampleStyles[2] = createBoldMagentaStyleRange(display); // key word try
		beforeSampleStyles[3] = createItalicGreenStyleRange(display); // comment
		beforeSampleStyles[4] = createItalicGreenStyleRange(display); // comment
		beforeSampleStyles[5] = createBoldMagentaStyleRange(display); // key word catch
		beforeSampleStyles[6] = createBoldMagentaStyleRange(display); // key word throw
		beforeSampleStyles[7] = createItalicGreenStyleRange(display); // comment
	}
	
	/**
	 *pre-load font style which will be use in program(Whole area)
	 */
	private void addAfterSampleStyle(Display display){
		afterSampleStyles[0] = createBoldMagentaStyleRange(display); // key word new
		afterSampleStyles[1] = createBoldMagentaStyleRange(display); // key word new
		afterSampleStyles[2] = createBoldMagentaStyleRange(display); // key word try
		afterSampleStyles[3] = createItalicGreenStyleRange(display); // comment
		afterSampleStyles[4] = createItalicGreenStyleRange(display); // comment
		afterSampleStyles[5] = createBoldMagentaStyleRange(display); // key word catch
		afterSampleStyles[6] = createBoldMagentaStyleRange(display); // key word throw
		afterSampleStyles[7] = createItalicGreenStyleRange(display); // comment
	}
	
	/**
	 * For key word
	 */
	private StyleRange createBoldMagentaStyleRange(Display display) {
		StyleRange styleRange = new StyleRange();
		styleRange.fontStyle = SWT.BOLD;
		styleRange.foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		return styleRange;
	}

	/**
	 * For comment
	 */
	private StyleRange createItalicGreenStyleRange(Display display) {
		StyleRange styleRange = new StyleRange();
		styleRange.fontStyle = SWT.ITALIC;
		styleRange.foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		return styleRange;
	}
	
	/**
	 * adjust TextBox's font style
	 */
	private void adjustFont(){
		// When Button is selected
		if(isOnlyDetectingInTryStatement.getSelection()){
			// set Template font style apply boundary
			int[] beforeRange=new int[]{17,3,68,3,102,3,112,32,162,19,184,5,210,5,233,19};
			//get Template font style
			StyleRange[] beforeStyles=new StyleRange[8];
			for(int i=0;i<beforeSampleStyles.length;i++){
				beforeStyles[i]=beforeSampleStyles[i];
			}
			//apply font boundary and style to Template 
			templateArea.setStyleRanges(beforeRange, beforeStyles);
		}
		
		// When Button isn't selected
		if(!isOnlyDetectingInTryStatement.getSelection()){
			// boundary of Template's font style
			int[] afterRange=new int[]{17,3,68,3,102,3,112,32,162,19,184,5,210,5,233,23};
			//get template font style
			StyleRange[] afterStyles=new StyleRange[8];
			for(int i=0;i<afterSampleStyles.length;i++){
				afterStyles[i]=afterSampleStyles[i];
			}
			//apply font boundary and style to Template 
			templateArea.setStyleRanges(afterRange, afterStyles);
		}
	}
	
	/**
	 * store user configure
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	@Override
	public boolean storeSettings() {
		smellSettings.removePatterns(SmellSettings.SMELL_CARELESSCLEANUP);
		smellSettings.removeExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		
		/* 
		 * get user selection to decide whether detect extra rule 
		 */
		if(isOnlyDetectingInTryStatement.getSelection()) {
			smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		}
		
		// save user define rule
		Iterator<String> userDefinedCodeIterator = userDefinedCode.keySet().iterator();
		while(userDefinedCodeIterator.hasNext()) {
			String key = userDefinedCodeIterator.next();
			smellSettings.addCarelessCleanupPattern(key, userDefinedCode.get(key));
		}
		
		// save user modify back to XML
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}
}
