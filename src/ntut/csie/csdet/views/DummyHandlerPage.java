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
 * ��user�w�q�@��²�檺detect rule
 * @author chewei
 */
public class DummyHandlerPage extends APropertyPage{
	// ��code template���ϰ�
	private StyledText templateArea;
	// ��code template���ϰ�
	// �O�_�n����System.out.println() and print()�����s
	private Button sysoBtn;
	// �O�_�n����e.printStackTrace��button
	private Button eprintBtn;
	// �O�_�n����log4j��button
	private Button log4jBtn;
	// �O�_�n����java.util.logging��button
	private Button javaUtillogBtn;
	//
	StyleRange[] sampleStyles = new StyleRange[9];
	// code template�e�b�����e
	private String mainText;
	//�@code template������"}"
	private String endText;
	// system.out.println��button���r��
	private String sysoText;
	// e.print��button���r��
	private String eprintText;
	// log4j��button���r��
	private String log4jText;
	// java.util.logging���r��
	private String javaUtillogText;
	// ���}extraLibDialog�����s
	private Button extraLibBtn;
	// ���}extraStDialog�����s
	private Button extraStBtn;

	// Library Data
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();
	// Statement Data
	private TreeMap<String, Boolean> stMap = new TreeMap<String, Boolean>();
	
	public DummyHandlerPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		//�{���X�����e
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

		//�[�J���������e
		addFirstSection(composite);
	}
	
	private void addFirstSection(final Composite dummyHandlerPage){
		Document docJDom = JDomUtil.readXMLFile();
		String eprint = "";
		String setting = "";
		String log4jSet = "";
		String javaLogSet = "";
		if(docJDom != null){
			//�qXML��Ū�X���e���]�w
			Element root = docJDom.getRootElement();
			Element rule = root.getChild(JDomUtil.DummyHandlerTag).getChild("rule");
			eprint = rule.getAttribute(JDomUtil.eprintstacktrace).getValue();
			setting = rule.getAttribute(JDomUtil.systemoutprint).getValue();
			log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			javaLogSet = rule.getAttribute(JDomUtil.java_Logger).getValue();

			//�qXML���o�~��Library
			Element libRule = root.getChild(JDomUtil.DummyHandlerTag).getChild("librule");
			List<Attribute> libRuleList = libRule.getAttributes();
			//�qXML���o�~��Statement
			Element stRule = root.getChild(JDomUtil.DummyHandlerTag).getChild("strule");
			List<Attribute> stRuleList = stRule.getAttributes();
			
			//��ϥΪ̩��x�s��Library�]�w�s��Map��Ƹ�
			for (int i=0;i<libRuleList.size();i++)
				libMap.put(libRuleList.get(i).getQualifiedName(),libRuleList.get(i).getValue().equals("Y"));
			//��ϥΪ̩��x�s��Statement�]�w�s��Map��Ƹ�
			for (int i=0;i<stRuleList.size();i++)
				stMap.put(stRuleList.get(i).getQualifiedName(),stRuleList.get(i).getValue().equals("Y"));
		}

		final Label detectSettingsLabel = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel.setText("��������(���İ���,�����Ĥ�����):");
		detectSettingsLabel.setBounds(10, 10, 210, 12);
		
		final Label detectSettingsLabel2 = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel2.setText("�����~��Statement����:");
		detectSettingsLabel2.setBounds(251, 11, 210, 12);
		
		extraLibBtn = new Button(dummyHandlerPage, SWT.NONE);
		extraLibBtn.setText("�}��");
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
		detectSettingsLabel3.setText("�����~��Library����:");
		
		extraStBtn = new Button(dummyHandlerPage, SWT.NONE);
		extraStBtn.setText("�}��");
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
				//���U���s�ӧ���Text��r�M�C��
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
				//���U���s�ӧ���Text��r�M�C��
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
				//���U���s�ӧ���Text��r�M�C��
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
				//���U���s�ӧ���Text��r�M�C��
				adjustText();
				adjustFont();
			}
		});
		if(javaLogSet.equals("Y")) {
			javaUtillogBtn.setSelection(true);
		}
		
		final Label codeTemplateLabel = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel.setText("�����d��:");
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

		//���J�w�w���r���B�C��
		addSampleStyle(dummyHandlerPage.getDisplay());

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
	 * �N�{���X�����i��|�Ψ쪺�r���B�C������J
	 * @param display
	 */
	private void addSampleStyle(Display display) {
		//Try
		sampleStyles[0] = new StyleRange();
		sampleStyles[0].fontStyle = SWT.BOLD;
		sampleStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// ����
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
		// ����
		sampleStyles[5] = new StyleRange();
		sampleStyles[5].fontStyle = SWT.ITALIC;
		sampleStyles[5].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);		
		// log4j
		sampleStyles[6] = new StyleRange();
		sampleStyles[6].fontStyle = SWT.ITALIC;
		sampleStyles[6].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// ����
		sampleStyles[7] = new StyleRange();
		sampleStyles[7].fontStyle = SWT.ITALIC;
		sampleStyles[7].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);	
		// java.util.logging.Logger
		sampleStyles[8] = new StyleRange();
		sampleStyles[8].fontStyle = SWT.ITALIC;
		sampleStyles[8].foreground = display.getSystemColor(SWT.COLOR_BLUE);
	}
	
	/**
	 * �N�{���X����Try ,catch,out�ФW�C��
	 */
	private void adjustFont(){
		//�ثe��r����
		int textLength = mainText.length();

		//(styles�Mranges)�ݭn�t�m�h�֪Ŷ�
		int spaceSize = 6;
		if (sysoBtn.getSelection())
			spaceSize+=4;
		if (log4jBtn.getSelection())
			spaceSize+=4;
		if (javaUtillogBtn.getSelection())
			spaceSize+=4;

		//ranges���r�����檺��m�d��A�ھ�spaceSize�ӨM�w�ݭn�h�֪Ŷ�
		int[] ranges = new int[spaceSize];
		//�r��������A�ھ�spaceSize�ӨM�w�ݭn�h�֪Ŷ�
		StyleRange[] styles = new StyleRange[spaceSize/2];

		//ranges�Mstyles��index
		int range_i=0;
		int style_i=0;

		//����(try catch)��r��������m(��Ӥ@��{�_�l��m,�Ӽ�})
		int[] main = new int[] {0,3,13,15,31,5};
		//�⥻��(try catch)��r���r������M��������m�s�J
		for (int i=0;i<3;i++)
			styles[style_i++] = sampleStyles[i];
		for (int i=0;i<6;i++)
			ranges[range_i++] = main[i];

		//�p�Ge.printStack�ﶵ�Q�襤
		if (eprintBtn.getSelection())
			textLength += eprintText.length();
		//�p�GSystemOut�ﶵ�Q�襤
		if (sysoBtn.getSelection())
		{
			//SystemOut��r��������m(�۹��m+�ثe�쳹������)
			int[] syso = new int[] {11 + textLength,3,38 + textLength,3};
			//�⥻��SystemOut��r���r������M��������m�s�J
			for (int i=0;i<4;i++)
				ranges[range_i++] = syso[i];
			for (int i=3;i<5;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += sysoText.length();
		}
		//�p�GLog4j�ﶵ�Q�襤
		if (log4jBtn.getSelection())
		{
			//Log4J��r��������m(�۹��m+�ثe�쳹������)
			int[] log4j = new int[] {4+textLength,14,23+textLength,6,};
			//�⥻��Log4j��r���r������M��������m�s�J
			for (int i=0;i<4;i++)
				ranges[range_i++] = log4j[i];			
			for (int i=5;i<7;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += log4jText.length();
		}
		//�p�GJavaUtillog�ﶵ�Q�襤
		if (javaUtillogBtn.getSelection())
		{
			//javaUtillog��r��������m(�۹��m+�ثe�쳹������)
			int[] javaUtillog = new int[] {4 + textLength,33,43 + textLength,11};
			//�⥻��JavaUtillog��r���r������M��������m�s�J
			for (int i=0;i<4;i++)
				ranges[range_i++] = javaUtillog[i];
			for (int i=7;i<9;i++)
				styles[style_i++] = sampleStyles[i];
			textLength += javaUtillogText.length();
		}

		//��r��������M���檺�d��M�ΦbTemplate�W
		templateArea.setStyleRanges(ranges, styles);
	}

	@Override
	public boolean storeSettings() {

		//����xml��root
		Element root = JDomUtil.createXMLContent();
		//�إ�dummyhandler��tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//���pe.printStackTrace���Q�Ŀ�_��
		if (eprintBtn.getSelection()){
			rule.setAttribute(JDomUtil.eprintstacktrace,"Y");
		}else{
			rule.setAttribute(JDomUtil.eprintstacktrace,"N");		
		}
		//���psystem.out.println���Q�Ŀ�_��
		if(sysoBtn.getSelection()){
			rule.setAttribute(JDomUtil.systemoutprint,"Y");	
		}else{
			rule.setAttribute(JDomUtil.systemoutprint,"N");
		}
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
		//���pdummy handler���s��rule�i�H�[�b�o��
		//���p���B�~�s�W��Library rule
		Element libRule = new Element("librule");
		Iterator<String> libIt = libMap.keySet().iterator();
		while(libIt.hasNext()){
			String temp = libIt.next();
			if (libMap.get(temp))
				libRule.setAttribute(temp,"Y");
			else
				libRule.setAttribute(temp,"N");
		}
		//���p���B�~�s�W��Library rule
		Element stRule = new Element("strule");
		Iterator<String> stIt = stMap.keySet().iterator();
		while(stIt.hasNext()){
			String temp = stIt.next();
			if (stMap.get(temp))
				stRule.setAttribute(temp,"Y");
			else
				stRule.setAttribute(temp,"N");
		}
		//�N�s�ت�tag�[�i�h
		dummyHandler.addContent(rule);
		dummyHandler.addContent(libRule);
		dummyHandler.addContent(stRule);
		root.addContent(dummyHandler);
		//�N�ɮ׼g�^
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
