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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
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
		toolbar = new ToolBar(parent, SWT.NONE);

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

		createToolItem();

		FormData comboForm = new FormData();
		comboForm.left = new FormAttachment(0, 97);
		comboForm.right = new FormAttachment(0, 185);
		comboForm.bottom = new FormAttachment(0, 30);
		comboForm.top = new FormAttachment(0, 10);
		projectCombo.setLayoutData(comboForm);
		
		FormData toolbarForm = new FormData();
		toolbarForm.top = new FormAttachment(0, 5);
		toolbar.setLayoutData(toolbarForm);

		FormData  browserForm = new FormData();
		browserForm.bottom = new FormAttachment(100, -5);
//		browserForm.bottom = new FormAttachment(status, -5, SWT.DEFAULT);
		browserForm.left = new FormAttachment(0, 0);
		browserForm.right = new FormAttachment(100, 0);
		browserForm.top = new FormAttachment(toolbar, 5, SWT.DEFAULT);
		
		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(browserForm);

		browser.setText("There is no report now !");
		
		initializeToolBar();
	}

	/**
	 * 
	 */
	private void createToolItem() {
		final ToolItem itemProduct = new ToolItem(toolbar, SWT.PUSH);
		itemProduct.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IProject[] projectList  = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				ReportBuilder report;
				for (IProject project : projectList) {
					if (project.getName().equals(projectCombo.getItem(projectCombo.getSelectionIndex()))) {
						data = new ReportModel();
						report = new ReportBuilder(project,data);
						if (browser != null)
							openHTM();
						break;
					}
				}
			}
		});
		itemProduct.setText("Product");
		itemProduct.setImage(ImageManager.getInstance().get("unchecked"));
		
		final ToolItem itemRefresh = new ToolItem(toolbar, SWT.PUSH);
		itemRefresh.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				projectCombo.removeAll();
				bindProjectCombo();
				projectCombo.setFocus();
			}
		});
		itemRefresh.setText("Refresh");
		itemRefresh.setImage(ImageManager.getInstance().get("refresh"));
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
		String showPath = "file:///" + data.getProjectPath() + "/sample.html";
		browser.setUrl(showPath);
	}
	
	private void initializeToolBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();

		filterAction = new Action() {
			public void run() {
				String selectProjectName = "";
				if (projectCombo.getSelectionIndex() != -1)
					selectProjectName = projectCombo.getItem(projectCombo.getSelectionIndex());
				FilterDialog filter = new FilterDialog(new Shell());
				filter.open();
			}
		};
		filterAction.setText("Filter");		
		filterAction.setImageDescriptor(ImageManager.getInstance().getDescriptor("showthrow"));
		toolBarManager.add(filterAction);
	}
}
