package ntut.csie.rleht.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.rleht.common.ConsoleLog;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.common.ErrorLog;
import ntut.csie.rleht.common.ImageManager;
import ntut.csie.rleht.common.RLUtils;
import ntut.csie.robusta.agile.exception.RTag;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
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


public class RLMethodView extends ViewPart implements IShowInSource {
	private static Logger logger = LoggerFactory.getLogger(RLMethodView.class);

	// -------------------------------------------------------------------------
	// GUI Object
	// -------------------------------------------------------------------------
	private static Color colorError = null;

	private static Color colorWarning = null;

	private static Color colorHighLight = null;

	private static Color colorNormal = null;

	private Table tableExList;

	private Table tableRLList;

	private TableEditor editor;

	private EventHandler eventHandler;

	// -------------------------------------------------------------------------
	// Business Object
	// -------------------------------------------------------------------------

	private RLMethodModel model = null;

	private ITextEditor actEditor;

	private IDocument actDocument;

	private boolean changeDocument = false;

	// -------------------------------------------------------------------------
	// Action Object
	// -------------------------------------------------------------------------
	private Action actionShowAllException;

	private Action actionShowExceptionWithRL;

	private Action actionShowExceptionWithoutHandling;

	private Action actionShowExceptionHasBeenCaught;

	private Action actionGotoSource;

	private Action actionAddRLAnnotation;

	private Action doubleClickAction;

	// *************************************************************************
	// initiation
	// *************************************************************************
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	/**
	 * The constructor.
	 */
	public RLMethodView() {
		model = new RLMethodModel();
	}

	public void init(IViewSite site) throws PartInitException {
		super.setSite(site);

		if (this.eventHandler == null) {
			this.eventHandler = new EventHandler(this);
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

		this.init2Table(parent);

		makeActions();
		contributeToActionBars();

		ConsoleLog.debug("initialize successfully！");
		try {

			IEditorPart part = EditorUtils.getActiveEditor();
			if (part instanceof ITextEditor) {
				getEditorInput((ITextEditor) part);
			}

		} catch (CoreException e) {
			ErrorLog.getInstance().logError(e.getLocalizedMessage(), e);
		}
	}

	// *************************************************************************
	// GUI
	// *************************************************************************

	private void init2Table(Composite parent) {
		parent.setLayout(new FillLayout());

		final SashForm sashForm = new SashForm(parent, SWT.VERTICAL);

		this.initRLListTable(sashForm);

		this.initExListTable(sashForm);

		colorError = parent.getDisplay().getSystemColor(SWT.COLOR_RED);
		colorWarning = parent.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		colorHighLight = parent.getDisplay().getSystemColor(SWT.COLOR_GREEN);
		colorNormal = parent.getDisplay().getSystemColor(SWT.COLOR_WHITE);

		sashForm.setWeights(new int[] { 1, 3 });

	}

	private void initRLListTable(Composite parent) {

		tableRLList = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);

		tableRLList.setLinesVisible(true);
		tableRLList.setHeaderVisible(true);

		final TableColumn colHandling = new TableColumn(tableRLList, SWT.CENTER);
		colHandling.setWidth(20);
		colHandling.setText("");

		final TableColumn colRLevel = new TableColumn(tableRLList, SWT.CENTER);
		colRLevel.setWidth(40);
		colRLevel.setText(resource.getString("level"));

		final TableColumn colExceptionName = new TableColumn(tableRLList, SWT.NONE);
		colExceptionName.setWidth(400);
		colExceptionName.setText(resource.getString("ex.type"));

		final TableColumn colMemo = new TableColumn(tableRLList, SWT.NONE);
		colMemo.setWidth(200);
		colMemo.setText(resource.getString("remark"));

	}

	private void initExListTable(Composite parent) {
		tableExList = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		tableExList.setLinesVisible(true);
		tableExList.setHeaderVisible(true);

		final TableColumn colHandling = new TableColumn(tableExList, SWT.CENTER);
		colHandling.setWidth(20);
		colHandling.setText("");

		final TableColumn colRLevel = new TableColumn(tableExList, SWT.CENTER);
		colRLevel.setWidth(40);
		colRLevel.setText(resource.getString("level"));

		final TableColumn colExceptionName = new TableColumn(tableExList, SWT.NONE);
		colExceptionName.setWidth(200);
		colExceptionName.setText(resource.getString("ex.type"));

		final TableColumn colMethodName = new TableColumn(tableExList, SWT.NONE);
		colMethodName.setWidth(400);
		colMethodName.setText(resource.getString("method.name"));

		final TableColumn colLineNum = new TableColumn(tableExList, SWT.NONE);
		colLineNum.setWidth(400);
		colLineNum.setText(resource.getString("LOC"));

		colorError = parent.getDisplay().getSystemColor(SWT.COLOR_RED);
		colorWarning = parent.getDisplay().getSystemColor(SWT.COLOR_BLUE);
	}

	/**
	 * show the information on the table of view
	 * 
	 * @param list
	 *            information's resource
	 */
	private void showExListTableContent() {
		this.tableExList.removeAll();
		this.tableExList.clearAll();

		if (!model.hasExceptionData()) {
			ConsoleLog.debug("there is not any exception information in this method！");
			return;
		}

		int idx = -1;
		int currentLineNumber = model.getCurrentLine();
		for (RLMessage msg : model.getExceptionList()) {
			idx++;

			if (msg.getRLData().getLevel() < 0) {
				continue;
			}

			if (this.actionShowExceptionWithRL.isChecked()) {
				if (msg.getRLData().getLevel() <= 0) {
					continue;
				}
			} else if (this.actionShowExceptionWithoutHandling.isChecked()) {
				if (msg.isHandleByCatch()) {
					continue;
				}
			} else if (this.actionShowExceptionHasBeenCaught.isChecked()) {
				if (!msg.isHandleByCatch()) {
					continue;
				}
			}

			TableItem item = new TableItem(tableExList, SWT.NONE);
			item.setData(String.valueOf(idx));
			item.setText(0, String.valueOf(idx));
			if (!msg.isHandling()) {
				item.setForeground(colorError);
				item.setImage(0, ImageManager.getInstance().get("error"));
			} else if (msg.isReduction()) {
				item.setForeground(colorWarning);
				item.setImage(0, ImageManager.getInstance().get("warning"));
			} else {
				item.setImage(0, ImageManager.getInstance().get("ok"));
			}

			item.setText(1, String.valueOf(msg.getRLData().getLevel()));

			item.setText(2, msg.getRLData().getExceptionType());
			item.setImage(2, msg.isCheckedException() ? ImageManager.getInstance().get("checked") : ImageManager.getInstance().get("unchecked"));

			item.setText(3, msg.getStatement());
			if (msg.getRLData().getLevel() > 0) {
				item.setImage(3, ImageManager.getInstance().get("annotation"));
			}

			item.setText(4, String.valueOf(msg.getLineNumber()));

			if (currentLineNumber == msg.getLineNumber()) {
				item.setBackground(colorHighLight);
			}
		}
	}

	private void showRLListTableContent() {
		this.tableRLList.removeAll();

		if (model.getRLAnnotationList() == null || model.getRLAnnotationList().size() == 0) {
			ConsoleLog.debug("There is not any ＠RL informaion！");
			return;
		}

		ConsoleLog.debug("there are" + model.getRLAnnotationList().size() + "＠RL information");

		int idx = -1;
		for (RLMessage msg : model.getRLAnnotationList()) {
			idx++;

			TableItem item = new TableItem(tableRLList, SWT.NONE);
			item.setData(String.valueOf(idx));
			item.setText(0, String.valueOf(idx));
			item.setText(1, String.valueOf(msg.getRLData().getLevel()));

			if (!RLData.validLevel(msg.getRLData().getLevel())) {
				item.setImage(0, ImageManager.getInstance().get("error"));
				item.setForeground(1, colorError);
			}

			if (msg.getRLData().getExceptionType() == null || msg.getRLData().getExceptionType().equals("")) {
				item.setText(2, resource.getString("error.ex.type"));
				item.setForeground(2, colorError);
			} else {
				item.setText(2, msg.getRLData().getExceptionType());
			}

			item.setImage(2, msg.isCheckedException() ? ImageManager.getInstance().get("checked") : ImageManager.getInstance().get("unchecked"));

			if (msg.isEdited()) {
				item.setForeground(colorWarning);
				item.setText(3, resource.getString("update.message"));
			}
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		this.tableExList.setFocus();
	}

	// *************************************************************************
	// Menu
	// *************************************************************************

	private void makeActions() {
		this.initActionShowAllException();
		this.initActionShowExceptionWithRL();
		this.initActionShowThrow();
		this.initActionShowCatch();
		this.initActionGotoSource();

		this.initActionAddRLAnnotation();

		this.initExListTableRowClick();
		this.initExListTableDoubleClick();
		this.initExListTablePopupMenu();

		this.initRLListTablePopupMenu();
		this.initRLListTableDoubleClick();
		this.initRLListTableRowClick();

	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(actionGotoSource);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(actionShowExceptionWithoutHandling);
		manager.add(actionShowExceptionWithRL);
		manager.add(actionShowAllException);
		manager.add(actionShowExceptionHasBeenCaught);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionGotoSource);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		manager.add(actionShowExceptionWithoutHandling);
		manager.add(actionShowExceptionWithRL);
		manager.add(actionShowAllException);
		manager.add(actionShowExceptionHasBeenCaught);
	}

	private void initActionShowAllException() {
		actionShowAllException = new Action() {
			public void run() {
				this.setChecked(true);
				this.setEnabled(false);

				actionShowExceptionWithRL.setEnabled(true);
				actionShowExceptionWithoutHandling.setEnabled(true);
				actionShowExceptionHasBeenCaught.setEnabled(true);
				actionShowExceptionWithRL.setChecked(false);
				actionShowExceptionWithoutHandling.setChecked(false);
				actionShowExceptionHasBeenCaught.setChecked(false);

				actEditor.setFocus();
			}
		};
		actionShowAllException.setText(resource.getString("display.all"));
		actionShowAllException.setToolTipText(resource.getString("display.all.in.method"));
		actionShowAllException.setImageDescriptor(ImageManager.getInstance().getDescriptor("showall"));
		actionShowAllException.setChecked(false);
	}

	private void initActionShowExceptionWithRL() {
		this.actionShowExceptionWithRL = new Action() {
			public void run() {
				this.setChecked(true);
				this.setEnabled(false);
				actionShowAllException.setEnabled(true);
				actionShowExceptionWithoutHandling.setEnabled(true);
				actionShowExceptionHasBeenCaught.setEnabled(true);
				actionShowAllException.setChecked(false);
				actionShowExceptionWithoutHandling.setChecked(false);
				actionShowExceptionHasBeenCaught.setChecked(false);
				actEditor.setFocus();
			}
		};
		actionShowExceptionWithRL.setText(resource.getString("display.rl"));
		actionShowExceptionWithRL.setToolTipText(resource.getString("display.rl.in.method"));
		actionShowExceptionWithRL.setImageDescriptor(ImageManager.getInstance().getDescriptor("annotation"));
		actionShowExceptionWithRL.setChecked(false);
	}

	private void initActionShowThrow() {
		this.actionShowExceptionWithoutHandling = new Action() {
			public void run() {
				this.setChecked(true);
				this.setEnabled(false);

				actionShowAllException.setEnabled(true);
				actionShowExceptionWithRL.setEnabled(true);
				actionShowExceptionHasBeenCaught.setEnabled(true);

				actionShowAllException.setChecked(false);
				actionShowExceptionWithRL.setChecked(false);
				actionShowExceptionHasBeenCaught.setChecked(false);
				actEditor.setFocus();
			}
		};
		actionShowExceptionWithoutHandling.setText(resource.getString("display.undealt.ex"));
		actionShowExceptionWithoutHandling.setToolTipText(resource.getString("display.undealt.ex.in.catch"));
		actionShowExceptionWithoutHandling.setImageDescriptor(ImageManager.getInstance().getDescriptor("showthrow"));

		actionShowExceptionWithoutHandling.setChecked(true);
		actionShowExceptionWithoutHandling.setEnabled(false);

	}

	private void initActionShowCatch() {
		actionShowExceptionHasBeenCaught = new Action() {
			public void run() {
				this.setChecked(true);
				this.setEnabled(false);

				actionShowAllException.setEnabled(true);
				actionShowExceptionWithRL.setEnabled(true);
				actionShowExceptionWithoutHandling.setEnabled(true);

				actionShowAllException.setChecked(false);
				actionShowExceptionWithRL.setChecked(false);
				actionShowExceptionWithoutHandling.setChecked(false);
				actEditor.setFocus();

			}
		};
		actionShowExceptionHasBeenCaught.setText(resource.getString("display.deal.ex"));
		actionShowExceptionHasBeenCaught.setToolTipText(resource.getString("display.deal.ex.in.catch"));
		actionShowExceptionHasBeenCaught.setImageDescriptor(ImageManager.getInstance().getDescriptor("showcatch"));
		actionShowExceptionHasBeenCaught.setChecked(false);
	}

	private void initActionGotoSource() {
		actionGotoSource = new Action() {
			public void run() {
			}
		};
		actionGotoSource.setText(resource.getString("skip.to.line.number"));
		actionGotoSource.setToolTipText(resource.getString("select.item.skip.to.line.number"));
		actionGotoSource.setImageDescriptor(ImageManager.getInstance().getDescriptor("link"));
		actionGotoSource.setChecked(false);

	}

	private void initActionAddRLAnnotation() {

		actionAddRLAnnotation = new Action() {
			public void run() {
			}
		};
		actionAddRLAnnotation.setText(resource.getString("add.tag.on.method"));
		actionAddRLAnnotation.setToolTipText(resource.getString("add.tag.on.method"));
		actionAddRLAnnotation.setImageDescriptor(ImageManager.getInstance().getDescriptor("link"));
		actionAddRLAnnotation.setChecked(false);

	}

	private void initExListTablePopupMenu() {
		Menu popupMenu = new Menu(this.tableExList);

		MenuItem itemAddRL = new MenuItem(popupMenu, SWT.NONE);
		itemAddRL.setText(resource.getString("add.tag"));
		itemAddRL.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selection = tableExList.getSelection();
				if (selection.length >= 1) {
					int pos = Integer.parseInt(selection[0].getText());
					int srcpos = model.getPosition();
					if (model.addOrRemoveRLAnnotation(true, pos)) {
						actEditor.setFocus();
						actEditor.setHighlightRange(srcpos, 0, true);
					}

				}

			}
		});

		this.tableExList.setMenu(popupMenu);
	}

	private void initRLListTablePopupMenu() {
		Menu popupMenu = new Menu(this.tableRLList);

		MenuItem itemRemoveRL = new MenuItem(popupMenu, SWT.NONE);
		itemRemoveRL.setText(resource.getString("remove.tag"));
		itemRemoveRL.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selection = tableRLList.getSelection();
				if (selection.length >= 1) {
					int pos = Integer.parseInt((String) selection[0].getData());
					int srcpos = model.getPosition();
					if (model.addOrRemoveRLAnnotation(false, pos)) {
						actEditor.setFocus();
						actEditor.setHighlightRange(srcpos, 0, true);
					}

				}

			}
		});

		MenuItem itemUpdateRL = new MenuItem(popupMenu, SWT.NONE);
		itemUpdateRL.setText(resource.getString("update.tag"));
		itemUpdateRL.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				List<RLData> rlmsgList = new ArrayList<RLData>();
				for (TableItem item : tableRLList.getItems()) {
					rlmsgList.add(new RLData(Integer.parseInt(item.getText(1)), item.getText(2).trim()));
				}
				int srcpos = model.getPosition();
				if (model.updateRLAnnotation(rlmsgList)) {
					actEditor.setFocus();
					actEditor.setHighlightRange(srcpos, 0, true);
				}
			}
		});

		new MenuItem(popupMenu, SWT.SEPARATOR);

		MenuItem itemUpRL = new MenuItem(popupMenu, SWT.NONE);
		itemUpRL.setText(resource.getString("up"));
		itemUpRL.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selection = tableRLList.getSelection();
				if (selection.length >= 1) {
					int pos = Integer.parseInt((String) selection[0].getData());
					model.swapRLAnnotation(pos, true);
					showRLListTableContent();
				}
			}
		});

		MenuItem itemDownRL = new MenuItem(popupMenu, SWT.NONE);
		itemDownRL.setText(resource.getString("down"));
		itemDownRL.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selection = tableRLList.getSelection();
				if (selection.length >= 1) {
					int pos = Integer.parseInt((String) selection[0].getData());
					model.swapRLAnnotation(pos, false);
					showRLListTableContent();
				}
			}
		});

		this.tableRLList.setMenu(popupMenu);
	}

	private void initExListTableRowClick() {
		tableExList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableItem[] selection = tableExList.getSelection();
				if (actionGotoSource.isChecked()) {
					if (selection.length >= 1) {
						int pos = Integer.parseInt(selection[0].getText());
						RLMessage msg = model.getExceptionList().get(pos);
						try {
							actEditor.setHighlightRange(msg.getPosition(), 0, true);
						} catch (Exception ex) {
							ConsoleLog.error("ERROR!!", ex);
						}
					}
				}
			}
		});

	}

	private void initExListTableDoubleClick() {
		tableExList.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				TableItem[] selection = tableExList.getSelection();
				if (selection.length >= 1) {
					int pos = Integer.parseInt(selection[0].getText());
					RLMessage msg = model.getExceptionList().get(pos);

					if (msg.isHandling()) {
						try {
							actEditor.setHighlightRange(msg.getPosition(), 0, true);
						} catch (Exception ex) {
							ConsoleLog.error("ERROR!!", ex);
						}
					} else {
						int srcpos = model.getPosition();
						if (model.addOrRemoveRLAnnotation(true, pos)) {
							actEditor.setFocus();
							actEditor.setHighlightRange(srcpos, 0, true);
						}

					}
				}
			}
		});
	}

	private void initRLListTableRowClick() {
		tableRLList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableItem[] selection = tableRLList.getSelection();
				int pos = RLUtils.str2int(String.valueOf(selection[0].getData()), -1);

				if (pos != -1) {
					RLMessage msg = model.getRLAnnotationList().get(pos);

					TableItem[] exItems = tableExList.getItems();
					for (TableItem exitem : exItems) {

						if (msg.isInHandleExMap(String.valueOf(exitem.getData()))) {
							exitem.setBackground(colorHighLight);
						} else {
							exitem.setBackground(colorNormal);
						}
					}
				}
			}
		});

	}

	private void initRLListTableDoubleClick() {

		editor = new TableEditor(tableRLList);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		tableRLList.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent event) {
				try {

					Control old = editor.getEditor();
					if (old != null)
						old.dispose();

					Point pt = new Point(event.x, event.y);

					final TableItem item = tableRLList.getItem(pt);

					if (item == null) {
						return;
					}
					int column = -1;
					for (int i = 0, n = tableRLList.getColumnCount(); i < n; i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							column = i;
							break;
						}
					}

					if (column != 1 && column != 2) {
						return;
					}

					final Text text = new Text(tableRLList, SWT.NONE);
					text.setForeground(item.getForeground());

					text.setText(item.getText(column));
					text.setForeground(item.getForeground());
					text.selectAll();
					text.setFocus();
					if (column == 1) {
						text.setTextLimit(1);
					}

					editor.minimumWidth = text.getBounds().width;
					editor.setEditor(text, item, column);

					final int col = column;

					text.addFocusListener(new FocusAdapter() {
						public void focusLost(FocusEvent e) {
							String inputVal = text.getText().trim();

							if (!item.getText(col).equals(inputVal)) {
								int pos = Integer.parseInt(String.valueOf(item.getData()));
								RLMessage msg = model.getRLAnnotationList().get(pos);
								msg.setEdited(true);
								if (col == 1) {
									msg.getRLData().setLevel(RLUtils.str2int(inputVal, RTag.LEVEL_1_ERR_REPORTING));
								}
								if (col == 2) {
									msg.getRLData().setExceptionType(inputVal);
								}
								model.getRLAnnotationList().set(pos, msg);

								editor.getEditor().dispose();

								showRLListTableContent();
							} else {
								editor.getEditor().dispose();
							}
						}
					});

				} catch (Exception ex) {
					ErrorLog.getInstance().logError("There are errors when processes @Tag Table！", ex);
				}

			}

		});

		tableRLList.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				TableItem[] selection = tableRLList.getSelection();
				if (selection.length >= 1) {
					int pos = Integer.parseInt(selection[0].getText());
					RLMessage msg = model.getExceptionList().get(pos);

					if (msg.isHandling()) {
						try {
							actEditor.setHighlightRange(msg.getPosition(), 0, true);
						} catch (Exception ex) {
							ConsoleLog.error("ERROR!!", ex);
						}
					} else {

						int srcpos = model.getPosition();
						if (model.addOrRemoveRLAnnotation(true, pos)) {
							actEditor.setFocus();
							actEditor.setHighlightRange(srcpos, 0, true);
						}
					}
				}
			}
		});
	}

	// *************************************************************************
	// Event Handling
	// *************************************************************************

	protected void handleSelectionChanged4View(ISelection selection) {
		ConsoleLog.debug("[doSelectionChanged4View]");

	}

	@SuppressWarnings( { "restriction" })
	protected void handleSelectionChanged4Editor(IWorkbenchPart part, ISelection selection) {
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

			if (this.changeDocument || !model.hasData() || part != actEditor) {
				ConsoleLog.debug("[handleSelectionChanged4Editor]reload java doc and editor!");
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
				ConsoleLog.debug("[handleSelectionChanged4Editor]java doc and editor are nowaday!");
				ConsoleLog.debug("[handleSelectionChanged4Editor]offset=" + offset + ",length=" + length);
			}

			model.parseDocument(offset, length);

			this.showExListTableContent();

			this.showRLListTableContent();
		}

		ConsoleLog.debug("[handleSelectionChanged4Editor] END ===================");
	}

	protected void handleDocumentChanged(IDocument document) {
		ConsoleLog.debug("[handleDocumentChanged]");
		changeDocument = true;
	}

	protected void handleDoubleClick(DoubleClickEvent event) {
		doubleClickAction.run();
	}

	protected void handleDocumentDisposed(IDocument document) {
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
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor == null)	return null;

		IEditorInput input = editor.getEditorInput();
		IFile file = (IFile) input.getAdapter(IFile.class);

		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		return new ShowInContext(file, selection);
	}

	final void notifyWorkbenchPartClosed(IWorkbenchPartReference partRef) {
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
}
