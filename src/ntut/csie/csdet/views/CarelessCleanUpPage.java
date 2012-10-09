package ntut.csie.csdet.views;

import java.util.Iterator;
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
	// ��code template���ϰ�
	private StyledText templateArea;
	//�O�_�n�����ϥΪ̦ۭq��k�����s
	private Button btnIsDetectReleaseResourceCodeInClass;
	// ��������template�r������
	StyleRange[] beforeSampleStyles = new StyleRange[5];
	// ������template���r������
	StyleRange[] afterSampleStyles = new StyleRange[9];
	// code template Before detect�����e
	private String beforeText;
	// code template After detect�����e
	private String afterText;
	// ���}extraRuleDialog�����s
	private Button extraRuleBtn;
	// Library Data
	private TreeMap<String, Boolean> userDefinedCode = new TreeMap<String, Boolean>();
	// �t�d�B�zŪ�gXML
	SmellSettings smellSettings;
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public CarelessCleanUpPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings){
		super(composite,page);
		//��������,TextBox�����e
		beforeText ="FileInputStream in = null;\n" +
					"try {   \n" +
					"     in = new FileInputStream(path);\n"+
					"     // do something here\n"+
	                "     in.close(); //Careless CleanUp\n"+
					"} catch (IOException e) { \n"+
					"}";
		
		//������,TextBox�����e
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
		//�[�J���������e
		addFirstSection(composite);
	}

	/**
	 * �[�J���������e
	 */
	private void addFirstSection(final Composite CarelessCleanUpPage){
		userDefinedCode = smellSettings.getSemllPatterns(SmellSettings.SMELL_CARELESSCLEANUP);
		
		// ��������
		final Label detectSettingsLabel = new Label(CarelessCleanUpPage, SWT.NONE);
		detectSettingsLabel.setText("��������(���İ���,�����Ĥ�����):");
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//Release Method Button
		btnIsDetectReleaseResourceCodeInClass = new Button(CarelessCleanUpPage,SWT.CHECK);
		btnIsDetectReleaseResourceCodeInClass.setText("�t�~��������귽���{���X�O�_�b�禡��");
		btnIsDetectReleaseResourceCodeInClass.setLocation(
				detectSettingsLabel.getLocation().x + 10,
				getBoundsPoint(detectSettingsLabel).y + 5);
		btnIsDetectReleaseResourceCodeInClass.pack();
		btnIsDetectReleaseResourceCodeInClass.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e1){
				//���U���s�ӧ���Text��r�M�C��
				adjustText();
				adjustFont();
			}
		});

		/// Customize Rule ///
		final Label detectSettingsLabel2 = new Label(CarelessCleanUpPage, SWT.NONE);
		detectSettingsLabel2.setText("�ۦ�w�q��������:");
		detectSettingsLabel2.setLocation(getBoundsPoint(btnIsDetectReleaseResourceCodeInClass).x+25, 10);
		detectSettingsLabel2.pack();
		//Open Dialog Button
		extraRuleBtn = new Button(CarelessCleanUpPage, SWT.NONE);
		extraRuleBtn.setText("�}��");
		extraRuleBtn.setLocation(detectSettingsLabel2.getLocation().x+5, getBoundsPoint(detectSettingsLabel2).y+5);
		extraRuleBtn.pack();
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),userDefinedCode);
				dialog.open();
				userDefinedCode = dialog.getLibMap();
			}
		});
		//�Y�n����,�Nbutton����,�ç���TextBox����r�M�C��
		btnIsDetectReleaseResourceCodeInClass.setSelection(
			smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD));

		/// ���j�u ///
		final Label separateLabel1 = new Label(CarelessCleanUpPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getBoundsPoint(btnIsDetectReleaseResourceCodeInClass).x+10, 5);
		separateLabel1.setSize(1, getBoundsPoint(btnIsDetectReleaseResourceCodeInClass).y+5);
		final Label separateLabel2 = new Label(CarelessCleanUpPage,SWT.SEPARATOR | SWT.HORIZONTAL);
		separateLabel2.setLocation(10, getBoundsPoint(extraRuleBtn).y+10);
		separateLabel2.setSize(getBoundsPoint(detectSettingsLabel2).x-10, 1);
		
		/// Template Label ///
		final Label detBeforeLbl = new Label(CarelessCleanUpPage, SWT.NONE);
		detBeforeLbl.setText("�����d��:");
		detBeforeLbl.setLocation(10, getBoundsPoint(separateLabel2).y+10);
		detBeforeLbl.pack();
		//TextBox
		templateArea = new StyledText(CarelessCleanUpPage, SWT.BORDER);
		Font font = new Font(CarelessCleanUpPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setLocation(10, getBoundsPoint(detBeforeLbl).y+5);
		templateArea.setSize(458, 300);
		templateArea.setEditable(false);
		templateArea.setText(beforeText);
		
		//���j�u�PTemplate����(���̪���)
		if (getBoundsPoint(separateLabel2).x < 458)
			separateLabel2.setSize(458, 1);
		else
			templateArea.setSize(getBoundsPoint(separateLabel2).x, 300);
		
		//���J�w�w���r���B�C��
		addBeforeSampleStyle(CarelessCleanUpPage.getDisplay());	
		addAfterSampleStyle(CarelessCleanUpPage.getDisplay());
		
		//�վ�TextBox����r
		adjustText();

		//�վ�{���X���C��
		adjustFont();
	}
	
	/**
	 * �վ�TextBox����r
	 */
	private void adjustText(){
		String temp=beforeText;
		
		if(btnIsDetectReleaseResourceCodeInClass.getSelection())
			temp=afterText;
		
		templateArea.setText(temp);
	}
	
	/**
	 * �N�{���X���i��|�Ψ쪺�r���B�C�������J(������)
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
		// ����
		beforeSampleStyles[2] = new StyleRange();
		beforeSampleStyles[2].fontStyle = SWT.ITALIC;
		beforeSampleStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// ����
		beforeSampleStyles[3] = new StyleRange();
		beforeSampleStyles[3].fontStyle = SWT.ITALIC;
		beforeSampleStyles[3].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// catch
		beforeSampleStyles[4] = new StyleRange();
		beforeSampleStyles[4].fontStyle = SWT.BOLD;
		beforeSampleStyles[4].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
	}
	
	/**
	 * �N�{���X���i��|�Ψ쪺�r���B�C�������J(�n����)
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
		// ����
		afterSampleStyles[2] = new StyleRange();
		afterSampleStyles[2].fontStyle = SWT.ITALIC;
		afterSampleStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// ����
		afterSampleStyles[3] = new StyleRange();
		afterSampleStyles[3].fontStyle = SWT.ITALIC;
		afterSampleStyles[3].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// ����
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
		// ����
		afterSampleStyles[8] = new StyleRange();
		afterSampleStyles[8].fontStyle = SWT.ITALIC;
		afterSampleStyles[8].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
	}
	
	/**
	 * �վ�TextBox����r���r���M�C��
	 */
	private void adjustFont(){
		//�YBotton�S���Q�Ŀ�
		if(!btnIsDetectReleaseResourceCodeInClass.getSelection()){
			//��������,Template���r�����檺��m�d��
			int[] beforeRange=new int[]{21,4,27,3,75,23,115,19,137,5};
			//���o��������template�r������
			StyleRange[] beforeStyles=new StyleRange[5];
			for(int i=0;i<beforeSampleStyles.length;i++){
				beforeStyles[i]=beforeSampleStyles[i];
			}
			//��r��������M���檺�d��M�ΦbTemplate�W
			templateArea.setStyleRanges(beforeRange, beforeStyles);
		}
		
		//�YBotton���Q�Ŀ�
		if(btnIsDetectReleaseResourceCodeInClass.getSelection()){
			//������,Template���r�����檺��m�d��
			int[] afterRange=new int[]{21,4,27,3,75,23,104,22,143,18,164,5,192,6,199,4,236,20};
			//���o�n������template�r������
			StyleRange[] afterStyles=new StyleRange[9];
			for(int i=0;i<afterSampleStyles.length;i++){
				afterStyles[i]=afterSampleStyles[i];
			}
			//��r��������M���檺�d��M�ΦbTemplate�W
			templateArea.setStyleRanges(afterRange, afterStyles);
		}
	}
	
	/**
	 * �x�s�ϥΪ̳]�w
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	@Override
	public boolean storeSettings() {
		smellSettings.removePatterns(SmellSettings.SMELL_CARELESSCLEANUP);
		smellSettings.removeExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		
		/* 
		 * �P�_�o��Smell��Extra Rule�O���O�n�Q�����C
		 * �p�G�Q���ĴN�O�n�Q�����C
		 */
		if(btnIsDetectReleaseResourceCodeInClass.getSelection()) {
			smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		}
		
		// �s�J�ϥΪ̦ۭqRule
		Iterator<String> userDefinedCodeIterator = userDefinedCode.keySet().iterator();
		while(userDefinedCodeIterator.hasNext()) {
			String key = userDefinedCodeIterator.next();
			smellSettings.addCarelessCleanupPattern(key, userDefinedCode.get(key));
		}
		
		// �N�ɮ׼g�^
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}
}
