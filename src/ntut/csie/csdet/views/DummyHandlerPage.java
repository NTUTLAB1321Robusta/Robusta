package ntut.csie.csdet.views;

import java.io.File;
import java.util.List;
import java.util.Iterator;
import java.util.TreeMap;
import ntut.csie.csdet.preference.JDomUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
	// �O�_�n����System.out.println() and print()�����s
	private Button sysoBtn;
	// �O�_�n����e.printStackTrace��button
	private Button eprintBtn;
	// �O�_�n����log4j��button
	private Button log4jBtn;
	// �O�_�n����java.util.logging��button
	private Button javaUtillogBtn;
	// �w�]��template���r������
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
	// ���}extraRuleDialog�����s
	private Button extraRuleBtn;
	// Library Data
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();
	
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
		if(docJDom != null) {
			//�qXML��Ū�X���e���]�w
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.DummyHandlerTag) != null) {
				Element rule = root.getChild(JDomUtil.DummyHandlerTag).getChild("rule");
				eprint = rule.getAttribute(JDomUtil.eprintstacktrace).getValue();
				setting = rule.getAttribute(JDomUtil.systemoutprint).getValue();
				log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
				javaLogSet = rule.getAttribute(JDomUtil.java_Logger).getValue();
				//�qXML���o�~��Library
				Element libRule = root.getChild(JDomUtil.DummyHandlerTag).getChild("librule");
				List<Attribute> libRuleList = libRule.getAttributes();
				
				//��ϥΪ̩��x�s��Library�]�w�s��Map��Ƹ�
				for (int i=0;i<libRuleList.size();i++) {
					//��EH_STAR���N���Ÿ�"*"
					String temp = libRuleList.get(i).getQualifiedName().replace("EH_STAR", "*");
					libMap.put(temp,libRuleList.get(i).getValue().equals("Y"));
				}
			}
		}

		/// �w�]��������  ///
		final Label detectSettingsLabel = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel.setText("��������(���İ���,�����Ĥ�����):");
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//�O�_����e.printStackTrace�����s
		eprintBtn = new Button(dummyHandlerPage, SWT.CHECK);
		eprintBtn.setText("e.printStackTrace();");
		eprintBtn.setLocation(detectSettingsLabel.getLocation().x+10, getBoundsPoint(detectSettingsLabel).y+5);
		eprintBtn.pack();
		eprintBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//���U���s�ӧ���Text��r�M�C��
				adjustText();
				adjustFont();
			}
		});
		//�O�_����System.out.print�����s
		sysoBtn = new Button(dummyHandlerPage, SWT.CHECK);
		sysoBtn.setText("System.out.print();");
		sysoBtn.setLocation(detectSettingsLabel.getLocation().x+10, getBoundsPoint(eprintBtn).y+5);
		sysoBtn.pack();
		sysoBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//���U���s�ӧ���Text��r�M�C��
				adjustText();
				adjustFont();
			}
		});
		//�O�_����Log4j�����s
		log4jBtn = new Button(dummyHandlerPage, SWT.CHECK);
		log4jBtn.setText("Detect using org.apache.log4j");
		log4jBtn.setLocation(detectSettingsLabel.getLocation().x+10, getBoundsPoint(sysoBtn).y+5);
		log4jBtn.pack();
		log4jBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//���U���s�ӧ���Text��r�M�C��
				adjustText();
				adjustFont();
			}
		});
		//�O�_����JavaUtillog�����s
		javaUtillogBtn = new Button(dummyHandlerPage, SWT.CHECK);
		javaUtillogBtn.setText("Detect using java.util.logging.Logger");
		javaUtillogBtn.setLocation(detectSettingsLabel.getLocation().x+10, getBoundsPoint(log4jBtn).y+5);
		javaUtillogBtn.pack();
		javaUtillogBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//���U���s�ӧ���Text��r�M�C��
				adjustText();
				adjustFont();
			}
		});

		/// Customize Rule ///
		final Label detectSettingsLabel2 = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel2.setText("�ۦ�w�q��������:");
		detectSettingsLabel2.setLocation(getBoundsPoint(javaUtillogBtn).x+43, 10);
		detectSettingsLabel2.pack();
		//Customize Rule Button
		extraRuleBtn = new Button(dummyHandlerPage, SWT.NONE);
		extraRuleBtn.setText("�}��");
		extraRuleBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getBoundsPoint(detectSettingsLabel2).y+5);
		extraRuleBtn.pack();
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});

		/// ���j�u ///
		final Label separateLabel1 = new Label(dummyHandlerPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getBoundsPoint(javaUtillogBtn).x+28, 5);
		separateLabel1.setSize(1, getBoundsPoint(javaUtillogBtn).y-5);
		final Label separateLabel2 = new Label(dummyHandlerPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		separateLabel2.setLocation(10, getBoundsPoint(javaUtillogBtn).y+5);
		separateLabel2.setSize(getBoundsPoint(detectSettingsLabel2).x, 1);

		/// Template ///
		final Label codeTemplateLabel = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel.setText("�����d��:");
		codeTemplateLabel.setLocation(10, getBoundsPoint(separateLabel2).y+10);
		codeTemplateLabel.pack();
		//Detect Template
		templateArea = new StyledText(dummyHandlerPage, SWT.BORDER);
		Font font = new Font(dummyHandlerPage.getDisplay(),"Courier New", 14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setLocation(10, getBoundsPoint(codeTemplateLabel).y+5);
		templateArea.setSize(458, 263);
		templateArea.setEditable(false);

		//���j�u�PTemplate����(���̪���)
		if (getBoundsPoint(separateLabel2).x < 458)
			separateLabel2.setSize(458, 1);
		else
			templateArea.setSize(getBoundsPoint(separateLabel2).x, 263);

		//�զ��ϥΪ̤��e�ҳ]�w
		if(eprint.equals("Y"))
			eprintBtn.setSelection(true);
		if(setting.equals("Y"))
			sysoBtn.setSelection(true);
		if(log4jSet.equals("Y"))
			log4jBtn.setSelection(true);
		if(javaLogSet.equals("Y"))
			javaUtillogBtn.setSelection(true);
		
		//���J�w�w���r���B�C��
		addSampleStyle(dummyHandlerPage.getDisplay());

		//�վ�Text����r
		adjustText();

		//�վ�{���X���C��
		adjustFont();
	}
	
	/**
	 * ���oControl���k�U���y��
	 * @param control
	 * @return			�k�U���y��
	 */
	private Point getBoundsPoint(Control control) {
		if (control == null) return new Point(0,0);
		return new Point(control.getBounds().x + control.getBounds().width ,
						 control.getBounds().y + control.getBounds().height);
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
		//�N�s�ت�tag�[�i�h
		dummyHandler.addContent(rule);
		dummyHandler.addContent(libRule);

		if (root.getChild(JDomUtil.DummyHandlerTag) != null)
			root.removeChild(JDomUtil.DummyHandlerTag);

		root.addContent(dummyHandler);

		//�N�ɮ׼g�^
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
