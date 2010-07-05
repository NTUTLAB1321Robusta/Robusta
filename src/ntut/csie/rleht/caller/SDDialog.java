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
 * 產生Sequence Diagram的設定視窗
 */
public class SDDialog extends Dialog{
	
	//是否要顯示package的RadioButton
	private Button packageRadBtn;
	//只顯示class的RadioButton	
	private Button classRadBtn;
	//是否要全顯示所有的package名字的RadioButton
	private Button allRadBtn;
	//選擇Package數目的按鈕(由最上層往下數)
	private Button topDownRadBtn;
	private Spinner topDownSpinner;
	//選擇Package數目的按鈕(由最下層往上數)
	private Button buttonUpRadBtn;
	private Spinner buttonUpSpinner;
	
	//選擇顯示RL
	private Button showRLRadBtn;
	private Button notShowRLRadBtn;
	//選擇顯示Exception Path
	private Button showAllPathRadBtn;
	private Button onlyShowNameRadBtn;
	
	//是否選擇顯示package
	private boolean isPackage;
	//是否選擇全部package都顯示
	private boolean isShowAll;
	//是否顯示由上到下，否則由上到上
	private boolean isTopDown;
	//欲顯示package數的變數
	private int packageCount;
	//是否顯示Robustness資訊
	private boolean isShowRL;
	//是否顯示完整的Exception路徑
	private boolean isShowAllPath;

	//是否選擇Cancel
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
		packageRadBtn.setText("顯示Package和Class名稱");

		final Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData());
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 3;
		composite.setLayout(gridLayout_1);

		//加入空白Label (為了縮排)
		final Label spaceLabel1 = new Label(composite, SWT.NONE);
		spaceLabel1.setLayoutData(new GridData());
		spaceLabel1.setText("                                  ");

		allRadBtn = new Button(composite, SWT.RADIO);
		allRadBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		allRadBtn.setText("全部的Package名稱都顯示");
		new Label(composite, SWT.NONE);

		topDownRadBtn = new Button(composite, SWT.RADIO);
		topDownRadBtn.setText("選擇Package數目(由最上層往下數)");
		topDownSpinner = new Spinner(composite, SWT.BORDER);
		new Label(composite, SWT.NONE);
		
		buttonUpRadBtn = new Button(composite, SWT.RADIO);
		buttonUpRadBtn.setText("選擇Package數目(由最下層往上數)");
		buttonUpSpinner = new Spinner(composite, SWT.BORDER);

		classRadBtn = new Button(container, SWT.RADIO);
		classRadBtn.setLayoutData(new GridData());
		classRadBtn.setText("只顯示Class名稱");

		//分隔線-----------------
		final Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		final GridData separatorGridData = new GridData(385, SWT.DEFAULT);
		separator.setLayoutData(separatorGridData);

		//Robustness Level的選項
		final Label robustnessLevelLabel = new Label(container, SWT.NONE);
		robustnessLevelLabel.setFont(new Font(null, "default", 10, SWT.BOLD));
		robustnessLevelLabel.setLayoutData(new GridData());
		robustnessLevelLabel.setText("Note:");

		//Robustness Level的Composite
		final Composite robustComposite = new Composite(container, SWT.NONE);
		final GridData robustWidthGridData = new GridData(386, SWT.DEFAULT);
		robustComposite.setLayoutData(robustWidthGridData);
		final GridLayout robustColumnGridData = new GridLayout();
		robustComposite.setLayout(robustColumnGridData);

		//顯示Robustness Level
		showRLRadBtn = new Button(robustComposite, SWT.RADIO);
		showRLRadBtn.setText("顯示Robustness資訊");
		
		final Composite subComposite = new Composite(robustComposite, SWT.NONE);
		subComposite.setLayoutData(new GridData(380, SWT.DEFAULT));
		final GridLayout subGridLayout = new GridLayout();
		subGridLayout.numColumns = 2;
		subComposite.setLayout(subGridLayout);

		//加入空白Label (為了縮排)
		final Label spaceLabel2 = new Label(subComposite, SWT.NONE);
		spaceLabel2.setLayoutData(new GridData(99, SWT.DEFAULT));
		spaceLabel2.setText("                                  ");

		//不顯示Robustness Level
		notShowRLRadBtn = new Button(robustComposite, SWT.RADIO);
		notShowRLRadBtn.setLayoutData(new GridData(379, SWT.DEFAULT));
		notShowRLRadBtn.setText("不顯示Robustness資訊");

		//是否顯示Exception完整路徑
		showAllPathRadBtn = new Button(subComposite, SWT.RADIO);
		showAllPathRadBtn.setLayoutData(new GridData(266, SWT.DEFAULT));
		showAllPathRadBtn.setText("顯示完整的例外路徑");
		new Label(subComposite, SWT.NONE);
		//只顯示Exception名稱
		onlyShowNameRadBtn = new Button(subComposite, SWT.RADIO);
		onlyShowNameRadBtn.setText("只顯示例外名稱");
		
		//加入事件的監聽
		addEventListener();
		
		//初始狀態
		initialState();
		
		return container;
	}

	/**
	 * 加入事件的監聽
	 */
	private void addEventListener() {
		//若按下顯示package按鈕，把顯示所有package和選擇package數按鈕設成能選
		packageRadBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e)
			{
				//把package按鈕設成true
				setPackageRadBtn(true);
				//若是選擇要顯示package且選擇到選擇package數目，把數字Spinner打開
				if (topDownRadBtn.getSelection())
					topDownSpinner.setEnabled(true);
				if (buttonUpRadBtn.getSelection())
					buttonUpSpinner.setEnabled(true);
			}
		});
		//若按下只顯示class按鈕，把顯示所有package和選擇package數按鈕設成不能選
		classRadBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e)
			{
				//把package按鈕設成false
				setPackageRadBtn(false);
				topDownSpinner.setEnabled(false);
				buttonUpSpinner.setEnabled(false);
			}
		});
		//若按下顯示所有package，把選擇package數的Spinner變成不能選
		allRadBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e)
			{
				topDownSpinner.setEnabled(false);
				buttonUpSpinner.setEnabled(false);
			}
		});
		//若按下TopDown按鈕，把TopDown的Spinner設成可選、ButtonUp的Spinner設成不能選
		topDownRadBtn.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(final SelectionEvent e) 
			{
				topDownSpinner.setEnabled(true);
				buttonUpSpinner.setEnabled(false);
			}
		});
		//若按下ButtonUp按鈕，把ButtonUp的Spinner設成可選、TopDown的Spinner設成不能選
		buttonUpRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				topDownSpinner.setEnabled(false);
				buttonUpSpinner.setEnabled(true);
			}
		});
		//不顯示RL資訊
		notShowRLRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				showAllPathRadBtn.setEnabled(false);
				onlyShowNameRadBtn.setEnabled(false);
			}
		});
		//顯示RL資訊
		showRLRadBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				showAllPathRadBtn.setEnabled(true);
				onlyShowNameRadBtn.setEnabled(true);
			}
		});
	}
	
	/**
	 * 初始狀態
	 */
	private void initialState() {
		//初始狀態
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

		//packageCount由選擇TopDown或Button的Spinner來決定
		if (isTopDown)
			packageCount = topDownSpinner.getSelection();
		else
			packageCount = buttonUpSpinner.getSelection();
		super.okPressed();
	}

	@Override
	protected void cancelPressed()
	{
		//若按下取消鍵，記錄起來
		isCancel = true;
		super.cancelPressed();
	}

	//傳出是否Cancel
	public boolean isCancel(){
		return isCancel;
	}
	//傳出是否選擇"要顯示package"
	public boolean isShowPackage(){
		return isPackage;
	}
	//傳出是否選擇"把所有package都顯示"
	public boolean isShowAllPackage(){
		return isShowAll;
	}
	//傳出是否選擇"由上往下顯示"
	public boolean isTopDown(){
		return isTopDown;
	}
	//傳出"要顯示package的階層數"
	public int getPackageCount(){
			return packageCount;
	}
	public boolean isShowRL() {
		return isShowRL;
	}
	public boolean isShowPath() {
		return isShowAllPath;
	}

	//設定Package系列按鈕為true或false
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