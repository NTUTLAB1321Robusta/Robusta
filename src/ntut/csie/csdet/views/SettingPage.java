package ntut.csie.csdet.views;

import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.jdom.Element;

/**
 * exception handling tool setting page
 * @author Shiau
 *
 */
public class SettingPage extends APropertyPage {
	
	private Composite mainPageComposite;
	/** Container of exception handling tool's setting page */
	private Composite selectComposite;
	/** frame of exception handling's bad smell type */
	private Group smellTypeGroup;
	/** bad smell list of exception handling*/
	private Table smellList;
	/** frame of bad smell's template code */
	private Group templateCodeGroup;
	/** bad smell's template code*/
	private StyledText templateCode;
	/** Checkbox for selecting all bad smell to be detected */
	private Button checkbox_DetectAllSmells;
	/** Checkbox for highlighting the line of bad smell */
	private Button checkbox_HighlightSmellCode;
	
	/** Preference group */
	private Group preferenceGroup;
	/** Checkbox for show warning to add RL annotation */
	private Button checkbox_ShowWarning;
	private boolean[] preferenceList;
	protected final int PRE_COUNT = 1;
	
	private boolean isDetectingAllSmells = false;
	private boolean[] detSmellList;
	private boolean isShowWarning = false;
	
	private TemplateText[] tempText;
	private String[] descText;
	private SmellSettings smellSettings;
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	public SettingPage(Composite composite, CSPropertyPage page, SmellSettings smellSettings) {
		super(composite, page);
		
		mainPageComposite = composite;
		this.smellSettings = smellSettings;
		
		initailState();
		readSetting();
		buildPage(composite);
		setWithUserSetting();
		
		// set preference with user setting
		for(int i=0; i<PRE_COUNT; i++) {
			checkbox_ShowWarning.setSelection(preferenceList[i]);
		}
		
		for (int i =0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++)
			tempText[i].setTemplateStyle(composite.getDisplay(), 0);

		smellList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				int index = smellList.getSelectionIndex();
				changeTemplateText(index);
				changeTemplateSize();
				TableItem[] item = smellList.getItems();
				for (int i=0; i < item.length; i++) {
					if(!item[i].getChecked()) {
						isDetectingAllSmells = false;
						checkbox_DetectAllSmells.setSelection(false);
						return;
					}
				}
			}
		});
	}

	private void setWithUserSetting() {
		TableItem[] item = smellList.getItems();
		for (int i=0; i < item.length; i++) {
			item[i].setChecked(detSmellList[i]);
		}
	}

	private void initailState() {
		detSmellList = new boolean[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		for (int i =0; i < detSmellList.length; i++)
			detSmellList[i] = true;
		
		preferenceList = new boolean[PRE_COUNT];
		preferenceList[0] = true; 

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
				
		tempText[5] = new TemplateText(temp, isShowWarning);
				
		descText = new String[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		descText[0] = resource.getString("empty.catch.description");
		descText[1] = resource.getString("dummy.handler.description");
		descText[2] = resource.getString("nested.try.statement.description");
		descText[3] = resource.getString("unprotected.main.program.description");
		descText[4] = resource.getString("careless.cleanup.description");
		descText[5] = resource.getString("exception.thrown.from.finally.block.description");
	}

	/**
	 * get the previous user setting
	 */
	private void readSetting() {
		Element[] smellElements = new Element[RLMarkerAttribute.CS_TOTAL_TYPE.length];
		smellElements[0] = smellSettings.getSmellType(SmellSettings.SMELL_EMPTYCATCHBLOCK);
		smellElements[1] = smellSettings.getSmellType(SmellSettings.SMELL_DUMMYHANDLER);
		smellElements[2] = smellSettings.getSmellType(SmellSettings.SMELL_NESTEDTRYSTATEMENT);
		smellElements[3] = smellSettings.getSmellType(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM);
		smellElements[4] = smellSettings.getSmellType(SmellSettings.SMELL_CARELESSCLEANUP);
		smellElements[5] = smellSettings.getSmellType(SmellSettings.SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK);
		
		for(int i = 0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++) {
			detSmellList[i] = Boolean.parseBoolean(smellElements[i].getAttributeValue(SmellSettings.ATTRIBUTE_ISDETECTING));
		}
		Element[] prefElements = new Element[PRE_COUNT];
		prefElements[0] = smellSettings.getPreference(SmellSettings.PRE_SHOWRLANNOTATIONWARNING);
		for(int i=0; i<PRE_COUNT; i++){
			preferenceList[i] = Boolean.parseBoolean(prefElements[i].getAttributeValue(SmellSettings.ATTRIBUTE_ENABLE));
		}
	}

	/**
	 * establish view of page
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
		
		// Preferences
		preferenceGroup = new Group(selectComposite, SWT.NONE);
		preferenceGroup.setText(resource.getString("settingPage.preference"));
		preferenceGroup.setLocation(10, 5);
		checkbox_ShowWarning = new Button(preferenceGroup, SWT.CHECK);
		checkbox_ShowWarning.setText(resource.getString("settingPage.remindRLAnnotation"));
		checkbox_ShowWarning.setLocation(10, 20);
		checkbox_ShowWarning.pack();
		checkbox_ShowWarning.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				preferenceList[0] = checkbox_ShowWarning.getSelection();
			}
		});
		preferenceGroup.pack();
		
		
		//select and put the smells to be detected in a group
		smellTypeGroup = new Group(selectComposite, SWT.NONE);
		smellTypeGroup.setText(resource.getString("settingPage.smell.type"));
		smellTypeGroup.setLocation(10, getLowerRightCoordinate(preferenceGroup).y + 5);

		checkbox_DetectAllSmells = new Button(smellTypeGroup, SWT.CHECK);
		checkbox_DetectAllSmells.setText(resource.getString("check.all.smell"));
		checkbox_DetectAllSmells.setLocation(10, 20);
		checkbox_DetectAllSmells.pack();
		checkbox_DetectAllSmells.setSelection(isDetectingAllSmells);
		checkbox_DetectAllSmells.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				isDetectingAllSmells = !isDetectingAllSmells;
				checkOrUncheckAllSmellSettings();
			}
		});
		
		smellList = new Table(smellTypeGroup, SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION );
		smellList.setLocation(checkbox_DetectAllSmells.getBounds().x, checkbox_DetectAllSmells.getBounds().y + checkbox_DetectAllSmells.getBounds().height);
		smellList.setFont(new Font(composite.getDisplay(),"Arial", 11, SWT.NONE));
		smellList.setLinesVisible(true);
		smellList.setHeaderVisible(true);
		smellList.setItemCount(6);

		final TableColumn smellColumn = new TableColumn(smellList, SWT.NONE);
		String smellColumnDisplayText = resource.getString("settingPage.smell.type");
		smellColumn.setText(smellColumnDisplayText);
		smellColumn.setWidth(smellColumnDisplayText.length() * 9);
		// adjust the size of bad smell type's column automatically
		smellColumn.pack();
		final TableColumn descColumn = new TableColumn(smellList, SWT.NONE);
		descColumn.setText(resource.getString("settingPage.smell.description"));

		for (int i =0; i < RLMarkerAttribute.CS_TOTAL_TYPE.length; i++) {
			TableItem item = smellList.getItem(i);
			item.setText(0, RLMarkerAttribute.CS_TOTAL_TYPE[i].replace('_', ' '));
			item.setFont(0, new Font(composite.getDisplay(),"Arial", 11, SWT.BOLD));			
			item.setText(1, descText[i]);
		}
		// adjust the size of bad smell description's column automatically
		descColumn.pack();
		
		// adjust the size of the top half of setting page automatically
		smellList.pack();
		smellTypeGroup.pack();

		//Template Group
		templateCodeGroup = new Group(selectComposite, SWT.NONE);
		templateCodeGroup.setText(resource.getString("settingPage.codeExample"));
		templateCodeGroup.setLocation(10, getLowerRightCoordinate(smellTypeGroup).y + 5);

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
		
		// adjust the size of setting page's container automatically 
		selectComposite.pack();
		selectComposite.setSize(getLowerRightCoordinate(smellTypeGroup).x + 15, selectComposite.getBounds().height);
	}

	private void checkOrUncheckAllSmellSettings() {
		TableItem[] item = smellList.getItems();
		for (int i=0; i < item.length; i++) {
			detSmellList[i] = isDetectingAllSmells;
		}
		
		setWithUserSetting();
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
		TableItem[] item = smellList.getItems();
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, item[0].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, item[1].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYSTATEMENT, SmellSettings.ATTRIBUTE_ISDETECTING, item[2].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, item[3].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, item[4].getChecked());
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, item[5].getChecked());

		smellSettings.setPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING, SmellSettings.ATTRIBUTE_ENABLE, preferenceList[0]);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		rebuildProjectInBackground();
		return true;
	}

	public void rebuildProjectInBackground(){
		final IProject currentProject=getCurrentProject();
		 Job job = new Job("Rebuilding Project") {
		     protected IStatus run(IProgressMonitor monitor) {
		    	   try {
		    		   currentProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
		           return Status.OK_STATUS;
		        }
		     };
		  job.setPriority(Job.SHORT);
		  job.schedule(); 
	} 
	
	public IProject getCurrentProject(){  
		IWorkbench iworkbench = PlatformUI.getWorkbench();
        ISelectionService selectionService = iworkbench.getActiveWorkbenchWindow().getSelectionService();    
        ISelection selection = selectionService.getSelection();    
        IProject project = null;    
        if(selection instanceof IStructuredSelection) {    
            Object element = ((IStructuredSelection)selection).getFirstElement();    
            if (element instanceof IResource) {    
                project= ((IResource)element).getProject();    
            }  else if (element instanceof IJavaElement) {    
                IJavaProject jProject= ((IJavaElement)element).getJavaProject();    
                project = jProject.getProject();    
            }    
        }     
        return project;    
    }
}
