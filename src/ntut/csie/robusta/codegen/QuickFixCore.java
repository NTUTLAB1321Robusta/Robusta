package ntut.csie.robusta.codegen;

import java.util.List;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.util.NodeUtils;

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
	
	protected IOpenable actOpenable = null;
	/**Java AST root node whick will be Quick Fix */
	private CompilationUnit compilationUnit = null;
	
	private ASTRewrite astRewrite = null;
	
	public void setJavaFileModifiable(IResource resource) {
		IJavaElement javaElement = JavaCore.create(resource);
		
		if (javaElement instanceof IOpenable) {
			actOpenable = (IOpenable) javaElement;
		}
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource((ICompilationUnit) javaElement);
		parser.setResolveBindings(true);
		compilationUnit = (CompilationUnit) parser.createAST(null);
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
	 * When a method is without any Robustness Level Annotation, you should use this method to create RL Annotation.
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
		
//		// the meaning of this code is removing original RLAnnotation
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
	 * add an exception's declaration on method's signature
	 * @param astRewrite
	 * @param methodDeclaration
	 */
	public void generateThrowExceptionOnMethodSignature(MethodDeclaration methodDeclaration, Class<?> exceptionType) {
		ListRewrite addingThrownException = astRewrite.getListRewrite(methodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		ASTNode simpleName = QuickFixUtils.generateThrowExceptionForDeclaration(methodDeclaration.getAST(), exceptionType);
		addingThrownException.insertLast(simpleName, null);
	}
	
	/**
	 * remove specified code statement in specified catch clause
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
		//find ExpressionStatement which will be removed with StartPosition
		StatementFinderVisitor statementFinderVisitor = new StatementFinderVisitor(removingStartPosition);
		methodDeclaration.accept(statementFinderVisitor);
		ASTNode removingNode = statementFinderVisitor.getFoundExpressionStatement();
		
		// find the block which contains ExpressionStatement which will be removed
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(removingNode, ASTNode.BLOCK);
		
		ListRewrite modifyingBlock = astRewrite.getListRewrite(parentNode, Block.STATEMENTS_PROPERTY);
		modifyingBlock.remove(parentNode, null);
	}
	
	/**
	 * add throw new xxxException(e) statement
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
	 * update modification of quick fix in editor 
	 * @param rewrite
	 */
	public void applyChange() {
		try {
			// consult org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator run
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
	 * update modification of refactoring in editor 
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
	 * invoke Quick Fix
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
	
	/**get statements after Quick Fix
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
	 * generate a try statement to contain all statements in method
	 * @param methodDeclaration 
	 * 						a method which will have a try statement to contain all statement of method
	 */
	public void generateBigOuterTryStatement(MethodDeclaration methodDeclaration) {
		ListRewrite addingBigOuterTry = astRewrite.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);

		AST ast = compilationUnit.getAST();
		// generate a Try statement
		TryStatement bigOuterTryStatement = ast.newTryStatement();
		// generate Catch Clause
		@SuppressWarnings("unchecked")
		List<CatchClause> catchStatement = bigOuterTryStatement.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = astRewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
		// set the exception type will be caught. ex. catch(Exception ex) 
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		StringBuffer comment = new StringBuffer();
		comment.append("// TODO: handle exception");
		ASTNode placeHolder = astRewrite.createStringPlaceholder(comment.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);
		
		/* move all statements of method in try block */
		ListRewrite tryStatement = astRewrite.getListRewrite(bigOuterTryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		int listSize = addingBigOuterTry.getRewrittenList().size();
		tryStatement.insertLast(addingBigOuterTry.createMoveTarget((ASTNode) addingBigOuterTry.getRewrittenList().get(0), 
								(ASTNode) addingBigOuterTry.getRewrittenList().get(listSize - 1)), null);

		addingBigOuterTry.insertLast(bigOuterTryStatement, null);
	}
	
	/**
	 * 
	 * move the variable declaration out, which is contained by try statement in method.
	 * @param tryStatement 
	 * @param variableDeclarationStatement 
	 * @param rootAST 
	 * 				compilation unit 
	 * @param methodDeclaration
	 * 				 method declaration which contains specified try statement
	 */
	public void moveOutVariableDeclarationStatementFromTry(TryStatement tryStatement, 
			VariableDeclarationStatement variableDeclarationStatement, AST rootAST, MethodDeclaration methodDeclaration) {
		List<?> fragments = variableDeclarationStatement.fragments();
		if(fragments.size() != 1) {
			throw new RuntimeException("Two variables declared in the same variable declaration statement is not yet supported.");
		}
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
		/* 
		 * change  InputStream fos = new ImputStream();
		 * to fos = new InputStream();
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
		// use "fos = new ImputStream();" to replace original statement 
		if(assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL) {
			ExpressionStatement expressionStatement = rootAST.newExpressionStatement(assignment);
			tsRewrite.replace(variableDeclarationStatement, expressionStatement, null);
		} else {
			tsRewrite.remove(variableDeclarationStatement, null);
		}
		
		// change "InputStream fos = new ImputStream();" to InputStream fos = null;
		VariableDeclarationFragment newFragment = rootAST.newVariableDeclarationFragment();
		newFragment.setName(rootAST.newSimpleName(fragment.getName().toString()));
		newFragment.setInitializer(rootAST.newNullLiteral());
		ListRewrite vdsRewrite = astRewrite.getListRewrite(variableDeclarationStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
		vdsRewrite.replace(fragment, newFragment, null);

		// move "InputStream fos = null" out of try statement
		ASTNode placeHolder = astRewrite.createMoveTarget(variableDeclarationStatement);
		ListRewrite moveRewrite = astRewrite.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
		moveRewrite.insertFirst(placeHolder, null);
	}
	
	/**
	 * get finally block of specified try statement.
	 * if finally block does not exist then create a new finally block. 
	 * @param tryStatement 
	 * 				specified try statement
	 * @param compilationUnit 
	 * 				which compilationUnit need to be modified
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
	 * move specified node in try block to finally block
	 * @param tryStatement 
	 * 			specified Node belongs to TryStatement
	 * @param node
	 * 			specified node
	 * @param finallyBlock 
	 * 			destination of Finally Block 
	 */
	public void moveNodeToFinally(TryStatement tryStatement, ASTNode node, Block finallyBlock) {
		ASTNode placeHolder = astRewrite.createMoveTarget(node);
		ListRewrite moveRewrite = astRewrite.getListRewrite(finallyBlock, Block.STATEMENTS_PROPERTY);
		moveRewrite.insertLast(placeHolder, null);
	}
	
	
}
