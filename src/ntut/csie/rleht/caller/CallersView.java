package ntut.csie.rleht.caller;

import java.util.Locale;
import java.util.ResourceBundle;

import net.java.amateras.uml.action.SaveAsImageAction;
import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.common.ConsoleLog;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.common.RLUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class CallersView extends ViewPart implements IDoubleClickListener, ICheckStateListener {
	private static Logger logger = LoggerFactory.getLogger(CallersView.class);

	public static final String ID = "ntut.csie.rleht.caller.CallersView"; //$NON-NLS-1$

	public static final boolean ACT_EDITOR_ON_SELECT = true;

	// private CallersEventHandler eventHandler;

	// -------------------------------------------------------------------------
	// Business Object
	// -------------------------------------------------------------------------

	// private ITextEditor actEditor;
	//
	// private IDocument actDocument;
	//
	// private boolean changeDocument = false;

	private CheckboxTreeViewer treeviewer = null;

	// private String lastMethodName = null;

	private CallersViewAction viewActions = null;

	private Menu treeContextMenu;

	private boolean showCaller = false;

	private IMethod lastMethod = null;

	private IProject lastProject = null;

	private boolean checkFlag = false;
	
	private boolean isShowRLInfo = false;
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	/**
	 * The constructor.
	 */
	public CallersView() {

	}

	public void init(IViewSite site) throws PartInitException {
		super.setSite(site);

		logger.debug("========[CallersView]=========");

		// if (this.eventHandler == null) {
		// this.eventHandler = new CallersEventHandler(this);
		// site.getWorkbenchWindow().getSelectionService().addPostSelectionListener(eventHandler);
		// site.getPage().addPartListener(eventHandler);
		// FileBuffers.getTextFileBufferManager().addFileBufferListener(eventHandler);
		// }
	}

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {

		this.createTreeViewer(parent, false);

		this.createActions();

		this.createTreePopupMenu();

		this.contributeToActionBars();
	}

	private void createTreeViewer(Composite parent, boolean isShow) {	
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());

		int style = SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE ;
		treeviewer = new CheckboxTreeViewer(composite, style);
		treeviewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));

		treeviewer.setContentProvider(new CallersContentProvider());
		treeviewer.setLabelProvider(new CallersLabelProvider());

		Tree tree = treeviewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

        treeviewer.setUseHashlookup(true);
        treeviewer.setAutoExpandLevel(2);

		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText(resource.getString("call.chain"));
		column1.setWidth(400);
		treeviewer.addFilter(new CallersViewerFilter());

		//createRLColumn(tree);

		treeviewer.addDoubleClickListener(this);
		treeviewer.addCheckStateListener(this);
	}

	/**
	 * @param tree
	 */
	private void createRLColumn(Tree tree) {
		TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
		column2.setText(resource.getString("tag"));
		column2.setWidth(250);

		TreeColumn column3 = new TreeColumn(tree, SWT.LEFT);
		column3.setText(resource.getString("ex"));
		column3.setWidth(350);
	}

	private void createActions() {
		viewActions = new CallersViewAction(this);
	}

	private void createTreePopupMenu() {
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		this.treeContextMenu = menuMgr.createContextMenu(this.treeviewer.getControl());
		this.treeviewer.getControl().setMenu(this.treeContextMenu);

		getSite().registerContextMenu(menuMgr, this.treeviewer);
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new GroupMarker(CallersViewAction.MENU_GROUP_ID));

		viewActions.setContext(new ActionContext(this.treeviewer.getSelection()));
		viewActions.fillContextMenu(manager);
		viewActions.setContext(null);

		manager.add(new GroupMarker(IContextMenuConstants.GROUP_ADDITIONS));
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		viewActions.fillActionBars(bars);
	}

	// *************************************************************************

	@Override
	public void setFocus() {
		// Set the focus
	}

	public void dispose() {
		if ((treeContextMenu != null) && !treeContextMenu.isDisposed()) {
			treeContextMenu.dispose();
		}
		super.dispose();

	}

	// *************************************************************************
	// 事件處理
	// *************************************************************************

	public void handleSelectionChanged4Editor() {

		try {
			IEditorPart editor = getSite().getPage().getActiveEditor();
			ISelection selection = editor.getEditorSite().getSelectionProvider().getSelection();
			if ((selection != null) && selection instanceof ITextSelection) {
				ITextSelection textSelection = (ITextSelection) selection;
				IWorkingCopyManager manager = JavaUI.getWorkingCopyManager();
				ICompilationUnit cu = manager.getWorkingCopy(editor.getEditorInput());

				if (cu != null) {
					IMethod method = getMethodAt(cu, textSelection.getOffset());
					if (method != null) {

						// logger.debug("$#$#$#=(handleSelectionChanged4Editor)--->"+method);
						// if (lastMethodName != null &&
						// method.toString().equals(lastMethodName)) {
						// return;
						// }
						// lastMethodName = method.toString();
						//取得使用者所選擇要Call Hierarchy的method
						lastMethod = method;
						// 取得使用者位於的Project
						lastProject = method.getJavaProject().getProject();
						
						this.updateView(method);
					}
				}
			}
		} catch (Exception ex) {
			logger.error("[handleSelectionChanged4Editor] EXCEPTION ", ex);
		}
	}

	public void updateView() {
		this.updateView(this.lastMethod);
	}

	protected void updateView(IMethod method) {
		if (method != null) {
			// 依據showCaller來決定是往上或往下做Call Hierarchy
			// getCallerRoot:由下往上call,getCalleeRoot:由上往下call
			IMember[] methodArray = new IMember[] {method};
			MethodWrapper[] mw = showCaller ? CallHierarchy.getDefault().getCallerRoots(methodArray)
											: CallHierarchy.getDefault().getCalleeRoots(methodArray);
			/* Eclipse3.3:
			 * MethodWrapper mw = showCaller ? CallHierarchy.getDefault().getCallerRoot(method)
			 * 								 : CallHierarchy.getDefault().getCalleeRoot(method);
			 */
			if (mw.length == 1) {
				// 不論是由上往下或由下往上的Call Hierarchy最多都先只展開兩層而已
				// 防止memory一次用太多,容易memory leak
				int expand = showCaller ? 2 : 2;

				CallersRoot root = new CallersRoot(mw[0]);
				treeviewer.setInput(root);
				if (root != null) {
					treeviewer.expandToLevel(expand);
					treeviewer.getTree().setFocus();
					treeviewer.getTree().setSelection(new TreeItem[] {treeviewer.getTree().getItems()[0]});
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private IMethod getMethodAt(ICompilationUnit cu, int offset) {
		if (cu != null) {
			synchronized (cu) {
				try {
					cu.reconcile();
				} catch (JavaModelException ex) {
					logger.error("[getMethodAt] EXCEPTION ", ex);
					return null;
				}
			}

			try {
				IType[] types = cu.getAllTypes();
				for (int i = 0, size = types.length; i < size; i++) {
					IType type = types[i];
					IMethod[] methods = type.getMethods();
					
					for (int j = 0; j < methods.length; j++) {
						if (isWithinMethodRange(offset, methods[j])) {
							return methods[j];
						}
					}

				}
			} catch (JavaModelException ex) {
				logger.error("[getMethodAt] EXCEPTION ", ex);

				return null;
			}
		}

		return null;
	}

	private boolean isWithinMethodRange(int offset, IMethod method) throws JavaModelException {
		ISourceRange range = method.getSourceRange();

		return ((offset >= range.getOffset()) && (offset <= (range.getOffset() + range.getLength())));
	}

	// *************************************************************************
	// 事件處理
	// *************************************************************************

	public void gotoSelection(ISelection selection) {
		ConsoleLog.debug("[gotoSelection]selection=" + selection);
		try {
			if ((selection != null) && selection instanceof IStructuredSelection) {
				Object structuredSelection = ((IStructuredSelection) selection).getFirstElement();

				if (structuredSelection instanceof MethodWrapper) {
					MethodWrapper methodWrapper = (MethodWrapper) structuredSelection;
					CallLocation firstCall = methodWrapper.getMethodCall().getFirstCallLocation();

					if (firstCall != null) {
						gotoLocation(firstCall);
					} else {
						gotoMethod((IMethod) methodWrapper.getMember());
					}
				} else if (structuredSelection instanceof CallLocation) {
					gotoLocation((CallLocation) structuredSelection);
				}
			}
		} catch (Exception e) {
			RLEHTPlugin.logError("Double click ERROR!!", e);
		}
	}

	private void gotoMethod(IMethod method) {
		if (method != null) {
			try {
				IEditorPart methodEditor = RLUtils.openInEditor(method, ACT_EDITOR_ON_SELECT);
				JavaUI.revealInEditor(methodEditor, (IJavaElement) method);
			} catch (JavaModelException e) {
				RLEHTPlugin.logError("取得資源錯誤！", e);
			} catch (PartInitException e) {
				RLEHTPlugin.logError("開啟編輯器錯誤！", e);
			}
		}
	}

	private void gotoLocation(CallLocation callLocation) {
		try {
			IEditorPart methodEditor = RLUtils.openInEditor((IMethod) callLocation.getMember(), ACT_EDITOR_ON_SELECT);

			if (methodEditor instanceof ITextEditor) {
				ITextEditor editor = (ITextEditor) methodEditor;
				editor.selectAndReveal(callLocation.getStart(), (callLocation.getEnd() - callLocation.getStart()));
			}
		} catch (PartInitException ex) {
			RLEHTPlugin.logError("開啟編輯器錯誤！", ex);
		} catch (JavaModelException ex) {
			RLEHTPlugin.logError("取得資源錯誤！", ex);
		} catch (Exception ex) {
			RLEHTPlugin.logError("其它錯誤！", ex);
		}
	}

	public void doubleClick(DoubleClickEvent event) {
		this.gotoSelection(event.getSelection());
	}

	// *************************************************************************
	// ICheckStateListener 事件處理 BEGIN
	// *************************************************************************

	public void checkStateChanged(CheckStateChangedEvent event) {
		logger.debug("[checkStateChanged] BEGIN --->");
		checkFlag = false;
		 
		Object obj = event.getElement();

		if (obj instanceof MethodWrapper) {
			TreeItem[] selection = this.treeviewer.getTree().getSelection();
			//logger.debug("~~~~=>" + selection.length + " :" + selection[0]);
			
			TreeItem item = this.findCheckedItem(selection, obj);
			if (item != null) {			
				//如果使用者勾選Tree中較下層的選項,則會幫他連上層的選項都勾選
				if (item.getChecked()) {
					if(showCaller){
						showCheckData(this.treeviewer.getTree().getItems(),item);
					}	
					item = item.getParentItem();					
					while (item != null) {
						if (!item.getChecked()) {
							item.setChecked(true);
							if(showCaller){
								showCheckData(this.treeviewer.getTree().getItems(),item);
							}
						}						
						item = item.getParentItem();
					}
				} else {
					//假設call chain 為A->B->C,使用者將B取消掉的話,要將C同時取消掉
					disableChildCheckData(item);
				}
			}
		}

		logger.debug("[checkStateChanged] END <---");
	}

	
	private TreeItem findCheckedItem(TreeItem[] items, Object selectedData) {
		logger.debug("\t---->findCheckedItem BEGIN");
		for (int i = 0, size = items.length; i < size; i++) {
			TreeItem item = items[i];
			//selectedData : 被勾選的項目
			logger.debug("\t---->"+ i+" >> " +item.getItemCount()+":"+ selectedData + ":" + item.getData() + " = " + (selectedData == item.getData()) + " = " + (selectedData.equals(item.getData())));
			if (selectedData == item.getData()) {
				return item;
			}

			if (item.getItemCount() >= 1) {
				item = findCheckedItem(item.getItems(), selectedData);
				if (item != null) {
					return item;
				}
			}
		}
		return null;
	}
	
	private void disableChildCheckData(TreeItem actitem) {

		actitem.setChecked(false);
		TreeItem[] items = actitem.getItems();

		if (items != null) {
			for (int i = 0, size = items.length; i < size; i++) {
				TreeItem item = items[i];
				if (item.getChecked()) {
					item.setChecked(false);
					
				}

				if (item.getItemCount() >= 1) {
					disableChildCheckData(item);
				}
			}
		}
	}

	/**
	 * 用來檢查在由下往上call hierarchy的時候是否選擇多條路徑
	 * @param items
	 * @param activeItem
	 */
	private void showCheckData(TreeItem[] items,TreeItem activeItem) {
		for (int i = 0, xsize = items.length; i < xsize; i++) {
			TreeItem item = items[i];
			
			if (item.getData() instanceof MethodWrapper) {
				MethodWrapper mwobj = (MethodWrapper) item.getData();
				MethodWrapper activeMW = (MethodWrapper) activeItem.getData();
				if(item.getChecked() && (activeMW.getLevel() == mwobj.getLevel()) && (item.getData() != activeItem.getData())){
					if(!checkFlag){						
						EditorUtils.showMessage(resource.getString("only.one.path"));
					}
					checkFlag = true;
					activeItem.setChecked(false);
					disableChildCheckData(activeItem);
					break;
				}
					
			}

			if (item.getItemCount() >= 1) {
				showCheckData(item.getItems(),activeItem);
			}

			// // ------------------------------------------------------
			// TreeItem[] items2 = item.getItems();
			// for (int yyy = 0, ysize = items2.length; yyy < ysize; yyy++) {
			// TreeItem item2 = items2[yyy];
			// if (item2.getChecked()) {
			// MethodWrapper mwobj = (MethodWrapper) item2.getData();
			// logger.debug(xxx + ") " + item2.getItemCount() + " : " +
			// mwobj.getName() + ":" + mwobj.getLevel());
			// }
			// }
			// // ------------------------------------------------------

		}
	}

	// *************************************************************************
	// ICheckStateListener 事件處理 END
	// *************************************************************************
	public void handleGenSeqDiagram(boolean isShowCallerType) {
		
		
		//在畫循序圖前先顯示設定視窗
		SDDialog dialog = new SDDialog(new Shell());
		dialog.open();

//		TreeItem[] items = treeviewer.getTree().getItems();
//		test(items);
		//若取消則不畫循序圖
		if (!dialog.isCancel())
			new CallersSeqDiagram().draw(lastProject, this.getSite(), this.treeviewer.getTree().getItems(),
										isShowCallerType, dialog.isShowPackage(), dialog.isShowAllPackage(),
										dialog.isTopDown(),dialog.getPackageCount(),
										dialog.isShowRL(), dialog.isShowPath(), isShowRLInfo);
	}

	public void handleAddRLAnnotation() {
		Object[] checkedItems = this.treeviewer.getCheckedElements();
		for (int i = 0; i < checkedItems.length; i++) {
			logger.debug("###===>" + checkedItems[i]);
		}

	}

	/**
	 * 利用GEF列印產生出來的循序圖
	 * @param editor
	 */
	public void printSequenceDiagram(IWorkbenchPart editor){		
		PrintAction printAction = new PrintAction(editor);
		printAction.run();
	}
	
	public void saveSequenceDiagram(IWorkbenchPart editor){
		SaveAsImageAction saveSDAction = new SaveAsImageAction((GraphicalViewer)editor);
		saveSDAction.run();
	}
	
	public void handleChangeShowView() {
		this.updateView(this.lastMethod);
	}

	// *************************************************************************
	// Setter / Getter
	// *************************************************************************

	public void setShowType(boolean showCaller) {
		this.showCaller = showCaller;
	}

	public boolean isShowCallerType() {
		return this.showCaller;
	}

	public void showRLInfo(boolean isShow) {
		Tree tree= treeviewer.getTree();
		this.isShowRLInfo = isShow;

		if (isShow) {
			createRLColumn(tree);
		} else {
			//取得Column
			TreeColumn column1 = tree.getColumn(1);
			TreeColumn column2 = tree.getColumn(2);
			//刪除Column
			column1.dispose();
			column2.dispose();
		}
	}
}
