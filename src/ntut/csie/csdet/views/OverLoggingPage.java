package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;

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
	private TemplateText calleeText = new TemplateText("", false);
	private TemplateText callerText = new TemplateText("", false);

	//CalleeTamplate和CallerTamplate的內容
	private String callee, calleeOrg, calleeTrans;
	private String callerHead, callerOrg, callerTrans, callerTail;
	// 負責處理讀寫XML
	private SmellSettings smellSettings;
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
		
	public OverLoggingPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings) {
		super(composite, page);

		initailState();

		this.smellSettings = smellSettings;
		
		//加入頁面的內容
		addFirstSection(composite);
	}

	private void initailState() {
		//TODO callee前半部throws RuntimeException，要改成Throw FileNotFoundException
		/// CalleeTemplate程式碼的內容 ///
		//文字前半段
		callee = 	"public void A() throws RuntimeException {\n" +
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
	}
	
	/**
	 * 加入頁面的內容
	 * @param overLoggingPage
	 */
	private void addFirstSection(final Composite overLoggingPage) {
		libMap = smellSettings.getSemllPatterns(SmellSettings.SMELL_OVERLOGGING);
		// 偵測條件Label
		final Label detectSettingsLabel = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel.setText(resource.getString("detect.rule"));
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//是否即使轉型仍偵測的按鈕
		detectTransExBtn = new Button(overLoggingPage, SWT.CHECK);
		detectTransExBtn.setText(resource.getString("cast.exception"));
		detectTransExBtn.setLocation(detectSettingsLabel.getLocation().x+10, getLowerRightCoordinate(detectSettingsLabel).y + 5);
		detectTransExBtn.pack();
		detectTransExBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont(overLoggingPage.getDisplay());
			}
		});
		detectTransExBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION));

		/// Customize定義偵測條件 Label ///
		final Label detectSettingsLabel2 = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel2.setText(resource.getString("customize.rule"));
		detectSettingsLabel2.setLocation(getLowerRightCoordinate(detectTransExBtn).x + 85, 11);
		detectSettingsLabel2.pack();
		//是否偵測Log4j的按鈕
		log4jBtn = new Button(overLoggingPage, SWT.CHECK);
		log4jBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getLowerRightCoordinate(detectSettingsLabel2).y + 5);
		log4jBtn.setText(resource.getString("detect.log4j"));
		log4jBtn.pack();
		log4jBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j));
		
		//是否偵測JavaUtillog的按鈕
		javaUtillogBtn = new Button(overLoggingPage, SWT.CHECK);
		javaUtillogBtn.setText(resource.getString("detect.logger"));
		javaUtillogBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getLowerRightCoordinate(log4jBtn).y + 5);
		javaUtillogBtn.pack();
		javaUtillogBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger));
		
		//Customize Rule Button
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

		/// 分隔線 ///
		final Label separateLabel1 = new Label(overLoggingPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getLowerRightCoordinate(detectTransExBtn).x+70, 5);
		separateLabel1.setSize(1, getLowerRightCoordinate(extraRuleBtn).y - 5);
		final Label separateLabel2 = new Label(overLoggingPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		separateLabel2.setLocation(5, this.getLowerRightCoordinate(extraRuleBtn).y+5);
		separateLabel2.setSize(getLowerRightCoordinate(javaUtillogBtn).x -5, 1);

		/// Template ///
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

		//分隔線與Template等長(取最長的)
		if (getLowerRightCoordinate(separateLabel2).x < 485)
			separateLabel2.setSize(485, 1);
		else {
			calleeTemplate.setSize(getLowerRightCoordinate(separateLabel2).x, 132);
			callerTemplate.setSize(getLowerRightCoordinate(separateLabel2).x, 132);
		}

		//調整Text的文字
		adjustText();
		//調整程式碼的顏色
		adjustFont(overLoggingPage.getDisplay());
	}
	
	/**
	 * 調整Text的文字
	 */
	private void adjustText() {
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
		
		
		// 存入使用者自訂Rule
		Iterator<String> userDefinedCodeIterator = libMap.keySet().iterator();
		while(userDefinedCodeIterator.hasNext()) {
			String key = userDefinedCodeIterator.next();
			smellSettings.addOverLoggingPattern(key, libMap.get(key));
		}
		
		//將檔案寫回
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}
}
