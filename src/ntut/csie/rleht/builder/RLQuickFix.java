
package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.csdet.quickfix.BaseQuickFix;
import ntut.csie.rleht.common.ErrorLog;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLChecker;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.robusta.agile.exception.RTag;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RLQuickFix extends BaseQuickFix implements IMarkerResolution, IMarkerResolution2 {
	private static Logger logger = LoggerFactory.getLogger(RLQuickFix.class);
	private List<RLMessage> currentMethodExceptionList = null;
	private List<RLMessage> currentMethodRobustnessLevelAnnotationList = null;
	private String label;
	private String errMsg;
	private int levelForUpdate;

	public RLQuickFix(String label, String errMsg) {
		this.label = label;
		this.errMsg = errMsg;
	}

	public RLQuickFix(String label, int level, String errMsg) {
		this.label = label;
		this.levelForUpdate = level;
		this.errMsg = errMsg;
	}

	public String getLabel() {
		return label;
	}
	
	@Override
	public String getDescription() {
		return Messages.format(CorrectionMessages.MarkerResolutionProposal_additionaldesc, errMsg);
	}

	@Override
	public Image getImage() {
		// annotation icon of resource 
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
	}

	public void run(IMarker marker) {
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			//there is a problem in tag level(if level is over from 1 to 3)
			if (problem != null && problem.equals(RLMarkerAttribute.ERR_RL_LEVEL)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if (isok) {
					this.updateRLAnnotation(Integer.parseInt(msgIdx), levelForUpdate);
				}
			}
			//when robustness level information is empty
			else if (problem != null && problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if (isok) {
					this.addOrRemoveRLAnnotation(true, Integer.parseInt(msgIdx));
				}
			}
			//swap the order of robustness level
			else if (problem != null && problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				// adjust the order of robustness level annotation.
				new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
			}
		} catch (CoreException e) {
			ErrorLog.getInstance().logError("[RLQuickFix]", e);
		}
	}

	/**
	 * get method's robustness level and exception information
	 * @param resource
	 * @param methodIdx		method的Index
	 * @return				success or not
	 */
	private boolean findMethod(IResource resource, int methodIdx) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {

			try {
				IJavaElement javaElement = JavaCore.create(resource);

				if (javaElement instanceof IOpenable) {
					this.actOpenable = (IOpenable) javaElement;
				}

				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);

				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.javaFileWillBeQuickFixed = (CompilationUnit) parser.createAST(null);
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				this.javaFileWillBeQuickFixed.accept(methodCollector);
				List<MethodDeclaration> methodList = methodCollector.getMethodList();

				MethodDeclaration method = methodList.get(methodIdx);
				if (method != null) {
					ExceptionAnalyzer visitor = new ExceptionAnalyzer(this.javaFileWillBeQuickFixed, method.getStartPosition(), 0);
					method.accept(visitor);
					this.methodNodeWillBeQuickFixed = visitor.getCurrentMethodNode();
					currentMethodRobustnessLevelAnnotationList = visitor.getMethodRLAnnotationList();

					if (this.methodNodeWillBeQuickFixed != null) {
						RLChecker checker = new RLChecker();
						currentMethodExceptionList = checker.check(visitor);
						return true;
					}
				}
			}
			catch (Exception ex) {
				logger.error("[findMethod] EXCEPTION ",ex);
			}
		}
		return false;
	}

	/**
	 * import robustness library if library has not been imported. 
	 */
	@SuppressWarnings("unchecked")
	private void addImportDeclaration() {
		List<ImportDeclaration> importList = this.javaFileWillBeQuickFixed.imports();

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

		AST rootAst = this.javaFileWillBeQuickFixed.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			this.javaFileWillBeQuickFixed.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			this.javaFileWillBeQuickFixed.imports().add(imp);
		}
	}

	/**
	 * add robustness level  annotation to specified method
	 * 
	 * @param add
	 * @param pos
	 */
	@SuppressWarnings("unchecked")
	private void addOrRemoveRLAnnotation(boolean add, int pos) {

		RLMessage msg = this.currentMethodExceptionList.get(pos);

		try {
			this.javaFileWillBeQuickFixed.recordModifications();

			AST ast = this.methodNodeWillBeQuickFixed.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);
			
			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			if (add) {
				addImportDeclaration();
				//add @Tag Annotation of the exception which has been selected.
				rlary.expressions().add(
						getRLAnnotation(ast, msg.getRLData().getLevel() <= 0 ? RTag.LEVEL_1_ERR_REPORTING : msg
								.getRLData().getLevel(), msg.getRLData().getExceptionType()));
			}
			//add original @Tag Annotation 
			int idx = 0;
			for (RLMessage rlmsg : currentMethodRobustnessLevelAnnotationList) {
				if (add) {
					// create
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
				}
				else {
					// remove 
					if (idx++ != pos) {
						rlary.expressions()
								.add(
										getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData()
												.getExceptionType()));
					}
				}
			}

			MethodDeclaration method = (MethodDeclaration) this.methodNodeWillBeQuickFixed;

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

			this.applyChange();
		}
		catch (Exception ex) {
			logger.error("[addOrRemoveRLAnnotation] EXCEPTION ",ex);
		}
	}

	/**
	 * update robustness level annotation
	 * 
	 * @param isAllUpdate	
	 * @param pos
	 * @param level
	 */
	private void updateRLAnnotation(int pos, int level) {
		try {

			this.javaFileWillBeQuickFixed.recordModifications();

			AST ast = methodNodeWillBeQuickFixed.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);

			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			
			int msgIdx = 0;
			for (RLMessage rlmsg : currentMethodRobustnessLevelAnnotationList) {
				if (msgIdx++ == pos) {
					rlary.expressions().add(getRLAnnotation(ast, level, rlmsg.getRLData().getExceptionType()));
				}
				else {
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
				}
			}

			MethodDeclaration method = (MethodDeclaration) methodNodeWillBeQuickFixed;

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

			this.applyChange();
		}
		catch (Exception ex) {
			logger.error("[updateRLAnnotation] EXCEPTION ",ex);
		}
	}

	/**
	 * generate robustness level information of robustness level annotation
	 * 
	 * @param astNode		
	 * @param robustnessLevelVal	
	 * @param exceptionClass	
	 * @return NormalAnnotation AST Node
	 */
	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST astNode, int robustnessLevelVal, String exceptionClass) {
		NormalAnnotation rl = astNode.newNormalAnnotation();
		rl.setTypeName(astNode.newSimpleName("RTag"));

		MemberValuePair level = astNode.newMemberValuePair();
		level.setName(astNode.newSimpleName("level"));
		level.setValue(astNode.newNumberLiteral(String.valueOf(robustnessLevelVal)));

		rl.values().add(level);

		MemberValuePair exception = astNode.newMemberValuePair();
		exception.setName(astNode.newSimpleName("exception"));
		TypeLiteral exclass = astNode.newTypeLiteral();
		exclass.setType(astNode.newSimpleType(astNode.newName(exceptionClass)));
		exception.setValue(exclass);

		rl.values().add(exception);
		return rl;
	}
}
