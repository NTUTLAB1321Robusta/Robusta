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
	//CalleeTamplate�����e
	private String calleeText;
	//CallerTamplate�����e
	private String callerHead;
	private String callerEnd;
	//Exception���૬�BException���૬
	private String callerEx, callerTransEx;
	//�w�]��CalleeTemplate���r������
	private StyleRange[] calleeStyles = new StyleRange[9];
	//�w�]��CallerTemplate���r������
	private StyleRange[] callerStyles = new StyleRange[7];

	public OverLoggingPage(Composite composite, CSPropertyPage page) {
		super(composite, page);
		//CalleeTemplate�{���X�����e
		calleeText = "public void A() throws FileNotFoundException {\n" +
		 			 "\ttry {\n" +
		 			 "\t\t// Do Something\n" +
		 			 "\t} catch (FileNotFoundException e) {\n" +
					 "\t\tlogger.info(e);	//OverLogging\n" +
					 "\t\tthrow e;\n" +
					 "\t}\n" +
					 "}";

		//CallerTemplate�{���X�����e
		callerHead = "public void B() {\n" +
					 "\ttry {\n" +
					 "\t\tA();\t\t\t//call method A\n";
		callerEx = "\t} catch (FileNotFoundException e) {\n";
		callerTransEx = "\t} catch (IOException e) { //Catch Homogeneous Excpetion\n";
		callerEnd =	"\t\tlogger.info(e);\n" +
					"\t}\n" +
					"}";

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
				adjustFont();
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

		//���J�w�w���r���B�C��
		addSampleStyle(overLoggingPage.getDisplay());

		//�վ�Text����r
		adjustText();

		//�վ�{���X���C��
		adjustFont();
	}
	
	/**
	 * �վ�Text����r
	 */
	private void adjustText()
	{
		String callerText = callerHead;
		//�]���O�_���U�M�wcaller template����r���e
		if(detectTransExBtn.getSelection())
			callerText += callerTransEx;
		else
			callerText += callerEx;
		callerText += callerEnd;

		//�]�wTemplate�����e
		callerTemplate.setText(callerText);
		calleeTemplate.setText(calleeText);
	}

	/**
	 * �N�{���X�����i��|�Ψ쪺�r���B�C������J
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
		// ����
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
		// ����
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
		// ����
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
		// ����
		callerStyles[5] = new StyleRange();
		callerStyles[5].fontStyle = SWT.ITALIC;
		callerStyles[5].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		//log
		callerStyles[6] = new StyleRange();
		callerStyles[6].fontStyle = SWT.ITALIC;
		callerStyles[6].foreground = display.getSystemColor(SWT.COLOR_BLUE);
	}
	
	/**
	 * �N�{���X����Try ,catch,out�ФW�C��
	 */
	private void adjustFont(){
		//CalleeTemplate���r�����檺��m�d��
		int[] calleeRange = new int[] {0,11,16,6,48,3,56,15,75,5,82,21,111,6,127,13,143,5};
		//��r��������M���檺�d��M�ΦbCalleeTemplate�W
		calleeTemplate.setStyleRanges(calleeRange, calleeStyles);

		//Caller Template�W�b���r�����檺��m�d��
		int[] upper = new int[] {0,11,19,3,34,15,53,5};
		//Caller Template�U�b���r�����檺��m�d��
		int[] lower;

		//�ثe��r����
		int textLength;
		//�p�Ge.printStack�ﶵ�Q�襤
		if (detectTransExBtn.getSelection()) {
			textLength = callerHead.length() + callerTransEx.length();
			lower = new int[] {60,11,77,29,textLength+2,6};
		} else {
			textLength = callerHead.length() + callerEx.length();
			//�Y�S����֤@��`��
			lower = new int[] {60,21,textLength+2,6};
		}

		//ranges���r�����檺��m�d��A�ھ�spaceSize�ӨM�w�ݭn�h�֪Ŷ�
		int[] callerRanges = new int [upper.length + lower.length];

		//ranges��index
		int range_i = 0;
		//�t�mcallerRanges�r�����檺��m�d��
		for (int i = 0; i < upper.length; i++)
			callerRanges[range_i++] = upper[i];
		for (int i = 0; i < lower.length; i++)
			callerRanges[range_i++] = lower[i];

		int size = callerRanges.length / 2;
		//�r��������A�ھ�spaceSize�ӨM�w�ݭn�h�֪Ŷ�
		StyleRange[] styles = new StyleRange[size];
		//�⥻��(try catch)��r���r������M��������m�s�J
		for (int i = 0, style_i = 0; i < 7; i++) {
			//�Y�S����exceptionBtn�A�]�֤@��`�ѡA�ҥH�ٲ��@�ӭ���
			if (size == 6 && i == 5)
				continue;
			styles[style_i++] = callerStyles[i];
		}
		//��r��������M���檺�d��M�ΦbTemplate�W
		callerTemplate.setStyleRanges(callerRanges, styles);
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
