package ntut.csie.csdet.views;

import java.io.File;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JarFileMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.PackageFragmentRootContainer;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.internal.Workbench;
import org.jdom.Element;

/**
 * EH TOOL設定畫面
 * @author Shiau
 *
 */
public class SettingPage extends APropertyPage {
	
	private Composite mainPageComposite;
	private Table smellList;
	private StyledText templateArea;
	private Group templateGroup;
	private Composite selectComposite;
	private Button detAllBtn;
	private Button showWarningBtn;
	private Button addLibBtn;
	
	private boolean isDetAll = false;
	private boolean[] detSmellList;
	private boolean isShowWarning = false;
	
	private TemplateText[] tempText;
	private String[] descText;
	// 負責處理讀寫XML
	SmellSettings smellSettings;

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
						isDetAll = false;
						detAllBtn.setSelection(isDetAll);
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
		
		descText = new String[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		descText[0] = "捕捉到例外後，Catch Block沒有做任何處理";
		descText[1] = "捕捉到例外後，僅只是將例外資訊列印或紀錄下來";
		descText[2] = "Try Block中出巢狀區塊";
		descText[3] = "Main Program中沒有補捉最上層的例外";
		descText[4] = "釋放資源的程式不是放在Finally Blcok";
		descText[5] = "一條Call Chain中出現重複的Logging動作";
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
		
		for(int i = 0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++) {
			detSmellList[i] = Boolean.parseBoolean(smellElements[i].getAttributeValue(SmellSettings.ATTRIBUTE_ISDETECTING));
		}
	}

	/**
	 * 建立Page外觀View
	 * @param composite
	 */
	private void buildPage(Composite composite) {
		//是否要選擇性偵測的Button
		detAllBtn = new Button(composite, SWT.CHECK);
		detAllBtn.setText("Check All Smell");
		detAllBtn.setLocation(10, 5);
		detAllBtn.pack();
		detAllBtn.setSelection(isDetAll);
		detAllBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				isDetAll = !isDetAll;
				setUserSetting();
			}
		});
		
		addLibBtn = new Button(composite, SWT.PUSH);
		addLibBtn.setText("Add RL Library");
		addLibBtn.setLocation(getBoundsPoint(detAllBtn).x + 20, 10);
		addLibBtn.pack();
		addLibBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISelectionService selectionService = Workbench.getInstance().getActiveWorkbenchWindow().getSelectionService();
				ISelection selection = selectionService.getSelection();
				IProject project = null;
				if(selection instanceof IStructuredSelection) {
				    Object element = ((IStructuredSelection)selection).getFirstElement();
				    if (element instanceof IResource) {
				        project= ((IResource)element).getProject();
				    } else if (element instanceof PackageFragmentRootContainer) {
				        IJavaProject jProject = ((PackageFragmentRootContainer)element).getJavaProject();
				        project = jProject.getProject();
				    } else if (element instanceof IJavaElement) {
				        IJavaProject jProject= ((IJavaElement)element).getJavaProject();
				        project = jProject.getProject();
				    }
				} else if (selection instanceof ITextSelection) {
				    System.out.println("Fail");
				    return;
				}
				File lib = new File(project.getLocation().toFile().getPath() + "/lib");
				if(!lib.exists())
					lib.mkdir();
				File jar = new File(project.getLocation().toFile().getPath() + "/lib/RL.jar");
				JarFileMaker jarFileMaker = new JarFileMaker();
				File test = new File("bin");
				jarFileMaker.createJarFile(jar, test, "agile.exception");
			}
		});

		//分隔線
		final Label label = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		label.setBounds(0, getBoundsPoint(addLibBtn).y + 5, 550, 1);

		selectComposite = new Composite(composite, SWT.NONE);
		selectComposite.setBounds(0, getBoundsPoint(label).y + 5, 550, 402);

		final Label label1 = new Label(selectComposite, SWT.NONE);
		label1.setText("Detect EH Smell Type:");
		label1.setLocation(10, 5);
		label1.pack();

		//選擇EH Smell List
		smellList = new Table(selectComposite, SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION );
		smellList.setLocation(10, getBoundsPoint(label1).y + 5);
		smellList.setFont(new Font(composite.getDisplay(),"Arial", 11, SWT.NONE));
		smellList.setLinesVisible(true);
		smellList.setHeaderVisible(true);
		smellList.setItemCount(6);
		smellList.pack();

		final TableColumn smellColumn = new TableColumn(smellList, SWT.NONE);
		smellColumn.setText("EH Smell Type");
		smellColumn.setWidth(220);
		final TableColumn descColumn = new TableColumn(smellList, SWT.NONE);
		descColumn.setText("Description");

		for (int i =0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++) {
			TableItem item = smellList.getItem(i);
			item.setText(0, RLMarkerAttribute.CS_TOTAL_TYPE[i].replace('_', ' '));
			item.setFont(0, new Font(composite.getDisplay(),"Arial", 11, SWT.BOLD));			
			item.setText(1, descText[i]);
		}
		descColumn.pack();
		smellList.setSize(500 , smellList.getSize().y);
		
		//Template Group
		templateGroup = new Group(selectComposite, SWT.NONE);
		templateGroup.setText("Detial");
		templateGroup.setLocation(10, getBoundsPoint(smellList).y + 10);

		//是否要選擇性偵測的Button
		showWarningBtn = new Button(templateGroup, SWT.CHECK);
		showWarningBtn.setText("Show EH Smell Code");
		showWarningBtn.setLocation(10, 15);
		showWarningBtn.pack();
		showWarningBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				isShowWarning = showWarningBtn.getSelection();

				int index = smellList.getSelectionIndex();
				if (index != -1) {
					changeTemplateText(index);
				}
			}
		});
		
		//Template Text
		templateArea = new StyledText(templateGroup, SWT.BORDER | SWT.V_SCROLL);
		templateArea.setBounds(10, this.getBoundsPoint(showWarningBtn).y +5, 500, 170);
		templateArea.setText("");
		Font font = new Font(composite.getDisplay(),"Courier New", 14,SWT.NORMAL);		
		templateArea.setFont(font);

		templateGroup.pack();
	}

	private void setUserSetting() {
		TableItem[] item = smellList.getItems();
		//去traverse整個table看item的Text和是否被勾選到
		for (int i=0; i < item.length; i++) {
			if(isDetAll)
				detSmellList[i] = isDetAll;
			item[i].setChecked(detSmellList[i]);
		}
	}
	
	private void changeTemplateText(int index) {
		if (index != -1) {
			templateArea.setText(tempText[index].getText());
			tempText[index].setShowWarning(isShowWarning);
			tempText[index].setTemplateStyle(mainPageComposite.getDisplay(), 0);
			templateArea.setStyleRanges(tempText[index].getLocationArray(), tempText[index].getStyleArrray());
		}
	}

	/**
	 * 
	 */
	private void changeTemplateSize() {
		templateArea.pack();
		Font font;
		if (templateArea.getBounds().height > templateGroup.getBounds().height)
			font = new Font(mainPageComposite.getDisplay(),"Courier New", 10,SWT.NORMAL);
		else
			font = new Font(mainPageComposite.getDisplay(),"Courier New", 14,SWT.NORMAL);	

		templateArea.setFont(font);
		templateArea.setBounds(10, this.getBoundsPoint(showWarningBtn).y +5, 500, 170);
	}

	@Override
	public boolean storeSettings() {
		//先將列表的item取出來
		TableItem[] item = smellList.getItems();
		//去traverse整個table看item的Text和是否被勾選到
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(item[0].getChecked()));
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(item[1].getChecked()));
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(item[2].getChecked()));
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(item[3].getChecked()));
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(item[4].getChecked()));
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERLOGGING, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(item[5].getChecked()));

		//將檔案寫回
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		return true;
	}

}
