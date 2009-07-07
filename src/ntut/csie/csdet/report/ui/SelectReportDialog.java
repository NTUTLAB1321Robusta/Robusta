package ntut.csie.csdet.report.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

/**
 * 
 * @author Shiau
 */
public class SelectReportDialog  extends Dialog {
	//Delete代碼
	private final int DELETE_SELECTION = 3337;

	private Combo projectCombo;
	private Table reportTable;

	//使用者所選擇的Report Path
	private String filePath;
	//全部的Project
	private List<String> projectList = new ArrayList<String>();
	//特定專案底下內全部的Report Path
	private List<String> pathList = new ArrayList<String>();
	
	public SelectReportDialog(Shell parentShell, List<String> projctList) {
		super(parentShell);

		this.projectList = projctList;
		filePath = "";
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Report List");
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);		
		composite.setLayout(new GridLayout(2,false));

		Label label = new Label(composite,SWT.None);
		label.setText("Project Name:");

		//建置Table
		buildTable(composite);

		return composite;
	}
	
	/**
	 * 建置Table
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

		//把Project名稱加入至ProjectCombo
		for (String projectName : projectList)
			projectCombo.add(projectName);

		//ProjectList預設為第一個
		if (projectList.size() >= 0)
			projectCombo.select(0);

		///Report List Table///
		reportTable = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		reportTable.setLinesVisible(true);
	    reportTable.setHeaderVisible(true);

	    TableColumn column1 = new TableColumn(reportTable, SWT.NONE);
	    column1.setText("Time");
	    column1.setWidth(200);

	    //set layout
	    GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
	    data.heightHint = 200;
	    data.widthHint = 300;
	    reportTable.setLayoutData(data);

	    //add listener
	    reportTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				//若點選到Item兩下，如同按下確定
				if (reportTable.getSelectionIndex() != -1)
					okPressed();
			}
	    });

	    //更新Table內容
	    updateTable();
	}
	
	/**
	 * 更新Table
	 */
	private void updateTable() {	
		//clear old item
		reportTable.clearAll();
		reportTable.setItemCount(0);
		pathList.clear();
		
		//set Report List
		List<String> fileList = getReportFile();

		for (String fileName : fileList) {
			//在Table內加入新的Item
			TableItem tableItem = new TableItem(reportTable, SWT.NONE);

			//取得報表名稱的日期
			int index = fileName.indexOf("_");
			Date date = new Date(Long.parseLong(fileName.substring(0,index)));

			tableItem.setText(date.toString());
		}
	}

	/**
	 * 取得Project內的Report資訊
	 * @return
	 */
	public ArrayList<String> getReportFile() {
		ArrayList<String> temp = new ArrayList<String>();
		//取得使用者使選擇的Project Name
		String projectName = projectList.get(projectCombo.getSelectionIndex());
		//取得WorkSpace
		String workPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();

		//Report目錄
		File folder = new File(workPath + "/" + projectName + "/" + projectName + "_Report/");
		
		//取得副檔名為.html的檔案
		File[] files = folder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {				
				return name.endsWith(".html");
			}
		});

		if (files != null) {
			//按照Report建立日期排序
			Arrays.sort(files);
			//把Report資訊記錄
			for(int i = 0; i < files.length; i++) {
				temp.add(files[i].getName());
				pathList.add(files[i].getAbsolutePath());
			}
		}

		return temp;
	}
	
	/**
	 * 定義按鍵
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
	    createButton(parent, DELETE_SELECTION, "Delete", true);
	}
	
	/**
	 * 若按下Delete，刪除使用者所選擇之Report
	 */
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		//若按下Delete
		if(buttonId == DELETE_SELECTION){ //delete by selection
			int[] selectIdx = reportTable.getSelectionIndices();
			
			//刪除所有選取的Report
			if (selectIdx.length != 0)
			{
				for (int index : selectIdx)
				{
					//取得Report位置
					String deleteFile = pathList.get(index);
					File reportPath = new File(deleteFile);
		
					//取得圖片位置
					String deletePhoto = deleteFile.replace("sample.html", "Report.jpg");
					File photoPath = new File(deletePhoto);
		
					//刪除Report
					reportPath.delete();
					//刪除圖片
					photoPath.delete();
				}
				updateTable();
			}
		}
	}

	/**
	 * 若按下OK鍵，記錄使用者所選取的Report路徑
	 */
	protected void okPressed() {	
		int index = reportTable.getSelectionIndex();

		if (index != -1)
			filePath = pathList.get(index);

		super.okPressed(); 
	}
	
	/**
	 * 取得使用者所選取的Report名稱
	 * @return
	 */
	public String getReportPath() {
		return filePath;
	}
}
