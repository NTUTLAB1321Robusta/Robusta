package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.csdet.quickfix.BaseQuickFix;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.common.ErrorLog;
import ntut.csie.rleht.views.ExceptionAnalyzer;

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

import agile.exception.SuppressSmell;

/**
 * Quick Fix SuppressSmell Annotation
 * @author Shiau
 * �ѦҸ�ơG
 * SuppressWarnings
 * org.eclipse.jdt.internal.ui.text.correction.SuppressWarningsSubProcessor
 * org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor
 */
public class CSQuickFix extends BaseQuickFix implements IMarkerResolution, IMarkerResolution2 {
	private static Logger logger = LoggerFactory.getLogger(CSQuickFix.class);

	/** �u������s���Ҫ��W�� */
	private String label;
	/** Annotation�O�_�[�bCatch���A�_�h�[�bMethod�W */
	private boolean inCatch;
	/** �s��ثe�n�ק諸.java�� */
	private CompilationUnit actRoot;
	/** �ثe��Method AST Node */
	private ASTNode currentMethodNode = null;
	/** Smell��Type */
	private String markerType;
	/** AST Rewrite */
	private ASTRewrite rewrite;	
	/** marker�bSource���}�l��m */
	private String markerStartPos;
	/** marker�Ҧb��Catch Index */
	private int catchIdx = -1;
	
	public CSQuickFix(String label, boolean inCatch) {
		this.label = label;
		this.inCatch = inCatch;
	}
	
	public CSQuickFix(String label, String type, boolean inCatch) {
		this.label = label;
		this.markerType = type;
		this.inCatch = inCatch;
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
			markerStartPos = (String) marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS);

			if (problem != null && problem.equals(RLMarkerAttribute.ERR_SS_FAULT_NAME)) {
				String faultName = (String) marker.getAttribute(RLMarkerAttribute.ERR_SS_FAULT_NAME);

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));

				if (isok) {
					// �O�_�[�bCatch���A�_�h�bMethod�W
					if (inCatch) {
						// ���o���ק�ASTNode
						CatchClause cc = getCatchClause();	
						SingleVariableDeclaration svd = cc.getException();
	
						// �ק�SuppressSmell Annotation�T��
						replaceSuppressSmellAnnotation(svd.modifiers(), faultName);
					} else {
						// ���o���ק諸ASTNode
						MethodDeclaration method = (MethodDeclaration) currentMethodNode;
						// �NAnnotation�T���W�[����wMethod�W
						replaceSuppressSmellAnnotation(method.modifiers(), faultName);
					}
				}
			} else {
				if (!problem.equals(RLMarkerAttribute.ERR_SS_NO_SMELL))
					markerType = problem;

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if (isok) {
					// �O�_�[�bCatch���A�_�h�bMethod�W
					if (inCatch) {
						// ���o���ק�ASTNode
						CatchClause cc = getCatchClause();

						SingleVariableDeclaration svd = cc.getException();
						
						// �NAnnotation�T���W�[����wCatch��
						addSuppressSmellAnnotation(svd, svd.modifiers(), SingleVariableDeclaration.MODIFIERS2_PROPERTY);
					} else {
						// ���o���ק諸ASTNode
						MethodDeclaration method = (MethodDeclaration) currentMethodNode;
						// �NAnnotation�T���W�[����wMethod�W
						addSuppressSmellAnnotation(method, method.modifiers(), method.getModifiersProperty());
					}
				}
			}
			// �w���Annotation�Ӧ�
			selectLine(marker, methodIdx);
		} catch (CoreException e) {
			ErrorLog.getInstance().logError("[CSQuickFix]", e);
		}
	}

	/**
	 * ���oMethod����T
	 * @param resource
	 * @param methodIdx		method��Index
	 * @return				�O�_���\
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
				actRoot = (CompilationUnit) parser.createAST(null);
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();

				ASTNode method = methodList.get(methodIdx);
				if (method != null) {
					ExceptionAnalyzer visitor = new ExceptionAnalyzer(actRoot, method.getStartPosition(), 0);
					method.accept(visitor);
					currentMethodNode = visitor.getCurrentMethodNode();

					if (currentMethodNode != null)
						return true;
				}
			}
			catch (Exception ex) {
				logger.error("[findMethod] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 * ���oMarker�Ҧb��Catch Clause
	 * @return
	 */
	private CatchClause getCatchClause() {
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();

		//�Y�w���oCatch��m�A�h������X
		if (catchIdx != -1)
			return catchList.get(catchIdx);

		for (int i = 0; i < catchList.size(); i++) {
			//����Catch(�p�GCatch����m�P���UQuick���檺�_�l��m�ۦP)
			if (catchList.get(i).getStartPosition() == Integer.parseInt(markerStartPos)) {
				catchIdx = i;
				return catchList.get(i);
			}
		}
		return null;
	}

	/**
	 * �s�WSuppressSmell Annotation�T��
	 * 
	 * @param node
	 * @param modifiers
	 * @param property
	 */
	private void addSuppressSmellAnnotation(ASTNode node, List<?> modifiers, ChildListPropertyDescriptor property) {
		AST ast = currentMethodNode.getAST();
		rewrite = ASTRewrite.create(ast);

		// �[�JSuppressSmell Library 
		addImportDeclaration();

		// �إ�Annotation root
		Annotation existing = findExistingAnnotation(modifiers);
		
		StringLiteral newStringLiteral = ast.newStringLiteral();
		newStringLiteral.setLiteralValue(markerType);

		// SuppressSmell Annotation���s�b
		if (existing == null) {
			
			ListRewrite listRewrite = rewrite.getListRewrite(node, property);
			
			SingleMemberAnnotation newAnnot = ast.newSingleMemberAnnotation();
			newAnnot.setTypeName(ast.newName("SuppressSmell"));
	
			newAnnot.setValue(newStringLiteral);

			listRewrite.insertFirst(newAnnot, null);
		// �Y�w�s�b @SuppressSmell()
		} else if (existing instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
			Expression value= annotation.getValue();
			
			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				rewrite.set(existing, SingleMemberAnnotation.VALUE_PROPERTY, newStringLiteral, null);
			}
		// �Y�w�s�b @SuppressSmell(value={})
		} else if (existing instanceof NormalAnnotation) {
			NormalAnnotation annotation = (NormalAnnotation) existing;
			Expression value = findValue(annotation.values());

			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				ListRewrite listRewrite= rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);

				MemberValuePair pair= ast.newMemberValuePair();
				pair.setName(ast.newSimpleName("value"));
				pair.setValue(newStringLiteral);

				listRewrite.insertFirst(pair, null);
			}
		}
		// �N�n�ܧ󪺸�Ƽg�^��Document�� 
		applyChange(rewrite);
	}

	/**
	 * �[�JSuppressSmell��Argument
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
	 * �ק�SuppressSmell Annotation�T��
	 * @param list 
	 * @param faultName 
	 */
	private void replaceSuppressSmellAnnotation(List<?> modifiers, String faultName) {
		// ���o���ק諸ASTNode
		AST ast = currentMethodNode.getAST();
		rewrite = ASTRewrite.create(ast);

		StringLiteral newStringLiteral = ast.newStringLiteral();
		newStringLiteral.setLiteralValue(markerType);
		
		// �إ�Annotation root
		Annotation existing = findExistingAnnotation(modifiers);
		if (existing instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
			Expression value= annotation.getValue();
			
			replaceSuppressArgument(newStringLiteral, value, faultName);
		// �Y�w�s�b @SuppressSmell(value={})
		} else if (existing instanceof NormalAnnotation) {
			NormalAnnotation annotation = (NormalAnnotation) existing;
			Expression value = findValue(annotation.values());

			replaceSuppressArgument(newStringLiteral, value, faultName);
		}
		// �N�n�ܧ󪺸�Ƽg�^��Document�� 
		applyChange();
	}

	/**
	 * �ק�SuppressSmell��Argument
	 * @param newStringLiteral
	 * @param value
	 * @param faultName 
	 */
	private void replaceSuppressArgument(StringLiteral newStringLiteral, Expression value, String faultName) {
		// �YAnnotation�̥u��String Literal�����ק�
		if (value instanceof StringLiteral) {
			rewrite.replace(value, newStringLiteral, null);
		// �YAnnotation�̦�Array�A�M��Fault Name�A���N
		} else if (value instanceof ArrayInitializer) {
			ArrayInitializer ai = (ArrayInitializer) value;
			// �M��fault name�íקאּ�ϥΪ̩ҿ�ܪ�Smell Type
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
	 * ���oAnnotation��Key Value
	 * Annotation(value={} <= �o��)
	 * @param keyValues
	 * @return
	 */
	private Expression findValue(List<?> keyValues) {
		for (int i = 0; i < keyValues.size(); i++) {
			MemberValuePair curr= (MemberValuePair) keyValues.get(i);
			if ("value".equals(curr.getName().getIdentifier()))
				return curr.getValue();
		}
		return null;
	}

	/**
	 * �M��w�s�b��SuppressSmell Annotation
	 * @param modifiers
	 * @return
	 */
	public static Annotation findExistingAnnotation(List<?> modifiers) {
		for (int i= 0; i < modifiers.size(); i++) {
			Object curr = modifiers.get(i);
			if (curr instanceof NormalAnnotation || curr instanceof SingleMemberAnnotation) {
				Annotation annotation= (Annotation) curr;
				ITypeBinding typeBinding= annotation.resolveTypeBinding();
				if (typeBinding != null) {
					if ("SuppressSmell".equals(typeBinding.getQualifiedName()) ||
						"agile.exception.SuppressSmell".equals(typeBinding.getQualifiedName())) {
						return annotation;
					}
				} else {
					String fullyQualifiedName = annotation.getTypeName().getFullyQualifiedName();
					if ("SuppressSmell".equals(fullyQualifiedName) ||
						"agile.exception.SuppressSmell".equals(fullyQualifiedName)) {
						return annotation;
					}
				}
			}
		}
		return null;
	}

	/**
	 * �P�_�O�_��import SuppressSmell Library�A�Y�S���h�⥦�[�J
	 */
	private void addImportDeclaration() {
		// �P�_�O�_�w�gImport Robustness��RL���ŧi
		ListRewrite listRewrite = rewrite.getListRewrite(this.actRoot, CompilationUnit.IMPORTS_PROPERTY);
		// �M�䦳�S��import agile.exception.SuppressSmell;
		boolean isSuppressSmellClass = false;
		for (Object obj : listRewrite.getRewrittenList()) {
			ImportDeclaration id = (ImportDeclaration)obj;
			if (SuppressSmell.class.getName().equals(id.getName().getFullyQualifiedName())) {
				isSuppressSmellClass = true;
			}			
		}
		// �Y���[�JSuppressSmell Class�A�h�[�Jimport
		AST rootAst = this.actRoot.getAST();
		if (!isSuppressSmellClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(SuppressSmell.class.getName()));
			listRewrite.insertLast(imp, null);
		}
	}
	
//	/**
//	 * �N�n�ܧ󪺸�Ƽg�^��Document��
//	 */
//	private void applyChange(){
//		// �g�^Edit��
//		try {
//			ICompilationUnit cu = (ICompilationUnit) actOpenable;
//			Document document = new Document(cu.getBuffer().getContents());
//
//			TextEdit edits = rewrite.rewriteAST(document,null);
//			edits.apply(document);
//
//			cu.getBuffer().setContents(document.get());
//		}catch (Exception ex) {
//			logger.error("[UMQuickFix] EXCEPTION ",ex);
//		}
//	}
//
//	/**
//	 * Catch�gAST�[�JAnnotation��AException�|����ĤG��
//	 * @param cu
//	 * @param document
//	 * @param anno
//	 * @throws BadLocationException
//	 * @throws JavaModelException
//	 */
//	private void deleteCatchClauseSpace(ICompilationUnit cu, Document document,	Annotation anno)
//			throws BadLocationException, JavaModelException {
//		// �M��Annotation�᪺����B�Ů�B'\t'����
//		int length = 2;
//		while (true) {
//			char inp = document.getChar(anno.getStartPosition()+ anno.getLength() + length);
//			if (inp != '\t' && inp != ' ')
//				break;
//			length++;
//		}
//		// �NAnnotation�᪺����B�Ů�B'\t'��" "���N
//		document.replace(anno.getStartPosition()+ anno.getLength(), length, " ");
//		cu.getBuffer().setContents(document.get());
//	}
	
	/**
	 * ��Щw��(�w���RL Annotation����)
	 * @param marker
	 * @param methodIdx
	 * @throws JavaModelException
	 */
	private void selectLine(IMarker marker, String methodIdx) throws JavaModelException {
		// ���s���o�s��Method��T(�]����Ƥw���ܡAmethod���Ǹ�T�O�ª�)
		boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
		if (isok) {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			
			// ���o�ثe��EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;

			try {
				if (inCatch) {
					// ���oCatch Clause
					CatchClause cc = getCatchClause();
					// ���oAnnotation
					Annotation anno  = findExistingAnnotation(cc.getException().modifiers());
					
					// �ϥոӦ� �bQuick fix������,�i�H�N��Щw��bQuick Fix����
					editor.selectAndReveal(anno.getStartPosition(), anno.getLength());
				} else {
					// ���oMethod���_�I��m
					int srcPos = currentMethodNode.getStartPosition();
					// ��Method�_�I��m���oMethod���ĴX���(�_�l��Ʊq0�}�l�A���O1�A�ҥH��1)
					int numLine = this.actRoot.getLineNumber(srcPos) - 1;
	
					// ���o��ƪ����
					IRegion lineInfo = document.getLineInformation(numLine);
					// �ϥոӦ� �bQuick fix������,�i�H�N��Щw��bQuick Fix����
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
		// Resource Icons��Annotation�ϥ�
		// return ImageManager.getInstance().get("annotation");
		// ���ت�
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
	}

}
