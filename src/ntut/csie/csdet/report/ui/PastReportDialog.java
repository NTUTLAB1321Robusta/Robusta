package ntut.csie.csdet.report.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.csdet.report.BadSmellDataEntity;
import ntut.csie.csdet.report.PastReportsHistory;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jdom.Element;

public class PastReportDialog extends Dialog {
	private final int DELETE_SELECTION = 3337;
	private final int APPLY_SELECTION = 3338;

	private Combo projectCombo;
	private Table reportTable;
	private Text reportDescriptionEditor;
	private String filePath;
	private List<String> projectList = new ArrayList<String>();
	private List<File> fileList = new ArrayList<File>();

	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	private String projectName;

	public PastReportDialog(Shell parentShell, List<String> projctList) {
		super(parentShell);

		this.projectList = projctList;
		filePath = "";
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(resource.getString("report.list"));
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.None);
		label.setText(resource.getString("project.name"));

		buildTable(composite);

		return composite;
	}

	private void buildTable(Composite composite) {
		// /ProjectCombo///
		projectCombo = new Combo(composite, SWT.READ_ONLY);
		projectCombo.setLayoutData(new GridData());
		projectCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTable();
			}
		});

		for (String projectName : projectList)
			projectCombo.add(projectName);

		if (projectList.size() >= 0)
			projectCombo.select(0);

		reportTable = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		reportTable.setLinesVisible(true);
		reportTable.setHeaderVisible(true);

		TableColumn column1 = new TableColumn(reportTable, SWT.NONE);
		column1.setText(resource.getString("time"));
		column1.setWidth(200);
		// create description column
		TableColumn column2 = new TableColumn(reportTable, SWT.NONE);
		column2.setText(resource.getString("description"));
		column2.setWidth(200);

		// set layout
		GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		data.heightHint = 200;
		data.widthHint = 350;
		reportTable.setLayoutData(data);

		// add listener
		reportTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				if (reportTable.getSelectionIndex() != -1)
					okPressed();
			}
		});

		updateTable();

		final TableEditor editor = new TableEditor(reportTable);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
		final int EDITABLECOLUMN = 1;

		reportTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Clean up any previous editor control
				Control oldEditor = editor.getEditor();
				if (oldEditor != null)
					oldEditor.dispose();
				// Identify the selected row
				TableItem item = (TableItem) e.item;
				if (item == null)
					return;

				reportDescriptionEditor = new Text(reportTable, SWT.NONE);
				reportDescriptionEditor.setTextLimit(255);
				reportDescriptionEditor.setText(item.getText(EDITABLECOLUMN));
				reportDescriptionEditor.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent me) {
						Text text = (Text) editor.getEditor();
						editor.getItem().setText(EDITABLECOLUMN, text.getText());
						getButton(APPLY_SELECTION).setEnabled(true);
					}
				});
				reportDescriptionEditor.selectAll();
				reportDescriptionEditor.setFocus();
				editor.setEditor(reportDescriptionEditor, item, EDITABLECOLUMN);
			}
		});
	}

	private void updateTable() {
		// clear old item
		reportTable.clearAll();
		reportTable.setItemCount(0);
		fileList.clear();

		if (reportDescriptionEditor != null) {
			reportDescriptionEditor.dispose();
			reportDescriptionEditor = null;
		}

		// get Report List
		PastReportsHistory pastReportsHistory = new PastReportsHistory();
		fileList = pastReportsHistory.getFileList(projectCombo.getItem(projectCombo.getSelectionIndex()));

		for (File file : fileList) {
			String fileName = file.getName();

			TableItem tableItem = new TableItem(reportTable, SWT.NONE);

			int index = fileName.indexOf("_");
			Date date = new Date(Long.parseLong(fileName.substring(0, index)));

			tableItem.setText(0, date.toString());
			String xmlFilePath = file.getAbsolutePath();
			BadSmellDataEntity badSmellDataManager = new BadSmellDataEntity(xmlFilePath);
			Element descriptionElement = badSmellDataManager.getDescriptionElement();
			String description = "";
			if (descriptionElement != null)
				description = descriptionElement.getValue().toString();
			tableItem.setText(1, description);
		}
	}

	private void saveReportDataDescriptions() {
		for (int i = 0; i < reportTable.getItemCount(); i++) {
			File xmlFile = fileList.get(i);
			String xmlPath = xmlFile.getAbsolutePath();
			String newDescription = reportTable.getItem(i).getText(1);
			BadSmellDataEntity badSmellDataManager = new BadSmellDataEntity(xmlPath);
			badSmellDataManager.setDescriptionElement(newDescription);
			badSmellDataManager.writeXMLFile(xmlPath);
		}
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, APPLY_SELECTION, resource.getString("apply"), true);
		getButton(APPLY_SELECTION).setEnabled(false);
		createButton(parent, DELETE_SELECTION, resource.getString("delete"), true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
	}

	@Override
	public boolean close() {
		if (getButton(APPLY_SELECTION).getEnabled()) {
			MessageBox dialog = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			dialog.setText("Warning");
			dialog.setMessage("Do you really want to save change of description?");
			int buttonId = dialog.open();
			if (buttonId == SWT.YES) {
				saveReportDataDescriptions();
			}
			if (buttonId == SWT.NO || buttonId == SWT.YES) {
				return super.close();
			} else
				return false;
		}
		return super.close();
	}

	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == DELETE_SELECTION) { // delete by selection
			int[] selectIdx = reportTable.getSelectionIndices();

			if (selectIdx.length != 0) {
				for (int index : selectIdx) {
					File reportFolder = fileList.get(index).getParentFile();

					File[] allFile = reportFolder.listFiles();
					for (File file : allFile)
						file.delete();

					reportFolder.delete();
				}
				updateTable();
			}
		}

		// apply modified history of each report for each project
		if (buttonId == APPLY_SELECTION) {
			saveReportDataDescriptions();
			getButton(APPLY_SELECTION).setEnabled(false);
		}
	}

	protected void okPressed() {
		int index = reportTable.getSelectionIndex();

		if (index != -1)
			filePath = fileList.get(index).getAbsolutePath();

		projectName = projectCombo.getItem(projectCombo.getSelectionIndex());
		super.okPressed();
	}

	public String getReportPath() {
		return filePath;
	}

	public String getProjectName() {
		return projectName;
	}
}