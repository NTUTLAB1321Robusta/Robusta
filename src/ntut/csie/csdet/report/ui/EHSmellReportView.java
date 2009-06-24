package ntut.csie.csdet.report.ui;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.report.ReportModel;
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
	//Project���
	private Combo projectCombo;
	//Report Browser
	static Browser browser;
	//Report�����
	private ReportModel data;
	//Filter������ʧ@
	private Action filterAction;
	//Select Report������ʧ@
	private Action selectAction;

	@Override
	public void createPartControl(Composite parent) {
		//Composite�w��
		FormLayout layout = new FormLayout();
		parent.setLayout(layout);

		//�ظmView�W��ToolBar
		buildToolItem(parent);

		//�ظmView�̪�Browser
		buildBrowser(parent);
		
		//�ظmView�̪�ToolBar 
		buildToolBar(parent);
	}

	/**
	 * �ظmView�̪�Browser
	 * @param parent
	 * @param status 
	 */
	private void buildBrowser(Composite parent) {
		///�t�mBrowser��m///
		FormData  browserForm = new FormData();
		browserForm.bottom = new FormAttachment(100, -5);
		browserForm.left = new FormAttachment(0, 0);
		browserForm.right = new FormAttachment(100, 0);
		browserForm.top = new FormAttachment(toolbar, 5, SWT.DEFAULT);

		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(browserForm);
		//�w�]Browser�}�l�ɪ��T��
		browser.setText("There is no report now !");
		browser.addLocationListener(new BrowserControl());
	}

	/**
	 * �ظmView�̪�ToolBar
	 */
	private void buildToolItem(Composite parent) {
		toolbar = new ToolBar(parent, SWT.NONE);
		///�t�mToolBar��m///
		FormData toolbarForm = new FormData();
		toolbarForm.top = new FormAttachment(0, 5);
		toolbar.setLayoutData(toolbarForm);
		
		///�ظmproduct ToolItem///
		final ToolItem itemGenerate = new ToolItem(toolbar, SWT.PUSH);
		itemGenerate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IProject[] projectList  = ResourcesPlugin.getWorkspace().getRoot().getProjects();

				//�Y�����Project�N���ͳ���A�ç�Browser���V�O��
				for (IProject project : projectList) {
					if (project.getName().equals(projectCombo.getItem(projectCombo.getSelectionIndex()))) {

						//���s�t�m�s��Model���
						data = new ReportModel();
						//����Report
						buildReport(project);

						break;
					}
				}
			}
		});
		itemGenerate.setText("Generate");
		itemGenerate.setImage(ImageManager.getInstance().get("unchecked"));

		///�ظmRefresh ToolItem///
		final ToolItem itemRefresh = new ToolItem(toolbar, SWT.PUSH);
		itemRefresh.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//�Y���URefresh��A���s���projectCombo���e
				projectCombo.removeAll();
				bindProjectCombo();
				projectCombo.setFocus();
			}
		});
		itemRefresh.setText("Refresh");
		itemRefresh.setImage(ImageManager.getInstance().get("refresh"));
		
		projectCombo = new Combo(parent, SWT.NONE);
		//��M�צW����ܦbCombo�W
		bindProjectCombo();
		///�ظmprojectCombo (�PToolBar�S�����Y)///
		FormData comboForm = new FormData();
		comboForm.bottom = new FormAttachment(0, 30);
		comboForm.top = new FormAttachment(0, 10);
		comboForm.right = new FormAttachment(toolbar, 88, SWT.RIGHT);
		comboForm.left = new FormAttachment(toolbar, 0, SWT.RIGHT);
		projectCombo.setLayoutData(comboForm);
	}
	
	/**
	 * ����Report
	 * @param project
	 * @return
	 */
	private void buildReport(IProject project) {
		//���X�{���ܰT����user,�]����coverage�n��@�q�ɶ�
		//����job�h�]builder,�p��code coverage
		final ProgressActionJob job = new ProgressActionJob("Generate EH Smell Report",project, data);
		
		//�]�w�u������
		job.setPriority(Job.SHORT);
		
		//�PPlugin�@���X
		final IWorkbenchSiteProgressService progressService = 
			(IWorkbenchSiteProgressService) RLEHTPlugin.getDefault().getWorkbench().
			getActiveWorkbenchWindow().getActivePage().getActivePart().getSite()
			.getAdapter(IWorkbenchSiteProgressService.class);
		progressService.showInDialog(
				RLEHTPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActivePart().getSite().getShell(), job);

		//�b�o��Τ@��listenre�hťJob�������ƥ�
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if(event.getResult().isOK()) {
					
					//Browser�}�ҹw�]��mHTML
					if (browser != null)
						openHTM();
				}
			}
		});

		//job�ʧ@
		job.schedule();
	}
	
	/**
	 * �ظmView�W��ToolBar
	 * @param parent 
	 */
	private void buildToolBar(Composite parent) {		
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		filterAction = new Action() {
			public void run() {
				//���U����XFilter Dialog
				FilterDialog filter = new FilterDialog(new Shell());
				filter.open();
			}
		};
		filterAction.setText("Filter");		
		filterAction.setImageDescriptor(ImageManager.getInstance().getDescriptor("filter"));
		toolBarManager.add(filterAction);

		selectAction = new Action() {
			public void run() {
				//���U����XSelect Report Dialog
				SelectReportDialog selectDialog = new SelectReportDialog(new Shell(), getProjectList());
				selectDialog.open();
				if(!selectDialog.getReportPath().equals("")){
					browser.setUrl(selectDialog.getReportPath());
				}
			}
		};
		selectAction.setText("Open Report");
		selectAction.setImageDescriptor(ImageManager.getInstance().getDescriptor("note_view"));		
		toolBarManager.add(selectAction);
	}

	/**
	 * ��M�צW����ܦbCombo�W
	 */
	private void bindProjectCombo() {
		List<String> projectList = getProjectList();

		for (String projectName : projectList)
				projectCombo.add(projectName);

		if (projectCombo.getItemCount() > 0)
			projectCombo.select(0);
	}

	/**
	 * ��������M�צW��
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
	 * �q�w�]���|�W���}HTM
	 */
	public void openHTM() {
		try {			
			//for different SWT Thread
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable(){
				public void run() {
					//���o�w�]���|
					String showPath = "file:///" + data.getFilePath("sample.html", true);
					//�}�Һ��}
					browser.setUrl(showPath);
				}
			});
		} catch (Exception e) {
			logger.error("[Exception] EXCEPTION ", e);
		}
	}
}
