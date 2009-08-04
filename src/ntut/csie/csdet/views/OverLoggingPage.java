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
 * OverLogging Setting����
 * @author Shiau
 *
 */
public class OverLoggingPage extends APropertyPage {	
	//Detect Logging��Rule
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();

	//Exception�Y���૬���~�򰻴���button
	private Button detectTransExBtn;
	//�O�_�n����log4j��button
	private Button log4jBtn;
	//�O�_�n����java.util.logging��button
	private Button javaUtillogBtn;
	//���}extraRuleDialog�����s
	private Button extraRuleBtn;
	
	//��code template���ϰ�
	private StyledText callerTemplate;
	//��code template���ϰ�
	private StyledText calleeTemplate;
	//CalleeTamplate�����e �M �r������
	private List<String> calleeText = new ArrayList<String>(),
						 calleeOrgText = new ArrayList<String>(),
						 calleeTransText = new ArrayList<String>();
	private List<Integer> calleeType = new ArrayList<Integer>(),
						  calleeOrgType = new ArrayList<Integer>(),
						  calleeTransType = new ArrayList<Integer>();
	//CallerTamplate�����e �M �r������
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

		/// CalleeTemplate�{���X�����e ///
		//��r�e�b�q
		String callee = "public void A() throws RuntimeException {\n" +
				 		"\ttry {\n" +
				 		"\t// Do Something\n" +
				 		"\t} catch (FileNotFoundException e) {\n" +
				 		"\t\tlogger.info(e);	//OverLogging\n";
		parserText(callee, calleeText, calleeType);
		//��r��b�q(�ﶵ���īe)
		String calleeOrg = "\t\tthrow e;\n" +
						   "\t}\n" +
				 		   "}";
		parserText(calleeOrg, calleeOrgText, calleeOrgType);
		//��r��b�q(�ﶵ���ī�)
		String calleeTrans = "\t\tthrow new RuntimeException(e);	//Transform Exception Type\n" +
				 			 "\t}\n" +
				 			 "}";
		parserText(calleeTrans, calleeTransText, calleeTransType);

		/// CallerTemplate�{���X�����e ///
		//��r�e�q
		String callerHead = "public void B() {\n" +
						"\ttry {\n" +
						"\t\tA();\t\t\t//call method A\n";
		parserText(callerHead, callerHeadText, callerHeadType);
		//��r���q(�ﶵ���īe)
		String callerOrg = "\t} catch (FileNotFoundException e) {\n";
		parserText(callerOrg, callerOrgText, callerOrgType);
		//��r���q(�ﶵ���ī�)
		String callerTrans = "\t} catch (RuntimeException e) { //Catch Transform Exception Type\n";
		parserText(callerTrans, callerTransText, callerTransType);
		//��r��q
		String callerTail = "\t\tlogger.info(e);\t//use log\n" +
							"\t}\n" +
							"}";
		parserText(callerTail, callerTailText, callerTailType);

		//�[�J���������e
		addFirstSection(composite);
	}
	
	/**
	 * �[�J���������e
	 * @param overLoggingPage
	 */
	private void addFirstSection(final Composite overLoggingPage){
		Document docJDom = JDomUtil.readXMLFile();
		String exSet = "";
		String log4jSet = "";
		String javaLogSet = "";
		if(docJDom != null){
			//�qXML��Ū�X���e���]�w
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.OverLoggingTag) != null) {
				Element rule = root.getChild(JDomUtil.OverLoggingTag).getChild("rule");
				log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
				javaLogSet = rule.getAttribute(JDomUtil.java_Logger).getValue();
				
				//�qXML���o�~��Library
				Element libRule = root.getChild(JDomUtil.OverLoggingTag).getChild("librule");
				List<Attribute> libRuleList = libRule.getAttributes();

				Element exrule = root.getChild(JDomUtil.OverLoggingTag).getChild("exrule");
				exSet = exrule.getAttribute(JDomUtil.transException).getValue();
				
				//��ϥΪ̩��x�s��Library�]�w�s��Map��Ƹ�
				for (int i=0;i<libRuleList.size();i++)
				{
					//��EH_STAR���N���Ÿ�"*"
					String temp = libRuleList.get(i).getQualifiedName().replace("EH_STAR", "*");
					libMap.put(temp,libRuleList.get(i).getValue().equals("Y"));
				}
			}
		}

		final Label detectSettingsLabel = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel.setText("��������:");
		detectSettingsLabel.setBounds(10, 10, 210, 12);
		
		final Label detectSettingsLabel2 = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel2.setText("�ۦ�w�q��������:");
		detectSettingsLabel2.setBounds(251, 11, 210, 12);
		
		extraRuleBtn = new Button(overLoggingPage, SWT.NONE);
		extraRuleBtn.setText("�}��");
		extraRuleBtn.setBounds(251, 73, 94, 22);
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});

		/// �O�_�Y���૬�����������s ///
		detectTransExBtn = new Button(overLoggingPage, SWT.CHECK);
		detectTransExBtn.setText("Excpetion�૬���~�򰻴�");
		detectTransExBtn.setBounds(10, 29, 159, 16);
		detectTransExBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//���U���s�ӧ���Text��r�M�C��
				adjustText();
				adjustFont(overLoggingPage.getDisplay());
			}
		});
		if (exSet.equals("Y"))
			detectTransExBtn.setSelection(true);

		/// �O�_����Log4j�����s ///
		log4jBtn = new Button(overLoggingPage, SWT.CHECK);
		log4jBtn.setBounds(251, 29, 159, 16);
		log4jBtn.setText("Detect using org.apache.log4j");
		if(log4jSet.equals("Y"))
			log4jBtn.setSelection(true);

		/// �O�_����JavaUtillog�����s ///
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

		//�վ�Text����r
		adjustText();

		//�վ�{���X���C��
		adjustFont(overLoggingPage.getDisplay());
	}
	
	/**
	 * �վ�Text����r
	 */
	private void adjustText()
	{
		/// CalleeTemplate���r�����檺��m�d�� ///
		String calleeTemp = "";
		for (int i = 0; i < calleeText.size();i++)
			calleeTemp += calleeText.get(i);
		if(!detectTransExBtn.getSelection())
			for (int i = 0; i < calleeOrgText.size();i++)
				calleeTemp += calleeOrgText.get(i);
		else
			for (int i = 0; i < calleeTransText.size();i++)
				calleeTemp += calleeTransText.get(i);
		//�]�wTemplate�����e
		calleeTemplate.setText(calleeTemp);
		
		/// CallerTemplate���r�����檺��m�d�� ///
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
		//�]�wTemplate�����e
		callerTemplate.setText(callerTemp);
	}
	
	/**
	 * �N�{���X������r�ФW�C��
	 */
	private void adjustFont(Display display) {
		int counter = 0;
		List<StyleRange> styleList  = new ArrayList<StyleRange>();
		List<Integer> rangeList = new ArrayList<Integer>();
		/// �]�mCalleeTemplate���r�����檺��m�d�� ///
		counter = setTemplateStyle(display,styleList,rangeList,calleeText,calleeType,counter);
		//�Y�ﶵ�S���Ŀ��
		if(!detectTransExBtn.getSelection())
			counter = setTemplateStyle(display,styleList,rangeList,calleeOrgText,calleeOrgType,counter);
		else
			counter = setTemplateStyle(display,styleList,rangeList,calleeTransText,calleeTransType,counter);

		//�NList�নArray
		StyleRange[] calleeStyles = styleList.toArray(new StyleRange[styleList.size()]);
		Integer[] rangeInt = rangeList.toArray(new Integer[rangeList.size()]);
		//�NInteger Array �ন int Array
		int[] calleeRanges = ArrayUtils.toPrimitive(rangeInt);

		//��r��������M���檺�d��M�ΦbCalleeTemplate�W
		calleeTemplate.setStyleRanges(calleeRanges, calleeStyles);


		counter = 0;
		styleList.clear();
		rangeList.clear();
		/// �]�mCalleeTemplate���r�����檺��m�d�� ///
		counter = setTemplateStyle(display,styleList,rangeList,callerHeadText,callerHeadType,counter);
		//�Y�ﶵ�S���Ŀ��
		if(!detectTransExBtn.getSelection())
			counter = setTemplateStyle(display,styleList,rangeList,callerOrgText,callerOrgType,counter);
		else
			counter = setTemplateStyle(display,styleList,rangeList,callerTransText,callerTransType,counter);

		counter = setTemplateStyle(display,styleList,rangeList,callerTailText,callerTailType,counter);

		//�NList�নArray
		StyleRange[] callerStyles = styleList.toArray(new StyleRange[styleList.size()]);
		Integer[] callerRangeInt = rangeList.toArray(new Integer[rangeList.size()]);
		//�NInteger Array �ন int Array
		int[] callerRanges = ArrayUtils.toPrimitive(callerRangeInt);

		//��r��������M���檺�d��M�ΦbCallerTemplate�W
		callerTemplate.setStyleRanges(callerRanges, callerStyles);
	}

	/**
	 * �x�s�ϥΪ̳]�w
	 */
	@Override
	public boolean storeSettings() {
		//����XML��root
		Element root = JDomUtil.createXMLContent();

		//�إ�OverLogging��tag
		Element overLogging = new Element(JDomUtil.OverLoggingTag);

		Element rule = new Element("rule");
		//���plog4j���Q�Ŀ�_��
		if(log4jBtn.getSelection()){
			rule.setAttribute(JDomUtil.apache_log4j,"Y");
		}else{
			rule.setAttribute(JDomUtil.apache_log4j,"N");	
		}
		//���pjava.util.logging.Logger���Q�Ŀ�_��
		if(javaUtillogBtn.getSelection()){
			rule.setAttribute(JDomUtil.java_Logger,"Y");	
		}else{
			rule.setAttribute(JDomUtil.java_Logger,"N");
		}

		//��ϥΪ̦ۭq��Rule�s�JXML
		Element libRule = new Element("librule");
		Iterator<String> libIt = libMap.keySet().iterator();
		while(libIt.hasNext()){
			String temp = libIt.next();
			//�Y���X�{*���N��EH_STAR(jdom����O"*"�o�Ӧr��)
			String libName = temp.replace("*", "EH_STAR");
			if (libMap.get(temp))
				libRule.setAttribute(libName,"Y");
			else
				libRule.setAttribute(libName,"N");
		}

		Element exRule = new Element("exrule");
		//��ϥΪ̳]�w��
		if(detectTransExBtn.getSelection()){
			exRule.setAttribute(JDomUtil.transException,"Y");
		}else{
			exRule.setAttribute(JDomUtil.transException,"N");	
		}

		//�N�s�ت�tag�[�i�h
		overLogging.addContent(rule);
		overLogging.addContent(libRule);
		overLogging.addContent(exRule);

		//�NOverLogging Tag�[�J��XML��
		if (root.getChild(JDomUtil.OverLoggingTag) != null)
			root.removeChild(JDomUtil.OverLoggingTag);
		root.addContent(overLogging);

		//�N�ɮ׼g�^
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
