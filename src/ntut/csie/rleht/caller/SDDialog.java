package ntut.csie.rleht.caller;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

/**
 * ����Sequence Diagram���]�w����
 */
public class SDDialog extends Dialog{
	
	//�O�_�n���package��RadioButton
	private Button packageRadBtn;
	//�u���class��RadioButton	
	private Button classRadBtn;
	//�O�_�n����ܩҦ���package�W�r��RadioButton
	private Button allRadBtn;
	//���Package�ƥت����s(�ѳ̤W�h���U��)
	private Button topDownRadBtn;
	private Spinner topDownSpinner;
	//���Package�ƥت����s(�ѳ̤U�h���W��)
	private Button buttonUpRadBtn;
	private Spinner buttonUpSpinner;
	
	//������RL
	private Button showRLRadBtn;
	private Button notShowRLRadBtn;
	//������Exception Path
	private Button showAllPathRadBtn;
	private Button onlyShowNameRadBtn;
	
	//�O�_������package
	private boolean isPackage;
	//�O�_��ܥ���package�����
	private boolean isShowAll;
	//�O�_��ܥѤW��U�A�_�h�ѤW��W
	private boolean isTopDown;
	//�����package�ƪ��ܼ�
	private int packageCount;
	//�O�_���Robustness��T
	private boolean isShowRL;
	//�O�_��ܧ��㪺Exception���|
	private boolean isShowAllPath;

	//�O�_���Cancel
	private boolean isCancel = true;

	/**
	 * Create the dialog
	 * @param parentShell
	 */
	public SDDialog(Shell parentShell) {
		super(parentShell);
	}
	
	/**
	 * Create contents of the dialog
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite container = (Composite) super.createDialogArea(parent);
		final GridLayout gridLayout = new GridLayout();
		container.setLayout(gridLayout);

		final Label label = new Label(container, SWT.NONE);
		label.setFont(new Font(null, "default", 10, SWT.BOLD));
		label.setLayoutData(new GridData(386, SWT.DEFAULT));
		label.setText("Instance:");

		packageRadBtn = new Button(container, SWT.RADIO);
		packageRadBtn.setLayoutData(new GridData());
		packageRadBtn.setText("���Package�MClass�W��");

		final Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData());
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 3;
		composite.setLayout(gridLayout_1);

		//�[�J�ť�Label (���F�Y��)
		final Label spaceLabel1 = new Label(composite, SWT.NONE);
		spaceLabel1.setLayoutData(new GridData());
		spaceLabel1.setText("                                  ");

		allRadBtn = new Button(composite, SWT.RADIO);
		allRadBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		allRadBtn.setText("������Package�W�ٳ����");
		new Label(composite, SWT.NONE);

		topDownRadBtn = new Button(composite, SWT.RADIO);
		topDownRadBtn.setText("���Package�ƥ�(�ѳ̤W�h���U��)");
		topDownSpinner = new Spinner(composite, SWT.BORDER);
		new Label(composite, SWT.NONE);
		
		buttonUpRadBtn = new Button(composite, SWT.RADIO);
		buttonUpRadBtn.setText("���Package�ƥ�(�ѳ̤U�h���W��)");
		buttonUpSpinner = new Spinner(composite, SWT.BORDER);

		classRadBtn = new Button(container, SWT.RADIO);
		classRadBtn.setLayoutData(new GridData());
		classRadBtn.setText("�u���Class�W��");

		//���j�u-----------------
		final Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		final GridData separatorGridData = new GridData(385, SWT.DEFAULT);
		separator.setLayoutData(separatorGridData);

		//Robustness Level���ﶵ
		final Label robustnessLevelLabel = new Label(container, SWT.NONE);
		robustnessLevelLabel.setFont(new Font(null, "default", 10, SWT.BOLD));
		robustnessLevelLabel.setLayoutData(new GridData());
		robustnessLevelLabel.setText("Note:");

		//Robustness Level��Composite
		final Composite robustComposite = new Composite(container, SWT.NONE);
		final GridData robustWidthGridData = new GridData(386, SWT.DEFAULT);
		robustComposite.setLayoutData(robustWidthGridData);
		final GridLayout robustColumnGridData = new GridLayout();
		robustComposite.setLayout(robustColumnGridData);

		//���Robustness Level
		showRLRadBtn = new Button(robustComposite, SWT.RADIO);
		showRLRadBtn.setText("���Robustness��T");
		
		final Composite subComposite = new Composite(robustComposite, SWT.NONE);
		subComposite.setLayoutData(new GridData(380, SWT.DEFAULT));
		final GridLayout subGridLayout = new GridLayout();
		subGridLayout.numColumns = 2;
		subComposite.setLayout(subGridLayout);

		//�[�J�ť�Label (���F�Y��)
		final Label spaceLabel2 = new Label(subComposite, SWT.NONE);
		spaceLabel2.setLayoutData(new GridData(99, SWT.DEFAULT));
		spaceLabel2.setText("                                  ");

		//�����Robustness Level
		notShowRLRadBtn = new Button(robustComposite, SWT.RADIO);
		notShowRLRadBtn.setLayoutData(new GridData(379, SWT.DEFAULT));
		notShowRLRadBtn.setText("�����Robustness��T");

		//�O�_���Exception������|
		showAllPathRadBtn = new Button(subComposite, SWT.RADIO);
		showAllPathRadBtn.setLayoutData(new GridData(266, SWT.DEFAULT));
		showAllPathRadBtn.setText("��ܧ��㪺�ҥ~���|");
		new Label(subComposite, SWT.NONE);
		//�u���Exception�W��
		onlyShowNameRadBtn = new Button(subComposite, SWT.RADIO);
		onlyShowNameRadBtn.setText("�u��ܨҥ~�W��");
		
		//�[�J�ƥ󪺺�ť
		addEventListener();
		
		//��l���A
		initialState();
		
		return container;
	}

	/**
	 * �[�J�ƥ󪺺�ť
	 */
	private void addEventListener() {
		//�Y���U���package���s�A����ܩҦ�package�M���package�ƫ��s�]�����
		packageRadBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e)
			{
				//��package���s�]��true
				setPackageRadBtn(true);
				//�Y�O��ܭn���package�B��ܨ���package�ƥءA��ƦrSpinner���}
				if (topDownRadBtn.getSelection())
					topDownSpinner.setEnabled(true);
				if (buttonUpRadBtn.getSelection())
					buttonUpSpinner.setEnabled(true);
			}
		});
		//�Y���U�u���class���s�A����ܩҦ�package�M���package�ƫ��s�]�������
		classRadBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e)
			{
				//��package���s�]��false
				setPackageRadBtn(false);
				topDownSpinner.setEnabled(false);
				buttonUpSpinner.setEnabled(false);
			}
		});
		//�Y���U��ܩҦ�package�A����package�ƪ�Spinner�ܦ������
		allRadBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e)
			{
				topDownSpinner.setEnabled(false);
				buttonUpSpinner.setEnabled(false);
			}
		});
		//�Y���UTopDown���s�A��TopDown��Spinner�]���i��BButtonUp��Spinner�]�������
		topDownRadBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e) 
			{
				topDownSpinner.setEnabled(true);
				buttonUpSpinner.setEnabled(false);
			}
		});
		//�Y���UButtonUp���s�A��ButtonUp��Spinner�]���i��BTopDown��Spinner�]�������
		buttonUpRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				topDownSpinner.setEnabled(false);
				buttonUpSpinner.setEnabled(true);
			}
		});
		//�����RL��T
		notShowRLRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				showAllPathRadBtn.setEnabled(false);
				onlyShowNameRadBtn.setEnabled(false);
			}
		});
		//���RL��T
		showRLRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				showAllPathRadBtn.setEnabled(true);
				onlyShowNameRadBtn.setEnabled(true);
			}
		});
	}
	
	/**
	 * ��l���A
	 */
	private void initialState() {
		//��l���A
		packageRadBtn.setSelection(true);
		allRadBtn.setSelection(true);

		topDownSpinner.setEnabled(false);
		buttonUpSpinner.setEnabled(false);

		showRLRadBtn.setSelection(true);
		notShowRLRadBtn.setSelection(false);

		showAllPathRadBtn.setSelection(true);		
	}

	/**
	 * Create contents of the button bar
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
	}

	protected void buttonPress(int buttonId){
		super.buttonPressed(buttonId);		
	}

	@Override
	protected void okPressed()
	{
		// ....collect data here
		isCancel = false;
		
		//Instance Info
		isPackage = packageRadBtn.getSelection();
		isShowAll = allRadBtn.getSelection();
		isTopDown = topDownRadBtn.getSelection();
		//Robustness Info
		isShowRL = showRLRadBtn.getSelection();
		isShowAllPath = showAllPathRadBtn.getSelection();

		//packageCount�ѿ��TopDown��Button��Spinner�ӨM�w
		if (isTopDown)
			packageCount = topDownSpinner.getSelection();
		else
			packageCount = buttonUpSpinner.getSelection();
		super.okPressed();
	}

	@Override
	protected void cancelPressed()
	{
		//�Y���U������A�O���_��
		isCancel = true;
		super.cancelPressed();
	}

	//�ǥX�O�_Cancel
	public boolean isCancel(){
		return isCancel;
	}
	//�ǥX�O�_���"�n���package"
	public boolean isShowPackage(){
		return isPackage;
	}
	//�ǥX�O�_���"��Ҧ�package�����"
	public boolean isShowAllPackage(){
		return isShowAll;
	}
	//�ǥX�O�_���"�ѤW���U���"
	public boolean isTopDown(){
		return isTopDown;
	}
	//�ǥX"�n���package�����h��"
	public int getPackageCount(){
			return packageCount;
	}
	public boolean isShowRL() {
		return isShowRL;
	}
	public boolean isShowPath() {
		return isShowAllPath;
	}

	//�]�wPackage�t�C���s��true��false
	private void setPackageRadBtn(boolean trueOrfalse) {
		allRadBtn.setEnabled(trueOrfalse);
		topDownRadBtn.setEnabled(trueOrfalse);
		buttonUpRadBtn.setEnabled(trueOrfalse);
	}
	/**
	 * Return the initial size of the dialog
	 */
	@Override
	protected Point getInitialSize()
	{
		return new Point(430, 370);
	}
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Sequence Diagram Model");
	}
}