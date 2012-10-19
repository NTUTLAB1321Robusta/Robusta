package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.csdet.quickfix.BaseQuickFix;
import ntut.csie.csdet.visitor.ASTCatchCollect;
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
 * 參考資料：
 * SuppressWarnings
 * org.eclipse.jdt.internal.ui.text.correction.SuppressWarningsSubProcessor
 * org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor
 */
public class CSQuickFix extends BaseQuickFix implements IMarkerResolution, IMarkerResolution2 {
	private static Logger logger = LoggerFactory.getLogger(CSQuickFix.class);

	/** 工具欄按鈕標籤的名稱 */
	private String label;
	/** Annotation是否加在Catch中，否則加在Method上 */
	private boolean inCatch;
	/** 存放目前要修改的.java檔 */
	private CompilationUnit actRoot;
	/** 目前的Method AST Node */
	private ASTNode currentMethodNode = null;
	/** Smell的Type */
	private String markerType;
	/** AST Rewrite */
	private ASTRewrite rewrite;	
	/** marker在Source的開始位置 */
	private String markerStartPos;
	/** marker所在的Catch Index */
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
					// 是否加在Catch內，否則在Method上
					if (inCatch) {
						// 取得欲修改ASTNode
						CatchClause cc = getCatchClause();	
						SingleVariableDeclaration svd = cc.getException();
	
						// 修改SuppressSmell Annotation訊息
						replaceSuppressSmellAnnotation(svd.modifiers(), faultName);
					} else {
						// 取得欲修改的ASTNode
						MethodDeclaration method = (MethodDeclaration) currentMethodNode;
						// 將Annotation訊息增加到指定Method上
						replaceSuppressSmellAnnotation(method.modifiers(), faultName);
					}
				}
			} else {
				if (!problem.equals(RLMarkerAttribute.ERR_SS_NO_SMELL))
					markerType = problem;

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if (isok) {
					// 是否加在Catch內，否則在Method上
					if (inCatch) {
						// 取得欲修改ASTNode
						CatchClause cc = getCatchClause();

						SingleVariableDeclaration svd = cc.getException();
						
						// 將Annotation訊息增加到指定Catch中
						addSuppressSmellAnnotation(svd, svd.modifiers(), SingleVariableDeclaration.MODIFIERS2_PROPERTY);
					} else {
						// 取得欲修改的ASTNode
						MethodDeclaration method = (MethodDeclaration) currentMethodNode;
						// 將Annotation訊息增加到指定Method上
						addSuppressSmellAnnotation(method, method.modifiers(), method.getModifiersProperty());
					}
				}
			}
			// 定位到Annotation該行
			selectLine(marker, methodIdx);
		} catch (CoreException e) {
			ErrorLog.getInstance().logError("[CSQuickFix]", e);
		}
	}

	/**
	 * 取得Method的資訊
	 * @param resource
	 * @param methodIdx		method的Index
	 * @return				是否成功
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
	 * 取得Marker所在的Catch Clause
	 * @return
	 */
	private CatchClause getCatchClause() {
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();

		//若已取得Catch位置，則直接輸出
		if (catchIdx != -1)
			return catchList.get(catchIdx);

		for (int i = 0; i < catchList.size(); i++) {
			//找到該Catch(如果Catch的位置與按下Quick那行的起始位置相同)
			if (catchList.get(i).getStartPosition() == Integer.parseInt(markerStartPos)) {
				catchIdx = i;
				return catchList.get(i);
			}
		}
		return null;
	}

	/**
	 * 新增SuppressSmell Annotation訊息
	 * 
	 * @param node
	 * @param modifiers
	 * @param property
	 */
	private void addSuppressSmellAnnotation(ASTNode node, List<?> modifiers, ChildListPropertyDescriptor property) {
		AST ast = currentMethodNode.getAST();
		rewrite = ASTRewrite.create(ast);

		// 加入SuppressSmell Library 
		addImportDeclaration();

		// 建立Annotation root
		Annotation existing = findExistingAnnotation(modifiers);
		
		StringLiteral newStringLiteral = ast.newStringLiteral();
		newStringLiteral.setLiteralValue(markerType);

		// SuppressSmell Annotation不存在
		if (existing == null) {
			
			ListRewrite listRewrite = rewrite.getListRewrite(node, property);
			
			SingleMemberAnnotation newAnnot = ast.newSingleMemberAnnotation();
			newAnnot.setTypeName(ast.newName("SuppressSmell"));
	
			newAnnot.setValue(newStringLiteral);

			listRewrite.insertFirst(newAnnot, null);
		// 若已存在 @SuppressSmell()
		} else if (existing instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
			Expression value= annotation.getValue();
			
			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				rewrite.set(existing, SingleMemberAnnotation.VALUE_PROPERTY, newStringLiteral, null);
			}
		// 若已存在 @SuppressSmell(value={})
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
		// 將要變更的資料寫回至Document中 
		applyChange(rewrite);
	}

	/**
	 * 加入SuppressSmell的Argument
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
	 * 修改SuppressSmell Annotation訊息
	 * @param list 
	 * @param faultName 
	 */
	private void replaceSuppressSmellAnnotation(List<?> modifiers, String faultName) {
		// 取得欲修改的ASTNode
		AST ast = currentMethodNode.getAST();
		rewrite = ASTRewrite.create(ast);

		StringLiteral newStringLiteral = ast.newStringLiteral();
		newStringLiteral.setLiteralValue(markerType);
		
		// 建立Annotation root
		Annotation existing = findExistingAnnotation(modifiers);
		if (existing instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
			Expression value= annotation.getValue();
			
			replaceSuppressArgument(newStringLiteral, value, faultName);
		// 若已存在 @SuppressSmell(value={})
		} else if (existing instanceof NormalAnnotation) {
			NormalAnnotation annotation = (NormalAnnotation) existing;
			Expression value = findValue(annotation.values());

			replaceSuppressArgument(newStringLiteral, value, faultName);
		}
		// 將要變更的資料寫回至Document中 
		applyChange();
	}

	/**
	 * 修改SuppressSmell的Argument
	 * @param newStringLiteral
	 * @param value
	 * @param faultName 
	 */
	private void replaceSuppressArgument(StringLiteral newStringLiteral, Expression value, String faultName) {
		// 若Annotation裡只有String Literal直接修改
		if (value instanceof StringLiteral) {
			rewrite.replace(value, newStringLiteral, null);
		// 若Annotation裡有Array，尋找Fault Name再取代
		} else if (value instanceof ArrayInitializer) {
			ArrayInitializer ai = (ArrayInitializer) value;
			// 尋找fault name並修改為使用者所選擇的Smell Type
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
	 * 取得Annotation的Key Value
	 * Annotation(value={} <= 這個)
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
	 * 尋找已存在的SuppressSmell Annotation
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
	 * 判斷是否有import SuppressSmell Library，若沒有則把它加入
	 */
	private void addImportDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		ListRewrite listRewrite = rewrite.getListRewrite(this.actRoot, CompilationUnit.IMPORTS_PROPERTY);
		// 尋找有沒有import ntut.csie.robusta.agile.exception.SuppressSmell;
		boolean isSuppressSmellClass = false;
		for (Object obj : listRewrite.getRewrittenList()) {
			ImportDeclaration id = (ImportDeclaration)obj;
			if (SuppressSmell.class.getName().equals(id.getName().getFullyQualifiedName())) {
				isSuppressSmellClass = true;
			}			
		}
		// 若未加入SuppressSmell Class，則加入import
		AST rootAst = this.actRoot.getAST();
		if (!isSuppressSmellClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(SuppressSmell.class.getName()));
			listRewrite.insertLast(imp, null);
		}
	}
	
//	/**
//	 * 將要變更的資料寫回至Document中
//	 */
//	private void applyChange(){
//		// 寫回Edit中
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
//	 * Catch經AST加入Annotation後，Exception會換到第二行
//	 * @param cu
//	 * @param document
//	 * @param anno
//	 * @throws BadLocationException
//	 * @throws JavaModelException
//	 */
//	private void deleteCatchClauseSpace(ICompilationUnit cu, Document document,	Annotation anno)
//			throws BadLocationException, JavaModelException {
//		// 尋找Annotation後的換行、空格、'\t'長度
//		int length = 2;
//		while (true) {
//			char inp = document.getChar(anno.getStartPosition()+ anno.getLength() + length);
//			if (inp != '\t' && inp != ' ')
//				break;
//			length++;
//		}
//		// 將Annotation後的換行、空格、'\t'用" "取代
//		document.replace(anno.getStartPosition()+ anno.getLength(), length, " ");
//		cu.getBuffer().setContents(document.get());
//	}
	
	/**
	 * 游標定位(定位到RL Annotation那行)
	 * @param marker
	 * @param methodIdx
	 * @throws JavaModelException
	 */
	private void selectLine(IMarker marker, String methodIdx) throws JavaModelException {
		// 重新取得新的Method資訊(因為資料已改變，method那些資訊是舊的)
		boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
		if (isok) {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			
			// 取得目前的EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;

			try {
				if (inCatch) {
					// 取得Catch Clause
					CatchClause cc = getCatchClause();
					// 取得Annotation
					Annotation anno  = findExistingAnnotation(cc.getException().modifiers());
					
					// 反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
					editor.selectAndReveal(anno.getStartPosition(), anno.getLength());
				} else {
					// 取得Method的起點位置
					int srcPos = currentMethodNode.getStartPosition();
					// 用Method起點位置取得Method位於第幾行數(起始行數從0開始，不是1，所以減1)
					int numLine = this.actRoot.getLineNumber(srcPos) - 1;
	
					// 取得行數的資料
					IRegion lineInfo = document.getLineInformation(numLine);
					// 反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
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
		// Resource Icons的Annotation圖示
		// return ImageManager.getInstance().get("annotation");
		// 內建的
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
	}

}
