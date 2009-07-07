package ntut.csie.csdet.report.ui;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.views.EditRuleDialog;
import ntut.csie.rleht.common.ImageManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

public class FilterDialog extends Dialog {

	private TreeMap<String, Boolean> filterMap = new TreeMap<String, Boolean>();
	private boolean isAllPackage;
	private Button AllRadBtn;
	private Button selectRadBtn;
	private Composite filterComposite;
	private Button editBtn;
	private Composite btnComposite;
	private Table displayTable;
	private Text tempText;

	public FilterDialog(Shell parentShell) {
		super(parentShell);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("EH Smell Filter");		
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(403, 366);
	}

	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);

		///��������Project��Button///
		AllRadBtn = new Button(container, SWT.RADIO);
		AllRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				setFilterRuleBtn(false);
			}
		});
		AllRadBtn.setBounds(10, 10, 374, 16);
		AllRadBtn.setText("Detect All Project");

		///���Package��Button///
		selectRadBtn = new Button(container, SWT.RADIO);
		selectRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				setFilterRuleBtn(true);
			}
		});
		selectRadBtn.setBounds(10, 32, 374, 16);
		selectRadBtn.setText("Detect Select Package");

		///Filter������Composite///
		filterComposite = new Composite(container, SWT.NONE);
		filterComposite.setBounds(41, 54, 343, 233);

		///��ܤ�r��Label///
		final Label nameLabel = new Label(filterComposite, SWT.NONE);
		nameLabel.setBounds(7, 10, 97, 12);
		nameLabel.setText("Filter Rule: ");

		///���~�ϥܻP���~�T��///
		final Label warningLabel = new Label(filterComposite, SWT.NONE);
		warningLabel.setBounds(32, 212, 85, 12);
		warningLabel.setVisible(false);
		warningLabel.setText("Filter Rule�w�s�b");
		final Label picLabel = new Label(filterComposite, SWT.NONE);
		picLabel.setBounds(10, 212, 16, 15);
		picLabel.setVisible(false);
		picLabel.setImage(ImageManager.getInstance().get("warning"));

		///����User��JRule��Text///
		tempText = new Text(filterComposite, SWT.BORDER);
		tempText.setBounds(10, 28, 243, 22);
		tempText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				//�YText�@���ܴN��ĵ�i�T������
				setWarning(warningLabel, picLabel, false);
			}
		});

		///���Rule��Table///
		displayTable = new Table(filterComposite, SWT.FULL_SELECTION | SWT.CHECK | SWT.MULTI | SWT.BORDER);
		displayTable.setFont(new Font(this.getShell().getDisplay(),"Arial", 11,SWT.NONE));
		displayTable.setBounds(10, 56, 243, 150);
		final GridData gd_testList = new GridData(SWT.FILL, SWT.FILL, true, true);
		displayTable.setLayoutData(gd_testList);
		//�p�G��ܪ�displayTable��Item���W����ܦbText�W
		displayTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				int selectionIndex = displayTable.getSelectionIndex();
				//����@�}Dialog�N���Ŀ�checkbox,�X�{index=-1�����p
				if(selectionIndex >= 0) {
					//���ܪ�Item��Library�W����ܦbText�W
					editBtn.setEnabled(true);
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});
		//�Y�bdisplayTable��Item�W�I���U�A���X�ק����
		displayTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				int selectionIndex = displayTable.getSelectionIndex();
				if (selectionIndex >= 0) {
					String temp = displayTable.getItem(selectionIndex).getText();
					//�I�s�ק�Dialog
					EditRuleDialog dialog = new EditRuleDialog(new Shell(),temp,displayTable);
					dialog.open();
					//��Edit���G�ק�ϥγ��ҿ����Item
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});

		///Button��Composite///
		btnComposite = new Composite(filterComposite, SWT.NONE);
		btnComposite.setBounds(259, 28, 68, 199);

		///�W�[Rule��Button///
		final Button addBtn = new Button(btnComposite, SWT.NONE);
		addBtn.setBounds(0, 0, 68, 22);
		addBtn.setText("Add");
		addBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				boolean isWarning = addRule();
				//�Y���ƴN���ĵ�i�T��
				if (isWarning)
					setWarning(warningLabel, picLabel, true);
			}
		});

		///�R��Rule��Button///
		final Button removeButton = new Button(btnComposite, SWT.NONE);
		removeButton.setBounds(0, 28, 68, 22);
		removeButton.setText("Remove");
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//Table�����Ū��B�����Library �N���ܪ�Library���R��
				if (displayTable.getItemCount() != 0 && displayTable.getSelectionIndex()!=-1) {
					displayTable.remove(displayTable.getSelectionIndices());
					//�R���ɧ�Text�M��
					tempText.setText("");
				}
			}
		});

		///�ק諸Button///
		editBtn = new Button(btnComposite, SWT.NONE);
		editBtn.setEnabled(false);
		editBtn.setBounds(0, 56, 68, 22);
		editBtn.setText("Edit");
		editBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int selectionIndex = displayTable.getSelectionIndex();
				if (selectionIndex >= 0) {
					String temp = displayTable.getItem(selectionIndex).getText();
					//�I�s�ק�Dialog
					EditRuleDialog dialog = new EditRuleDialog(new Shell(),temp,displayTable);
					dialog.open();
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});

		///��ܥ���Item��Button///
		final Button selectBtn = new Button(btnComposite, SWT.NONE);
		selectBtn.setBounds(0, 84, 68, 22);
		selectBtn.setText("Select All");
		selectBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//��ܥ�����Item
				for (int i=0;i<displayTable.getItemCount();i++) {
					TableItem item = displayTable.getItem(i);
					item.setChecked(true);
				}
			}
		});

		///��������ܪ�Button///
		final Button clearBtn = new Button(btnComposite, SWT.NONE);
		clearBtn.setBounds(0, 112, 68, 22);
		clearBtn.setText("Deselect All");
		clearBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//����������Item
				for (int i=0;i<displayTable.getItemCount();i++) {
					TableItem item = displayTable.getItem(i);
					item.setChecked(false);
				}
			}
		});
		
		///��������///
		final Button explainBtn = new Button(btnComposite, SWT.NONE);
		explainBtn.setBounds(0, 140,68, 22);
		explainBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//���X������Dialog
				MessageDialog.openInformation(
						new Shell(),
						"����",
						"1.Package \n" +
						"   (eg. 'sample.test' -> �u�����S�wPackage�W�٪�Package)\n\n" +
						"2.Package + .* \n" +
						"   (eg. 'sample.*' -> �������N�}�Y��sampleg��Package \n" +
						"                              �p: 'sample.test' �B 'sample.test.example'... ) \n\n");
			}
		});
		explainBtn.setText("HELP");
		explainBtn.setImage(ImageManager.getInstance().get("help"));
		

		//�qXML�����o���eUser���]�w
		getFilterSettings();

		//�N�Ҧ������ר���ܦbtable��
		setInput();

		//�Y���e�]�w��Select�b�}�l�ɴN���Select
		if (!isAllPackage)
			selectRadBtn.setSelection(true);

		return parent;
	}

	/**
	 * �qXML�����o���eUser���]�w
	 */
	private void getFilterSettings() {
		Document docJDom = JDomUtil.readXMLFile();
		if(docJDom != null) {
			//�qXML��Ū�X���e���]�w
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.EHSmellFilterTaq) != null)
			{
				Element filter = root.getChild(JDomUtil.EHSmellFilterTaq).getChild("filter");
				isAllPackage = Boolean.valueOf(filter.getAttribute("IsAllPackage").getValue());
				
				List<Attribute> libRuleList = filter.getAttributes();
				//��ϥΪ̩��x�s��Filter�W�h�]�w�s��Map��Ƹ�
				for (int i=0;i<libRuleList.size();i++) {
					if (libRuleList.get(i).getQualifiedName() == "IsAllPackage")
						continue;

					//��EH_STAR���N���Ÿ�"*"
					String temp = libRuleList.get(i).getQualifiedName().replace("EH_STAR", "*");
					filterMap.put(temp,libRuleList.get(i).getValue().equals("true"));
				}
			} else {
				isAllPackage = true;
			}
		} else {
			isAllPackage = true;
		}
	}
	
	/**
	 * �x�s�]�w��XML
	 */
	private void storeSettings() {
		//����xml��root
		Element root = JDomUtil.createXMLContent();
		//�إ�dummyhandler��tag		
		Element smellFilter = new Element(JDomUtil.EHSmellFilterTaq);
		Element filter = new Element("filter");

		//�x�s�O�_����������Package
		filter.setAttribute("IsAllPackage", Boolean.toString(AllRadBtn.getSelection()));

		smellFilter.addContent(filter);

		//�Y���e�w�x�sFilter Rule�h�R��
		if (root.getChild(JDomUtil.EHSmellFilterTaq) != null)
			root.removeChild(JDomUtil.EHSmellFilterTaq);

		TableItem[] temp = displayTable.getItems();
		//�htraverse���table��item��Text�M�O�_�Q�Ŀ��
		for(int i=0;i<temp.length;i++) {
			String libName = temp[i].getText().replace("*", "EH_STAR");
			filter.setAttribute(libName, String.valueOf(temp[i].getChecked()));
		}

		root.addContent(smellFilter);

		//�N�ɮ׼g�^
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}
	
	/**
	 * �O�_�NFilter Composite������]���i�ϥ�
	 * @param isEnable
	 */
	private void setFilterRuleBtn(boolean isEnable)
	{
		Control[] fcChild = filterComposite.getChildren();
		for (Control control: fcChild)
			control.setEnabled(isEnable);

		Control[] bcChild = btnComposite.getChildren();
		for (Control control: bcChild)
			control.setEnabled(isEnable);
			
		editBtn.setEnabled(false);
	}
	
	/**
	 * �W�[Rule���
	 */
	private boolean addRule() {
		boolean isWarning = false;
		//�R��Text�e��Ů泡��
		String temp = tempText.getText().trim();

		if (tempText.getText().length() != 0) {
			boolean isExist = false;
			//��Library��Name���S������
			for(int i=0;i<displayTable.getItemCount();i++)
			{
				if(temp.equals(displayTable.getItem(i).getText()))
					isExist = true;
			}
			//�S�����ƴN�[�J�s��Library
			if (!isExist){
				TableItem item = new TableItem(displayTable,SWT.NONE);
				item.setText(temp);
				item.setChecked(true);
				tempText.setText("");
			}else{
				tempText.setText(temp);
				isWarning = true;
			}
		}
		return isWarning;
	}
	
	/**
	 * �]�wWarning����ܻP�_
	 * @param warningLabel
	 * @param picLabel
	 */
	private void setWarning(final Label warningLabel, final Label picLabel, boolean isWarning) {
		picLabel.setVisible(isWarning);
		warningLabel.setVisible(isWarning);
	}
	
	/**
	 * �N�Ҧ������ר���ܦbtable��
	 */
	private void setInput(){
		Iterator<String> filterIt = filterMap.keySet().iterator();
		while(filterIt.hasNext()){
			String temp = filterIt.next();
			TableItem item = new TableItem(displayTable,SWT.NONE);
			item.setText(temp);
			item.setChecked(filterMap.get(temp));
		}
	}

	/**
	 * store dates
	 */
	protected void okPressed() {
		//�x�s�]�w��XML
		storeSettings();

		super.okPressed();
	}
}
