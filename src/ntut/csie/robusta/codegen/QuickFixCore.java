package ntut.csie.robusta.codegen;

import java.util.List;

import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickFixCore {
	private static Logger logger = LoggerFactory.getLogger(QuickFixCore.class);
	
	/** 要被設定為Navigatable的Java檔，後面要從resource去設定他 */
	protected IOpenable actOpenable = null;
	/** 要被Quick Fix的Java AST root node */
	private CompilationUnit compilationUnit = null;
	
	private ASTRewrite astRewrite = null;
	
	public void setJavaFileModifiable(IResource resource) {
		IJavaElement javaElement = JavaCore.create(resource);
		
		if (javaElement instanceof IOpenable) {
			actOpenable = (IOpenable) javaElement;
		}
		
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource((ICompilationUnit) javaElement);
		parser.setResolveBindings(true);
		compilationUnit = (CompilationUnit) parser.createAST(null);
		//AST 2.0紀錄方式
		compilationUnit.recordModifications();
		astRewrite = ASTRewrite.create(compilationUnit.getRoot().getAST());
	}
	
	public void generateRobustnessLevelAnnotation(MethodDeclaration methodDeclaration, int level, Class<?> exceptionType) {
		AST rootAST = compilationUnit.getAST();
		
		// Detect if the Robustness Level Annotation exists or not.
		if(QuickFixUtils.getExistingRLAnnotation(methodDeclaration) != null) {
			appendRLAnnotation(level, exceptionType, rootAST, methodDeclaration);
		} else {
			createRLAnnotation(level, exceptionType, rootAST, methodDeclaration);
		}
		
		//Import robustness level package if it's not imported.
		boolean isRobustnessClassImported = 
			QuickFixUtils.isClassImported(Robustness.class, compilationUnit);
		boolean isRLClassImported = 
			QuickFixUtils.isClassImported(RTag.class, compilationUnit);
		
		if(!isRobustnessClassImported){
			ImportDeclaration importDeclaration = rootAST.newImportDeclaration();
			importDeclaration.setName(rootAST.newName(Robustness.class.getName()));
			
			//The rewrite list from the AST of compilation unit that you want to modify.
			ListRewrite addImportDeclarationList = astRewrite.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
			addImportDeclarationList.insertLast(importDeclaration, null);
		}
		
		if(!isRLClassImported){
			ImportDeclaration importDeclaration = rootAST.newImportDeclaration();
			importDeclaration.setName(rootAST.newName(RTag.class.getName()));
			
			//The rewrite list from the AST of compilation unit that you want to modify.
			ListRewrite addImportDeclarationList = astRewrite.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
			addImportDeclarationList.insertLast(importDeclaration, null);
		}
	}
	
	/**
	 * When a method without any Robustness Level Annotation, you should use this method to create RL Annotation.
	 * @param level
	 * @param exceptionType
	 * @param rootAST
	 * @param methodDeclaration
	 */
	private void createRLAnnotation(int level, Class<?> exceptionType, AST rootAST, MethodDeclaration methodDeclaration){
				
		/*
		 * Add Robustness level annotation
		 */
		//Robustness level annotation belongs NormalAnnotation class.
		NormalAnnotation normalAnnotation = rootAST.newNormalAnnotation();
		normalAnnotation.setTypeName(rootAST.newSimpleName(Robustness.class.getSimpleName()));
		//RL "level" and "exception type"
		MemberValuePair value = rootAST.newMemberValuePair();
		value.setName(rootAST.newSimpleName(Robustness.VALUE));

		ListRewrite normalAnnotationRewrite = astRewrite.getListRewrite(normalAnnotation, NormalAnnotation.VALUES_PROPERTY);
		normalAnnotationRewrite.insertLast(value, null);
		
		ArrayInitializer rlArrayInitializer = rootAST.newArrayInitializer();
		value.setValue(rlArrayInitializer);

		ListRewrite rlArrayInitializerRewrite = astRewrite.getListRewrite(rlArrayInitializer, ArrayInitializer.EXPRESSIONS_PROPERTY);
		rlArrayInitializerRewrite.insertLast(QuickFixUtils.makeRLAnnotation(rootAST, level, exceptionType.getName()), null);
		
//		// 這段程式碼的意義，在於移除全部舊的RLAnnotation
//		List<IExtendedModifier> lstModifiers = methodDeclaration.modifiers();
//		
//		for(IExtendedModifier ieModifier : lstModifiers){
//			//remove old Robustness Annotation
//			if(ieModifier.isAnnotation() && ieModifier.toString().indexOf("Robustness") != -1){
//				ListRewrite mdModifiers = astRewrite.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
//				mdModifiers.remove((ASTNode)ieModifier, null);
//				break;
//			}
//		}

		/*
		 * Add Annotation into MethodDeclaration node.
		 * Attention: 
 				The ASTNode and the ChildListPropertyDescriptor are special.
		 */
		ListRewrite addAnnotationList = astRewrite.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);	
		addAnnotationList.insertFirst(normalAnnotation, null);
	}
	
	/**
	 * 在Method Declaration 後面加上拋出例外的宣告
	 * @param astRewrite
	 * @param methodDeclaration
	 */
	public void generateThrowExceptionOnMethodDeclaration(MethodDeclaration methodDeclaration, Class<?> exceptionType) {
		ListRewrite addingThrowsException = astRewrite.getListRewrite(methodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		ASTNode simpleName = QuickFixUtils.generateThrowExceptionForDeclaration(methodDeclaration.getAST(), exceptionType);
		addingThrowsException.insertLast(simpleName, null);
	}
	
	/**
	 * 根據給定的程式碼，移除CatchClause中特定的節點
	 * @param catchClause
	 * @param removingStatement
	 */
	public void removeNodeInCatchClause(CatchClause catchClause, String... removingStatements) {
		//The rewrite list from the AST of catch clause that you want to modify.
		ListRewrite modifyingCatchClause = astRewrite.getListRewrite(catchClause.getBody(), Block.STATEMENTS_PROPERTY);
		for(String statement : removingStatements) {
			ExpressionStatementStringFinderVisitor expressionStatementFinder = new ExpressionStatementStringFinderVisitor(statement);
			catchClause.accept(expressionStatementFinder);
			ASTNode removeNode = expressionStatementFinder.getFoundExpressionStatement();
			if(removeNode != null) {
				modifyingCatchClause.remove(removeNode, null);
			}
		}
	}
	
	public void removeExpressionStatement(int removingStartPosition, MethodDeclaration methodDeclaration) {
		// 用StartPosition找出要移除的ExpressionStatement
		StatementFinderVisitor statementFinderVisitor = new StatementFinderVisitor(removingStartPosition);
		methodDeclaration.accept(statementFinderVisitor);
		ASTNode removingNode = statementFinderVisitor.getFoundExpressionStatement();
		
		// 找出要移除的ExpressionStatement所屬的Block
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(removingNode, ASTNode.BLOCK);
		
		ListRewrite modifyingBlock = astRewrite.getListRewrite(parentNode, Block.STATEMENTS_PROPERTY);
		modifyingBlock.remove(parentNode, null);
	}
	
	/**
	 * 增加throw new XxxException(e)的程式碼
	 * @param cc
	 * @param exceptionType
	 */
	public void addThrowRefinedExceptionInCatchClause(CatchClause cc, Class<?> exceptionType) {
		ASTNode createInstanceNode = QuickFixUtils.generateThrowNewExceptionNode(cc.getException().resolveBinding().getName(), cc.getAST(), exceptionType);
		ListRewrite modifyingCatchClause = astRewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
		modifyingCatchClause.insertLast(createInstanceNode, null);
	}
	
	public void addThrowExceptionInCatchClause(CatchClause cc) {
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		ListRewrite modifyingCatchClauseList = astRewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
		ASTNode throwCheckedException = astRewrite.createStringPlaceholder(
				"throw " + svd.resolveBinding().getName() + ";",
				ASTNode.EMPTY_STATEMENT);
		modifyingCatchClauseList.insertLast(throwCheckedException, null);
	}
	
	/**
	 * When there is existing Robustness Level Annotation, and you just want to append new annotation, you should use this.
	 * @param level
	 * @param exceptionType
	 * @param rootAST
	 * @param methodDeclaration
	 */
	private void appendRLAnnotation(int level, Class<?> exceptionType, AST rootAST, MethodDeclaration methodDeclaration){
		NormalAnnotation na = QuickFixUtils.getExistingRLAnnotation(methodDeclaration);
		MemberValuePair mvp = (MemberValuePair) na.values().get(na.values().size() - 1);
		ListRewrite normalA = astRewrite.getListRewrite(mvp.getValue(), ArrayInitializer.EXPRESSIONS_PROPERTY);
		normalA.insertLast(QuickFixUtils.makeRLAnnotation(rootAST, level, exceptionType.getName()), null);
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}
	
	public ICompilationUnit getICompilationUnit() {
		return (ICompilationUnit)actOpenable;
	}
	
	/**
	 * 將Quick Fix 要變更的內容寫回Edit中
	 * @param rewrite
	 */
	public void applyChange() {
		try {
			// 參考org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator run
			// org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal apply與performChange
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			IEditorPart part = EditorUtility.isOpenInEditor(cu);
			IEditorInput input = part.getEditorInput();
			IDocument doc = JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getDocument(input);

			performChange(JavaPlugin.getActivePage().getActiveEditor(), doc, astRewrite);
		} catch (CoreException e) {
			logger.error("[Core Exception] EXCEPTION ",e);
		}
	}
	
	/**
	 * 套用Refactoring要變更的內容
	 * @param textFileChange
	 * @throws CoreException 
	 */
	public TextFileChange applyRefactoringChange() throws JavaModelException {
		ICompilationUnit cu = (ICompilationUnit) actOpenable;
		Document document = new Document(cu.getBuffer().getContents());
		TextEdit edits = compilationUnit.rewrite(document, cu.getJavaProject().getOptions(true));
		TextFileChange textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
		textFileChange.setEdit(edits);
		return textFileChange;
	}
	
	/**
	 * 執行 Quick Fix 變更
	 * @param activeEditor
	 * @param document
	 * @param rewrite
	 * @throws CoreException
	 */
	private void performChange(IEditorPart activeEditor, IDocument document, ASTRewrite rewrite) throws CoreException {
		Change change= null;
		IRewriteTarget rewriteTarget= null;
		try {
			change= getChange(compilationUnit, rewrite);
			if (change != null) {
				if (document != null) {
					LinkedModeModel.closeAllModels(document);
				}
				if (activeEditor != null) {
					rewriteTarget= (IRewriteTarget) activeEditor.getAdapter(IRewriteTarget.class);
					if (rewriteTarget != null) {
						rewriteTarget.beginCompoundChange();
					}
				}

				change.initializeValidationData(new NullProgressMonitor());
				RefactoringStatus valid= change.isValid(new NullProgressMonitor());
				if (valid.hasFatalError()) {
					IStatus status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
						valid.getMessageMatchingSeverity(RefactoringStatus.FATAL), null);
					throw new CoreException(status);
				} else {
					IUndoManager manager= RefactoringCore.getUndoManager();
					manager.aboutToPerformChange(change);
					Change undoChange= change.perform(new NullProgressMonitor());
					manager.changePerformed(change, true);
					if (undoChange != null) {
						undoChange.initializeValidationData(new NullProgressMonitor());
						manager.addUndo("Quick Undo", undoChange);
					}
				}
			}
		} finally {
			if (rewriteTarget != null) {
				rewriteTarget.endCompoundChange();
			}

			if (change != null) {
				change.dispose();
			}
		}
	}
	
	/**
	 * 取得Quick Fix後改變的程式碼
	 */
	private Change getChange(CompilationUnit actRoot, ASTRewrite rewrite) {
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());

			TextEdit edits = null;
			edits = rewrite.rewriteAST(document, null);

			TextFileChange textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);

			return textFileChange;
		} catch (JavaModelException e) {
			logger.error("[Apply Change Rethrow Unchecked Exception] EXCEPTION ",e);
		}
		return null;
	}
	
	/**
	 * 產生一個大的TryStatement包住MethodDeclaration裡面所有程式碼
	 * @param methodDeclaration 要產生大TryStatement的MethodDeclaration
	 */
	public void generateBigOuterTryStatement(MethodDeclaration methodDeclaration) {
		ListRewrite addingBigOuterTry = astRewrite.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);

		AST ast = compilationUnit.getAST();
		/* 產生一個TryStatement */
		// 產生Try
		TryStatement bigOuterTryStatement = ast.newTryStatement();
		// 產生Catch Clause
		@SuppressWarnings("unchecked")
		List<CatchClause> catchStatement = bigOuterTryStatement.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = astRewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
		// 建立catch的type為 catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		// 在Catch中加入TODO的註解
		StringBuffer comment = new StringBuffer();
		comment.append("// TODO: handle exception");
		ASTNode placeHolder = astRewrite.createStringPlaceholder(comment.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);
		
		/* 將原本在try block之外的程式都移動進來 */
		ListRewrite tryStatement = astRewrite.getListRewrite(bigOuterTryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		int listSize = addingBigOuterTry.getRewrittenList().size();
		tryStatement.insertLast(addingBigOuterTry.createMoveTarget((ASTNode) addingBigOuterTry.getRewrittenList().get(0), 
								(ASTNode) addingBigOuterTry.getRewrittenList().get(listSize - 1)), null);

		addingBigOuterTry.insertLast(bigOuterTryStatement, null);
	}
	
	/**
	 * 將Try裡面宣告的變數移到Try外面
	 * @param tryStatement 裡面有宣告變數的Try
	 * @param variableDeclarationStatement 宣告變數的那行程式碼
	 * @param rootAST 整個compilation unit 的 AST
	 * @param methodDeclaration 這個Try所屬的method declaration
	 */
	public void moveOutVariableDeclarationStatementFromTry(TryStatement tryStatement, 
			VariableDeclarationStatement variableDeclarationStatement, AST rootAST, MethodDeclaration methodDeclaration) {
		List<?> fragments = variableDeclarationStatement.fragments();
		if(fragments.size() != 1) {
			throw new RuntimeException("Two variables declared in the same variable declaration statement is not yet supported.");
		}
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
		/* 
		 * 將   InputStream fos = new ImputStream();
		 * 改為 fos = new InputStream();
		 */
		Assignment assignment = rootAST.newAssignment();
		assignment.setOperator(Assignment.Operator.ASSIGN);
		// fos
		assignment.setLeftHandSide(rootAST.newSimpleName(fragment.getName().toString()));
		// new InputStream
		Expression init = fragment.getInitializer();
		ASTNode copyNode = ASTNode.copySubtree(init.getAST(), init);
		assignment.setRightHandSide((Expression) copyNode);

		ListRewrite tsRewrite = astRewrite.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		// 將fos = new ImputStream(); 替換到原本的程式裡
		if(assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL) {
			ExpressionStatement expressionStatement = rootAST.newExpressionStatement(assignment);
			tsRewrite.replace(variableDeclarationStatement, expressionStatement, null);
		} else {
			tsRewrite.remove(variableDeclarationStatement, null);
		}
		
		// 將InputStream fos = new ImputStream(); 改成 InputStream fos = null;
		VariableDeclarationFragment newFragment = rootAST.newVariableDeclarationFragment();
		newFragment.setName(rootAST.newSimpleName(fragment.getName().toString()));
		newFragment.setInitializer(rootAST.newNullLiteral());
		ListRewrite vdsRewrite = astRewrite.getListRewrite(variableDeclarationStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
		vdsRewrite.replace(fragment, newFragment, null);

		// 將InputStream fos = null，移到try外面
		ASTNode placeHolder = astRewrite.createMoveTarget(variableDeclarationStatement);
		ListRewrite moveRewrite = astRewrite.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
		moveRewrite.insertFirst(placeHolder, null);
	}
	
	/**
	 * 取得TryStatement附屬的finally block。若不存在，新增一個出來。
	 * @param tryStatement 你想從哪個TryStatement找finally block
	 * @param compilationUnit 你要告訴我是哪個compilation unit，我才能幫你修改
	 */
	public Block getFinallyBlock(TryStatement tryStatement, CompilationUnit compilationUnit) {
		if(tryStatement.getFinally() != null) {
			return tryStatement.getFinally();
		}
		Block finallyBody = compilationUnit.getAST().newBlock();
		astRewrite.set(tryStatement, TryStatement.FINALLY_PROPERTY, finallyBody, null);
		return finallyBody;
	}
	
	/**
	 * 將TryStatement裡面某個Node移動到Finally
	 * @param tryStatement 某個Node所屬的TryStatement
	 * @param node 某個Node
	 * @param finallyBlock 你想移動的那個Finally Block
	 */
	public void moveNodeToFinally(TryStatement tryStatement, ASTNode node, Block finallyBlock) {
		ASTNode placeHolder = astRewrite.createMoveTarget(node);
		ListRewrite moveRewrite = astRewrite.getListRewrite(finallyBlock, Block.STATEMENTS_PROPERTY);
		moveRewrite.insertLast(placeHolder, null);
	}
	
	/**
	 * 重新整理強健度等級註記的順序
	 */
	public void rearrangeRobustnessAnnotationOrder() {
		
	}
}
