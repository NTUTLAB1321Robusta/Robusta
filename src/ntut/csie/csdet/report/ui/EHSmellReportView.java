package ntut.csie.csdet.report.ui;

import ntut.csie.csdet.report.ReportBuilder;
import ntut.csie.csdet.report.ReportModel;
import ntut.csie.rleht.common.ImageManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;

public class EHSmellReportView extends ViewPart{
	private ToolBar toolbar;
	private Combo projectCombo;
	static Browser browser;
	private ReportModel data;
	private Action filterAction;
	
	@Override
	public void createPartControl(Composite parent) {		
		projectCombo = new Combo(parent, SWT.NONE);
		bindProjectCombo();

		FormLayout layout = new FormLayout();
		parent.setLayout(layout);

//		final Label status = new Label(parent, SWT.NONE);
//		final ProgressBar progressBar = new ProgressBar(parent, SWT.NONE);
//		
//		FormData stateForm = new FormData();
//		stateForm.left = new FormAttachment(0, 5);
//		stateForm.right = new FormAttachment(progressBar, 0, SWT.DEFAULT);
//		stateForm.bottom = new FormAttachment(100, -5);
//		status.setLayoutData(stateForm);
//		FormData processForm = new FormData();
//		processForm.right = new FormAttachment(100, -5);
//		processForm.bottom = new FormAttachment(100, -3);
//		progressBar.setLayoutData(processForm);

		//建置View上的ToolBar
		buildToolItem(parent);

		//建置View裡的Browser
		buildBrowser(parent);
		
		//建置View裡的ToolBar 
		buildToolBar();
	}

	/**
	 * 建置View裡的Browser
	 * @param parent
	 */
	private void buildBrowser(Composite parent) {
		///配置Browser位置///
		FormData  browserForm = new FormData();
		browserForm.bottom = new FormAttachment(100, -5);
//		browserForm.bottom = new FormAttachment(status, -5, SWT.DEFAULT);
		browserForm.left = new FormAttachment(0, 0);
		browserForm.right = new FormAttachment(100, 0);
		browserForm.top = new FormAttachment(toolbar, 5, SWT.DEFAULT);
		
		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(browserForm);

		//預設Browser開始時的訊息
		browser.setText("There is no report now !");
	}

	/**
	 * 建置View裡的ToolBar
	 */
	private void buildToolItem(Composite parent) {
		toolbar = new ToolBar(parent, SWT.NONE);
		///配置ToolBar位置///
		FormData toolbarForm = new FormData();
		toolbarForm.top = new FormAttachment(0, 5);
		toolbar.setLayoutData(toolbarForm);
		
		///建置product ToolItem///
		final ToolItem itemProduct = new ToolItem(toolbar, SWT.PUSH);
		itemProduct.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IProject[] projectList  = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				ReportBuilder report;
				//若有選擇Project就產生報表，並把Browser指向記頁
				for (IProject project : projectList) {
					if (project.getName().equals(projectCombo.getItem(projectCombo.getSelectionIndex()))) {
						//重新配置新的Model資料
						data = new ReportModel();
						//建置Report
						report = new ReportBuilder(project,data);
						//Browser開啟預設位置HTML
						if (browser != null)
							openHTM();
						break;
					}
				}
			}
		});
		itemProduct.setText("Generate");
		itemProduct.setImage(ImageManager.getInstance().get("unchecked"));

		///建置Refresh ToolItem///
		final ToolItem itemRefresh = new ToolItem(toolbar, SWT.PUSH);
		itemRefresh.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//若按下Refresh鍵，重新抓取projectCombo內容
				projectCombo.removeAll();
				bindProjectCombo();
				projectCombo.setFocus();
			}
		});
		itemRefresh.setText("Refresh");
		itemRefresh.setImage(ImageManager.getInstance().get("refresh"));
		
		///建置projectCombo (與ToolBar沒有關係)///
		FormData comboForm = new FormData();
		comboForm.left = new FormAttachment(0, 97);
		comboForm.right = new FormAttachment(0, 185);
		comboForm.bottom = new FormAttachment(0, 30);
		comboForm.top = new FormAttachment(0, 10);
		projectCombo.setLayoutData(comboForm);
	}
	
	/**
	 * 建置View上的ToolBar
	 */
	private void buildToolBar() {		
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		filterAction = new Action() {
			public void run() {
				//按下後跳出Filter Dialog
				FilterDialog filter = new FilterDialog(new Shell());
				filter.open();
			}
		};
		filterAction.setText("Filter");		
		filterAction.setImageDescriptor(ImageManager.getInstance().getDescriptor("showthrow"));
		toolBarManager.add(filterAction);
	}

	/**
	 * 獲取全部Project的資料
	 */
	private void bindProjectCombo() {
		IProject[] projectList  = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i=0; i < projectList.length; i++)
			if (projectList[i].isOpen())
				projectCombo.add(projectList[i].getName());
		if (projectCombo.getItemCount() > 0)
			projectCombo.select(0);
	}

	@Override
	public void setFocus() {
		
	}
	
	/**
	 * 從預設路徑上打開HTM
	 */
	public void openHTM(){
		String showPath = "file:///" + data.getFilePath("sample.html", true);
		browser.setUrl(showPath);
	}
}
