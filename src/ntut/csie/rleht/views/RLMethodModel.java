package ntut.csie.rleht.views;

import java.util.List;

import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.builder.RLNature;
import ntut.csie.rleht.common.ASTHandler;
import ntut.csie.rleht.common.ConsoleLog;
import ntut.csie.rleht.common.ErrorLog;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RLMethodModel {
	private static Logger logger = LoggerFactory.getLogger(RLMethodModel.class);
	private List<RLMessage> exceptionList = null;

	private List<RLMessage> rlAnnotationList = null;

	private ASTNode methodNode = null;

	private ASTHandler astHandler = null;

	private CompilationUnit actRoot;

	private IOpenable actOpenable;

	private int currentLine = 0;

	public RLMethodModel() {
		astHandler = new ASTHandler();
	}

	
	public void clear(){
		if(exceptionList != null) exceptionList.clear();
		if(rlAnnotationList != null) rlAnnotationList.clear();
		methodNode = null;
		astHandler = null;
		actRoot=null;
		actOpenable=null;
		System.gc();
	}

	/**
	 * 
	 * @param offset
	 * @param length
	 */
	public void parseDocument(int offset, int length) {
		ExceptionAnalyzer visitor = new ExceptionAnalyzer(actRoot, offset, length);
		this.actRoot.accept(visitor);
		this.methodNode = visitor.getCurrentMethodNode();
		this.rlAnnotationList = visitor.getMethodRLAnnotationList();

		this.setCurrentLine(offset);

		if (methodNode != null) {
			RLChecker checker = new RLChecker();
			this.exceptionList = checker.check(visitor);
		}
	}

	/**
	 * @param openable
	 * @param pos
	 * @return
	 * @throws CoreException
	 */
	public boolean createAST(IOpenable openable, int pos) throws CoreException {
		if (openable == null) {
			System.err.println("There is not a java program in editor！");
			throw createCoreException("編輯器內容不是java程式！", null);
		}

		this.actOpenable = openable;

		// check whether Java version is above 1.5(due to using annotation)
		IJavaProject project = (IJavaProject) ((IJavaElement) actOpenable).getAncestor(IJavaElement.JAVA_PROJECT);
		String option = project.getOption(JavaCore.COMPILER_SOURCE, true);
		if (!JavaCore.VERSION_1_5.equals(option) && !JavaCore.VERSION_1_6.equals(option)) {
			throw createCoreException("java程式不是1.5以上版本！", null);
		}

		try {

			this.actRoot = astHandler.createAST(actOpenable, pos);

			return (this.actRoot != null);

		} catch (RuntimeException ex) {
			logger.error("[createAST] EXCEPTION ", ex);
			throw createCoreException("無法產生AST:\n" + ex.getMessage(), ex);
		}
	}

	private CoreException createCoreException(String msg, Throwable cause) {
		return new CoreException(new Status(IStatus.ERROR, RLEHTPlugin.PLUGIN_ID, IStatus.ERROR, msg, cause));
	}

	/**
	 * add nature to this project
	 * 
	 * @throws CoreException
	 */
	public void associateWithRL(IProject project) throws CoreException {

		if (actOpenable == null) {
			ConsoleLog.info("Can't add the RLNature  to this Porject.");
			return;
		}

		if (project == null) {
			ConsoleLog.info("*Error adding the RLNature to this Porject.");
			return;
		}

		if (!project.hasNature(RLNature.NATURE_ID)) {
			try {
				IProjectDescription projectDesc = project.getDescription();
				String[] natures = projectDesc.getNatureIds();
				int naturesSize = natures.length;
				String[] newNatures = new String[naturesSize + 1];
				System.arraycopy(natures, 0, newNatures, 0, naturesSize);
				newNatures[naturesSize] = RLNature.NATURE_ID;
				projectDesc.setNatureIds(newNatures);
				project.setDescription(projectDesc, null);

				ConsoleLog.info("RLMethodNature added to the " + project.getName() + " Porject.");
			} catch (Exception e) {
				ErrorLog.getInstance().logError("Error adding the RLMethodNature to the " + project.getName() + " Porject.", e);
			}
		} else {
			ConsoleLog.info("The RLMethodNature is already associated with the " + project.getName() + " Porject.");
		}

	}

	/**
	 * add robustness level annotation information to specific method 
	 */
	@SuppressWarnings("unchecked")
	public boolean addOrRemoveRLAnnotation(boolean add, int pos) {

		RLMessage msg = this.exceptionList.get(pos);

		try {

			actRoot.recordModifications();

			AST ast = methodNode.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);

			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			if (add) {
				addImportDeclaration();

				//add selected exception @tag annotation 
				rlary.expressions().add(getRLAnnotation(ast, msg.getRLData()));
			}

			//add original @tag annotation
			int idx = 0;
			for (RLMessage rlmsg : rlAnnotationList) {
				if (add) {
					// add
					rlary.expressions().add(getRLAnnotation(ast, rlmsg.getRLData()));
				} else {
					// remove
					if (idx++ != pos) {
						rlary.expressions().add(getRLAnnotation(ast, rlmsg.getRLData()));
					}
				}
			}

			MethodDeclaration method = (MethodDeclaration) methodNode;

			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}

			if (rlary.expressions().size() > 0) {
				method.modifiers().add(0, root);
			}

			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());

			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));

			edits.apply(document);

			cu.getBuffer().setContents(document.get());

			return true;

		} catch (Exception ex) {
			logger.error("[addOrRemoveRLAnnotation] EXCEPTION ", ex);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void addImportDeclaration() {
		List<ImportDeclaration> importList = this.actRoot.imports();
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;
		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName())) {
				isImportRobustnessClass = true;
			}
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName())) {
				isImportRLClass = true;
			}
		}

		AST rootAst = this.actRoot.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			this.actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			this.actRoot.imports().add(imp);
		}

	}

	@SuppressWarnings("unchecked")
	public boolean updateRLAnnotation(List<RLData> rllist) {
		try {

			actRoot.recordModifications();

			AST ast = methodNode.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);

			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			for (RLData rl : rllist) {
				rlary.expressions().add(getRLAnnotation(ast, rl));
			}

			MethodDeclaration method = (MethodDeclaration) methodNode;

			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}

			if (rlary.expressions().size() > 0) {
				method.modifiers().add(0, root);
			}

			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());

			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));

			edits.apply(document);

			cu.getBuffer().setContents(document.get());

			return true;
		} catch (Exception ex) {
			logger.error("[updateRLAnnotation] EXCEPTION ", ex);
		}
		return false;
	}

	/**
	 * generate robustness level information for robustness level's annotation
	 * 
	 * @param ast
	 *            AST Object
	 * @param levelVal
	 *            robustness level value
	 * @param exClass
	 *            exception class
	 * @return NormalAnnotation AST Node
	 */

	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, RLData rldata) {
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("Tag"));

		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		level.setValue(ast.newNumberLiteral(String.valueOf(rldata.getLevel() <= 0 ? 1 : rldata.getLevel())));

		rl.values().add(level);

		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		exclass.setType(ast.newSimpleType(ast.newName(rldata.getExceptionType())));
		exception.setValue(exclass);

		rl.values().add(exception);

		return rl;
	}

	public void swapRLAnnotation(int pos, boolean goingUp) {
		if (goingUp) {
			if (pos > 0) {
				RLMessage msg1 = rlAnnotationList.get(pos);
				RLMessage msg2 = rlAnnotationList.get(pos - 1);
				msg1.setEdited(true);
				msg2.setEdited(true);
				rlAnnotationList.set(pos - 1, msg1);
				rlAnnotationList.set(pos, msg2);
			}
		} else {
			if (pos < rlAnnotationList.size() - 1) {
				RLMessage msg1 = rlAnnotationList.get(pos);
				RLMessage msg2 = rlAnnotationList.get(pos + 1);
				msg1.setEdited(true);
				msg2.setEdited(true);
				rlAnnotationList.set(pos + 1, msg1);
				rlAnnotationList.set(pos, msg2);
			}
		}
	}

	// ************************************************************************
	// Setter/Getter
	// ************************************************************************
	/**
	 * get cursor position line number
	 *  
	 * @param pos
	 * @return
	 */
	public int getCurrentLine() {
		return this.currentLine;
	}

	public void setCurrentLine(int pos) {
		if (this.actRoot != null) {
			this.currentLine = this.actRoot.getLineNumber(pos);
		} else {
			this.currentLine = -1;
		}
	}

	public boolean hasExceptionData() {
		return hasData() && exceptionList != null && exceptionList.size() > 0;
	}

	public void clearData() {
		this.actRoot = null;
	}

	public boolean hasData() {
		return methodNode != null && actRoot != null;
	}

	public int getPosition() {
		return (this.methodNode != null ? this.methodNode.getStartPosition() : 0);
	}

	public List<RLMessage> getExceptionList() {
		return exceptionList;
	}

	public List<RLMessage> getRLAnnotationList() {
		return rlAnnotationList;
	}

	public IOpenable getActOpenable() {
		return actOpenable;
	}

	public String getMethodName() {
		if (methodNode != null) {
			MethodDeclaration method = (MethodDeclaration) methodNode;
			return method.getName().getIdentifier();
		} else {
			return null;
		}
	}
}
