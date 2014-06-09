package ntut.csie.csdet.report.ui;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.rleht.common.ImageManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EHSmellReportViewer extends ViewPart implements ISmellReportView {
	private static Logger logger = LoggerFactory.getLogger(EHSmellReportViewer.class);

	// Report ToolBar
	private ToolBar toolbar;
	// Project list
	private Combo projectCombo;
	// Report browser
	static Browser browser;
	// Select past report
	private Action selectAction;
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	private ReportViewPresentationModel viewPresentationModel;

	public EHSmellReportViewer() {
		super();
		viewPresentationModel = new ReportViewPresentationModel(this);
	}

	@Override
	public void createPartControl(Composite parent) {

		FormLayout layout = new FormLayout();
		parent.setLayout(layout);

		buildToolItem(parent);

		buildBrowser(parent);

		buildToolBar(parent);

		viewPresentationModel.subscribeProjectEvents();
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				viewPresentationModel.unsubscribeProjectEvents();
			}
		});
	}

	private void buildBrowser(Composite parent) {
		FormData browserForm = new FormData();
		browserForm.bottom = new FormAttachment(100, -5);
		browserForm.left = new FormAttachment(0, 0);
		browserForm.right = new FormAttachment(100, 0);
		browserForm.top = new FormAttachment(toolbar, 5, SWT.DEFAULT);
		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(browserForm);
		browser.setText(resource.getString("SmellReport.browser.default"));
		browser.addLocationListener(new BrowserControl());
	}

	private void buildToolItem(Composite parent) {
		toolbar = new ToolBar(parent, SWT.NONE);
		FormData toolbarForm = new FormData();
		toolbarForm.top = new FormAttachment(0, 5);
		toolbar.setLayoutData(toolbarForm);

		final ToolItem itemGenerate = new ToolItem(toolbar, SWT.PUSH);
		itemGenerate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				viewPresentationModel.generateDensityReport();
			}
		});
		itemGenerate.setText(resource.getString("SmellReport.generate"));
		itemGenerate.setImage(ImageManager.getInstance().get("unchecked"));

		// Trend Report ToolItem
		final ToolItem itemTrendReport = new ToolItem(toolbar, SWT.PUSH);
		itemTrendReport.setText(resource.getString("SmellReport.trendReport"));
		itemTrendReport.setImage(ImageManager.getInstance().get("trendReport"));
		itemTrendReport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				viewPresentationModel.generateTrendReport();
			}
		});

		projectCombo = new Combo(parent, SWT.NONE);
		updateProjectList(viewPresentationModel.getProjectNameList());
		FormData comboForm = new FormData();
		comboForm.bottom = new FormAttachment(0, 30);
		comboForm.top = new FormAttachment(0, 10);
		comboForm.right = new FormAttachment(toolbar, 88, SWT.RIGHT);
		comboForm.left = new FormAttachment(toolbar, 0, SWT.RIGHT);
		projectCombo.setLayoutData(comboForm);
		projectCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewPresentationModel.setSelectProjectIndex(projectCombo.getSelectionIndex());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				viewPresentationModel.setSelectProjectIndex(projectCombo.getSelectionIndex());
			}
		});
	}

	private void buildToolBar(Composite parent) {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();

		selectAction = new Action() {
			public void run() {
				if (viewPresentationModel.getProjectNameList().size() == 0) {
					return;
				}
				PastReportDialog selectDialog = new PastReportDialog(new Shell(), viewPresentationModel.getProjectNameList());
				selectDialog.open();
				if (!selectDialog.getReportPath().equals("")) {
					String dataPath = selectDialog.getReportPath();
					String projectName = selectDialog.getProjectName();
					viewPresentationModel.openDensityReport(dataPath, projectName);
				}
			}
		};
		selectAction.setText(resource.getString("SmellReport.open.report"));
		selectAction.setImageDescriptor(ImageManager.getInstance().getDescriptor("note_view"));
		toolBarManager.add(selectAction);
	}

	@Override
	public void setFocus() {

	}

	@Override
	public void setBrowserText(String text) {
		browser.setText(text);
	}

	@Override
	public void setBrowserUrl(String url) {
		browser.setJavascriptEnabled(true);
		browser.setUrl(url);
	}

	@Override
	public void updateProjectList(List<String> projectList) {
		viewPresentationModel.setSelectProjectIndex(-1);
		projectCombo.removeAll();
		for (String projectName : projectList)
			projectCombo.add(projectName);
		// Auto resize
		projectCombo.pack();
		if (projectCombo.getItemCount() > 0) {
			projectCombo.select(0);
			viewPresentationModel.setSelectProjectIndex(0);
		}
	}
}