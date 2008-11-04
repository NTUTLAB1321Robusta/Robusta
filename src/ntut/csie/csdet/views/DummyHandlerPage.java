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
	
	public DummyHandlerPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		//�{���X�����e
		templateText = "try {   \n" +
		               "  \n"+
	 	               "} catch (Exception e) { \n" +
	                   "    System.out.println(e);\n" +
	                   "    System.out.print(e);\n" +
		               "    e.printStackTrace();\n"+
		               "}";
		//�[�J���������e
		addFirstSection(composite);
	}
	
	private void addFirstSection(Composite dummyHandlerPage){
		Document docJDom = JDomUtil.readXMLFile();
		String setting = "";
		if(docJDom != null){
			Element root = docJDom.getRootElement();
			Element rule = root.getChild(JDomUtil.DummyHandlerTag).getChild("rule");
			setting = rule.getAttribute(JDomUtil.systemoutprint).getValue();
		}
		
		systemoutprintlnButton = new Button(dummyHandlerPage, SWT.CHECK);
		systemoutprintlnButton.setText("System.out.print();");
		systemoutprintlnButton.setBounds(20, 28, 123, 16);
		if(setting.equals("Y")){
			systemoutprintlnButton.setSelection(true);
		}

		final Label detectSettingsLabel = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel.setText("Detect Settings");
		detectSettingsLabel.setBounds(10, 10, 77, 12);

		// ����o�Ӯ���
//		eprintButton = new Button(dummyHandlerPage, SWT.CHECK);
//		eprintButton.setText("System.out.println();");
//		eprintButton.setBounds(20, 50, 123, 16);

		final Label label = new Label(dummyHandlerPage, SWT.SEPARATOR| SWT.HORIZONTAL);
		label.setBounds(10, 72, 472, 12);

		templateArea = new StyledText(dummyHandlerPage, SWT.BORDER);
		Font font = new Font(dummyHandlerPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setText(templateText);
		templateArea.setBounds(10, 115, 440, 177);
		templateArea.setEditable(false);

		final Label codeTemplateLabel = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel.setText("Code Template:");
		codeTemplateLabel.setBounds(10, 90, 96, 12);
		//�վ�{���X���C��
		adjustFont(dummyHandlerPage.getDisplay());
	}
	
	/**
	 * �N�{���X����Try ,catch,out�ФW�C��
	 * @param display
	 */
	private void adjustFont(Display display){
		StyleRange[] styles = new StyleRange[4];
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
		
		int[] ranges = new int[] {0,3,14,5,48,3,75,3};
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
