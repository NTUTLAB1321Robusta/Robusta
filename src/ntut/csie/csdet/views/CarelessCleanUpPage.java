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
	
	// 打開extraRuleDialog的按鈕
	private Button extraRuleBtn;
	
	// Library Data
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();
	
	public CarelessCleanUpPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		//不偵測時,TextBox的內容
		beforeText =	"FileInputStream in = null;\n" +
						"try {   \n" +
						"     in = new FileInputStream(path);\n"+
						"     // do something here\n"+
		                "     in.close(); //Careless CleanUp\n"+
						"} catch (IOException e) { \n"+
						"}";
		
		//偵測時,TextBox的內容
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
	
	/**
	 * 加入頁面的內容
	 */
	private void addFirstSection(final Composite CarelessCleanUpPage){
		Document docJDom = JDomUtil.readXMLFile();
		String methodSet="";
		if(docJDom != null){
			//從XML裡讀出之前的設定
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.CarelessCleanUpTag) != null) {
				Element rule=root.getChild(JDomUtil.CarelessCleanUpTag).getChild("rule");
				methodSet = rule.getAttribute(JDomUtil.detUserMethod).getValue();
			
				//從XML取得外部Library
				Element libRule = root.getChild(JDomUtil.CarelessCleanUpTag).getChild("librule");
				List<Attribute> libRuleList = libRule.getAttributes();
				
				//把使用者所儲存的Library設定存到Map資料裡
				for (int i=0;i<libRuleList.size();i++)
				{
					//把EH_STAR取代為符號"*"
					String temp = libRuleList.get(i).getQualifiedName().replace("EH_STAR", "*");
					libMap.put(temp,libRuleList.get(i).getValue().equals("Y"));
				}
			}
		}
		
		//Label
		final Label detectSettingsLabel = new Label(CarelessCleanUpPage, SWT.NONE);
		detectSettingsLabel.setText("偵測條件(打勾偵測,不打勾不偵測):");
		detectSettingsLabel.setBounds(10, 10, 210, 12);
		
		//Label
		final Label detectSettingsLabel2 = new Label(CarelessCleanUpPage, SWT.NONE);
		detectSettingsLabel2.setText("自行定義偵測條件:");
		detectSettingsLabel2.setBounds(290, 10, 210, 12);
		
		
		extraRuleBtn = new Button(CarelessCleanUpPage, SWT.NONE);
		extraRuleBtn.setText("開啟");
		extraRuleBtn.setBounds(290, 29, 94, 22);
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});
		//Botton
		detUserMethodBtn=new Button(CarelessCleanUpPage,SWT.CHECK);
		detUserMethodBtn.setText("另外偵測釋放資源的程式碼是否在函式中");
		detUserMethodBtn.setBounds(20, 28, 250, 16);
		detUserMethodBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e1){
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});
		
		//分隔線
		final Label label1 = new Label(CarelessCleanUpPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		label1.setBounds(10, 60, 472, 12);
		
		//分隔線
		final Label label2 = new Label(CarelessCleanUpPage, SWT.VERTICAL | SWT.SEPARATOR);
		label2.setBounds(270, 5, 5, 55);
		label2.setText("Label");
		
		//Label
		final Label detBeforeLbl = new Label(CarelessCleanUpPage, SWT.NONE);
		detBeforeLbl.setText("偵測範例:");
		detBeforeLbl.setBounds(10, 75, 96, 12);
		
		//TextBox
		templateArea = new StyledText(CarelessCleanUpPage, SWT.BORDER);
		Font font = new Font(CarelessCleanUpPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setBounds(10, 95, 458, 300);
		templateArea.setEditable(false);
		templateArea.setText(beforeText);
		
		//載入預定的字型、顏色
		addBeforeSampleStyle(CarelessCleanUpPage.getDisplay());	
		addAfterSampleStyle(CarelessCleanUpPage.getDisplay());
		
		//調整TextBox的文字
		adjustFont();
		
		//以下Line124~128必須寫在這裡,不然會出錯
		//若要偵測,將botton打勾,並改變TextBox的文字和顏色
		if(methodSet.equals("Y")){
			detUserMethodBtn.setSelection(true);
			adjustText();
			adjustFont();
		}
	}
	
	/**
	 * 調整TextBox的文字
	 */
	private void adjustText(){
		String temp=beforeText;
		
		if(detUserMethodBtn.getSelection())
			temp=afterText;
		
		templateArea.setText(temp);
	}
	
	/**
	 * 將程式碼中可能會用到的字型、顏色先行載入(不偵測)
	 * @param display
	 */
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
	
	/**
	 * 將程式碼中可能會用到的字型、顏色先行載入(要偵測)
	 * @param display
	 */
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
	
	/**
	 * 調整TextBox的文字的字型和顏色
	 */
	private void adjustFont(){
		//若Botton沒有被勾選
		if(!detUserMethodBtn.getSelection()){
			//不偵測時,Template的字型風格的位置範圍
			int[] beforeRange=new int[]{21,4,27,3,75,23,115,19,137,5};
			//取得不偵測的template字型風格
			StyleRange[] beforeStyles=new StyleRange[5];
			for(int i=0;i<beforeSampleStyles.length;i++){
				beforeStyles[i]=beforeSampleStyles[i];
			}
			//把字型的風格和風格的範圍套用在Template上
			templateArea.setStyleRanges(beforeRange, beforeStyles);
		}
		
		//若Botton有被勾選
		if(detUserMethodBtn.getSelection()){
			//偵測時,Template的字型風格的位置範圍
			int[] afterRange=new int[]{21,4,27,3,75,23,104,22,143,18,164,5,192,6,199,4,236,20};
			//取得要偵測的template字型風格
			StyleRange[] afterStyles=new StyleRange[9];
			for(int i=0;i<afterSampleStyles.length;i++){
				afterStyles[i]=afterSampleStyles[i];
			}
			//把字型的風格和風格的範圍套用在Template上
			templateArea.setStyleRanges(afterRange, afterStyles);
		}
	}
	
	/**
	 * 儲存使用者設定
	 */
	@Override
	public boolean storeSettings() {
		//取得xml的root
		Element root=JDomUtil.createXMLContent();
		
		//建立CarelessCleanUp的tag
		Element CarelessCleanUp = new Element(JDomUtil.CarelessCleanUpTag);
		Element rule = new Element("rule");
		
		//假如detUserMethod有被勾選起來
		if(detUserMethodBtn.getSelection()){
			rule.setAttribute(JDomUtil.detUserMethod,"Y");
		}else{
			rule.setAttribute(JDomUtil.detUserMethod,"N");
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
		
		//將新建的tag加進去
		CarelessCleanUp.addContent(rule);
		CarelessCleanUp.addContent(libRule);
		
		//將Careless CleanUp的Tag加入至XML中
		if(root.getChild(JDomUtil.CarelessCleanUpTag)!=null)
			root.removeChild(JDomUtil.CarelessCleanUpTag);
		root.addContent(CarelessCleanUp);
		
		//將檔案寫回
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
		return true;
	}
}
