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
import org.jdom.Document;
import org.jdom.Element;

public class CarelessCleanUpPage  extends APropertyPage {
	// ��code template���ϰ�
	private StyledText templateArea;
	
	//�O�_�n�����ϥΪ̦ۭq��k�����s
	private Button detUserMethodBtn;
	
	// ��������template�r������
	StyleRange[] beforeSampleStyles = new StyleRange[4];
	
	// ������template���r������
	StyleRange[] afterSampleStyles = new StyleRange[6];
	
	// code template Before detect�����e
	private String beforeText;
	
	// code template After detect�����e
	private String afterText;
	
	public CarelessCleanUpPage(Composite composite,CSPropertyPage page){
		super(composite,page);
		
		beforeText =	"try {   \n" +
						"     FileOutputStream fos=new FileOutputStream(\"file.txt\");\n"+
						"     // do something here\n"+
		                "     fos.close(); //This is Careless CleanUp!!\n"+
						"} catch (IOException e) { \n"+
						"}";
		afterText =		"try {   \n" +
		                "     FileOutputStream fos=new FileOutputStream(\"file.txt\");\n"+
						"     // do something here\n" +
						"     close(fos); //Parse this method to find Careless CleanUp!!\n"+
						"} catch (IOException e) { \n"+
						"}\n\n"+
						"public void close(FileOutputStream fos){\n"+
			            "     // do something here\n"+
			            "     fos.close(); //This is Careless CleanUp!!\n"+
			            "}";

		//�[�J���������e
		addFirstSection(composite);
	}
	
	private void addFirstSection(final Composite CarelessCleanUpPage){
//		Document docJDom = JDomUtil.readXMLFile();
//		String xxxClose="";
//		if(docJDom != null){
//			//�qXML��Ū�X���e���]�w
//			Element root = docJDom.getRootElement();
//			if (root.getChild(JDomUtil.detUserMethod) != null) {
//				Element rule=root.getChild(JDomUtil.detUserMethod);
//				xxxClose = rule.getAttribute(JDomUtil.detUserMethod).getValue();
//			}
//		}
		final Label detectSettingsLabel = new Label(CarelessCleanUpPage, SWT.NONE);
		detectSettingsLabel.setText("��������(���İ���,�����Ĥ�����):");
		detectSettingsLabel.setBounds(10, 10, 210, 12);
		
		detUserMethodBtn=new Button(CarelessCleanUpPage,SWT.CHECK);
		detUserMethodBtn.setText("�����ۭq��k����close()");
		detUserMethodBtn.setBounds(20, 28, 200, 16);
		detUserMethodBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e1){
				addText();
				adjustFont();
			}
		});
		
		final Label label1 = new Label(CarelessCleanUpPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		label1.setBounds(10, 50, 670, 10);
		
		final Label detBeforeLbl = new Label(CarelessCleanUpPage, SWT.NONE);
		detBeforeLbl.setText("�����d��:");
		detBeforeLbl.setBounds(10, 62, 96, 12);
		
		
		templateArea = new StyledText(CarelessCleanUpPage, SWT.BORDER);
		Font font = new Font(CarelessCleanUpPage.getDisplay(),"Courier New",14,SWT.NORMAL);		
		templateArea.setFont(font);
		templateArea.setBounds(10, 80, 700, 250);
		templateArea.setEditable(false);
		templateArea.setText(beforeText);
		
		//���J�w�w���r���B�C��
		addBeforeSampleStyle(CarelessCleanUpPage.getDisplay());	
		addAfterSampleStyle(CarelessCleanUpPage.getDisplay());
		
		//�վ�{���X���C��
		adjustFont();
	}
	private void addText(){
		String temp=beforeText;
		
		if(detUserMethodBtn.getSelection())
			temp=afterText;
		
		templateArea.setText(temp);
	}
	
	private void addBeforeSampleStyle(Display display){
		//Try
		beforeSampleStyles[0] = new StyleRange();
		beforeSampleStyles[0].fontStyle = SWT.BOLD;
		beforeSampleStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// ����
		beforeSampleStyles[1] = new StyleRange();
		beforeSampleStyles[1].fontStyle = SWT.ITALIC;
		beforeSampleStyles[1].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// ����
		beforeSampleStyles[2] = new StyleRange();
		beforeSampleStyles[2].fontStyle = SWT.ITALIC;
		beforeSampleStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		//catch
		beforeSampleStyles[3] = new StyleRange();
		beforeSampleStyles[3].fontStyle = SWT.BOLD;
		beforeSampleStyles[3].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		
	}
	
	private void addAfterSampleStyle(Display display){
		//Try
		afterSampleStyles[0] = new StyleRange();
		afterSampleStyles[0].fontStyle = SWT.BOLD;
		afterSampleStyles[0].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// ����
		afterSampleStyles[1] = new StyleRange();
		afterSampleStyles[1].fontStyle = SWT.ITALIC;
		afterSampleStyles[1].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		
		// ����
		afterSampleStyles[2] = new StyleRange();
		afterSampleStyles[2].fontStyle = SWT.ITALIC;
		afterSampleStyles[2].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		//catch
		afterSampleStyles[3] = new StyleRange();
		afterSampleStyles[3].fontStyle = SWT.BOLD;
		afterSampleStyles[3].foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		// ����
		afterSampleStyles[4] = new StyleRange();
		afterSampleStyles[4].fontStyle = SWT.ITALIC;
		afterSampleStyles[4].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		// ����
		afterSampleStyles[5] = new StyleRange();
		afterSampleStyles[5].fontStyle = SWT.ITALIC;
		afterSampleStyles[5].foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
	}
	private void adjustFont(){
		
		if(!detUserMethodBtn.getSelection()){
			int[] beforeRange=new int[]{0,3,74,20,112,29,144,5};
			
			StyleRange[] beforeStyles=new StyleRange[4];
			
			for(int i=0;i<beforeSampleStyles.length;i++){
				beforeStyles[i]=beforeSampleStyles[i];
			}
			templateArea.setStyleRanges(beforeRange, beforeStyles);
		}
		
		
		if(detUserMethodBtn.getSelection()){
			
			int[] afterRange=new int[]{0,3,74,20,111,47,161,5,235,20,274,29};
			StyleRange[] afterStyles=new StyleRange[6];
		
			for(int i=0;i<afterSampleStyles.length;i++){
				afterStyles[i]=afterSampleStyles[i];
			}
					templateArea.setStyleRanges(afterRange, afterStyles);

			
		}
	}
	
	public boolean storeSettings() {
		Element root=JDomUtil.createXMLContent();
		
		//�إ�CarelessCleanUp��tag
		Element carelessCleanUp = new Element(JDomUtil.detUserMethod);
		Element rule = new Element("rule");
		return true;
	}
}
