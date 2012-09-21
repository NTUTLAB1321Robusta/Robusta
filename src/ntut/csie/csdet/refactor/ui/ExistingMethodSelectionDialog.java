package ntut.csie.csdet.refactor.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 提供選擇已存在Method的Dialog
 * @author Shiau
 */
public class ExistingMethodSelectionDialog  extends TwoPaneElementSelector  {
	private static Logger logger = LoggerFactory.getLogger(ExistingMethodSelectionDialog.class);

	//UI元件
	private ProgressBar progressBar;	
	private Button parameBtn;
	private Button privateBtn;
	private Button protectedBtn;
	
	private Object[] methods = new Object[0];

	private String className = "";

	private List<String> superClassList = new ArrayList<String>();

	public ExistingMethodSelectionDialog(Shell parent, MethodDeclaration methodNode) {
		super(parent, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT),
					  new ClassRenderer());

		//取得Method所有
		analyzeSuperClass(methodNode);
	}

	/**
	 * 分析Method的Class，找出所有的SuperClass
	 * @param methodNode
	 */
	private void analyzeSuperClass(MethodDeclaration methodNode) {
		if (methodNode.getParent() instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) methodNode.getParent();
			//取得Method的Class Name
			className = typeDeclaration.getName() + ".java";
			superClassList.add(typeDeclaration.resolveBinding().getBinaryName());
			
			//取得所有SuperClass
			if (typeDeclaration.resolveBinding().getSuperclass() != null) {
				ITypeBinding type = typeDeclaration.resolveBinding().getSuperclass();					
				superClassList.add(type.getBinaryName());

				//一直追蹤到沒有SuperClass
				while (true) {
					if (type.getSuperclass() == null)
						break;
					type = type.getSuperclass();
					superClassList.add(type.getBinaryName());
				}
			}
			
		}
	}

	/**
	 * 記錄傳入的Method資料
	 */
    public void setElements(Object[] elements) {
    	//記錄傳入資料
    	methods = elements;

        super.setElements(elements);
    }
    
    /**
     * Filter Check Button改變時的動作
     */
    private void handleFilterChange() {
    	List<Object> filterList = new ArrayList<Object>();
    	try {
    		progressBar.setVisible(true);
    		int counter =0;
	    	for (Object element : methods) {
	    		progressBar.setSelection((int)((double)counter++/(double)(methods.length-1)*100));

	    		boolean isFiltered = false;

				if (element instanceof IMethod) {
					IMethod method = (IMethod) element;
	
					//濾掉Private
		    		if (!isFiltered && privateBtn.getSelection())
	    				isFiltered = filterPrivate(method);

		    		//轉成MethodDeclaration (速度會變很慢)
		    		MethodDeclaration md = transMethodNode(method);
		    		if (md == null)
		    			continue;

		    		//濾掉Protected
		    		if (!isFiltered && protectedBtn.getSelection())
		    			isFiltered = filterProtected(method, md);

		    		//濾掉參數不合的
		    		if (!isFiltered && parameBtn.getSelection())
						isFiltered = filterParame(method, md);
				}

				if (!isFiltered)
					filterList.add(element);
	    	}
	    	progressBar.setVisible(false);
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}
		super.setListElements(filterList.toArray(new IJavaElement[filterList.size()]));
    }

    /**
     * 濾掉不同Class的Private Method
     * @param method
     * @return
     * @throws JavaModelException
     */
	private boolean filterPrivate(IMethod method) throws JavaModelException {
		//若為Private則濾掉
		if ((method.getFlags() & Flags.AccPrivate) != 0) {
			//同一class內的Private Method不用濾掉
			if (className.equals(method.getCompilationUnit().getElementName()))
				return false;

			return true;
		}
		return false;
	}

	/**
	 * 濾掉不為SuperClass的Protected Method
	 * @param isFiltered
	 * @param method
	 * @param md
	 * @return
	 * @throws JavaModelException
	 */
	private boolean filterProtected(IMethod method,	MethodDeclaration md) throws JavaModelException {
		//若要濾掉Protected
		if ((method.getFlags() & Flags.AccProtected) != 0) {
			if (md.getParent() instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) md.getParent();
				String className = typeDeclaration.resolveBinding().getBinaryName();
				for (String superClass : superClassList) {
					if (className.equals(superClass))
						return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 濾掉參數內沒有Close型態的Method
	 * @param isFiltered
	 * @param md
	 * @return
	 */
	private boolean filterParame(IMethod method, MethodDeclaration md) {
		//若沒有參數直接濾掉
		if (method.getNumberOfParameters() == 0)
			return true;

		if (md != null) {
			List<?> paramTypes = md.parameters();

			for (int i=0; i < paramTypes.size(); i++) {
				//若參數內有包含有Close的Method則不濾掉
				if (paramTypes.get(i) instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration svd = (SingleVariableDeclaration) paramTypes.get(i);
					if (svd.resolveBinding().getType().toString().contains("close()"))
						return false;
				}
			}
		}
		return true;
	}
    
	/**
	 * 轉換成ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		
		try {
			//Parser Jar檔時，會取不到ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			//產生AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode ast = parserAST.createAST(null);

			//取得AST的Method部份
			ASTNode methodNode = NodeFinder.perform(ast, method.getSourceRange().getOffset(), method.getSourceRange().getLength());

			//若此ASTNode屬於MethodDeclaration，則轉型
			if(methodNode instanceof MethodDeclaration) {
				md = (MethodDeclaration) methodNode;
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}

		return md;
	}

	/**
	 * 在Dialog下方建立ProgressBar
	 */
    public Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		
		//建立ProgressBar (Range 0~100)
		createProgressBar(contents, 0, 100);

    	return contents;
    }
	
    /**
     * 建立不顯示的Progress Bar
     * @param contents
     * @param min
     * @param max
     */
	private void createProgressBar(Composite contents, int min, int max) {
		progressBar = new ProgressBar(contents, SWT.FILL);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		progressBar.setMinimum(min);
		progressBar.setMinimum(max);
		progressBar.setVisible(false);
	}
	
    /**
     * 在Filter Text下方新建立Filter CheckButton
     */
    protected Text createFilterText(Composite parent) {
    	Text contents = (Text) super.createFilterText(parent);

    	/* Filter Selection */
		Composite filterComposite = createComposite(parent);
		createLabel(filterComposite, "Filter: ");
		//Create Check Button
		parameBtn = createCheckButton(filterComposite, "Closeable");
		protectedBtn = createCheckButton(filterComposite, "Protected");
		privateBtn = createCheckButton(filterComposite, "Private");
		//Create Select All Button
		createSelectAllButton(filterComposite);

		return contents;
    }

	/**
	 * 建立Composite
	 * @param parent
	 * @return
	 */
	private Composite createComposite(Composite parent) {
		Composite filterComposite= new Composite(parent, SWT.NONE);
		filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		GridLayout layout= new GridLayout();
		layout= new GridLayout();
		layout.numColumns= 5;
		layout.marginWidth= 0;
		filterComposite.setLayout(layout);

		return filterComposite;
	}

	/**
	 * 建立CheckButton
	 * @param composite
	 * @return 
	 */
	private Button createCheckButton(Composite composite, String name) {
		Button button = new Button(composite, SWT.CHECK);
		button.setText(name);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFilterChange();
			}
		});
		return button;
	}
	
	/**
	 * 建立選擇所有Filter條件的Button
	 * @param parent
	 */
	private void createSelectAllButton(Composite parent) {
		Button button = new Button(parent, SWT.None);
		button.setText("Select All");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				parameBtn.setSelection(true);
				protectedBtn.setSelection(true);
				privateBtn.setSelection(true);
				handleFilterChange();
			}
		});
	}

	/**
	 * 顯示Class資訊的ElementLabel
	 * @author Shiau
	 */
	private static class ClassRenderer extends JavaElementLabelProvider {
		public ClassRenderer() {
			super(JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_POST_QUALIFIED | JavaElementLabelProvider.SHOW_ROOT);	
		}

		public Image getImage(Object element) {
			IMethod method = (IMethod) element;
			return super.getImage(method.getParent());
		}
		
		public String getText(Object element) {
			IMethod method = (IMethod) element;
			
			return super.getText(method.getParent());
		}
	}
}
