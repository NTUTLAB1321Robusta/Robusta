package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

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

public class CarelessCleanUpPage  extends APropertyPage {
	// 放code template的區域
	private StyledText templateArea;
	//是否要偵測使用者自訂方法的按鈕
	private Button btnIsDetectReleaseResourceCodeInClass;
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
	private TreeMap<String, Boolean> userDefinedCode = new TreeMap<String, Boolean>();
	// 負責處理讀寫XML
	private SmellSettings smellSettings;

	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public CarelessCleanUpPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings){
		super(composite,page);
		//不偵測時,TextBox的內容
		beforeText ="FileInputStream in = null;\n" +
					"try {   \n" +
					"     in = new FileInputStream(path);\n"+
					"     // do something here\n"+
	                "     in.close(); //Careless CleanUp\n"+
					"} catch (IOException e) { \n"+
					"}";
		
		//偵測時,TextBox的內容
		afterText =	"FileInputStream in = null;\n" +
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

		this.smellSettings = smellSettings;
		//加入頁面的內容
		addFirstSection(composite);
	}

	/**
	 * 加入頁面的內容
	 */
	private void addFirstSection(final Composite CarelessCleanUpPage){
		userDefinedCode = smellSettings.getSemllPatterns(SmellSettings.SMELL_CARELESSCLEANUP);
		// 偵測條件
		final Label detectSettingsLabel = new Label(CarelessCleanUpPage, SWT.NONE);
		detectSettingsLabel.setText(resource.getString("detect.rule"));
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//Release Method Button
		btnIsDetectReleaseResourceCodeInClass = new Button(CarelessCleanUpPage,SWT.CHECK);
		btnIsDetectReleaseResourceCodeInClass.setText(resource.getString("detect.release.resource.in.method"));
		btnIsDetectReleaseResourceCodeInClass.setLocation(
				detectSettingsLabel.getLocation().x + 10,
				getLowerRightCoordinate(detectSettingsLabel).y + 5);
		btnIsDetectReleaseResourceCodeInClass.pack();
		btnIsDetectReleaseResourceCodeInClass.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e1){
				//按下按鈕而改變Text文字和顏色
				adjustText();
				adjustFont();
			}
		});

		/// Customize Rule ///
		final Label detectSettingsLabel2 = new Label(CarelessCleanUpPage, SWT.NONE);
		detectSettingsLabel2.setText(resource.getString("customize.rule"));
		detectSettingsLabel2.setLocation(getLowerRightCoordinate(btnIsDetectReleaseResourceCodeInClass).x+25, 10);
		detectSettingsLabel2.pack();
		//Open Dialog Button
		extraRuleBtn = new Button(CarelessCleanUpPage, SWT.NONE);
		extraRuleBtn.setText(resource.getString("extra.rule"));
		extraRuleBtn.setLocation(detectSettingsLabel2.getLocation().x+5, getLowerRightCoordinate(detectSettingsLabel2).y+5);
		extraRuleBtn.pack();
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),userDefinedCode);
				dialog.open();
				userDefinedCode = dialog.getLibMap();
			}
		});
		//若要偵測,將button打勾,並改變TextBox的文字和顏色
		btnIsDetectReleaseResourceCodeInClass.setSelection(
			smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD));

		/// 分隔線 ///
		final Label separateLabel1 = new Label(CarelessCleanUpPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getLowerRightCoordinate(btnIsDetectReleaseResourceCodeInClass).x+10, 5);
		separateLabel1.setSize(1, getLowerRightCoordinate(btnIsDetectReleaseResourceCodeInClass).y+5);
		final Label separateLabel2 = new Label(CarelessCleanUpPage,SWT.SEPARATOR | SWT.HORIZONTAL);
		separateLabel2.setLocation(10, getLowerRightCoordinate(extraRuleBtn).y+10);
		separateLabel2.setSize(getLowerRightCoordinate(detectSettingsLabel2).x-10, 1);
		
		/// Template Label ///
		final Label detBeforeLbl = new Label(CarelessCleanUpPage, SWT.NONE);
		detBeforeLbl.setText(resource.getString("detect.example"));
		detBeforeLbl.setLocation(10, getLowerRightCoordinate(separateLabel2).y+10);
		detBeforeLbl.pack();
		//TextBox
		templateArea = new StyledText(CarelessCleanUpPage, SWT.BORDER);
		Font font = new Font(CarelessCleanUpPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setLocation(10, getLowerRightCoordinate(detBeforeLbl).y+5);
		templateArea.setSize(458, 300);
		templateArea.setEditable(false);
		templateArea.setText(beforeText);
		
		//分隔線與Template等長(取最長的)
		if (getLowerRightCoordinate(separateLabel2).x < 458)
			separateLabel2.setSize(458, 1);
		else
			templateArea.setSize(getLowerRightCoordinate(separateLabel2).x, 300);
		
		//載入預定的字型、顏色
		addBeforeSampleStyle(CarelessCleanUpPage.getDisplay());	
		addAfterSampleStyle(CarelessCleanUpPage.getDisplay());
		
		//調整TextBox的文字
		adjustText();

		//調整程式碼的顏色
		adjustFont();
	}
	
	/**
	 * 調整TextBox的文字
	 */
	private void adjustText(){
		String temp=beforeText;
		
		if(btnIsDetectReleaseResourceCodeInClass.getSelection())
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
		//若Button沒有被勾選
		if(!btnIsDetectReleaseResourceCodeInClass.getSelection()){
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
		
		//若Button有被勾選
		if(btnIsDetectReleaseResourceCodeInClass.getSelection()){
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	@Override
	public boolean storeSettings() {
		smellSettings.removePatterns(SmellSettings.SMELL_CARELESSCLEANUP);
		smellSettings.removeExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		
		/* 
		 * 判斷這個Smell的Extra Rule是不是要被偵測。
		 * 如果被打勾就是要被偵測。
		 */
		if(btnIsDetectReleaseResourceCodeInClass.getSelection()) {
			smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		}
		
		// 存入使用者自訂Rule
		Iterator<String> userDefinedCodeIterator = userDefinedCode.keySet().iterator();
		while(userDefinedCodeIterator.hasNext()) {
			String key = userDefinedCodeIterator.next();
			smellSettings.addCarelessCleanupPattern(key, userDefinedCode.get(key));
		}
		
		// 將檔案寫回
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}
}
