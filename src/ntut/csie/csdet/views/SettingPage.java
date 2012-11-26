package ntut.csie.csdet.views;

import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jdom.Element;

/**
 * EH TOOL設定畫面
 * @author Shiau
 *
 */
public class SettingPage extends APropertyPage {
	
	private Composite mainPageComposite;
	/** 例外處理壞味道設定主畫面的Container */
	private Composite selectComposite;
	/** Frame，例外處理壞味道類型外框 */
	private Group smellTypeGroup;
	/** 例外處理壞味道清單 */
	private Table smellList;
	/** Frame，壞味道範例程式碼外框 */
	private Group templateCodeGroup;
	/** 例外處理壞味道程式碼*/
	private StyledText templateCode;
	/** Checkbox，選擇偵測所有例外處理壞味道 */
	private Button checkbox_DetectAllSmells;
	/** Checkbox，選擇是否要將壞味道程式碼反白 */
	private Button checkbox_HighlightSmellCode;
	
	private boolean isDetectingAllSmells = false;
	private boolean[] detSmellList;
	private boolean isShowWarning = false;
	
	private TemplateText[] tempText;
	private String[] descText;
	// 負責處理讀寫XML
	private SmellSettings smellSettings;
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	public SettingPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings) {
		super(composite, page);
		
		mainPageComposite = composite;
		this.smellSettings = smellSettings;
		
		initailState();
		readSetting();
		buildPage(composite);
		setUserSetting();
		
		for (int i =0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++)
			tempText[i].setTemplateStyle(composite.getDisplay(), 0);

		smellList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				//取得點選的Item
				int index = smellList.getSelectionIndex();
				changeTemplateText(index);
				changeTemplateSize();
				TableItem[] item = smellList.getItems();
				for (int i=0; i < item.length; i++) {
					if(!item[i].getChecked()) {
						isDetectingAllSmells = false;
						checkbox_DetectAllSmells.setSelection(isDetectingAllSmells);
						return;
					}
				}
			}
		});
	}

	private void initailState() {
		detSmellList = new boolean[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		for (int i =0; i < detSmellList.length; i++)
			detSmellList[i] = true;

		tempText = new TemplateText[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		String temp =	"try {\n" +
						"\t// code in here\n" +
						"} $catch (Exception e) {$\n" +
						"\n" +
						"$}$";
		tempText[0] = new TemplateText(temp, isShowWarning);
		temp =	"try {\n" +
				"\t// code in here\n" +
				"} catch (Exception e) {\n" +
				"\t// TODO Auto-generated catch block\n" +
				"$\te.printStackTrace();$\n" +
				"}";
		tempText[1] = new TemplateText(temp, isShowWarning);
		temp =	"try {\n" +
				"\tfos = new FileOutputStream(path);\n" +
				"} catch (IOException e) {\n" +
				"\tlogger.error(e.getMessage());\n" +
				"} finally {\n" +
				"\t$try {\n" +
				"\t\tfos.close();\n" +
				"\t} catch (IOException e) {\n" +
				"\t\te.printStackTrace();\n" +
				"\t}$\n" +
				"}";
		tempText[2] = new TemplateText(temp, isShowWarning);
		temp = 	"public static void main (String[] args) {\n" +
				"\t//Nothing...\n" +
				"\t$Model model = new Model();\n" +
				"\tView view = new View(model);\n" +
				"\tview.run();$\n" +
				"}";
		tempText[3] = new TemplateText(temp, isShowWarning);
		temp =	"try {\n" +
				"\tInputStream is = file.openStream();\n" +
				"\t$is.close();$\n" +
				"} catch (IOException e) {\n" +
				"\tlogger.info(e.getMessage());\n" +
				"}";
		tempText[4] = new TemplateText(temp, isShowWarning);
		temp =	"public void A() throws FileNotFoundException {\n" +
				"\ttry {\n" +
				"\t\t// Do Something\n" +
				"\t} catch (FileNotFoundException e) {\n" +
				"\t\t$logger.info(e);$	//OverLogging\n" +
				"\t\tthrow e;\n" +
				"\t}\n" +
				"}\n" +
				"public void B() {\n" +
				"\ttry {\n" +
				"\t\tA();\t//call method A\n" +
				"\t} catch (FileNotFoundException e) {\n" +
				"\t\t$logger.info(e);$	//use log\n" +
				"\t}\n" +
				"}";
		tempText[5] = new TemplateText(temp, isShowWarning);
		temp =	"public void A(String path) throws FileNotFoundException {\n" +
				"\tFileOutputStream fos = null;\n" +
				"\ttry {\n" +
				"\t\tfos = new FileOutputStream(path);\n" +
				"\t\tfos.write();\t//it throws lead exception\n" +
				"\t} catch (FileNotFoundException e) {\n" +
				"\t\tthrow e;\n" +
				"\t} finally {\n" +
				"\t\t$fos.close();$\t//it throws exception too\n" +
				"\t}\n" +
				"}\n";
				
		tempText[6] = new TemplateText(temp, isShowWarning);
		
		descText = new String[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		descText[0] = resource.getString("ignore.checked.description");
		descText[1] = resource.getString("dummy.handler.description");
		descText[2] = resource.getString("nested.try.statement.description");
		descText[3] = resource.getString("unprotected.main.program.description");
		descText[4] = resource.getString("careless.cleanup.description");
		descText[5] = resource.getString("over.logging.description");
		descText[6] = resource.getString("overwritten.lead.description");
	}

	/**
	 * 讀取使用者之前設定
	 */
	private void readSetting() {
		Element[] smellElements = new Element[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		smellElements[0] = smellSettings.getSmellType(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION);
		smellElements[1] = smellSettings.getSmellType(SmellSettings.SMELL_DUMMYHANDLER);
		smellElements[2] = smellSettings.getSmellType(SmellSettings.SMELL_NESTEDTRYBLOCK);
		smellElements[3] = smellSettings.getSmellType(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM);
		smellElements[4] = smellSettings.getSmellType(SmellSettings.SMELL_CARELESSCLEANUP);
		smellElements[5] = smellSettings.getSmellType(SmellSettings.SMELL_OVERLOGGING);
		smellElements[6] = smellSettings.getSmellType(SmellSettings.SMELL_OVERWRITTENLEADEXCEPTION);
		
		for(int i = 0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++) {
			detSmellList[i] = Boolean.parseBoolean(smellElements[i].getAttributeValue(SmellSettings.ATTRIBUTE_ISDETECTING));
		}
	}

	/**
	 * 建立Page外觀View
	 * @param composite
	 */
	private void buildPage(Composite composite) {
		selectComposite = new Composite(composite, SWT.NONE);
		
//		addLibBtn = new Button(composite, SWT.PUSH);
//		addLibBtn.setText("Add Tag Library");
//		addLibBtn.setLocation(getBoundsPoint(detAllBtn).x + 20, 10);
//		addLibBtn.pack();
//		addLibBtn.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				ISelectionService selectionService = Workbench.getInstance().getActiveWorkbenchWindow().getSelectionService();
//				ISelection selection = selectionService.getSelection();
//				IProject project = null;
//				if(selection instanceof IStructuredSelection) {
//				    Object element = ((IStructuredSelection)selection).getFirstElement();
//				    if (element instanceof IResource) {
//				        project= ((IResource)element).getProject();
//				    } else if (element instanceof PackageFragmentRootContainer) {
//				        IJavaProject jProject = ((PackageFragmentRootContainer)element).getJavaProject();
//				        project = jProject.getProject();
//				    } else if (element instanceof IJavaElement) {
//				        IJavaProject jProject= ((IJavaElement)element).getJavaProject();
//				        project = jProject.getProject();
//				    }
//				} else if (selection instanceof ITextSelection) {
//				    System.out.println("Fail");
//				    return;
//				}
//				File lib = new File(project.getLocation().toFile().getPath() + "/lib");
//				if(!lib.exists())
//					lib.mkdir();
//				File jar = new File(project.getLocation().toFile().getPath() + "/lib/Tag.jar");
//				JarFileMaker jarFileMaker = new JarFileMaker();
//				File test = new File("bin");
//				jarFileMaker.createJarFile(jar, test, "ntut.csie.robusta.agile.exception");
//			}
//		});

		// 上半部，選擇要偵測的Smell群組
		smellTypeGroup = new Group(selectComposite, SWT.NONE);
		smellTypeGroup.setText(resource.getString("settingPage.smell.type"));
		smellTypeGroup.setLocation(10, 5);

		//是否要選擇性偵測的Button
		checkbox_DetectAllSmells = new Button(smellTypeGroup, SWT.CHECK);
		checkbox_DetectAllSmells.setText(resource.getString("check.all.smell"));
		checkbox_DetectAllSmells.setLocation(10, 20);
		checkbox_DetectAllSmells.pack();
		checkbox_DetectAllSmells.setSelection(isDetectingAllSmells);
		checkbox_DetectAllSmells.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				isDetectingAllSmells = !isDetectingAllSmells;
				setUserSetting();
			}
		});
		
		//選擇EH Smell List
		smellList = new Table(smellTypeGroup, SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION );
		smellList.setLocation(checkbox_DetectAllSmells.getBounds().x, checkbox_DetectAllSmells.getBounds().y + checkbox_DetectAllSmells.getBounds().height);
		smellList.setFont(new Font(composite.getDisplay(),"Arial", 11, SWT.NONE));
		smellList.setLinesVisible(true);
		smellList.setHeaderVisible(true);
		smellList.setItemCount(7);

		final TableColumn smellColumn = new TableColumn(smellList, SWT.NONE);
		String smellColumnDisplayText = resource.getString("settingPage.smell.type");
		smellColumn.setText(smellColumnDisplayText);
		smellColumn.setWidth(smellColumnDisplayText.length() * 9);
		// 自動調整bad smell Type Column的大小
		smellColumn.pack();
		final TableColumn descColumn = new TableColumn(smellList, SWT.NONE);
		descColumn.setText(resource.getString("settingPage.smell.description"));

		for (int i =0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++) {
			TableItem item = smellList.getItem(i);
			item.setText(0, RLMarkerAttribute.CS_TOTAL_TYPE[i].replace('_', ' '));
			item.setFont(0, new Font(composite.getDisplay(),"Arial", 11, SWT.BOLD));			
			item.setText(1, descText[i]);
		}
		// 自動調整bad smell Description Column的大小
		descColumn.pack();
		
		// 自動調整設定主頁面上半部視窗的大小
		smellList.pack();
		smellTypeGroup.pack();

		//Template Group
		templateCodeGroup = new Group(selectComposite, SWT.NONE);
		templateCodeGroup.setText(resource.getString("settingPage.codeExample"));
		templateCodeGroup.setLocation(10, getLowerRightCoordinate(smellTypeGroup).y + 5);

		//是否要醒目標記程式碼
		checkbox_HighlightSmellCode = new Button(templateCodeGroup, SWT.CHECK);
		checkbox_HighlightSmellCode.setText(resource.getString("settingPage.codeExample.highlight"));
		checkbox_HighlightSmellCode.setLocation(10, 20);
		checkbox_HighlightSmellCode.pack();
		checkbox_HighlightSmellCode.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				isShowWarning = checkbox_HighlightSmellCode.getSelection();

				int index = smellList.getSelectionIndex();
				if (index != -1) {
					changeTemplateText(index);
				}
			}
		});
		
		//Template Text
		templateCode = new StyledText(templateCodeGroup, SWT.BORDER | SWT.V_SCROLL);
		templateCode.setBounds(10, this.getLowerRightCoordinate(checkbox_HighlightSmellCode).y, 500, 170);
		templateCode.setText("");
		Font font = new Font(composite.getDisplay(),"Courier New", 14,SWT.NORMAL);		
		templateCode.setFont(font);
		templateCodeGroup.pack();
		
		// 自動調整設定主頁面Container的大小
		selectComposite.pack();
		selectComposite.setSize(getLowerRightCoordinate(smellTypeGroup).x + 15, selectComposite.getBounds().height);
	}

	private void setUserSetting() {
		TableItem[] item = smellList.getItems();
		//去traverse整個table看item的Text和是否被勾選到
		for (int i=0; i < item.length; i++) {
			if(isDetectingAllSmells)
				detSmellList[i] = isDetectingAllSmells;
			item[i].setChecked(detSmellList[i]);
		}
	}
	
	private void changeTemplateText(int index) {
		if (index != -1) {
			templateCode.setText(tempText[index].getText());
			tempText[index].setShowWarning(isShowWarning);
			tempText[index].setTemplateStyle(mainPageComposite.getDisplay(), 0);
			templateCode.setStyleRanges(tempText[index].getLocationArray(), tempText[index].getStyleArrray());
		}
	}

	/**
	 * 
	 */
	private void changeTemplateSize() {
		templateCode.pack();
		Font font;
		if (templateCode.getBounds().height > templateCodeGroup.getBounds().height)
			font = new Font(mainPageComposite.getDisplay(),"Courier New", 10,SWT.NORMAL);
		else
			font = new Font(mainPageComposite.getDisplay(),"Courier New", 14,SWT.NORMAL);	

		templateCode.setFont(font);
		templateCode.setBounds(10, this.getLowerRightCoordinate(checkbox_HighlightSmellCode).y +5, 500, 170);
		selectComposite.pack();
	}

	@Override
	public boolean storeSettings() {
		//先將列表的item取出來
		TableItem[] item = smellList.getItems();
		//去traverse整個table看item的Text和是否被勾選到
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, item[0].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, item[1].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, item[2].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, item[3].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, item[4].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERLOGGING, SmellSettings.ATTRIBUTE_ISDETECTING, item[5].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERWRITTENLEADEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, item[6].getChecked());

		//將檔案寫回
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}

}
