package ntut.csie.csdet.report.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.report.BadSmellDataStorage;
import ntut.csie.csdet.report.ReportContentCreator;
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
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
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

public class EHSmellReportViewer extends ViewPart {
	private static Logger logger = LoggerFactory.getLogger(EHSmellReportViewer.class);

	//Report ToolBar
	private ToolBar toolbar;
	//Project選單
	private Combo projectCombo;
	//Report Browser
	static Browser browser;
	//Select Report的按鍵動作
	private Action selectAction;
	
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	private BadSmellDataStorage dataStorage;

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
		
		dataStorage = new BadSmellDataStorage(project.getLocation().toString());
		final ReportBuildingJob job = new ReportBuildingJob(resource.getString("SmellReport.generateReportProgressBarTitle"), project, dataStorage);
		
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
				if (browser != null) {
					if (event.getResult().isOK()) {
						openBrowser(true);
					} else {
						openBrowser(false);
					}
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

		selectAction = new Action() {
			public void run() {
				//按下後跳出Select Report Dialog
				SelectReportDialog selectDialog = new SelectReportDialog(new Shell(), getProjectList());
				selectDialog.open();
				if(!selectDialog.getReportPath().equals("")){
					String dataPath = selectDialog.getReportPath();
					openReport(dataPath);
				}
			}
		};
		selectAction.setText(resource.getString("SmellReport.open.report"));
		selectAction.setImageDescriptor(ImageManager.getInstance().getDescriptor("note_view"));		
		toolBarManager.add(selectAction);
	}
	
	/**
	 * @param dataPath
	 */
	private void openReport(String dataPath) {
		ReportContentCreator contentCreator = new ReportContentCreator(dataPath);
		contentCreator.buildReportContent();
		browser.setJavascriptEnabled(true);
		browser.setUrl("file:///" + contentCreator.getResultPath());
		
		/* We need to refresh the page to load all resources.
		 * Maybe this is a bug of the SWT browser. Fix it later.
		*/
		browser.addProgressListener(new ProgressListener() {
		    @Override
		    public void completed(ProgressEvent event) {
		    	browser.execute("document.location.reload();");
		    	browser.removeProgressListener(this);
		    }
		    @Override
		    public void changed(ProgressEvent event) {
		    }
		});
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

	public void openBrowser(final boolean isCompletedSucessful) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (isCompletedSucessful) {
					String dataPath = dataStorage.getResultDataPath();
					openReport(dataPath);
				} else {
					browser.setText(resource.getString("SmellReport.browser.canceled"));
				}
			}
		});
	}
}
