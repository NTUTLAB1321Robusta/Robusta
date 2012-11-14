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
	
	/** �n�Q�]�w��Navigatable��Java�ɡA�᭱�n�qresource�h�]�w�L */
	protected IOpenable actOpenable = null;
	/** �n�QQuick Fix��Java AST root node */
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
		//AST 2.0�����覡
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
		
//		// �o�q�{���X���N�q�A�b�󲾰������ª�RLAnnotation
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
	 * �bMethod Declaration �᭱�[�W�ߥX�ҥ~���ŧi
	 * @param astRewrite
	 * @param methodDeclaration
	 */
	public void generateThrowExceptionOnMethodDeclaration(MethodDeclaration methodDeclaration, Class<?> exceptionType) {
		ListRewrite addingThrowsException = astRewrite.getListRewrite(methodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		ASTNode simpleName = QuickFixUtils.generateThrowExceptionForDeclaration(methodDeclaration.getAST(), exceptionType);
		addingThrowsException.insertLast(simpleName, null);
	}
	
	/**
	 * �ھڵ��w���{���X�A����CatchClause���S�w���`�I
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
		// ��StartPosition��X�n������ExpressionStatement
		StatementFinderVisitor statementFinderVisitor = new StatementFinderVisitor(removingStartPosition);
		methodDeclaration.accept(statementFinderVisitor);
		ASTNode removingNode = statementFinderVisitor.getFoundExpressionStatement();
		
		// ��X�n������ExpressionStatement���ݪ�Block
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(removingNode, ASTNode.BLOCK);
		
		ListRewrite modifyingBlock = astRewrite.getListRewrite(parentNode, Block.STATEMENTS_PROPERTY);
		modifyingBlock.remove(parentNode, null);
	}
	
	/**
	 * �W�[throw new XxxException(e)���{���X
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
	 * �NQuick Fix �n�ܧ󪺤��e�g�^Edit��
	 * @param rewrite
	 */
	public void applyChange() {
		try {
			// �Ѧ�org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator run
			// org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal apply�PperformChange
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
	 * �M��Refactoring�n�ܧ󪺤��e
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
	 * ���� Quick Fix �ܧ�
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
	 * ���oQuick Fix����ܪ��{���X
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
	 * ���ͤ@�Ӥj��TryStatement�]��MethodDeclaration�̭��Ҧ��{���X
	 * @param methodDeclaration �n���ͤjTryStatement��MethodDeclaration
	 */
	public void generateBigOuterTryStatement(MethodDeclaration methodDeclaration) {
		ListRewrite addingBigOuterTry = astRewrite.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);

		AST ast = compilationUnit.getAST();
		/* ���ͤ@��TryStatement */
		// ����Try
		TryStatement bigOuterTryStatement = ast.newTryStatement();
		// ����Catch Clause
		@SuppressWarnings("unchecked")
		List<CatchClause> catchStatement = bigOuterTryStatement.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = astRewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
		// �إ�catch��type�� catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		// �bCatch���[�JTODO������
		StringBuffer comment = new StringBuffer();
		comment.append("// TODO: handle exception");
		ASTNode placeHolder = astRewrite.createStringPlaceholder(comment.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);
		
		/* �N�쥻�btry block���~���{�������ʶi�� */
		ListRewrite tryStatement = astRewrite.getListRewrite(bigOuterTryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		int listSize = addingBigOuterTry.getRewrittenList().size();
		tryStatement.insertLast(addingBigOuterTry.createMoveTarget((ASTNode) addingBigOuterTry.getRewrittenList().get(0), 
								(ASTNode) addingBigOuterTry.getRewrittenList().get(listSize - 1)), null);

		addingBigOuterTry.insertLast(bigOuterTryStatement, null);
	}
	
	/**
	 * �NTry�̭��ŧi���ܼƲ���Try�~��
	 * @param tryStatement �̭����ŧi�ܼƪ�Try
	 * @param variableDeclarationStatement �ŧi�ܼƪ�����{���X
	 * @param rootAST ���compilation unit �� AST
	 * @param methodDeclaration �o��Try���ݪ�method declaration
	 */
	public void moveOutVariableDeclarationStatementFromTry(TryStatement tryStatement, 
			VariableDeclarationStatement variableDeclarationStatement, AST rootAST, MethodDeclaration methodDeclaration) {
		List<?> fragments = variableDeclarationStatement.fragments();
		if(fragments.size() != 1) {
			throw new RuntimeException("Two variables declared in the same variable declaration statement is not yet supported.");
		}
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
		/* 
		 * �N   InputStream fos = new ImputStream();
		 * �אּ fos = new InputStream();
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
		// �Nfos = new ImputStream(); ������쥻���{����
		if(assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL) {
			ExpressionStatement expressionStatement = rootAST.newExpressionStatement(assignment);
			tsRewrite.replace(variableDeclarationStatement, expressionStatement, null);
		} else {
			tsRewrite.remove(variableDeclarationStatement, null);
		}
		
		// �NInputStream fos = new ImputStream(); �令 InputStream fos = null;
		VariableDeclarationFragment newFragment = rootAST.newVariableDeclarationFragment();
		newFragment.setName(rootAST.newSimpleName(fragment.getName().toString()));
		newFragment.setInitializer(rootAST.newNullLiteral());
		ListRewrite vdsRewrite = astRewrite.getListRewrite(variableDeclarationStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
		vdsRewrite.replace(fragment, newFragment, null);

		// �NInputStream fos = null�A����try�~��
		ASTNode placeHolder = astRewrite.createMoveTarget(variableDeclarationStatement);
		ListRewrite moveRewrite = astRewrite.getListRewrite(methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
		moveRewrite.insertFirst(placeHolder, null);
	}
	
	/**
	 * ���oTryStatement���ݪ�finally block�C�Y���s�b�A�s�W�@�ӥX�ӡC
	 * @param tryStatement �A�Q�q����TryStatement��finally block
	 * @param compilationUnit �A�n�i�D�ڬO����compilation unit�A�ڤ~�����A�ק�
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
	 * �NTryStatement�̭��Y��Node���ʨ�Finally
	 * @param tryStatement �Y��Node���ݪ�TryStatement
	 * @param node �Y��Node
	 * @param finallyBlock �A�Q���ʪ�����Finally Block
	 */
	public void moveNodeToFinally(TryStatement tryStatement, ASTNode node, Block finallyBlock) {
		ASTNode placeHolder = astRewrite.createMoveTarget(node);
		ListRewrite moveRewrite = astRewrite.getListRewrite(finallyBlock, Block.STATEMENTS_PROPERTY);
		moveRewrite.insertLast(placeHolder, null);
	}
	
	/**
	 * ���s��z�j���׵��ŵ��O������
	 */
	public void rearrangeRobustnessAnnotationOrder() {
		
	}
}
