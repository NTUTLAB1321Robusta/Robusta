package ntut.csie.robusta.codegen;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

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
import org.eclipse.jdt.core.dom.Statement;
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
	/** Java AST root node whick will be Quick Fix */
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

	public void generateRobustnessLevelAnnotation(
			MethodDeclaration methodDeclaration, int level,
			Class<?> exceptionType) {
		AST rootAST = compilationUnit.getAST();

		// Detect if the Robustness Level Annotation exists or not.
		if (QuickFixUtils.getExistingRLAnnotation(methodDeclaration) != null) {
			appendRLAnnotation(level, exceptionType, rootAST, methodDeclaration);
		} else {
			createRLAnnotation(level, exceptionType, rootAST, methodDeclaration);
		}

		// Import robustness level package if it's not imported.
		boolean isRobustnessClassImported = QuickFixUtils.isClassImported(
				Robustness.class, compilationUnit);
		boolean isRLClassImported = QuickFixUtils.isClassImported(RTag.class,
				compilationUnit);

		if (!isRobustnessClassImported) {
			ImportDeclaration importDeclaration = rootAST
					.newImportDeclaration();
			importDeclaration.setName(rootAST.newName(Robustness.class
					.getName()));

			// The rewrite list from the AST of compilation unit that you want
			// to modify.
			ListRewrite addImportDeclarationList = astRewrite.getListRewrite(
					compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
			addImportDeclarationList.insertLast(importDeclaration, null);
		}

		if (!isRLClassImported) {
			ImportDeclaration importDeclaration = rootAST
					.newImportDeclaration();
			importDeclaration.setName(rootAST.newName(RTag.class.getName()));

			// The rewrite list from the AST of compilation unit that you want
			// to modify.
			ListRewrite addImportDeclarationList = astRewrite.getListRewrite(
					compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
			addImportDeclarationList.insertLast(importDeclaration, null);
		}
	}

	/**
	 * When a method is without any Robustness Level Annotation, you should use
	 * this method to create RL Annotation.
	 * 
	 * @param level
	 * @param exceptionType
	 * @param rootAST
	 * @param methodDeclaration
	 */
	private void createRLAnnotation(int level, Class<?> exceptionType,
			AST rootAST, MethodDeclaration methodDeclaration) {

		/*
		 * Add Robustness level annotation
		 */
		// Robustness level annotation belongs NormalAnnotation class.
		NormalAnnotation normalAnnotation = rootAST.newNormalAnnotation();
		normalAnnotation.setTypeName(rootAST.newSimpleName(Robustness.class
				.getSimpleName()));
		// RL "level" and "exception type"
		MemberValuePair value = rootAST.newMemberValuePair();
		value.setName(rootAST.newSimpleName(Robustness.VALUE));

		ListRewrite normalAnnotationRewrite = astRewrite.getListRewrite(
				normalAnnotation, NormalAnnotation.VALUES_PROPERTY);
		normalAnnotationRewrite.insertLast(value, null);

		ArrayInitializer rlArrayInitializer = rootAST.newArrayInitializer();
		value.setValue(rlArrayInitializer);

		ListRewrite rlArrayInitializerRewrite = astRewrite.getListRewrite(
				rlArrayInitializer, ArrayInitializer.EXPRESSIONS_PROPERTY);
		rlArrayInitializerRewrite.insertLast(
				QuickFixUtils.makeRLAnnotation(rootAST, level,
						exceptionType.getName()), null);

		// // the meaning of this code is removing original RLAnnotation
		// List<IExtendedModifier> lstModifiers = methodDeclaration.modifiers();
		//
		// for(IExtendedModifier ieModifier : lstModifiers){
		// //remove old Robustness Annotation
		// if(ieModifier.isAnnotation() &&
		// ieModifier.toString().indexOf("Robustness") != -1){
		// ListRewrite mdModifiers =
		// astRewrite.getListRewrite(methodDeclaration,
		// MethodDeclaration.MODIFIERS2_PROPERTY);
		// mdModifiers.remove((ASTNode)ieModifier, null);
		// break;
		// }
		// }

		/*
		 * Add Annotation into MethodDeclaration node. Attention: The ASTNode
		 * and the ChildListPropertyDescriptor are special.
		 */
		ListRewrite addAnnotationList = astRewrite.getListRewrite(
				methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		addAnnotationList.insertFirst(normalAnnotation, null);
	}

	/**
	 * add an exception's declaration on method's signature
	 * 
	 * @param astRewrite
	 * @param methodDeclaration
	 */
	public void generateThrowExceptionOnMethodSignature(
			MethodDeclaration methodDeclaration, Class<?> exceptionType) {
		ListRewrite addingThrownException = astRewrite
				.getListRewrite(methodDeclaration,
						MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		ASTNode simpleName = QuickFixUtils
				.generateThrowExceptionForDeclaration(
						methodDeclaration.getAST(), exceptionType);
		addingThrownException.insertLast(simpleName, null);
	}

	/**
	 * remove specified code statement in specified catch clause
	 * 
	 * @param catchClause
	 * @param removingStatement
	 */
	public void removeNodeInCatchClause(CatchClause catchClause,
			String... removingStatements) {
		// The rewrite list from the AST of catch clause that you want to
		// modify.
		ListRewrite modifyingCatchClause = astRewrite.getListRewrite(
				catchClause.getBody(), Block.STATEMENTS_PROPERTY);
		for (String statement : removingStatements) {
			ExpressionStatementStringFinderVisitor expressionStatementFinder = new ExpressionStatementStringFinderVisitor(
					statement);
			catchClause.accept(expressionStatementFinder);
			ASTNode removeNode = expressionStatementFinder
					.getFoundExpressionStatement();
			if (removeNode != null) {
				modifyingCatchClause.remove(removeNode, null);
			}
		}
	}

	public void removeExpressionStatement(int removingStartPosition,
			MethodDeclaration methodDeclaration) {
		// find ExpressionStatement which will be removed with StartPosition
		StatementFinderVisitor statementFinderVisitor = new StatementFinderVisitor(
				removingStartPosition);
		methodDeclaration.accept(statementFinderVisitor);
		ASTNode removingNode = statementFinderVisitor
				.getFoundExpressionStatement();

		// find the block which contains ExpressionStatement which will be
		// removed
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(removingNode,
				ASTNode.BLOCK);

		ListRewrite modifyingBlock = astRewrite.getListRewrite(parentNode,
				Block.STATEMENTS_PROPERTY);
		modifyingBlock.remove(parentNode, null);
	}

	/**
	 * add throw new xxxException(e) statement
	 * 
	 * @param cc
	 * @param exceptionType
	 */
	public void addThrowRefinedExceptionInCatchClause(CatchClause cc,
			Class<?> exceptionType) {
		ASTNode createInstanceNode = QuickFixUtils
				.generateThrowNewExceptionNode(cc.getException()
						.resolveBinding().getName(), cc.getAST(), exceptionType);
		ListRewrite modifyingCatchClause = astRewrite.getListRewrite(
				cc.getBody(), Block.STATEMENTS_PROPERTY);
		modifyingCatchClause.insertLast(createInstanceNode, null);
	}

	public void addThrowExceptionInCatchClause(CatchClause cc) {
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
				.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		ListRewrite modifyingCatchClauseList = astRewrite.getListRewrite(
				cc.getBody(), Block.STATEMENTS_PROPERTY);
		ASTNode throwCheckedException = astRewrite.createStringPlaceholder(
				"throw " + svd.resolveBinding().getName() + ";",
				ASTNode.EMPTY_STATEMENT);
		modifyingCatchClauseList.insertLast(throwCheckedException, null);
	}

	/**
	 * When there is existing Robustness Level Annotation, and you just want to
	 * append new annotation, you should use this.
	 * 
	 * @param level
	 * @param exceptionType
	 * @param rootAST
	 * @param methodDeclaration
	 */
	private void appendRLAnnotation(int level, Class<?> exceptionType,
			AST rootAST, MethodDeclaration methodDeclaration) {
		NormalAnnotation na = QuickFixUtils
				.getExistingRLAnnotation(methodDeclaration);
		MemberValuePair mvp = (MemberValuePair) na.values().get(
				na.values().size() - 1);
		ListRewrite normalA = astRewrite.getListRewrite(mvp.getValue(),
				ArrayInitializer.EXPRESSIONS_PROPERTY);
		normalA.insertLast(
				QuickFixUtils.makeRLAnnotation(rootAST, level,
						exceptionType.getName()), null);
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public ICompilationUnit getICompilationUnit() {
		return (ICompilationUnit) actOpenable;
	}

	/**
	 * update modification of quick fix in editor
	 * 
	 * @param rewrite
	 */
	public void applyChange() {
		try {
			// consult
			// org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator
			// run
			// org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			IEditorPart part = EditorUtility.isOpenInEditor(cu);
			IEditorInput input = part.getEditorInput();
			IDocument doc = JavaPlugin.getDefault()
					.getCompilationUnitDocumentProvider().getDocument(input);
			performChange(JavaPlugin.getActivePage().getActiveEditor(), doc,
					astRewrite);
		} catch (CoreException e) {
			logger.error("[Core Exception] EXCEPTION ", e);
		}
	}

	/**
	 * update modification of refactoring in editor
	 * 
	 * @param textFileChange
	 * @throws CoreException
	 */
	public TextFileChange applyRefactoringChange() throws JavaModelException {
		ICompilationUnit cu = (ICompilationUnit) actOpenable;
		Document document = new Document(cu.getBuffer().getContents());
		TextEdit edits = compilationUnit.rewrite(document, cu.getJavaProject()
				.getOptions(true));
		TextFileChange textFileChange = new TextFileChange(cu.getElementName(),
				(IFile) cu.getResource());
		textFileChange.setEdit(edits);
		return textFileChange;
	}

	/**
	 * invoke Quick Fix
	 * 
	 * @param activeEditor
	 * @param document
	 * @param rewrite
	 * @throws CoreException
	 */
	private void performChange(IEditorPart activeEditor, IDocument document,
			ASTRewrite rewrite) throws CoreException {
		Change change = null;
		IRewriteTarget rewriteTarget = null;
		try {
			change = getChange(compilationUnit, rewrite);
			if (change != null) {
				if (document != null) {
					LinkedModeModel.closeAllModels(document);
				}
				if (activeEditor != null) {
					rewriteTarget = (IRewriteTarget) activeEditor
							.getAdapter(IRewriteTarget.class);
					if (rewriteTarget != null) {
						rewriteTarget.beginCompoundChange();
					}
				}

				change.initializeValidationData(new NullProgressMonitor());
				RefactoringStatus valid = change
						.isValid(new NullProgressMonitor());
				if (valid.hasFatalError()) {
					IStatus status = new Status(
							IStatus.ERROR,
							JavaPlugin.getPluginId(),
							IStatus.ERROR,
							valid.getMessageMatchingSeverity(RefactoringStatus.FATAL),
							null);
					throw new CoreException(status);
				} else {
					IUndoManager manager = RefactoringCore.getUndoManager();
					manager.aboutToPerformChange(change);
					Change undoChange = change
							.perform(new NullProgressMonitor());
					manager.changePerformed(change, true);
					if (undoChange != null) {
						undoChange
								.initializeValidationData(new NullProgressMonitor());
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

	private boolean isCatchingAllException(TryStatement tryStatement) {
		List<CatchClause> catchClauseList = tryStatement.catchClauses();
		for (CatchClause catchClause : catchClauseList) {
			if (catchClause.getException().getType().toString()
					.equals("Exception")
					|| catchClause.getException().getType().toString()
							.equals("Throwable")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * get statements after Quick Fix
	 */
	private Change getChange(CompilationUnit actRoot, ASTRewrite rewrite) {
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());

			TextEdit edits = null;
			edits = rewrite.rewriteAST(document, null);

			TextFileChange textFileChange = new TextFileChange(
					cu.getElementName(), (IFile) cu.getResource());
			textFileChange.setEdit(edits);

			return textFileChange;
		} catch (JavaModelException e) {
			logger.error(
					"[Apply Change Rethrow Unchecked Exception] EXCEPTION ", e);
		}
		return null;
	}

	/**
	 * generate a try statement to contain all statements in method
	 * 
	 * @param methodDeclaration
	 *            a method which will have a try statement to contain all
	 *            statement of method
	 */
	public void generateTryStatementForQuickFix(MethodDeclaration methodDeclaration) {
		List<ASTNode> statements = methodDeclaration.getBody().statements();
		Queue<TryStatement> tryStatements = new LinkedList<>();
		Queue<ASTNode> moveTargets = new LinkedList<>();
		classifyStatementsToDifferentQueue(statements, tryStatements, moveTargets);
		if(tryStatements.isEmpty()){
			TryStatement tryStatement = createTryCatchStatement();
			moveAllStatementInTryStatement(methodDeclaration, tryStatement);
			return;
		}
		Stack<ASTNode> placeHolders = new Stack<>();
		Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer = new Stack<>();
		while (!tryStatements.isEmpty()) {
			TryStatement tryStatement = tryStatements.poll();
			if (!isCatchingAllException(tryStatement)){
				appendCatchClause(tryStatement);
			}
			ListRewrite body = astRewrite.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
			ListRewrite neededToBeRefactoredMethodBody = astRewrite.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
			Stack<ASTNode> expressionStatements = new Stack<>();
			if (!tryStatements.isEmpty()) {
				moveStatementsAboveTryStatementInTryBlock(moveTargets, placeHolders, variableDeclarationEndWithLiteralOrNullInitializer, tryStatement, body, expressionStatements);
			} else {
				moveStatementsAboveTryStatementInTryBlock(moveTargets, placeHolders, variableDeclarationEndWithLiteralOrNullInitializer, tryStatement, body, expressionStatements);
				moveStatementsBelowTryStatementInTryBlock(moveTargets, placeHolders, variableDeclarationEndWithLiteralOrNullInitializer, tryStatement, body, expressionStatements);
				insertStatementsToMethodDeclaration(placeHolders, variableDeclarationEndWithLiteralOrNullInitializer, neededToBeRefactoredMethodBody);
			}
			moveReturnAndThrowStatementToTheLastOfTryStatement(tryStatement);
		}
	}

	private void classifyStatementsToDifferentQueue(List<ASTNode> statements,
			Queue<TryStatement> tryStatements, Queue<ASTNode> moveTargets) {
		for (ASTNode statement : statements) {
			if (statement.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT && statement.getNodeType() != ASTNode.TRY_STATEMENT) {
				ASTNode target = astRewrite.createMoveTarget(statement);
				target.setProperty("startPosition",statement.getStartPosition());
				moveTargets.offer(target);
			} else if (statement.getNodeType() == ASTNode.TRY_STATEMENT) {
				tryStatements.offer((TryStatement) statement);
			} else {
				moveTargets.offer(statement);
			}
		}
	}

	private void insertStatementsToMethodDeclaration(
			Stack<ASTNode> placeHolders,
			Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer,
			ListRewrite neededToBeRefactoredMethodBody) {
		while(!placeHolders.isEmpty()){
			neededToBeRefactoredMethodBody.insertFirst(placeHolders.pop(), null);
		}
		while(!variableDeclarationEndWithLiteralOrNullInitializer.isEmpty()){
			neededToBeRefactoredMethodBody.insertFirst(variableDeclarationEndWithLiteralOrNullInitializer.pop(), null);
		}
	}

	private void moveStatementsBelowTryStatementInTryBlock(
			Queue<ASTNode> moveTargets, Stack<ASTNode> placeHolders,
			Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer,
			TryStatement tryStatement, ListRewrite body,
			Stack<ASTNode> expressionStatements) {
		int targetStartPos = -1;
		targetStartPos = getTargetStartPosition(moveTargets);
		while (targetStartPos > tryStatement.getStartPosition() && !moveTargets.isEmpty()) {
			ASTNode statement = moveTargets.poll();
			if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				identifyVariableDeclaration(placeHolders, variableDeclarationEndWithLiteralOrNullInitializer, expressionStatements, statement);
			} else {
				expressionStatements.push(statement);
			}
			targetStartPos = getTargetStartPosition(moveTargets);
		}
		for(ASTNode expressionStatement : expressionStatements){
			body.insertLast(expressionStatement, null);
		}
		expressionStatements.clear();
	}

	private void moveStatementsAboveTryStatementInTryBlock(
			Queue<ASTNode> moveTargets, Stack<ASTNode> placeHolders,
			Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer,
			TryStatement tryStatement, ListRewrite body,
			Stack<ASTNode> expressionStatements) {
		int targetStartPos = -1;
		targetStartPos = getTargetStartPosition(moveTargets);
		while (targetStartPos < tryStatement.getStartPosition() && !moveTargets.isEmpty()) {
			ASTNode statement = moveTargets.poll();
			if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				identifyVariableDeclaration(placeHolders, variableDeclarationEndWithLiteralOrNullInitializer, expressionStatements, statement);
			} else {
				expressionStatements.push(statement);
			}
			targetStartPos = getTargetStartPosition(moveTargets);
		}
		while(!expressionStatements.isEmpty()){
			body.insertFirst(expressionStatements.pop(), null);
		}
	}

	private void identifyVariableDeclaration(Stack<ASTNode> placeHolders,
			Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer,
			Stack<ASTNode> expressionStatements, ASTNode statement) {
		VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
		AST rootAST = compilationUnit.getAST();
		List<?> fragments = variableDeclarationStatement.fragments();
		VariableDeclarationFragment declarationFragment = (VariableDeclarationFragment) (fragments.get(fragments.size() - 1));
		Expression initializer = declarationFragment.getInitializer();
		if (initializer != null && !initializer.getClass().getName().endsWith("Literal")) {
			for (Object node : fragments) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) node;
				VariableDeclarationFragment newFragment = rootAST.newVariableDeclarationFragment();
				newFragment.setName(rootAST.newSimpleName(fragment.getName().toString()));
				if (variableDeclarationStatement.getType().getNodeType() != ASTNode.PRIMITIVE_TYPE) {
					newFragment.setInitializer(rootAST.newNullLiteral());
				}
				ListRewrite variableDeclarationRewrite = astRewrite.getListRewrite(variableDeclarationStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
				variableDeclarationRewrite.replace(fragment, newFragment, null);
				ASTNode placeHolder = astRewrite.createMoveTarget(variableDeclarationStatement);
				placeHolders.push(placeHolder);

				Assignment assignment = rootAST.newAssignment();
				assignment.setOperator(Assignment.Operator.ASSIGN);
				// InputStream fos = null;
				assignment.setLeftHandSide(rootAST.newSimpleName(fragment.getName().toString()));
				// fos = new InputStream();
				ASTNode copyNode = ASTNode.copySubtree(initializer.getAST(), initializer);
				assignment.setRightHandSide((Expression) copyNode);
				ExpressionStatement exp = rootAST.newExpressionStatement(assignment);
				expressionStatements.push(exp);
			}
		}else{
			ASTNode target = astRewrite.createMoveTarget(variableDeclarationStatement);
			variableDeclarationEndWithLiteralOrNullInitializer.push(target);
		}
	}

	private int getTargetStartPosition(Queue<ASTNode> moveTargets) {
		ASTNode top = moveTargets.peek();
		if (top != null) {
			Object nextStarPos = top.getProperty("startPosition");
			if (nextStarPos != null) {
				return (int) nextStarPos;
			} else {
				return moveTargets.peek().getStartPosition();
			}
		}
		return -1;
	}

	private void moveReturnAndThrowStatementToTheLastOfTryStatement(TryStatement tryStatement) {
		List<Statement> statementInTryStatement = tryStatement.getBody().statements();
		ListRewrite body = astRewrite.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		for (ASTNode node : statementInTryStatement) {
			if (node.getNodeType() == ASTNode.THROW_STATEMENT || node.getNodeType() == ASTNode.RETURN_STATEMENT) {
				body.insertLast(body.createMoveTarget(node, node), null);
			}
		}
	}

	private void appendCatchClause(TryStatement tryStatement) {
		AST ast = compilationUnit.getAST();
		// generate a Try statement
		ListRewrite catchRewrite = astRewrite.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
		// generate Catch Clause
		@SuppressWarnings("unchecked")
		CatchClause cc = ast.newCatchClause();
		ListRewrite newCreateCatchRewrite = astRewrite.getListRewrite(
				cc.getBody(), Block.STATEMENTS_PROPERTY);
		// set the exception type will be caught. ex. catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		StringBuffer comment = new StringBuffer();
		comment.append("// TODO: handle exception");
		ASTNode placeHolder = astRewrite.createStringPlaceholder(
				comment.toString(), ASTNode.EMPTY_STATEMENT);
		newCreateCatchRewrite.insertLast(placeHolder, null);
		catchRewrite.insertLast(cc, null);
	}

	private void moveAllStatementInTryStatement(
			MethodDeclaration methodDeclaration,
			TryStatement tryStatementCreatedByQuickFix) {
		/* move all statements of method in try block */
		ListRewrite tryStatement = astRewrite.getListRewrite(tryStatementCreatedByQuickFix.getBody(), Block.STATEMENTS_PROPERTY);
		ListRewrite neededToBeRefactoredMethodBody = astRewrite.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
		List<ASTNode> statements = methodDeclaration.getBody().statements();
		for(ASTNode statement : statements){
			ASTNode target = astRewrite.createMoveTarget(statement);
			tryStatement.insertLast(target, null);
		}
		neededToBeRefactoredMethodBody.insertLast(tryStatementCreatedByQuickFix, null);
	}

	private TryStatement createTryCatchStatement() {
		AST ast = compilationUnit.getAST();
		// generate a Try statement
		TryStatement bigOuterTryStatement = ast.newTryStatement();
		// generate Catch Clause
		@SuppressWarnings("unchecked")
		List<CatchClause> catchStatement = bigOuterTryStatement.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = astRewrite.getListRewrite(cc.getBody(),
				Block.STATEMENTS_PROPERTY);
		// set the exception type will be caught. ex. catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		StringBuffer comment = new StringBuffer();
		comment.append("// TODO: handle exception");
		ASTNode placeHolder = astRewrite.createStringPlaceholder(
				comment.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);
		return bigOuterTryStatement;
	}

	/**
	 * 
	 * move the variable declaration out, which is contained by try statement in
	 * method.
	 * 
	 * @param tryStatement
	 * @param variableDeclarationStatement
	 * @param rootAST
	 *            compilation unit
	 * @param methodDeclaration
	 *            method declaration which contains specified try statement
	 */
	public void prepareToMoveVariableDeclarationStatementOutOfTry(
			VariableDeclarationStatement variableDeclarationStatement,
			AST rootAST, Stack<ASTNode> placeHolderStack,
			Stack<Statement> ExpressionStatementStack) {
		Stack<Statement> ExpressionStatement = ExpressionStatementStack;
		if (variableDeclarationStatement == null) {
			ExpressionStatement.push(null);
		} else {
			/*
			 * change InputStream fos = new ImputStream(); to InputStream fos =
			 * null; and fos = new InputStream();
			 */
			Stack<ASTNode> placeHolders = placeHolderStack;
			List<?> fragments = variableDeclarationStatement.fragments();
			VariableDeclarationFragment declarationFragment = (VariableDeclarationFragment) (fragments
					.get(fragments.size() - 1));
			Expression initializer = declarationFragment.getInitializer();
			for (Object node : fragments) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) node;
				VariableDeclarationFragment newFragment = rootAST
						.newVariableDeclarationFragment();
				newFragment.setName(rootAST.newSimpleName(fragment.getName()
						.toString()));
				// check variable is primitive type or not. ex. int, double and
				// String ect...
				if (variableDeclarationStatement.getType().getNodeType() != ASTNode.PRIMITIVE_TYPE) {
					newFragment.setInitializer(rootAST.newNullLiteral());
				}
				ListRewrite variableDeclarationRewrite = astRewrite
						.getListRewrite(variableDeclarationStatement,
								VariableDeclarationStatement.FRAGMENTS_PROPERTY);
				variableDeclarationRewrite.replace(fragment, newFragment, null);
				ASTNode placeHolder = astRewrite
						.createMoveTarget(variableDeclarationStatement);
				placeHolders.push(placeHolder);

				Assignment assignment = rootAST.newAssignment();
				assignment.setOperator(Assignment.Operator.ASSIGN);
				// InputStream fos = null;
				assignment.setLeftHandSide(rootAST.newSimpleName(fragment
						.getName().toString()));
				// fos = new InputStream();
				ASTNode copyNode = ASTNode.copySubtree(initializer.getAST(),
						initializer);
				assignment.setRightHandSide((Expression) copyNode);
				ExpressionStatement expressionStatement = rootAST
						.newExpressionStatement(assignment);
				ExpressionStatement.push(expressionStatement);
			}
		}
	}

	/**
	 * get finally block of specified try statement. if finally block does not
	 * exist then create a new finally block.
	 * 
	 * @param tryStatement
	 *            specified try statement
	 * @param compilationUnit
	 *            which compilationUnit need to be modified
	 */
	public Block getFinallyBlock(TryStatement tryStatement,
			CompilationUnit compilationUnit) {
		if (tryStatement.getFinally() != null) {
			return tryStatement.getFinally();
		}
		Block finallyBody = compilationUnit.getAST().newBlock();
		astRewrite.set(tryStatement, TryStatement.FINALLY_PROPERTY,
				finallyBody, null);
		return finallyBody;
	}

	/**
	 * move specified node in try block to finally block
	 * 
	 * @param tryStatement
	 *            specified Node belongs to TryStatement
	 * @param node
	 *            specified node
	 * @param finallyBlock
	 *            destination of Finally Block
	 */
	public void moveNodeToFinally(TryStatement tryStatement, ASTNode node,
			Block finallyBlock) {
		ASTNode placeHolder = astRewrite.createMoveTarget(node);
		ListRewrite moveRewrite = astRewrite.getListRewrite(finallyBlock,
				Block.STATEMENTS_PROPERTY);
		moveRewrite.insertLast(placeHolder, null);
	}

}
