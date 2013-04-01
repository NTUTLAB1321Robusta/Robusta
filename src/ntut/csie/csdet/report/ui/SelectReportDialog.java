package ntut.csie.csdet.report.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.csdet.report.ReportModel;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jdom.Element;

/**
 * 
 * @author Shiau
 */
public class SelectReportDialog  extends Dialog {
	//Delete�N�X
	private final int DELETE_SELECTION = 3337;

	private Combo projectCombo;
	private Table reportTable;
	
	//�ϥΪ̩ҿ�ܪ�Report Path
	private String filePath;
	//������Project
	private List<String> projectList = new ArrayList<String>();
	//�S�w�M�ש��U��������Report Path
	private List<File> fileList = new ArrayList<File>();
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	private ReportModel model = new ReportModel();
	
	public SelectReportDialog(Shell parentShell, List<String> projctList, ReportModel data) {
		super(parentShell);

		this.projectList = projctList;
		filePath = "";
		this.model = data;
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(resource.getString("report.list"));
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);		
		composite.setLayout(new GridLayout(1,false));

		Label label = new Label(composite,SWT.None);
		label.setText(resource.getString("project.name"));

		//�ظmTable
		buildTable(composite);

		return composite;
	}
	
	/**
	 * �ظmTable
	 * @param composite
	 */
	private void buildTable(Composite composite) {
		///ProjectCombo///
		projectCombo = new Combo(composite,SWT.READ_ONLY);
		projectCombo.setLayoutData(new GridData());
		projectCombo.addSelectionListener(new SelectionAdapter(){		
			public void widgetSelected(SelectionEvent e) {
				updateTable();
			}
		});

		//��Project�W�٥[�J��ProjectCombo
		for (String projectName : projectList)
			projectCombo.add(projectName);

		//ProjectList�w�]���Ĥ@��
		if (projectList.size() >= 0)
			projectCombo.select(0);

		///Report List Table///
		reportTable = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		reportTable.setLinesVisible(true);
	    reportTable.setHeaderVisible(true);

	    //table col 1 title
	    TableColumn column1 = new TableColumn(reportTable, SWT.NONE);
	    column1.setText(resource.getString("time"));
	    column1.setWidth(200);
	    
	    //table col 2 title
	    TableColumn column2 = new TableColumn(reportTable, SWT.NONE);
	    column2.setText("DescriptionContent");
	    column2.setWidth(200);
	    
	    //set layout
	    GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
	    data.heightHint = 200;
	    data.widthHint = 300;
	    reportTable.setLayoutData(data);
	    
	    //add listener
	    reportTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				//�Y�I���Item��U�A�p�P���U�T�w
				if (reportTable.getSelectionIndex() != -1)
					okPressed();
			}
	    });

	    //��sTable���e
	    updateTable();
	}
	/**
	 * ��sTable
	 */
	private void updateTable() {	
		//clear old item
		reportTable.clearAll();
		reportTable.setItemCount(0);
		fileList.clear();
		
		//set Report List
		getFileList();

		for (File file : fileList) {
			String fileName = file.getName();
			//System.out.println(file.getName());
			//�bTable���[�J�s��Item
			TableItem tableItem = new TableItem(reportTable, SWT.NONE);
			//���o����W�٪����
			int index = fileName.indexOf("_");		
			Date date = new Date(Long.parseLong(fileName.substring(0,index)));
			tableItem.setText(date.toString());
			
			//get xml value
//			Element summary = new Element("Summary");
//			Object descContent = summary.getContent().get(3);
//			Element eleDescContent = (Element) descContent;
			//description content , (index , content)
		//	tableItem.setText(1,eleDescContent.getAttributeValue("DescriptionContent"));
			
		}
	}

	/**
	 * ���oProject����Report��T
	 * @return
	 */
	public void getFileList() {
		//���o�ϥΪ̨Ͽ�ܪ�Project Name
		String projectName = projectList.get(projectCombo.getSelectionIndex());
		//���oWorkSpace
		String workPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		
		//Report�ؿ�
		File directory = new File(workPath + "/" + projectName + "/" + projectName + "_Report/");
		
		//���o�ؿ����C�@�Ӹ�Ƨ����|
		File[] allFolder = directory.listFiles();
		
		//�YProject���إ�Report���|
		if (allFolder == null)
			return;

		for (File folder: allFolder) {
			if (folder.isDirectory()) {
				//���o���ɦW��.html���ɮ�
				File[] files = folder.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".html");
					}
				});
				//��Report��T�O��
				for (File file : files)
					fileList.add(file);
			}
		}
	}
	
	/**
	 * �w�q����
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
	    createButton(parent, DELETE_SELECTION, resource.getString("delete"), true);
	}
	
	/**
	 * �Y���UDelete�A�R���ϥΪ̩ҿ�ܤ�Report
	 */
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		//�Y���UDelete
		if(buttonId == DELETE_SELECTION){ //delete by selection
			int[] selectIdx = reportTable.getSelectionIndices();
			
			//�R���Ҧ������Report
			if (selectIdx.length != 0) {
				for (int index : selectIdx) {
					//���oReport��Ƨ�
					File reportFolder = fileList.get(index).getParentFile();

					//�R����Ƨ����Ҧ��ɮ�
					File[] allFile = reportFolder.listFiles();
					for (File file: allFile)
						file.delete();

					//�R����Ƨ�
					reportFolder.delete();
				}
				updateTable();
			}
		}
	}

	/**
	 * �Y���UOK��A�O���ϥΪ̩ҿ����Report���|
	 */
	protected void okPressed() {	
		int index = reportTable.getSelectionIndex();

		if (index != -1)
			filePath = fileList.get(index).getAbsolutePath();

		super.okPressed(); 
	}
	
	/**
	 * ���o�ϥΪ̩ҿ����Report�W��
	 * @return
	 */
	public String getReportPath() {
		return filePath;
	}
}
