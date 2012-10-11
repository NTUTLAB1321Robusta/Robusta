package ntut.csie.csdet.smell.ui;

import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.common.ConsoleLog;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.common.ErrorLog;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EHSmellView extends ViewPart implements IShowInSource {
	private static Logger logger = LoggerFactory.getLogger(EHSmellView.class);
	
	private Table smellList;
	
	// -------------------------------------------------------------------------
	// GUI Object
	// -------------------------------------------------------------------------
	private TableEditor editor;

	private EHSmellViewEventHandler eventHandler;

	// -------------------------------------------------------------------------
	// Business Object
	// -------------------------------------------------------------------------

	private EHSmellModel model = null;

	private ITextEditor actEditor;

	private IDocument actDocument;

	private boolean changeDocument = false;

	// *************************************************************************
	// initiation
	// *************************************************************************
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	public EHSmellView(){
		model = new EHSmellModel();
	}
	
	public void init(IViewSite site) throws PartInitException {
		super.setSite(site);
		//�[�J���b�ϥ�Edit��Listener
		if (this.eventHandler == null) {
			this.eventHandler = new EHSmellViewEventHandler(this);
			site.getWorkbenchWindow().getSelectionService().addPostSelectionListener(eventHandler);
			site.getPage().addPartListener(eventHandler);
			FileBuffers.getTextFileBufferManager().addFileBufferListener(eventHandler);
		}
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		//�ظmsmellList
		buildTableList(parent);
		//�ظmsmellList��Listener
		smellListTableDoubleClick();
		
		//TODO Menu�٥��g
		//�ظmMenu
		buildMenu();

		ConsoleLog.debug("��l�Ƨ����I");
		try {
			IEditorPart part = EditorUtils.getActiveEditor();
			if (part instanceof ITextEditor) {
				getEditorInput((ITextEditor) part);
			}

		} catch (CoreException e) {
			ErrorLog.getInstance().logError(e.getLocalizedMessage(), e);
		}
		initializeToolBar();
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		this.smellList.setFocus();
	}

	// *************************************************************************
	// GUI
	// *************************************************************************
	/**
	 * �ظmMenu
	 */
	private void buildMenu() {
		//hook context menu
//		final Action dh= new DHAction(this); 
//		final Action nt= new NTAction(this);
//		final Action te= new TEAction(this);
//		final Action um= new UMAction(this);
//
//		MenuManager menuMgr = new MenuManager();
//		menuMgr.setRemoveAllWhenShown(true);
//		menuMgr.addMenuListener(new IMenuListener() {
//			public void menuAboutToShow(IMenuManager manager) {
//				manager.add(dh);
//				manager.add(nt);
//				manager.add(te);
//				manager.add(um);
//			}
//		});
//		Menu menu = menuMgr.createContextMenu(smellList);		
//		smellList.setMenu(menu);
	}

	/**
	 * �ظmsmellList
	 */
	private void buildTableList(Composite parent) {
		smellList = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		smellList.setLinesVisible(true);
		smellList.setHeaderVisible(true);
		final TableColumn colLineNum = new TableColumn(smellList, SWT.NONE);
		colLineNum.setWidth(100);
		colLineNum.setText(resource.getString("LOC"));
		final TableColumn colMethodName = new TableColumn(smellList, SWT.NONE);
		colMethodName.setWidth(400);
		colMethodName.setText(resource.getString("smell.type"));
	}

	/**
	 * �N�����ܦbView��Table�W
	 * @param list
	 */
	private void showExListTableContent() {
		//�M�����
		this.smellList.removeAll();
		this.smellList.clearAll();

		int idx = -1;
		int currentLineNumber = model.getCurrentLine();

		for (MarkerInfo msg : model.getAllSmellList()) {
			idx++;
			TableItem item = new TableItem(smellList, SWT.NONE);
			item.setData(String.valueOf(idx));

			//�Ĥ@����ܦ��
			item.setText(0, String.valueOf(msg.getLineNumber()));
			//��Smell Type�����u�令�Ů�
			String smellType = msg.getCodeSmellType();
			smellType = smellType.replace('_' , ' ');
			//�ĤG�����Smell Type
			item.setText(1, smellType);

			//�p�G���쪺��ƬOSmell�����
			if (currentLineNumber == msg.getLineNumber()) {
				item.setBackground(new Color(null,0,255,0));
			}
		}
	}

	// *************************************************************************
	// Event Handling
	// *************************************************************************

	public void handleSelectionChanged4View(ISelection selection) {
		ConsoleLog.debug("[doSelectionChanged4View]");
	}

	@SuppressWarnings( { "restriction" })
	public void handleSelectionChanged4Editor(IWorkbenchPart part, ISelection selection) {
		ConsoleLog.debug("[handleSelectionChanged4Editor] BEGIN ===================");
		if (selection instanceof ITextSelection || selection instanceof IStructuredSelection) {

			int offset = -1;
			int length = 0;

			if (selection instanceof ITextSelection) {
				ITextSelection textSelection = (ITextSelection) selection;
				offset = textSelection.getOffset();
				length = textSelection.getLength();
			} else if (selection instanceof IStructuredSelection) {
				Object element = ((IStructuredSelection) selection).getFirstElement();
				if (element instanceof IMember) {
					IMember member = (IMember) element;
					try {
						if (member != null || member.getElementType() == IMember.METHOD) {
							offset = member.getNameRange().getOffset();
							length = member.getNameRange().getLength();
						}
					} catch (Exception e) {
						ConsoleLog.error("[handleSelectionChanged4Editor][IStructuredSelection]ERROR!!", e);
					}
				}
			}

			if (offset == -1) {
				return;
			}

			// �P�_�ثe�s�边�Τ��O�_���{��View�e�{��
			if (this.changeDocument || !model.hasData() || part != actEditor) {
				ConsoleLog.debug("[handleSelectionChanged4Editor]���s�A���oJava���νs�边!");
				// ���s�A���oJava���νs�边
				if (part instanceof ITextEditor && (EditorUtils.getJavaInput((ITextEditor) part) != null)) {
					try {
						this.getEditorInput((ITextEditor) part);
					} catch (CoreException ex) {
						logger.error("[handleSelectionChanged4Editor] EXCEPTION ",ex);
						
						setContentDescription(ex.getStatus().getMessage());
						return;
					}
				}
			} else {
				// ���νs�边���{�s��
				ConsoleLog.debug("[handleSelectionChanged4Editor]���νs�边���{�s��!");
				ConsoleLog.debug("[handleSelectionChanged4Editor]offset=" + offset + ",length=" + length);
			}

			model.parseDocument(offset, length);

			this.showExListTableContent();
		}

		ConsoleLog.debug("[handleSelectionChanged4Editor] END ===================");
	}

	public void handleDocumentChanged(IDocument document) {
		ConsoleLog.debug("[handleDocumentChanged]");
		changeDocument = true;
	}

	public void handleDocumentDisposed(IDocument document) {
		uninstallListener();
	}

	private void uninstallListener() {
		if (actDocument != null) {
			actDocument.removeDocumentListener(eventHandler);
			actDocument = null;
		}
	}

	private void installListener() {
		actDocument = actEditor.getDocumentProvider().getDocument(actEditor.getEditorInput());
		actDocument.addDocumentListener(eventHandler);
	}

	public ShowInContext getShowInContext() {
		//����Eclipse 3.4�o��|�o��null point exception,�ҥH�o��令�o�� 
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor == null)	return null;

		IEditorInput input = editor.getEditorInput();
		IFile file = (IFile) input.getAdapter(IFile.class);

		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		return new ShowInContext(file, selection);
	}

	public final void notifyWorkbenchPartClosed(IWorkbenchPartReference partRef) {
		if (this.actEditor != null && this.actEditor.equals(partRef.getPart(false))) {
			try {
				this.getEditorInput(null);
			} catch (CoreException ex) {
				logger.error("[notifyWorkbenchPartClosed] EXCEPTION ",ex);
			}
		}
	}

	private void getEditorInput(ITextEditor editor) throws CoreException {
		if (actEditor != null) {
			uninstallListener();
		}

		actEditor = null;
		model.clearData();

		if (editor != null) {
			ISelection selection = editor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection = (ITextSelection) selection;

				// model.associateWithRL(EditorUtils.getProject(editor));
				if (!model.createAST(EditorUtils.getJavaInput(editor), textSelection.getOffset())) {
					setContentDescription("AST could not be created.");
				}
			}

			actEditor = editor;
			installListener();
		} else {
			ConsoleLog.debug("[setInput]Editor is NULL!");
		}
	}

	private void smellListTableDoubleClick() {
		editor = new TableEditor(smellList);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		//Table����U�ɯ�۰ʸ���Ӧ�{���X
		smellList.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				//���oTable�̪��Ҧ����
				TableItem[] allItems = smellList.getItems();
				//���o�I�諸Item
				TableItem[] selection = smellList.getSelection();
				if (selection.length >= 1) {
					int pos = Integer.parseInt(selection[0].getText()) -1;

					IRegion lineInfo = null;
					try {
						lineInfo = actDocument.getLineInformation(pos);
					} catch (BadLocationException e1) {
						logger.error("[BadLocation] EXCEPTION ",e);
					}
					//��Щw��
					actEditor.selectAndReveal(lineInfo.getOffset(), 0);
					//�M���Ҧ���Item�C��
					for (TableItem a:allItems)
						a.setBackground(null);
					//���I�諸Item�ФW�C��
					selection[0].setBackground(new Color(null,0,255,0));
				}
			}
		});
	}

	private void initializeToolBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
	}
}
