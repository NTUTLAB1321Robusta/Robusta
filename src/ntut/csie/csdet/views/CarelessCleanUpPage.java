package ntut.csie.csdet.views;

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
import org.jdom.Element;

public class CarelessCleanUpPage  extends APropertyPage {
	// 放code template的區域
	private StyledText templateArea;
	
	//是否要偵測使用者自訂方法的按鈕
	private Button detUserMethodBtn;
	
	// 不偵測的template字型風格
	StyleRange[] beforeSampleStyles = new StyleRange[5];
	
	// 偵測的template的字型風格
	StyleRange[] afterSampleStyles = new StyleRange[9];
	
	// code template Before detect的內容
	private String beforeText;
	
	// code template After detect的內容
	private String afterText;
	
	public CarelessCleanUpPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		
		beforeText =	"FileInputStream in = null;\n" +
						"try {   \n" +
						"     in = new FileInputStream(path);\n"+
						"     // do something here\n"+
		                "     in.close(); //Careless CleanUp\n"+
						"} catch (IOException e) { \n"+
						"}";
		
		afterText =		"FileInputStream in = null;\n" +
						"try {   \n" +
		                "     in = new FileInputStream(path);\n"+
						"     // do something here\n" +
						"     //check method content\n" +
						"     close(in); //Careless CleanUp\n"+
						"} catch (IOException e) { \n"+
						"}\n\n"+
						"public void close(FileInputStream in){\n"+
			            "     // close stream here\n"+
			            "     in.close(); \n"+
			            "}";

		//加入頁面的內容
		addFirstSection(composite);
	}
	
	private void addFirstSection(final Composite CarelessCleanUpPage){
//		Document docJDom = JDomUtil.readXMLFile();
//		String xxxClose="";
//		if(docJDom != null){
//			//從XML裡讀出之前的設定
//			Element root = docJDom.getRootElement();
//			if (root.getChild(JDomUtil.detUserMethod) != null) {
//				Element rule=root.getChild(JDomUtil.detUserMethod);
//				xxxClose = rule.getAttribute(JDomUtil.detUserMethod).getValue();
//			}
//		}
		final Label detectSettingsLabel = new Label(CarelessCleanUpPage, SWT.NONE);
		detectSettingsLabel.setText("偵測條件(打勾偵測,不打勾不偵測):");
		detectSettingsLabel.setBounds(10, 10, 210, 12);
		
		detUserMethodBtn=new Button(CarelessCleanUpPage,SWT.CHECK);
		detUserMethodBtn.setText("另外偵測釋放資源的程式碼是否在函式中");
		detUserMethodBtn.setBounds(20, 28, 230, 16);
		detUserMethodBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e1){
				addText();
				adjustFont();
			}
		});
		
		final Label label1 = new Label(CarelessCleanUpPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		label1.setBounds(10, 50, 472, 12);
		
		final Label detBeforeLbl = new Label(CarelessCleanUpPage, SWT.NONE);
		detBeforeLbl.setText("偵測範例:");
		detBeforeLbl.setBounds(10, 62, 96, 12);
		
		
		templateArea = new StyledText(CarelessCleanUpPage, SWT.BORDER);
		Font font = new Font(CarelessCleanUpPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setBounds(10, 80, 458, 300);
		templateArea.setEditable(false);
		templateArea.setText(beforeText);
		
		//載入預定的字型、顏色
		addBeforeSampleStyle(CarelessCleanUpPage.getDisplay());	
		addAfterSampleStyle(CarelessCleanUpPage.getDisplay());
		
		//調整程式碼的顏色
		adjustFont();
	}
	private void addText(){
		String temp=beforeText;
		
		if(detUserMethodBtn.getSelection())
			temp=afterText;
		
		templateArea.setText(temp);
	}
	
	private void addBeforeSampleStyle(Display display){
		// null
		beforeSampleStyles[0] = new StyleRange();
		beforeSampleStyles[0].fontStyle = SWT.BOLD;
		beforeSampleStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// try
		beforeSampleStyles[1] = new StyleRange();
		beforeSampleStyles[1].fontStyle = SWT.BOLD;
		beforeSampleStyles[1].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// 註解
		beforeSampleStyles[2] = new StyleRange();
		beforeSampleStyles[2].fontStyle = SWT.ITALIC;
		beforeSampleStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// 註解
		beforeSampleStyles[3] = new StyleRange();
		beforeSampleStyles[3].fontStyle = SWT.ITALIC;
		beforeSampleStyles[3].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// catch
		beforeSampleStyles[4] = new StyleRange();
		beforeSampleStyles[4].fontStyle = SWT.BOLD;
		beforeSampleStyles[4].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		
	}
	
	private void addAfterSampleStyle(Display display){
		// null
		afterSampleStyles[0] = new StyleRange();
		afterSampleStyles[0].fontStyle = SWT.BOLD;
		afterSampleStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// try
		afterSampleStyles[1] = new StyleRange();
		afterSampleStyles[1].fontStyle = SWT.BOLD;
		afterSampleStyles[1].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);		
		// 註解
		afterSampleStyles[2] = new StyleRange();
		afterSampleStyles[2].fontStyle = SWT.ITALIC;
		afterSampleStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// 註解
		afterSampleStyles[3] = new StyleRange();
		afterSampleStyles[3].fontStyle = SWT.ITALIC;
		afterSampleStyles[3].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// 註解
		afterSampleStyles[4] = new StyleRange();
		afterSampleStyles[4].fontStyle = SWT.ITALIC;
		afterSampleStyles[4].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// catch
		afterSampleStyles[5] = new StyleRange();
		afterSampleStyles[5].fontStyle = SWT.BOLD;
		afterSampleStyles[5].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// public
		afterSampleStyles[6] = new StyleRange();
		afterSampleStyles[6].fontStyle = SWT.BOLD;
		afterSampleStyles[6].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// void
		afterSampleStyles[7] = new StyleRange();
		afterSampleStyles[7].fontStyle = SWT.BOLD;
		afterSampleStyles[7].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// 註解
		afterSampleStyles[8] = new StyleRange();
		afterSampleStyles[8].fontStyle = SWT.ITALIC;
		afterSampleStyles[8].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);

	}
	private void adjustFont(){
		
		if(!detUserMethodBtn.getSelection()){
			int[] beforeRange=new int[]{21,4,27,3,75,23,115,19,137,5};
			
			StyleRange[] beforeStyles=new StyleRange[5];
			
			for(int i=0;i<beforeSampleStyles.length;i++){
				beforeStyles[i]=beforeSampleStyles[i];
			}
			templateArea.setStyleRanges(beforeRange, beforeStyles);
		}
		
		
		if(detUserMethodBtn.getSelection()){
			
			int[] afterRange=new int[]{21,4,27,3,75,23,104,22,143,18,164,5,192,6,199,4,236,20};
			StyleRange[] afterStyles=new StyleRange[9];
		
			for(int i=0;i<afterSampleStyles.length;i++){
				afterStyles[i]=afterSampleStyles[i];
			}
			templateArea.setStyleRanges(afterRange, afterStyles);

			
		}
	}
	
	public boolean storeSettings() {
		Element root=JDomUtil.createXMLContent();
		
		//建立CarelessCleanUp的tag
		Element carelessCleanUp = new Element(JDomUtil.detUserMethod);
		Element rule = new Element("rule");
		return true;
	}
}
