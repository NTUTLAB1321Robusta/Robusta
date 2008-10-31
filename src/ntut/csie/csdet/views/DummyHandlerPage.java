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
import org.jdom.Element;


public class DummyHandlerPage extends APropertyPage{
	//放code template的區域
	private StyledText templateArea;
	//code template的內容
	private String templateText;
	//使否要捕捉System.out.println()的按鈕
	private Button systemoutprinteButton;
	//使否要捕捉e.printStackTrace()的按鈕
	private Button eprintButton;
	
	public DummyHandlerPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		//程式碼的內容
		templateText = "try {   \n" +
		               "  \n"+
	 	               "} catch (Exception e) { \n" +
	                   "    System.out.println(e);\n" +
		               "    e.printStackTrace();\n"+
		               "}";
		//加入頁面的內容
		addFirstSection(composite);
	}
	
	private void addFirstSection(Composite dummyHandlerPage){
	    systemoutprinteButton = new Button(dummyHandlerPage, SWT.CHECK);
		systemoutprinteButton.setText("System.out.print();");
		systemoutprinteButton.setBounds(20, 28, 123, 16);

		final Label detectSettingsLabel = new Label(dummyHandlerPage, SWT.NONE);
		detectSettingsLabel.setText("Detect Settings");
		detectSettingsLabel.setBounds(10, 10, 77, 12);

		eprintButton = new Button(dummyHandlerPage, SWT.CHECK);
		eprintButton.setText("System.out.println();");
		eprintButton.setBounds(20, 50, 123, 16);

		final Label label = new Label(dummyHandlerPage, SWT.SEPARATOR| SWT.HORIZONTAL);
		label.setBounds(10, 72, 472, 12);

		templateArea = new StyledText(dummyHandlerPage, SWT.BORDER);
		Font font = new Font(dummyHandlerPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setText(templateText);
		templateArea.setBounds(10, 115, 440, 177);

		final Label codeTemplateLabel = new Label(dummyHandlerPage, SWT.NONE);
		codeTemplateLabel.setText("Code Template:");
		codeTemplateLabel.setBounds(10, 90, 96, 12);
		//調整程式碼的顏色
		adjustFont(dummyHandlerPage.getDisplay());
	}
	
	/**
	 * 將程式碼中的Try ,catch,out標上顏色
	 * @param display
	 */
	private void adjustFont(Display display){
		StyleRange[] styles = new StyleRange[3];
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
		
		int[] ranges = new int[] {0,3,14,5,42,3};
		templateArea.setStyleRanges(ranges, styles);
	}


	@Override
	public boolean storeSettings() {
		//取的xml的root
		Element root = JDomUtil.createXMLContent();
		//建立dummyhandler的tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//假如system.out.println有被勾選起來
		if(systemoutprinteButton.getSelection()){
			rule.setAttribute(JDomUtil.systemoutprint,"Y");	
		}else{
			rule.setAttribute(JDomUtil.systemoutprint,"N");
		}
		//假如e.printStackTrace()有被勾選起來
		if(eprintButton.getSelection()){
			rule.setAttribute(JDomUtil.printStackTrace, "Y");
		}else{
			rule.setAttribute(JDomUtil.printStackTrace, "N");
		}
		//將新建的tag加進去
		dummyHandler.addContent(rule);
		root.addContent(dummyHandler);
		//將檔案寫回
		String path = JDomUtil.getProjectPath()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
