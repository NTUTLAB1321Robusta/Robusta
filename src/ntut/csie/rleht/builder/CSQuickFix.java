package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.analyzer.ASTCatchCollect;
import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.csdet.quickfix.BaseQuickFix;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.common.ErrorLog;
import ntut.csie.robusta.agile.exception.SuppressSmell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Quick Fix SuppressSmell Annotation
 * @author Shiau
 * Reference：
 * SuppressWarnings
 * org.eclipse.jdt.internal.ui.text.correction.SuppressWarningsSubProcessor
 * org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor
 */
public class CSQuickFix extends BaseQuickFix implements IMarkerResolution, IMarkerResolution2 {
	private static Logger logger = LoggerFactory.getLogger(CSQuickFix.class);

	private String label;
	private boolean isInCatchClause;
	private CompilationUnit javaFileWhichWillBeQuickFix;
	private ASTNode currentMethodNode = null;
	private String badSmellType;
	private ASTRewrite rewrite;	
	private String markerStartPositionInSourceCode;
	private int catchClauseIndexOfMarker = -1;
	
	public CSQuickFix(String label, String type, boolean inCatch) {
		this.label = label;
		this.badSmellType = type;
		this.isInCatchClause = inCatch;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			markerStartPositionInSourceCode = (String) marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS);

			if (problem != null && problem.equals(RLMarkerAttribute.ERR_SS_FAULT_NAME)) {
				String faultName = (String) marker.getAttribute(RLMarkerAttribute.ERR_SS_FAULT_NAME);

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));

				if (isok) {
					// if isInCatchClause is true then add SuppressSmell annotation on catch clause or add SuppressSmell annotation on method declare
					if (isInCatchClause) {
						CatchClause cc = getCatchClause();	
						SingleVariableDeclaration svd = cc.getException();
	
						updateSuppressSmellAnnotation(svd.modifiers(), faultName);
					} else {
						MethodDeclaration method = (MethodDeclaration) currentMethodNode;
						updateSuppressSmellAnnotation(method.modifiers(), faultName);
					}
				}
			} else {
				if (!problem.equals(RLMarkerAttribute.ERR_SS_NO_SMELL))
					badSmellType = problem;

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if (isok) {
					// if isInCatchClause is true then add SuppressSmell annotation on catch clause or add SuppressSmell annotation on method declare
					if (isInCatchClause) {
						CatchClause cc = getCatchClause();

						SingleVariableDeclaration svd = cc.getException();
						
						addSuppressSmellAnnotation(svd, svd.modifiers(), SingleVariableDeclaration.MODIFIERS2_PROPERTY);
					} else {
						MethodDeclaration method = (MethodDeclaration) currentMethodNode;
						addSuppressSmellAnnotation(method, method.modifiers(), method.getModifiersProperty());
					}
				}
			}
			selectLine(marker, methodIdx);
		} catch (CoreException e) {
			ErrorLog.getInstance().logError("[CSQuickFix]", e);
		}
	}

	/**
	 * get information of method which will be quick fixed
	 * @param resource
	 * @param methodIdx		
	 * 				method's Index
	 * @return	successfully or not
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
				javaFileWhichWillBeQuickFix = (CompilationUnit) parser.createAST(null);
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				javaFileWhichWillBeQuickFix.accept(methodCollector);
				List<MethodDeclaration> methodList = methodCollector.getMethodList();

				currentMethodNode = methodList.get(methodIdx);
				return true;
			}
			catch (Exception ex) {
				logger.error("[findMethod] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 * get catch clause at marker's position
	 * @return
	 */
	private CatchClause getCatchClause() {
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();

		if (catchClauseIndexOfMarker != -1)
			return catchList.get(catchClauseIndexOfMarker);

		for (int i = 0; i < catchList.size(); i++) {
			if (catchList.get(i).getStartPosition() == Integer.parseInt(markerStartPositionInSourceCode)) {
				catchClauseIndexOfMarker = i;
				return catchList.get(i);
			}
		}
		return null;
	}

	/**
	 * create SuppressSmell annotation
	 * 
	 * @param node
	 * @param modifiers
	 * @param property
	 */
	private void addSuppressSmellAnnotation(ASTNode node, List<?> modifiers, ChildListPropertyDescriptor property) {
		AST ast = currentMethodNode.getAST();
		rewrite = ASTRewrite.create(ast);

		importSuppressSmllLibrary();

		// establish annotation's root
		Annotation existing = getExistingAnnotation(modifiers);
		
		StringLiteral newStringLiteral = ast.newStringLiteral();
		newStringLiteral.setLiteralValue(badSmellType);

		if (existing == null) {
			
			ListRewrite listRewrite = rewrite.getListRewrite(node, property);
			
			SingleMemberAnnotation newAnnot = ast.newSingleMemberAnnotation();
			newAnnot.setTypeName(ast.newName("SuppressSmell"));
	
			newAnnot.setValue(newStringLiteral);

			listRewrite.insertFirst(newAnnot, null);
		// if "@SuppressSmell()", SingleMemberAnnotation, has existed
		} else if (existing instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
			Expression value= annotation.getValue();
			
			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				rewrite.set(existing, SingleMemberAnnotation.VALUE_PROPERTY, newStringLiteral, null);
			}
		// if "@SuppressSmell(value={})", NormalAnnotation, has existed
		} else if (existing instanceof NormalAnnotation) {
			NormalAnnotation annotation = (NormalAnnotation) existing;
			Expression value = getValue(annotation.values());

			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				ListRewrite listRewrite= rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);

				MemberValuePair pair= ast.newMemberValuePair();
				pair.setName(ast.newSimpleName("value"));
				pair.setValue(newStringLiteral);

				listRewrite.insertFirst(pair, null);
			}
		}
		applyChange(rewrite);
	}

	/**
	 * add Argument of SuppressSmell
	 * @param rewrite
	 * @param value
	 * @param newStringLiteral
	 * @return
	 */
	private boolean addSuppressArgument(ASTRewrite rewrite, Expression value, StringLiteral newStringLiteral) {
		if (value instanceof ArrayInitializer) {
			ListRewrite listRewrite= rewrite.getListRewrite(value, ArrayInitializer.EXPRESSIONS_PROPERTY);
			listRewrite.insertLast(newStringLiteral, null);
		} else if (value instanceof StringLiteral) {
			ArrayInitializer newArr = rewrite.getAST().newArrayInitializer();
			newArr.expressions().add(rewrite.createMoveTarget(value));
			newArr.expressions().add(newStringLiteral);
			rewrite.replace(value, newArr, null);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * update SuppressSmell annotation
	 * @param list 
	 * @param faultName 
	 */
	private void updateSuppressSmellAnnotation(List<?> modifiers, String faultName) {
		AST ast = currentMethodNode.getAST();
		rewrite = ASTRewrite.create(ast);

		StringLiteral newStringLiteral = ast.newStringLiteral();
		newStringLiteral.setLiteralValue(badSmellType);
		
		Annotation existing = getExistingAnnotation(modifiers);
		if (existing instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
			Expression value= annotation.getValue();
			
			updateSuppressArgument(newStringLiteral, value, faultName);
		// if "@SuppressSmell(value={})", NormalAnnotation, has existed
		} else if (existing instanceof NormalAnnotation) {
			NormalAnnotation annotation = (NormalAnnotation) existing;
			Expression value = getValue(annotation.values());

			updateSuppressArgument(newStringLiteral, value, faultName);
		}
		applyChange();
	}

	/**
	 * update the argument of SuppressSmell
	 * @param newStringLiteral
	 * @param value
	 * @param faultName 
	 */
	private void updateSuppressArgument(StringLiteral newStringLiteral, Expression value, String faultName) {
		if (value instanceof StringLiteral) {
			rewrite.replace(value, newStringLiteral, null);
		} else if (value instanceof ArrayInitializer) {
			ArrayInitializer ai = (ArrayInitializer) value;
			for(Object obj: ai.expressions()) {
				StringLiteral sl = (StringLiteral)obj;
				if (sl.getLiteralValue().equals(faultName)) {
					rewrite.replace(sl, newStringLiteral, null);
					break;
				}
			}
		}
	}
	
	/**
	 * get the value by annotation's key
	 * take "Annotation(value={})" as example, we will get the content of "value={}" 
	 * @param keys
	 * @return
	 */
	private Expression getValue(List<?> keys) {
		for (int i = 0; i < keys.size(); i++) {
			MemberValuePair curr= (MemberValuePair) keys.get(i);
			if ("value".equals(curr.getName().getIdentifier()))
				return curr.getValue();
		}
		return null;
	}

	
	public static Annotation getExistingAnnotation(List<?> modifiers) {
		for (int i= 0; i < modifiers.size(); i++) {
			Object curr = modifiers.get(i);
			if (curr instanceof NormalAnnotation || curr instanceof SingleMemberAnnotation) {
				Annotation annotation= (Annotation) curr;
				ITypeBinding typeBinding= annotation.resolveTypeBinding();
				if (typeBinding != null) {
					if ("SuppressSmell".equals(typeBinding.getQualifiedName()) ||
						"ntut.csie.robusta.agile.exception.SuppressSmell".equals(typeBinding.getQualifiedName())) {
						return annotation;
					}
				} else {
					String fullyQualifiedName = annotation.getTypeName().getFullyQualifiedName();
					if ("SuppressSmell".equals(fullyQualifiedName) ||
						"ntut.csie.robusta.agile.exception.SuppressSmell".equals(fullyQualifiedName)) {
						return annotation;
					}
				}
			}
		}
		return null;
	}

	/**
	 * import SuppressSmell Library if it has not been imported.
	 */
	private void importSuppressSmllLibrary() {
		ListRewrite listRewrite = rewrite.getListRewrite(this.javaFileWhichWillBeQuickFix, CompilationUnit.IMPORTS_PROPERTY);
		boolean isSuppressSmellClass = false;
		for (Object obj : listRewrite.getRewrittenList()) {
			ImportDeclaration id = (ImportDeclaration)obj;
			if (SuppressSmell.class.getName().equals(id.getName().getFullyQualifiedName())) {
				isSuppressSmellClass = true;
			}			
		}
		AST rootAst = this.javaFileWhichWillBeQuickFix.getAST();
		if (!isSuppressSmellClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(SuppressSmell.class.getName()));
			listRewrite.insertLast(imp, null);
		}
	}
	
	/**
	 * set the position of cursor(set the cursor's position the same as the position of robustness level annotation)
	 * @param marker
	 * @param methodIdx
	 * @throws JavaModelException
	 */
	private void selectLine(IMarker marker, String methodIdx) throws JavaModelException {
		// update method's information(due to method's information has changed)
		boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
		if (isok) {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;

			try {
				if (isInCatchClause) {
					CatchClause cc = getCatchClause();
					Annotation anno  = getExistingAnnotation(cc.getException().modifiers());
					//highlight the the line which has been quick fixed 
					editor.selectAndReveal(anno.getStartPosition(), anno.getLength());
				} else {
					int srcPos = currentMethodNode.getStartPosition();
					// due to the line number is 0 base, we need to -1  after get line number
					int numLine = this.javaFileWhichWillBeQuickFix.getLineNumber(srcPos) - 1;
					IRegion lineInfo = document.getLineInformation(numLine);
					//highlight the the line which has been quick fixed 
					editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
				}
			} catch (BadLocationException e) {
				logger.error("[BadLocation] EXCEPTION ",e);
			}
		}
	}
	
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
	}

}
