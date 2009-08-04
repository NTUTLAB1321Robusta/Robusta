package ntut.csie.csdet.views;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.JDomUtil;

import org.apache.commons.lang.ArrayUtils;
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
	//CalleeTamplate的內容 和 字型風格
	private List<String> calleeText = new ArrayList<String>(),
						 calleeOrgText = new ArrayList<String>(),
						 calleeTransText = new ArrayList<String>();
	private List<Integer> calleeType = new ArrayList<Integer>(),
						  calleeOrgType = new ArrayList<Integer>(),
						  calleeTransType = new ArrayList<Integer>();
	//CallerTamplate的內容 和 字型風格
	private List<String> callerHeadText = new ArrayList<String>(),
						 callerTailText = new ArrayList<String>(),
						 callerOrgText = new ArrayList<String>(),
						 callerTransText = new ArrayList<String>();
	private List<Integer> callerHeadType = new ArrayList<Integer>(),
						  callerTailType = new ArrayList<Integer>(),
						  callerOrgType = new ArrayList<Integer>(),
						  callerTransType = new ArrayList<Integer>();
		
	public OverLoggingPage(Composite composite, CSPropertyPage page) {
		super(composite, page);

		/// CalleeTemplate程式碼的內容 ///
		//文字前半段
		String callee = "public void A() throws RuntimeException {\n" +
				 		"\ttry {\n" +
				 		"\t// Do Something\n" +
				 		"\t} catch (FileNotFoundException e) {\n" +
				 		"\t\tlogger.info(e);	//OverLogging\n";
		parserText(callee, calleeText, calleeType);
		//文字後半段(選項打勾前)
		String calleeOrg = "\t\tthrow e;\n" +
						   "\t}\n" +
				 		   "}";
		parserText(calleeOrg, calleeOrgText, calleeOrgType);
		//文字後半段(選項打勾後)
		String calleeTrans = "\t\tthrow new RuntimeException(e);	//Transform Exception Type\n" +
				 			 "\t}\n" +
				 			 "}";
		parserText(calleeTrans, calleeTransText, calleeTransType);

		/// CallerTemplate程式碼的內容 ///
		//文字前段
		String callerHead = "public void B() {\n" +
						"\ttry {\n" +
						"\t\tA();\t\t\t//call method A\n";
		parserText(callerHead, callerHeadText, callerHeadType);
		//文字中段(選項打勾前)
		String callerOrg = "\t} catch (FileNotFoundException e) {\n";
		parserText(callerOrg, callerOrgText, callerOrgType);
		//文字中段(選項打勾後)
		String callerTrans = "\t} catch (RuntimeException e) { //Catch Transform Exception Type\n";
		parserText(callerTrans, callerTransText, callerTransType);
		//文字後段
		String callerTail = "\t\tlogger.info(e);\t//use log\n" +
							"\t}\n" +
							"}";
		parserText(callerTail, callerTailText, callerTailType);

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
				adjustFont(overLoggingPage.getDisplay());
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
		for (int i = 0; i < calleeText.size();i++)
			calleeTemp += calleeText.get(i);
		if(!detectTransExBtn.getSelection())
			for (int i = 0; i < calleeOrgText.size();i++)
				calleeTemp += calleeOrgText.get(i);
		else
			for (int i = 0; i < calleeTransText.size();i++)
				calleeTemp += calleeTransText.get(i);
		//設定Template的內容
		calleeTemplate.setText(calleeTemp);
		
		/// CallerTemplate的字型風格的位置範圍 ///
		String callerTemp = "";
		for (int i = 0; i < callerHeadText.size();i++)
			callerTemp += callerHeadText.get(i);
		if(!detectTransExBtn.getSelection())
			for (int i = 0; i < callerOrgText.size();i++)
				callerTemp += callerOrgText.get(i);
		else
			for (int i = 0; i < callerTransText.size();i++)
				callerTemp += callerTransText.get(i);
		for (int i = 0; i < callerTailText.size();i++)
			callerTemp += callerTailText.get(i);
		//設定Template的內容
		callerTemplate.setText(callerTemp);
	}
	
	/**
	 * 將程式碼中的文字標上顏色
	 */
	private void adjustFont(Display display) {
		int counter = 0;
		List<StyleRange> styleList  = new ArrayList<StyleRange>();
		List<Integer> rangeList = new ArrayList<Integer>();
		/// 設置CalleeTemplate的字型風格的位置範圍 ///
		counter = setTemplateStyle(display,styleList,rangeList,calleeText,calleeType,counter);
		//若選項沒有勾選時
		if(!detectTransExBtn.getSelection())
			counter = setTemplateStyle(display,styleList,rangeList,calleeOrgText,calleeOrgType,counter);
		else
			counter = setTemplateStyle(display,styleList,rangeList,calleeTransText,calleeTransType,counter);

		//將List轉成Array
		StyleRange[] calleeStyles = styleList.toArray(new StyleRange[styleList.size()]);
		Integer[] rangeInt = rangeList.toArray(new Integer[rangeList.size()]);
		//將Integer Array 轉成 int Array
		int[] calleeRanges = ArrayUtils.toPrimitive(rangeInt);

		//把字型的風格和風格的範圍套用在CalleeTemplate上
		calleeTemplate.setStyleRanges(calleeRanges, calleeStyles);


		counter = 0;
		styleList.clear();
		rangeList.clear();
		/// 設置CalleeTemplate的字型風格的位置範圍 ///
		counter = setTemplateStyle(display,styleList,rangeList,callerHeadText,callerHeadType,counter);
		//若選項沒有勾選時
		if(!detectTransExBtn.getSelection())
			counter = setTemplateStyle(display,styleList,rangeList,callerOrgText,callerOrgType,counter);
		else
			counter = setTemplateStyle(display,styleList,rangeList,callerTransText,callerTransType,counter);

		counter = setTemplateStyle(display,styleList,rangeList,callerTailText,callerTailType,counter);

		//將List轉成Array
		StyleRange[] callerStyles = styleList.toArray(new StyleRange[styleList.size()]);
		Integer[] callerRangeInt = rangeList.toArray(new Integer[rangeList.size()]);
		//將Integer Array 轉成 int Array
		int[] callerRanges = ArrayUtils.toPrimitive(callerRangeInt);

		//把字型的風格和風格的範圍套用在CallerTemplate上
		callerTemplate.setStyleRanges(callerRanges, callerStyles);
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
