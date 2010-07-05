package ntut.csie.rleht.caller;

import net.java.amateras.uml.sequencediagram.SequenceDiagramEditor;
import ntut.csie.rleht.common.ImageManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;

public class CallersViewAction extends ActionGroup {

	private static final String TYPE_CALL_UP = "顯示上層呼叫者階層";
	private static final String TYPE_CALL_DOWN = "顯示呼叫階層";

	public static final String MENU_GROUP_ID = "MAIN_MENU";

	private CallersView view;

	private RefreshAction refreshAction;

	private boolean showAddRLAnnotation = false;

	private AddRLAnnotationAction addRLAnnotationAction;

	private GenSeqDiagramSelectedAction genSeqDiagramSelectedAction;

	private ChangeCallerTypeAction showCallerInfoAction;

	private ChangeCallerTypeAction showCalleeInfoAction;

	//顯示Robustness Level與例外資訊
	private ShowRobustnessInfoAction showRLInfoAction;
	
	//列印產生的Sequence Diagram的動作
	private PrintSDAction printSDAction; 
	
	//將產生的Sequence Diagram存檔的動作
//	private SaveSDAction saveSDAction;

	public CallersViewAction(CallersView view) {
		this.view = view;
		createActions();
	}

	/**
	 * Create the actions
	 */
	private void createActions() {

		this.showRLInfoAction = new ShowRobustnessInfoAction();

		this.refreshAction = new RefreshAction();
		if (showAddRLAnnotation) {
			this.addRLAnnotationAction = new AddRLAnnotationAction();
		}
		this.genSeqDiagramSelectedAction = new GenSeqDiagramSelectedAction();
		this.showCallerInfoAction = new ChangeCallerTypeAction(TYPE_CALL_UP, TYPE_CALL_UP, ImageManager.getInstance().getDescriptor("CallUp"), this.getView().isShowCallerType());
		this.showCalleeInfoAction = new ChangeCallerTypeAction(TYPE_CALL_DOWN, TYPE_CALL_DOWN, ImageManager.getInstance().getDescriptor("CallDown"), !this.getView().isShowCallerType());

		this.printSDAction = new PrintSDAction();
//		this.saveSDAction = new SaveSDAction();
	}
	
	/**
	 * 顯示例外與強健度資訊
	 * @author Shiau
	 */
	private class ShowRobustnessInfoAction extends Action {
		public ShowRobustnessInfoAction() {
			super("&顯示例外資訊", ImageManager.getInstance().getDescriptor("annotation"));
			setToolTipText("&顯示例外資訊");
			this.setChecked(false);
		}

		/**
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			//設置是否顯示Exception資訊
			if (showRLInfoAction.isChecked())
				getView().showRLInfo(true);
			else
				getView().showRLInfo(false);
			
			//更新
			getView().updateView();
		}
	}
	
	private class RefreshAction extends Action {
		public RefreshAction() {
			super("&Refresh@F5", ImageManager.getInstance().getDescriptor("refresh"));
			setToolTipText("&Refresh the view (F5)");
		}

		/**
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			getView().updateView();
		}
	}

	/**
	 * print the Sequence Diagram 
	 */
	private class PrintSDAction extends Action{
		public PrintSDAction(){
			super("列印循序圖",ImageManager.getInstance().getDescriptor("print_sd"));
			setToolTipText("列印循序圖");
			
		}
		
		public void run(){				
			IWorkbenchPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();			
			if(editor instanceof SequenceDiagramEditor){
				getView().printSequenceDiagram(editor);
				
			}
		}
	}
	
	private class AddRLAnnotationAction extends Action {
		public AddRLAnnotationAction() {
			super("新增所有勾選項目的RL Annotation", ImageManager.getInstance().getDescriptor("annotation"));
			setToolTipText("新增所有勾選項目的RL Annotation");
		}

		/**
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			getView().handleAddRLAnnotation();
		}
	}

	/**
	 * 清除所勾選的項目
	 * @author chenyf
	 */
	private class GenSeqDiagramSelectedAction extends Action {
		public GenSeqDiagramSelectedAction() {
			super("產生循序圖", ImageManager.getInstance().getDescriptor("unchecked"));
			setToolTipText("產生循序圖");
		}

		/*
		 * @see Action#run
		 */
		public void run() {
			getView().handleGenSeqDiagram(getView().isShowCallerType());	
		}
	}

	private class ChangeCallerTypeAction extends Action {
		public ChangeCallerTypeAction(String text, String tooltip, ImageDescriptor image, boolean checked) {
			super(text, image);
			setToolTipText(tooltip);
			this.setChecked(checked);
		}

		public void run() {
			resetOtherChecked(this);
			getView().setShowType(showCallerInfoAction.isChecked());
			getView().handleChangeShowView();
		}
	}

	protected void resetOtherChecked(ChangeCallerTypeAction action) {
		(action == showCallerInfoAction ? showCalleeInfoAction : showCallerInfoAction).setChecked(false);
	}

	protected CallersView getView() {
		return view;
	}

	// *************************************************************************

	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		fillToolBar(actionBars.getToolBarManager());
		fillViewMenu(actionBars.getMenuManager());

	}

	void fillToolBar(IToolBarManager toolBar) {
		toolBar.removeAll();
		toolBar.add(showRLInfoAction);
		
		toolBar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		
		toolBar.add(showCallerInfoAction);
		toolBar.add(showCalleeInfoAction);

		toolBar.add(refreshAction);
		
		toolBar.add(genSeqDiagramSelectedAction);
		toolBar.add(printSDAction);
		//toolBar.add(saveSDAction);
		
		if (showAddRLAnnotation) {
			toolBar.add(addRLAnnotationAction);
		}
	}

	void fillViewMenu(IMenuManager menu) {
		menu.add(this.showRLInfoAction);
		
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(this.showCallerInfoAction);
		menu.add(this.showCalleeInfoAction);

		menu.add(refreshAction);
		menu.add(genSeqDiagramSelectedAction);
		menu.add(printSDAction);
		//menu.add(saveSDAction);

		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		if (showAddRLAnnotation) {
			menu.add(addRLAnnotationAction);
		}

	}

	public void fillContextMenu(IMenuManager menuMgr) {
		menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, this.showRLInfoAction);
		
		menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, new Separator());

		menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, this.showCallerInfoAction);
		menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, this.showCalleeInfoAction);

		menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, this.refreshAction);
		menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, this.genSeqDiagramSelectedAction);
		menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, this.printSDAction);
		//menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, this.saveSDAction);
		menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		if (showAddRLAnnotation) {
			menuMgr.appendToGroup(CallersViewAction.MENU_GROUP_ID, this.addRLAnnotationAction);
		}
		super.fillContextMenu(menuMgr);
	}

}
