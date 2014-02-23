package ntut.csie.csdet.views;

import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.preference.SmellSettings;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
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
import org.jdom.Element;

public class SelectedFolderPage extends APropertyPage {
	private Composite mainPageComposite;
	private Composite selectComposite;
	private Group folderGroup;
	private Table selectedFolderList;
	private Button checkbox_DetectAllFolder;
	private RobustaSettings robustaSettings;

	private boolean[] folderList;

	public static String[] rootFolderList;
	private boolean isFolderSelect = false;

	private ResourceBundle resource = ResourceBundle.getBundle("robusta",
			new Locale("en", "US"));

	public SelectedFolderPage(Composite composite, CSPropertyPage page,
			RobustaSettings robustaSettings) {
		super(composite, page);
		mainPageComposite = composite;
		this.robustaSettings = robustaSettings;

		initailState(robustaSettings.getProject());
		readSetting();
		buildPage(composite);
		setUserSetting();
		selectedFolderList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				// 取得點選的Item
				TableItem[] item = selectedFolderList.getItems();
				for (int i = 0; i < item.length; i++) {
					if (!item[i].getChecked()) {
						isFolderSelect = false;
						checkbox_DetectAllFolder.setSelection(isFolderSelect);
						return;
					}
				}
			}
		});

	}

	// get all java source folders in an eclipse project in the workspace
	private String[] getJavaProjectSourceDirectories(String projectName) {
		ArrayList paths = new ArrayList();
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		if (project.isOpen() && JavaProject.hasJavaNature(project)) {

			IJavaProject javaProject = JavaCore.create(project);

			IClasspathEntry[] classpathEntries = null;
			try {
				classpathEntries = javaProject.getResolvedClasspath(true);
			} catch (Exception e) {

			}
			for (int i = 0; i < classpathEntries.length; i++) {
				IClasspathEntry entry = classpathEntries[i];

				if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
					IPath path = entry.getPath();

					String srcPath = path.segments()[path.segmentCount() - 1];
					paths.add(srcPath);

				}

			}
		}
		return (String[]) paths.toArray(new String[0]);

	}

	private void initailState(IProject project) {
		rootFolderList = getJavaProjectSourceDirectories(project.getName());
		folderList = new boolean[rootFolderList.length];
		for (int i = 0; i < folderList.length; i++)
			folderList[i] = true;

	}

	private void readSetting() {
		Element[] folderElements = new Element[rootFolderList.length];
		for (int i = 0; i < rootFolderList.length; i++) {
			folderElements[i] = robustaSettings
					.getProjectDetect(rootFolderList[i]);
		}
		for (int i = 0; i < rootFolderList.length; i++) {
			folderList[i] = Boolean.parseBoolean(folderElements[i]
					.getAttributeValue(SmellSettings.ATTRIBUTE_ENABLE));
		}

	}

	private void buildPage(Composite composite) {

		selectComposite = new Composite(composite, SWT.NONE);
		selectComposite = new Composite(composite, SWT.NONE);
		// create group
		folderGroup = new Group(selectComposite, SWT.NONE);
		folderGroup.setText(resource.getString("selectedFolderPage.folderList"));
		folderGroup.setLocation(10, 5);

		// create checkbox in group
		checkbox_DetectAllFolder = new Button(folderGroup, SWT.CHECK);
		checkbox_DetectAllFolder.setText(resource
				.getString("selectedFolderPage.checkAllFolder"));
		checkbox_DetectAllFolder.setLocation(10, 20);
		checkbox_DetectAllFolder.pack();
		checkbox_DetectAllFolder.setSelection(isFolderSelect);
		checkbox_DetectAllFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				isFolderSelect = !isFolderSelect;
				setUserSetting();
			}
		});

		// create table
		selectedFolderList = new Table(folderGroup, SWT.CHECK | SWT.SINGLE
				| SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION);
		selectedFolderList.setLocation(checkbox_DetectAllFolder.getBounds().x,
				checkbox_DetectAllFolder.getBounds().y
						+ checkbox_DetectAllFolder.getBounds().height);
		selectedFolderList.setFont(new Font(composite.getDisplay(), "Arial",
				11, SWT.NONE));
		selectedFolderList.setLinesVisible(true);
		selectedFolderList.setHeaderVisible(true);
		selectedFolderList.setItemCount(rootFolderList.length);

		// create 1 column
		final TableColumn folderColumn = new TableColumn(selectedFolderList,
				SWT.NONE);
		String folderColumnDisplayText = "List";
		folderColumn.setText(folderColumnDisplayText);
		folderColumn.setWidth(folderColumnDisplayText.length() * 9);

		for (int i = 0; i < rootFolderList.length; i++) {
			TableItem item = selectedFolderList.getItem(i);
			item.setText(0, rootFolderList[i]);
			item.setFont(0, new Font(composite.getDisplay(), "Arial", 11,
					SWT.BOLD));

		}

		folderColumn.pack();

		selectedFolderList.pack();

		folderGroup.pack();

		selectComposite.pack();

		selectComposite.setSize(getLowerRightCoordinate(folderGroup).x + 15,
				selectComposite.getBounds().height);

	}

	private void setUserSetting() {
		TableItem[] item = selectedFolderList.getItems();
		// 去traverse整個table看item的Text和是否被勾選到
		for (int i = 0; i < item.length; i++) {
			if (isFolderSelect)
				folderList[i] = isFolderSelect;
			item[i].setChecked(folderList[i]);
		}
	}

	@Override
	public boolean storeSettings() {
		// update enable Attribute
		TableItem[] item = selectedFolderList.getItems();
		for (int i = 0; i < rootFolderList.length; i++) {
			robustaSettings.setProjectDetectAttribute(rootFolderList[i],
					robustaSettings.ATTRIBUTE_ENABLE, item[i].getChecked());
		}

		String projectPath = UserDefinedMethodAnalyzer
				.getRobustaSettingXMLPath(robustaSettings.getProject());
		robustaSettings.writeNewXMLFile(projectPath);
		return true;

	}

}
