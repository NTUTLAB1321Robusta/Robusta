package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;

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

/**
 * OverLogging Setting page
 * @author Shiau
 */
public class OverLoggingPage extends APropertyPage {	
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();

	//button for detecting casted exception 
	private Button detectTransExBtn;
	private Button log4jBtn;
	private Button javaUtillogBtn;
	private Button extraRuleBtn;
	
	//code template area for caller
	private StyledText callerTemplate;
	//code template area for callee
	private StyledText calleeTemplate;

	private TemplateText calleeText = new TemplateText("", false);
	private TemplateText callerText = new TemplateText("", false);

	private String callee, calleeOrg, calleeTrans;
	private String callerHead, callerOrg, callerTrans, callerTail;

	private SmellSettings smellSettings;
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
		
	public OverLoggingPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings) {
		super(composite, page);

		initailState();

		this.smellSettings = smellSettings;
		
		//add page content
		addFirstSection(composite);
	}

	private void initailState() {
		//TODO "throws RuntimeException" in callee, should use "throw FileNotFoundException" to replace 
		/// CalleeTemplate content ///
		//first half of template code
		callee = 	"public void A() throws RuntimeException {\n" +
				 	"\ttry {\n" +
				 	"\t// Do Something\n" +
				 	"\t} catch (FileNotFoundException e) {\n" +
				 	"\t\tlogger.info(e);	//OverLogging\n";

		//After half of template code(unchecked)
		calleeOrg = "\t\tthrow e;\n" +
					"\t}\n" +
				 	"}";

		//After half of template code(checked)
		calleeTrans = "\t\tthrow new RuntimeException(e);	//Transform Exception Type\n" +
		 			 "\t}\n" +
		 			 "}";

		/// CallerTemplate content ///
		//first half of template code
		callerHead = "public void B() {\n" +
					"\ttry {\n" +
					"\t\tA();\t\t\t//call method A\n";

		//middle part of template code(unchecked)
		callerOrg = "\t} catch (FileNotFoundException e) {\n";

		//middle part of template code(checked)
		callerTrans = "\t} catch (RuntimeException e) { //Catch Transform Exception Type\n";

		//rest part of template code
		callerTail = "\t\tlogger.info(e);\t//use log\n" +
					"\t}\n" +
					"}";
	}
	
	/**
	 * add element on page
	 * @param overLoggingPage
	 */
	private void addFirstSection(final Composite overLoggingPage) {
		libMap = smellSettings.getSmellPatterns(SmellSettings.SMELL_OVERLOGGING);
		final Label detectSettingsLabel = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel.setText(resource.getString("detect.rule"));
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();

		detectTransExBtn = new Button(overLoggingPage, SWT.CHECK);
		detectTransExBtn.setText(resource.getString("cast.exception"));
		detectTransExBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(detectSettingsLabel).y + 5);
		detectTransExBtn.pack();
		detectTransExBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				adjustText();
				adjustFont(overLoggingPage.getDisplay());
			}
		});
		detectTransExBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION));

		final Label detectSettingsLabel2 = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel2.setText(resource.getString("customize.rule"));
		detectSettingsLabel2.setLocation(getLowerRightCoordinate(detectTransExBtn).x + 85, 11);
		detectSettingsLabel2.pack();

		log4jBtn = new Button(overLoggingPage, SWT.CHECK);
		log4jBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getLowerRightCoordinate(detectSettingsLabel2).y + 5);
		log4jBtn.setText(resource.getString("detect.log4j"));
		log4jBtn.pack();
		log4jBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j));
		
		javaUtillogBtn = new Button(overLoggingPage, SWT.CHECK);
		javaUtillogBtn.setText(resource.getString("detect.logger"));
		javaUtillogBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getLowerRightCoordinate(log4jBtn).y + 5);
		javaUtillogBtn.pack();
		javaUtillogBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger));
		
		extraRuleBtn = new Button(overLoggingPage, SWT.NONE);
		extraRuleBtn.setText(resource.getString("extra.rule"));
		extraRuleBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getLowerRightCoordinate(javaUtillogBtn).y + 5);
		extraRuleBtn.pack();
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});

		final Label separateLabel1 = new Label(overLoggingPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getLowerRightCoordinate(detectTransExBtn).x+70, 5);
		separateLabel1.setSize(1, getLowerRightCoordinate(extraRuleBtn).y - 5);
		final Label separateLabel2 = new Label(overLoggingPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		separateLabel2.setLocation(5, this.getLowerRightCoordinate(extraRuleBtn).y+5);
		separateLabel2.setSize(getLowerRightCoordinate(javaUtillogBtn).x -5, 1);

		final Label callerLabel = new Label(overLoggingPage, SWT.NONE);
		callerLabel.setText(resource.getString("call.chain.example"));
		callerLabel.setLocation(detectSettingsLabel.getLocation().x, getLowerRightCoordinate(separateLabel2).y + 5);
		callerLabel.pack();
		Font templateFont = new Font(overLoggingPage.getDisplay(),"Courier New",9,SWT.NORMAL);		
		//Callee Template
		calleeTemplate = new StyledText(overLoggingPage, SWT.BORDER);
		calleeTemplate.setFont(templateFont);
		calleeTemplate.setBounds(detectSettingsLabel.getLocation().x, getLowerRightCoordinate(callerLabel).y+5, 485, 132);
		calleeTemplate.setEditable(false);
		//Caller Template
		callerTemplate = new StyledText(overLoggingPage, SWT.BORDER);
		callerTemplate.setFont(templateFont);
		callerTemplate.setBounds(detectSettingsLabel.getLocation().x, getLowerRightCoordinate(calleeTemplate).y+10, 485, 132);
		callerTemplate.setEditable(false);

		//set separateLabel1's length the same as to length of the callerLabel
		if (getLowerRightCoordinate(separateLabel2).x < 485)
			separateLabel2.setSize(485, 1);
		else {
			calleeTemplate.setSize(getLowerRightCoordinate(separateLabel2).x, 132);
			callerTemplate.setSize(getLowerRightCoordinate(separateLabel2).x, 132);
		}

		adjustText();
		adjustFont(overLoggingPage.getDisplay());
	}
	
	private void adjustText() {
		// boundary of calleeTemplate's font style
		String calleeTemp = "";

		calleeTemp += callee;
		if(!detectTransExBtn.getSelection())
			calleeTemp += calleeOrg;
		else
			calleeTemp += calleeTrans;

		calleeTemplate.setText(calleeTemp);
		calleeText.setTemplateText(calleeTemp, false);
		
		// boundary of callerTemplate's font style
		String callerTemp = "";

		callerTemp += callerHead;
		if(!detectTransExBtn.getSelection())
			callerTemp += callerOrg;
		else
			callerTemp += callerTrans;
		callerTemp += callerTail;

		callerTemplate.setText(callerTemp);
		callerText.setTemplateText(callerTemp, false);
	}
	
	/**
	 * color code
	 */
	private void adjustFont(Display display) {
		// set the boundary of calleeTemplate's font style
		calleeText.setTemplateStyle(display, 0);
		//apply font style and boundary of font style on CalleeTemplate
		calleeTemplate.setStyleRanges(calleeText.getLocationArray(), calleeText.getStyleArrray());

		// set the boundary of callerTemplate's font style
		callerText.setTemplateStyle(display, 0);
		//apply font style and boundary of font style on CallerTemplate
		callerTemplate.setStyleRanges(callerText.getLocationArray(), callerText.getStyleArrray());
	}

	@Override
	public boolean storeSettings() {
		smellSettings.removePatterns(SmellSettings.SMELL_OVERLOGGING);
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		
		if(detectTransExBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		if(log4jBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		if(javaUtillogBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		
		
		// save user defined rule
		Iterator<String> userDefinedCodeIterator = libMap.keySet().iterator();
		while(userDefinedCodeIterator.hasNext()) {
			String key = userDefinedCodeIterator.next();
			smellSettings.addOverLoggingPattern(key, libMap.get(key));
		}
		
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}
}
