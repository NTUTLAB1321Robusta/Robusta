package ntut.csie.csdet.refactor.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
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
 * provide dialog to select existing method
 * @author Shiau
 */
public class ExistingMethodSelectionDialog  extends TwoPaneElementSelector  {
	private static Logger logger = LoggerFactory.getLogger(ExistingMethodSelectionDialog.class);

	//UI element
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

		analyzeSuperClass(methodNode);
	}

	/**
	 * analyze the class, which contains specified method, to find out all super class
	 * @param methodNode
	 */
	private void analyzeSuperClass(MethodDeclaration methodNode) {
		if (methodNode.getParent() instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) methodNode.getParent();
			//get class name which contains specified method
			className = typeDeclaration.getName() + ".java";
			superClassList.add(typeDeclaration.resolveBinding().getBinaryName());
			
			//find out all super class of class which contains specified method
			if (typeDeclaration.resolveBinding().getSuperclass() != null) {
				ITypeBinding type = typeDeclaration.resolveBinding().getSuperclass();					
				superClassList.add(type.getBinaryName());

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
	 * record method information
	 */
    public void setElements(Object[] elements) {
    	methods = elements;

        super.setElements(elements);
    }
    
    /**
     * listing Check Button change
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
	
					//filter out private method
		    		if (!isFiltered && privateBtn.getSelection())
	    				isFiltered = filterOutPrivate(method);

		    		//transform IMethod to MethodDeclaration 
		    		MethodDeclaration md = transMethodNode(method);
		    		if (md == null)
		    			continue;

		    		//filter out protected method
		    		if (!isFiltered && protectedBtn.getSelection())
		    			isFiltered = filterOutProtected(method, md);

		    		//filter out method with unsuitable parameter
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
     * filter out private method from different class
     * @param method
     * @return
     * @throws JavaModelException
     */
	private boolean filterOutPrivate(IMethod method) throws JavaModelException {
		if ((method.getFlags() & Flags.AccPrivate) != 0) {
			//bypass private in the same class
			if (className.equals(method.getCompilationUnit().getElementName()))
				return false;

			return true;
		}
		return false;
	}

	/**
	 * filter out protected method which the superclass of its' class is not in superClassList
	 * @param isFiltered
	 * @param method
	 * @param md
	 * @return
	 * @throws JavaModelException
	 */
	private boolean filterOutProtected(IMethod method,	MethodDeclaration md) throws JavaModelException {
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
	 * filter out method whose parameter is not named xxxclose() 
	 * @param isFiltered
	 * @param md
	 * @return
	 */
	private boolean filterParame(IMethod method, MethodDeclaration md) {
		//filter out method without parameter
		if (method.getNumberOfParameters() == 0)
			return true;

		if (md != null) {
			List<?> paramTypes = md.parameters();

			for (int i=0; i < paramTypes.size(); i++) {
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
	 * transform method to ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		
		try {
			//if method is from xx.jar, we can not get method's ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode ast = parserAST.createAST(null);

			ASTNode methodNode = NodeFinder.perform(ast, method.getSourceRange().getOffset(), method.getSourceRange().getLength());

			if(methodNode instanceof MethodDeclaration) {
				md = (MethodDeclaration) methodNode;
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}

		return md;
	}

	/**
	 *  create progress bar under dialog
	 */
    public Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		
		createProgressBar(contents, 0, 100);

    	return contents;
    }
	
    /**
     * create invisible progress bar
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
     * create filter check button under filter text
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
	 * create button to select all filter rule
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
	 * element label to display class information
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
