package ntut.csie.rleht.rlAdvice;

import java.util.List;

import ntut.csie.analyzer.ASTCatchCollect;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;


/**
 * this is a tool used to quick fix
 * @author Charles
 * @version 0.0.1
 */
public class QuickFixUtil {
	public static final String runtimeException = "RuntimeException";
	
	public static final String[] dummyHandlerStrings = {"System.out.print", "printStackTrace"};

	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#deleteStatement
	 * remove specified expression statement, such as "System.out.print" and "printStackTrace"
	 * @param statements
	 * @param delStrings
	 */
	public void deleteStatement(List<Statement> statements, String[] delStrings) {
		if(statements.size() != 0) {
			for(int i = 0; i<statements.size(); i++) {
				//TODO find another way to replace "instanceof"  
				if(statements.get(i) instanceof ExpressionStatement) {
					ExpressionStatement expStatement = (ExpressionStatement) statements.get(i);
					for(int j=0; j<delStrings.length; j++) {
						if(expStatement.getExpression().toString().contains(delStrings[j])) {
							statements.remove(i);
							deleteStatement(statements, delStrings);
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#addAnnotationRoot
	 * add robustness level annotation in method signature(also import required class)
	 * @param actRoot
	 * @param currentMethodDeclarationNode 
	 * 				must be a ASTNode with MethodDeclaration type 
	 * @param rlValue 
	 * 				value of robustness level
	 * @param exceptionClass 
	 * 				
	 */
	@SuppressWarnings("unchecked")
	public void addAnnotationRoot(CompilationUnit actRoot,
			ASTNode currentMethodDeclarationNode, int rlValue,
			String exceptionClass) {
		// to establish the annotation to be like "@Robustness(value={@RTag(level=1, exception=java.lang.RuntimeException.class)})"
		
		AST ast = currentMethodDeclarationNode.getAST();
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));
		
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(actRoot, currentMethodDeclarationNode.getStartPosition(), 0);
		List<RLMessage> currentMethodRLList = exVisitor.getMethodRLAnnotationList();
		
		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		MethodDeclaration method = (MethodDeclaration) currentMethodDeclarationNode;		
		if (currentMethodRLList.size() == 0) {
			rlary.expressions().add(getRLAnnotation(ast, rlValue, exceptionClass));
		} else {
			for (RLMessage rlmsg : currentMethodRLList) {
				//add original annotation and check duplicate annotation. if the annotation is duplicate then ignore it.
				
				if((!rlmsg.getRLData().getExceptionType().toString().contains(exceptionClass)) && (rlmsg.getRLData().getLevel() == rlValue))				
					rlary.expressions().add(getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
			}
			rlary.expressions().add(getRLAnnotation(ast, rlValue, exceptionClass));
			
			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				// remove original annotation 
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		// import robustness level library
		addImportDeclaration(actRoot);
	}
	
	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#addImportDeclaration
	 * import Robustness & Tag class for using annotation
	 * @param actRoot
	 */
	@SuppressWarnings("unchecked")
	private void addImportDeclaration(CompilationUnit actRoot) {
		// check whether has imported robustness and robustness level class
		List<ImportDeclaration> importList = actRoot.imports();

		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;

		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName()))
				isImportRobustnessClass = true;
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName()))
				isImportRLClass = true;
		}

		AST rootAst = actRoot.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			actRoot.imports().add(imp);
		}
	}
	
	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#getRLAnnotation
	 * generate information of robustness level annotation 
	 * @param ast: AST Object
	 * @param levelVal
	 * 			robustness level
	 * @param excption
	 * 			exception type
	 * @return NormalAnnotation AST Node
	 */
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//generate annotation to be like "@Robustness(value={@RTag(level=1, exception=java.lang.RuntimeException.class)})"
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RTag"));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}
	

	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#addThrowStatement
	 * add throw xxxException at catch clause.
	 * @param cc
	 * @param currentMethodDeclarationNode
	 * @param exceptionClass
	 */
	public void addThrowStatement(ASTNode cc, AST currentMethodDeclarationNode, String exceptionClass) {
		// get exception variable from catch clause expression
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		CatchClause clause = (CatchClause) cc;
		ThrowStatement throwStatement = currentMethodDeclarationNode.newThrowStatement();
		
		//input variable which will be throw
		ClassInstanceCreation cic = currentMethodDeclarationNode.newClassInstanceCreation();
		// throw new RuntimeException()
		cic.setType(currentMethodDeclarationNode.newSimpleType(currentMethodDeclarationNode.newSimpleName(exceptionClass)));
		// access argument of throw new RuntimeException()
		cic.arguments().add(currentMethodDeclarationNode.newSimpleName(svd.resolveBinding().getName()));

		// update modification of editor
		throwStatement.setExpression(cic);
		clause.getBody().statements().add(throwStatement);
	}
	
	public void reThrowException(int rlValue, String exception, int msgIdx, CompilationUnit actRoot, ASTNode currentMethodNode) {
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		// add throw exception statement in Catch Caluse
		// collect all catch clause in method
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();

		for (int i = 0; i < catchList.size(); i++) {
				addAnnotationRoot(actRoot, currentMethodNode, rlValue, exception);
				
				// add throw exception statement in Catch Caluse
				addThrowStatement(catchList.get(i), ast);
				//check whether a throw exception statement on method signature 
				addThrownException(ast, exception, currentMethodNode);
		}
	}
	
	/**
	 * add "throw exception" in catch clause
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(CatchClause cc, AST ast) {
		// get exception variable of catch clause expression
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		ThrowStatement ts = ast.newThrowStatement();

		// get exception variable from catch clause expression
		SimpleName name = ast.newSimpleName(svd.resolveBinding().getName());		
		
		ts.setExpression(name);

		//update the modification to program code
		cc.getBody().statements().add(ts);
	}
	
	/**
	 * add "throws exception" on method signature
	 * @param ast
	 * @param exception
	 * @param currentMethodNode
	 */
	@SuppressWarnings("unchecked")
	private void addThrownException(AST ast, String exception, ASTNode currentMethodNode) {
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		boolean isExist = false;
		for(int i=0;i<md.thrownExceptions().size();i++) {
			if(md.thrownExceptions().get(i) instanceof SimpleName) {
				SimpleName sn = (SimpleName)md.thrownExceptions().get(i);
				if(sn.getIdentifier().equals(exception)) {
					isExist = true;
					break;
				}
			}
		}
		if (!isExist) {
			md.thrownExceptions().add(ast.newSimpleName(exception));
		}
	}
}
