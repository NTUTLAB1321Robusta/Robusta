package ntut.csie.csdet.views;

import java.io.File;

import ntut.csie.csdet.preference.JDomUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.jdom.Document;
import org.jdom.Element;

/**
 * ��user�w�q�@��²�檺detect rule
 * @author chewei
 */
public class DummyHandlerPage extends APropertyPage{
	// ��code template���ϰ�
	private StyledText templateArea;
	// code template�����e
	private String templateText;
	// �O�_�n����System.out.println() and print()�����s
	private Button systemoutprintlnButton;
	// �O�_�n����log4j��button
	private Button log4jButton;
	// �O�_�n����java.util.logging��button
	private Button javaUtillogBtn;
	
	public DummyHandlerPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		//�{���X�����e
		templateText = "try {   \n" +
		               "  \n"+
	 	               "} catch (Exception e) { \n" +
	                   "    System.out.println(e);\n" +
	                   "    System.out.print(e);\n" +
		               "    e.printStackTrace();\n"+
		               "    // using log4j\n" +
		               "    logger.info(e.getMessage()"+ ");\n"+ 
		               "    // using java.util.logging.Logger \n" +
		               "    java_logger.info(e.getMessage()"+ "); \n"+ 
		               "}";
		//�[�J���������e
		addFirstSection(composite);
	}
	
	private void addFirstSection(Composite dummyHandlerPage){
		final Label detectSettingsLabel = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel.setText("Detect Settings");
		detectSettingsLabel.setBounds(10, 10, 90, 12);
		
		Document docJDom = JDomUtil.readXMLFile();
		String setting = "";
		String log4jSet = "";
		String javaLogSet = "";
		if(docJDom != null){
			Element root = docJDom.getRootElement();
			Element rule = root.getChild(JDomUtil.DummyHandlerTag).getChild("rule");
			setting = rule.getAttribute(JDomUtil.systemoutprint).getValue();
			log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			javaLogSet = rule.getAttribute(JDomUtil.java_Logger).getValue();
		}
		
		systemoutprintlnButton = new Button(dummyHandlerPage, SWT.CHECK);
		systemoutprintlnButton.setText("System.out.print();");
		systemoutprintlnButton.setBounds(20, 28, 123, 16);
		if(setting.equals("Y")){
			systemoutprintlnButton.setSelection(true);
		}

		log4jButton = new Button(dummyHandlerPage, SWT.CHECK);
		log4jButton.setText("Detect using org.apache.log4j");
		log4jButton.setBounds(20, 50, 160, 16);
		if(log4jSet.equals("Y")){
			log4jButton.setSelection(true);
		}

		javaUtillogBtn = new Button(dummyHandlerPage, SWT.CHECK);
		javaUtillogBtn.setText("Detect using java.util.logging.Logger");
		javaUtillogBtn.setBounds(20, 72, 190, 16);
		if(javaLogSet.equals("Y")){
			javaUtillogBtn.setSelection(true);
		}
		
		final Label codeTemplateLabel = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel.setText("Code Template:");
		codeTemplateLabel.setBounds(10, 100, 96, 12);

		final Label label = new Label(dummyHandlerPage, SWT.SEPARATOR| SWT.HORIZONTAL);
		label.setBounds(10, 112, 472, 12);

		templateArea = new StyledText(dummyHandlerPage, SWT.BORDER);
		Font font = new Font(dummyHandlerPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setText(templateText);
		templateArea.setBounds(10, 125, 458, 263);
		templateArea.setEditable(false);

		//�վ�{���X���C��
		adjustFont(dummyHandlerPage.getDisplay());
	}
	
	/**
	 * �N�{���X����Try ,catch,out�ФW�C��
	 * @param display
	 */
	private void adjustFont(Display display){
		StyleRange[] styles = new StyleRange[8];
		//Try
		styles[0] = new StyleRange();
		styles[0].fontStyle = SWT.BOLD;
		styles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//catch
		styles[1] = new StyleRange();
		styles[1].fontStyle = SWT.BOLD;
		styles[1].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		//out
		styles[2] = new StyleRange();
		styles[2].fontStyle = SWT.ITALIC;
		styles[2].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// out
		styles[3] = new StyleRange();
		styles[3].fontStyle = SWT.ITALIC;
		styles[3].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// ����
		styles[4] = new StyleRange();
		styles[4].fontStyle = SWT.ITALIC;
		styles[4].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);		
		// log4j
		styles[5] = new StyleRange();
		styles[5].fontStyle = SWT.ITALIC;
		styles[5].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		// ����
		styles[6] = new StyleRange();
		styles[6].fontStyle = SWT.ITALIC;
		styles[6].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);	
		// java.util.logging.Logger
		styles[7] = new StyleRange();
		styles[7].fontStyle = SWT.ITALIC;
		styles[7].foreground = display.getSystemColor(SWT.COLOR_BLUE);
		
		int[] ranges = new int[] {0,3,14,5,48,3,75,3,118,14,137,6,170,33,209,11};
		templateArea.setStyleRanges(ranges, styles);
	}


	@Override
	public boolean storeSettings() {

		//����xml��root
		Element root = JDomUtil.createXMLContent();
		//�إ�dummyhandler��tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//���psystem.out.println���Q�Ŀ�_��
		if(systemoutprintlnButton.getSelection()){
			rule.setAttribute(JDomUtil.systemoutprint,"Y");	
		}else{
			rule.setAttribute(JDomUtil.systemoutprint,"N");
		}
		
		//���plog4j���Q�Ŀ�_��
		if(log4jButton.getSelection()){
			rule.setAttribute(JDomUtil.apache_log4j,"Y");	
		}else{
			rule.setAttribute(JDomUtil.apache_log4j,"N");	
		}
		
		// ���pjava.util.logging.Logger���Q�Ŀ�_��
		if(javaUtillogBtn.getSelection()){
			rule.setAttribute(JDomUtil.java_Logger,"Y");	
		}else{
			rule.setAttribute(JDomUtil.java_Logger,"N");
		}
		//���pdummy handler���s��rule�i�H�[�b�o��
		
		//�N�s�ت�tag�[�i�h
		dummyHandler.addContent(rule);
		root.addContent(dummyHandler);
		//�N�ɮ׼g�^
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
