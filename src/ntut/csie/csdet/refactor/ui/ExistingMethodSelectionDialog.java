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
 * ���ѿ�ܤw�s�bMethod��Dialog
 * @author Shiau
 */
public class ExistingMethodSelectionDialog  extends TwoPaneElementSelector  {
	private static Logger logger = LoggerFactory.getLogger(ExistingMethodSelectionDialog.class);

	//UI����
	private ProgressBar progressBar;	
	private Button parameBtn;
	private Button privateBtn;
	private Button protectedBtn;
	
	private Object[] methods = new Object[0];

	private String className = "";

	private List<String> superClassList = new ArrayList<String>();

	public ExistingMethodSelectionDialog(Shell parent, ASTNode methodNode) {
		super(parent, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT),
					  new ClassRenderer());

		//���oMethod�Ҧ�
		analyzeSuperClass(methodNode);
	}

	/**
	 * ���RMethod��Class�A��X�Ҧ���SuperClass
	 * @param methodNode
	 */
	private void analyzeSuperClass(ASTNode methodNode) {
		if (methodNode instanceof MethodDeclaration) {
			MethodDeclaration md = (MethodDeclaration) methodNode;

			if (md.getParent() instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) md.getParent();
				//���oMethod��Class Name
				className = typeDeclaration.getName() + ".java";
				superClassList.add(typeDeclaration.resolveBinding().getBinaryName());
				
				//���o�Ҧ�SuperClass
				if (typeDeclaration.resolveBinding().getSuperclass() != null) {
					ITypeBinding type = typeDeclaration.resolveBinding().getSuperclass();					
					superClassList.add(type.getBinaryName());

					//�@���l�ܨ�S��SuperClass
					while (true) {
						if (type.getSuperclass() == null)
							break;
						type = type.getSuperclass();
						superClassList.add(type.getBinaryName());
					}
				}
				
			}
		}
	}

	/**
	 * �O���ǤJ��Method���
	 */
    public void setElements(Object[] elements) {
    	//�O���ǤJ���
    	methods = elements;

        super.setElements(elements);
    }
    
    /**
     * Filter Check Button���ܮɪ��ʧ@
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
	
					//�o��Private
		    		if (!isFiltered && privateBtn.getSelection())
	    				isFiltered = filterPrivate(method);

		    		//�নMethodDeclaration (�t�׷|�ܫܺC)
		    		MethodDeclaration md = transMethodNode(method);
		    		if (md == null)
		    			continue;

		    		//�o��Protected
		    		if (!isFiltered && protectedBtn.getSelection())
		    			isFiltered = filterProtected(method, md);

		    		//�o���ѼƤ��X��
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
     * �o�����PClass��Private Method
     * @param method
     * @return
     * @throws JavaModelException
     */
	private boolean filterPrivate(IMethod method) throws JavaModelException {
		//�Y��Private�h�o��
		if ((method.getFlags() & Flags.AccPrivate) != 0) {
			//�P�@class����Private Method�����o��
			if (className.equals(method.getCompilationUnit().getElementName()))
				return false;

			return true;
		}
		return false;
	}

	/**
	 * �o������SuperClass��Protected Method
	 * @param isFiltered
	 * @param method
	 * @param md
	 * @return
	 * @throws JavaModelException
	 */
	private boolean filterProtected(IMethod method,	MethodDeclaration md) throws JavaModelException {
		//�Y�n�o��Protected
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
	 * �o���ѼƤ��S��Close���A��Method
	 * @param isFiltered
	 * @param md
	 * @return
	 */
	private boolean filterParame(IMethod method, MethodDeclaration md) {
		//�Y�S���Ѽƪ����o��
		if (method.getNumberOfParameters() == 0)
			return true;

		if (md != null) {
			List<?> paramTypes = md.parameters();

			for (int i=0; i < paramTypes.size(); i++) {
				//�Y�ѼƤ����]�t��Close��Method�h���o��
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
	 * �ഫ��ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		
		try {
			//Parser Jar�ɮɡA�|������ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			//����AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode ast = parserAST.createAST(null);

			//���oAST��Method����
			ASTNode methodNode = NodeFinder.perform(ast, method.getSourceRange().getOffset(), method.getSourceRange().getLength());

			//�Y��ASTNode�ݩ�MethodDeclaration�A�h�૬
			if(methodNode instanceof MethodDeclaration) {
				md = (MethodDeclaration) methodNode;
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}

		return md;
	}

	/**
	 * �bDialog�U��إ�ProgressBar
	 */
    public Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		
		//�إ�ProgressBar (Range 0~100)
		createProgressBar(contents, 0, 100);

    	return contents;
    }
	
    /**
     * �إߤ���ܪ�Progress Bar
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
     * �bFilter Text�U��s�إ�Filter CheckButton
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
	 * �إ�Composite
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
	 * �إ�CheckButton
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
	 * �إ߿�ܩҦ�Filter����Button
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
	 * ���Class��T��ElementLabel
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
