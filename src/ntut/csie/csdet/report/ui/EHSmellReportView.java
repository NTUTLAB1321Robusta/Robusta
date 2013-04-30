package ntut.csie.csdet.report.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.report.ReportModel;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.common.ImageManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EHSmellReportView extends ViewPart {
	private static Logger logger = LoggerFactory.getLogger(EHSmellReportView.class);

	//Report ToolBar
	private ToolBar toolbar;
	//Project選單
	private Combo projectCombo;
	//Report Browser
	static Browser browser;
	//Report的資料
	private ReportModel data;
	//Filter的按鍵動作
	private Action filterAction;
	//Select Report的按鍵動作
	private Action selectAction;
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	@Override
	public void createPartControl(Composite parent) {
		//Composite定位
		FormLayout layout = new FormLayout();
		parent.setLayout(layout);

		//建置View上的ToolBar
		buildToolItem(parent);

		//建置View裡的Browser
		buildBrowser(parent);
		
		//建置View裡的ToolBar 
		buildToolBar(parent);
	}

	/**
	 * 建置View裡的Browser
	 * @param parent
	 * @param status 
	 */
	private void buildBrowser(Composite parent) {
		///配置Browser位置///
		FormData  browserForm = new FormData();
		browserForm.bottom = new FormAttachment(100, -5);
		browserForm.left = new FormAttachment(0, 0);
		browserForm.right = new FormAttachment(100, 0);
		browserForm.top = new FormAttachment(toolbar, 5, SWT.DEFAULT);
		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(browserForm);
		//預設Browser開始時的訊息
		browser.setText(resource.getString("SmellReport.browser.default"));
		browser.addLocationListener(new BrowserControl());
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
		final ToolItem itemGenerate = new ToolItem(toolbar, SWT.PUSH);
		itemGenerate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IProject[] projectList  = ResourcesPlugin.getWorkspace().getRoot().getProjects();

				// 沒有設定檔存在時，幫使用者預設為所有的條件都勾選
				SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
				smellSettings.activateAllConditions(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
				
				//若有選擇Project就產生報表，並把Browser指向記頁
				for (IProject project : projectList) {
					if (project.getName().equals(projectCombo.getItem(projectCombo.getSelectionIndex()))) {

						//重新配置新的Model資料
						data = new ReportModel();
						//產生Report
						buildReport(project);

						break;
					}
				}
			}
		});
		itemGenerate.setText(resource.getString("SmellReport.generate"));
		itemGenerate.setImage(ImageManager.getInstance().get("unchecked"));

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
		itemRefresh.setText(resource.getString("SmellReport.refresh"));
		itemRefresh.setImage(ImageManager.getInstance().get("refresh"));

		projectCombo = new Combo(parent, SWT.NONE);
		//把專案名稱顯示在Combo上
		bindProjectCombo();
		///建置projectCombo (與ToolBar沒有關係)///
		FormData comboForm = new FormData();
		comboForm.bottom = new FormAttachment(0, 30);
		comboForm.top = new FormAttachment(0, 10);
		comboForm.right = new FormAttachment(toolbar, 88, SWT.RIGHT);
		comboForm.left = new FormAttachment(toolbar, 0, SWT.RIGHT);
		projectCombo.setLayoutData(comboForm);
	}

	/**
	 * 產生Report
	 * @param project
	 * @return
	 */
	private void buildReport(IProject project) {
		//先出現提示訊息給user,因為算coverage要花一段時間
		//先讓job去跑builder,計算code coverage
		final ProgressActionJob job = new ProgressActionJob(resource.getString("SmellReport.generateReportProgressBarTitle"), project, data);
		
		//設定優先順序
		job.setPriority(Job.SHORT);
		
		//與Plugin作結合
		final IWorkbenchSiteProgressService progressService = 
			(IWorkbenchSiteProgressService) RLEHTPlugin.getDefault().getWorkbench().
			getActiveWorkbenchWindow().getActivePage().getActivePart().getSite()
			.getAdapter(IWorkbenchSiteProgressService.class);
		progressService.showInDialog(
				RLEHTPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActivePart().getSite().getShell(), job);

		//在這邊用一個listenre去聽Job完成的事件
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if(event.getResult().isOK()) {
					
					//Browser開啟預設位置HTML
					if (browser != null)
						openHTM();
				}
			}
		});

		//job動作
		job.schedule();
	}
	
	/**
	 * 建置View上的ToolBar
	 * @param parent 
	 */
	private void buildToolBar(Composite parent) {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		filterAction = new Action() {
			public void run() {
				//按下後跳出Filter Dialog
				FilterDialog filter = new FilterDialog(new Shell());
				filter.open();
			}
		};
		filterAction.setText(resource.getString("SmellReport.filter"));		
		filterAction.setImageDescriptor(ImageManager.getInstance().getDescriptor("filter"));
		toolBarManager.add(filterAction);

		selectAction = new Action() {
			public void run() {
				//按下後跳出Select Report Dialog
				SelectReportDialog selectDialog = new SelectReportDialog(new Shell(), getProjectList());
				selectDialog.open();
				if(!selectDialog.getReportPath().equals("")){
					browser.setUrl(selectDialog.getReportPath());
				}
			}
		};
		selectAction.setText(resource.getString("SmellReport.open.report"));
		selectAction.setImageDescriptor(ImageManager.getInstance().getDescriptor("note_view"));		
		toolBarManager.add(selectAction);
	}

	/**
	 * 把專案名稱顯示在Combo上
	 */
	private void bindProjectCombo() {
		List<String> projectList = getProjectList();

		for (String projectName : projectList)
				projectCombo.add(projectName);
		// Auto Size
		projectCombo.pack();

		if (projectCombo.getItemCount() > 0)
			projectCombo.select(0);

	}

	/**
	 * 獲取全部專案名稱
	 * @return
	 */
	private List<String> getProjectList() {
		List<String> projectName = new ArrayList<String>();

		IProject[] projectList  = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i=0; i < projectList.length; i++)
			if (projectList[i].isOpen())
				projectName.add(projectList[i].getName());
		
		return projectName;
	}

	@Override
	public void setFocus() {

	}
	
	/**
	 * 從預設路徑上打開HTM
	 */
	public void openHTM() {
		try {
			//for different SWT Thread
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable(){
				public void run() {
					//取得預設路徑
					String showPath = "file:///" + data.getFilePath("sample.html", true);
					//開啟網址
					browser.setUrl(showPath);
				}
			});
		} catch (Exception e) {
			logger.error("[Exception] EXCEPTION ", e);
		}
	}
}
