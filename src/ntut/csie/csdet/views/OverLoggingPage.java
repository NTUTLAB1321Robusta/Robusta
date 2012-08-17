package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.TreeMap;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * OverLogging Setting����
 * @author Shiau
 */
public class OverLoggingPage extends APropertyPage {	
	//Detect Logging��Rule
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();

	//Exception�Y���૬���~�򰻴���button
	private Button detectTransExBtn;
	//�O�_�n����log4j��button
	private Button log4jBtn;
	//�O�_�n����java.util.logging��button
	private Button javaUtillogBtn;
	//���}extraRuleDialog�����s
	private Button extraRuleBtn;
	
	//��code template���ϰ�
	private StyledText callerTemplate;
	//��code template���ϰ�
	private StyledText calleeTemplate;

	//CalleeTamplate�MCallerTamplate�����e �M �r������
	TemplateText calleeText = new TemplateText("", false);
	TemplateText callerText = new TemplateText("", false);

	//CalleeTamplate�MCallerTamplate�����e
	String callee, calleeOrg, calleeTrans;
	String callerHead, callerOrg, callerTrans, callerTail;
	// �t�d�B�zŪ�gXML
	SmellSettings smellSettings;
		
	public OverLoggingPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings) {
		super(composite, page);

		initailState();

		this.smellSettings = smellSettings;
		
		//�[�J���������e
		addFirstSection(composite);
	}

	private void initailState() {
		//TODO callee�e�b��throws RuntimeException�A�n�令Throw FileNotFoundException
		/// CalleeTemplate�{���X�����e ///
		//��r�e�b�q
		callee = 	"public void A() throws RuntimeException {\n" +
				 	"\ttry {\n" +
				 	"\t// Do Something\n" +
				 	"\t} catch (FileNotFoundException e) {\n" +
				 	"\t\tlogger.info(e);	//OverLogging\n";

		//��r��b�q(�ﶵ���īe)
		calleeOrg = "\t\tthrow e;\n" +
					"\t}\n" +
				 	"}";

		//��r��b�q(�ﶵ���ī�)
		calleeTrans = "\t\tthrow new RuntimeException(e);	//Transform Exception Type\n" +
		 			 "\t}\n" +
		 			 "}";

		/// CallerTemplate�{���X�����e ///
		//��r�e�q
		callerHead = "public void B() {\n" +
					"\ttry {\n" +
					"\t\tA();\t\t\t//call method A\n";

		//��r���q(�ﶵ���īe)
		callerOrg = "\t} catch (FileNotFoundException e) {\n";

		//��r���q(�ﶵ���ī�)
		callerTrans = "\t} catch (RuntimeException e) { //Catch Transform Exception Type\n";

		//��r��q
		callerTail = "\t\tlogger.info(e);\t//use log\n" +
					"\t}\n" +
					"}";
	}
	
	/**
	 * �[�J���������e
	 * @param overLoggingPage
	 */
	private void addFirstSection(final Composite overLoggingPage) {
		libMap = smellSettings.getSemllPatterns(SmellSettings.SMELL_OVERLOGGING);
		
		/// ��������Label ///
		final Label detectSettingsLabel = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel.setText("��������G");
		detectSettingsLabel.setLocation(10, 10);
		detectSettingsLabel.pack();
		//�O�_�Y���૬�����������s
		detectTransExBtn = new Button(overLoggingPage, SWT.CHECK);
		detectTransExBtn.setText("Excpetion�૬���~�򰻴�");
		detectTransExBtn.setLocation(detectSettingsLabel.getLocation().x+10, getBoundsPoint(detectSettingsLabel).y + 5);
		detectTransExBtn.pack();
		detectTransExBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//���U���s�ӧ���Text��r�M�C��
				adjustText();
				adjustFont(overLoggingPage.getDisplay());
			}
		});
		detectTransExBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION));

		/// Customize�w�q�������� Label ///
		final Label detectSettingsLabel2 = new Label(overLoggingPage, SWT.NONE);
		detectSettingsLabel2.setText("�ۦ�w�q��������:");
		detectSettingsLabel2.setLocation(getBoundsPoint(detectTransExBtn).x + 85, 11);
		detectSettingsLabel2.pack();
		//�O�_����Log4j�����s
		log4jBtn = new Button(overLoggingPage, SWT.CHECK);
		log4jBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getBoundsPoint(detectSettingsLabel2).y + 5);
		log4jBtn.setText("Detect using org.apache.log4j");
		log4jBtn.pack();
		log4jBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j));
		
		//�O�_����JavaUtillog�����s
		javaUtillogBtn = new Button(overLoggingPage, SWT.CHECK);
		javaUtillogBtn.setText("Detect using java.util.logging.Logger");
		javaUtillogBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getBoundsPoint(log4jBtn).y + 5);
		javaUtillogBtn.pack();
		javaUtillogBtn.setSelection(smellSettings.isExtraRuleExist(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger));
		
		//Customize Rule Button
		extraRuleBtn = new Button(overLoggingPage, SWT.NONE);
		extraRuleBtn.setText("�}��");
		extraRuleBtn.setLocation(detectSettingsLabel2.getLocation().x+10, getBoundsPoint(javaUtillogBtn).y + 5);
		extraRuleBtn.pack();
		extraRuleBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ExtraRuleDialog dialog = new ExtraRuleDialog(new Shell(),libMap);
				dialog.open();
				libMap = dialog.getLibMap();
			}
		});

		/// ���j�u ///
		final Label separateLabel1 = new Label(overLoggingPage, SWT.VERTICAL | SWT.SEPARATOR);
		separateLabel1.setLocation(getBoundsPoint(detectTransExBtn).x+70, 5);
		separateLabel1.setSize(1, getBoundsPoint(extraRuleBtn).y - 5);
		final Label separateLabel2 = new Label(overLoggingPage,SWT.SEPARATOR| SWT.HORIZONTAL);
		separateLabel2.setLocation(5, this.getBoundsPoint(extraRuleBtn).y+5);
		separateLabel2.setSize(getBoundsPoint(javaUtillogBtn).x -5, 1);

		/// Template ///
		final Label callerLabel = new Label(overLoggingPage, SWT.NONE);
		callerLabel.setText("Call Chain Example:");
		callerLabel.setLocation(detectSettingsLabel.getLocation().x, getBoundsPoint(separateLabel2).y + 5);
		callerLabel.pack();
		Font templateFont = new Font(overLoggingPage.getDisplay(),"Courier New",9,SWT.NORMAL);		
		//Callee Template
		calleeTemplate = new StyledText(overLoggingPage, SWT.BORDER);
		calleeTemplate.setFont(templateFont);
		calleeTemplate.setBounds(detectSettingsLabel.getLocation().x, getBoundsPoint(callerLabel).y+5, 485, 132);
		calleeTemplate.setEditable(false);
		//Caller Template
		callerTemplate = new StyledText(overLoggingPage, SWT.BORDER);
		callerTemplate.setFont(templateFont);
		callerTemplate.setBounds(detectSettingsLabel.getLocation().x, getBoundsPoint(calleeTemplate).y+10, 485, 132);
		callerTemplate.setEditable(false);

		//���j�u�PTemplate����(���̪���)
		if (getBoundsPoint(separateLabel2).x < 485)
			separateLabel2.setSize(485, 1);
		else {
			calleeTemplate.setSize(getBoundsPoint(separateLabel2).x, 132);
			callerTemplate.setSize(getBoundsPoint(separateLabel2).x, 132);
		}

		//�վ�Text����r
		adjustText();
		//�վ�{���X���C��
		adjustFont(overLoggingPage.getDisplay());
	}
	
	/**
	 * �վ�Text����r
	 */
	private void adjustText() {
		/// CalleeTemplate���r�����檺��m�d�� ///
		String calleeTemp = "";

		calleeTemp += callee;
		if(!detectTransExBtn.getSelection())
			calleeTemp += calleeOrg;
		else
			calleeTemp += calleeTrans;

		//�]�wTemplate�����e
		calleeTemplate.setText(calleeTemp);
		calleeText.setTemplateText(calleeTemp, false);
		
		/// CallerTemplate���r�����檺��m�d�� ///
		String callerTemp = "";

		callerTemp += callerHead;
		if(!detectTransExBtn.getSelection())
			callerTemp += callerOrg;
		else
			callerTemp += callerTrans;
		callerTemp += callerTail;

		//�]�wTemplate�����e
		callerTemplate.setText(callerTemp);
		callerText.setTemplateText(callerTemp, false);
	}
	
	/**
	 * �N�{���X������r�ФW�C��
	 */
	private void adjustFont(Display display) {
		/// �]�mCalleeTemplate���r�����檺��m�d�� ///
		calleeText.setTemplateStyle(display, 0);
		//��r��������M���檺�d��M�ΦbCalleeTemplate�W
		calleeTemplate.setStyleRanges(calleeText.getLocationArray(), calleeText.getStyleArrray());

		/// �]�mCalleeTemplate���r�����檺��m�d�� ///
		callerText.setTemplateStyle(display, 0);
		//��r��������M���檺�d��M�ΦbCallerTemplate�W
		callerTemplate.setStyleRanges(callerText.getLocationArray(), callerText.getStyleArrray());
	}

	/**
	 * �x�s�ϥΪ̳]�w
	 */
	@Override
	public boolean storeSettings() {
		smellSettings.removePatterns(SmellSettings.SMELL_OVERLOGGING);
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		
		if(detectTransExBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		if(log4jBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		if(javaUtillogBtn.getSelection())
			smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		
		
		// �s�J�ϥΪ̦ۭqRule
		Iterator<String> userDefinedCodeIterator = libMap.keySet().iterator();
		while(userDefinedCodeIterator.hasNext()) {
			String key = userDefinedCodeIterator.next();
			smellSettings.addOverLoggingPattern(key, libMap.get(key));
		}
		
		//�N�ɮ׼g�^
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}
}
