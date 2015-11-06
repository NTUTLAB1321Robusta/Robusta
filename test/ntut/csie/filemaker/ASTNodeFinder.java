package ntut.csie.filemaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.common.RLBaseVisitor;
import ntut.csie.util.NodeUtils;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * get CompilationUnit from specified project
 * @author charles
 *
 */
public class ASTNodeFinder {
	public static CompilationUnit getCompilationUnit(Class<?> clazz, String projectName) {
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(clazz, projectName));

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin
				.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);

		CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		return compilationUnit;
	}

	public static ASTNode getNodeFromSpecifiedClass (Class<?> className, String projectName, int lineNumber) throws IOException, CoreException {
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(className, projectName));

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);

		CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		
		LineNumberOfASTNodeVisitor astNodeVisitor = new ASTNodeFinder().new LineNumberOfASTNodeVisitor(compilationUnit, lineNumber);
		
		compilationUnit.accept(astNodeVisitor);
		
		return astNodeVisitor.getCurrentNode();
	}
	
	/**
	 * use method name to get specified MethodDeclaration Node <br />
	 * Known Issue: {@link NameOfMethodDeclarationVisitor}
	 * @param clazz
	 * @param projectName
	 * @param methodName
	 * @return
	 */
	public static MethodDeclaration getMethodDeclarationNodeByName(
			Class<?> clazz, String projectName, String methodName) {
		
		CompilationUnit compilationUnit = getCompilationUnit(clazz, projectName);
		return getMethodDeclarationNodeByName(compilationUnit, methodName);
	}

	/**
	 * use method name to search the first matching MethodDeclaration in java file
	 * @param compilationUnit
	 * @param methodName
	 * @return
	 */
	public static MethodDeclaration getMethodDeclarationNodeByName(
			CompilationUnit compilationUnit, String methodName) {
		
		NameOfMethodDeclarationVisitor nameOfMethodDeclarationVisitor = 
			new ASTNodeFinder().new NameOfMethodDeclarationVisitor(methodName);
		
		compilationUnit.accept(nameOfMethodDeclarationVisitor);
		return nameOfMethodDeclarationVisitor.getFoundMethodDeclaration();
	}
	
	/**
	 * find out all try statement in a specified method with method name
	 * (nested try statement will be ignored)
	 * @param compilationUnit
	 * @param methodName
	 * @return
	 */
	public static List<TryStatement> getTryStatementNodeListByMethodDeclarationName(
			CompilationUnit compilationUnit, String methodName) {
		
		NameOfMethodDeclarationVisitor nameOfMethodDeclarationVisitor =
			 new ASTNodeFinder().new NameOfMethodDeclarationVisitor(methodName);
		
		compilationUnit.accept(nameOfMethodDeclarationVisitor);
		return nameOfMethodDeclarationVisitor.getCollectedTryStatement();
	}
	
	public static List<MethodInvocation> getMethodInvocationByMethodNameAndCode(
			Class<?> clazz, String projectName, String methodName, String code) {
		
		CompilationUnit compilationUnit = getCompilationUnit(clazz, projectName);
		return getMethodInvocationByMethodNameAndCode(compilationUnit, methodName, code);
	}
	
	public static List<MethodInvocation> getMethodInvocationByMethodNameAndCode(
			CompilationUnit compilationUnit, String methodName, String code) {
		CodeOfMethodInvocationVisitor codeOfMethodInvocationVisitor = 
			new ASTNodeFinder().new CodeOfMethodInvocationVisitor(methodName, code);
		
		compilationUnit.accept(codeOfMethodInvocationVisitor);
		return codeOfMethodInvocationVisitor.getFoundNodes();
	}
	
	/**
	 * get inner try statement of nested try statement
	 * @param nestedTryStatment 
	 * @param locationToFindInnerTryStatement 
	 * 								decide where to find inner try statement, in try block or in catch clause  
	 * @return
	 */
	public static List<TryStatement> getTryStatementInNestedTryStatement(TryStatement nestedTryStatment, int locationToFindInnerTryStatement) {
		TryStatementInNestedTryStatementVisitor nestedTryStatementCollectorVisitor = new ASTNodeFinder().new TryStatementInNestedTryStatementVisitor(nestedTryStatment, locationToFindInnerTryStatement);
		nestedTryStatment.accept(nestedTryStatementCollectorVisitor);
		
		return nestedTryStatementCollectorVisitor.getResultTryStatementList();
	}
	
	/**
	 * collect try statement from nested try statemen<br />
	 * can decide the location to search try statement, from try block or catch clause<br />
	 * lacks: if we decide to search in CatchClause, visitor will scan try statement in all CatchClause. we can not decide a specified CatchClause to scan.
	 * lacks: we only take one level deep nested try statement in consider. 
	 * if nested level is too deep, the result would be over estimated.
	 * @author charles
	 *
	 */
	class TryStatementInNestedTryStatementVisitor extends ASTVisitor {
		TryStatement bigTryStatement;
		int subNodeTypeOfOuterTryStatement;
		List<TryStatement> collectResultOfTryStatements;
		
		/**
		 * 
		 * @param nestedTryStatement nested TryStatement
		 * @param locationToFindInnerTryStatement 
										decide where to find inner try statement, in try block or in catch clause  
		 */
		public TryStatementInNestedTryStatementVisitor(TryStatement nestedTryStatement, int locationToFindInnerTryStatement) {
			bigTryStatement = nestedTryStatement;
			this.subNodeTypeOfOuterTryStatement = locationToFindInnerTryStatement;
			collectResultOfTryStatements = new ArrayList<TryStatement>();
		}
		
		public boolean visit(TryStatement tryStatement) {
			if(subNodeTypeOfOuterTryStatement == ASTNode.TRY_STATEMENT) {
				ASTNode parentNode = NodeUtils.getSpecifiedParentNode(tryStatement, ASTNode.TRY_STATEMENT);
				if(parentNode != null) {
					collectResultOfTryStatements.add(tryStatement);
				}
			} else if (subNodeTypeOfOuterTryStatement == ASTNode.CATCH_CLAUSE) {
				ASTNode parentNode = NodeUtils.getSpecifiedParentNode(tryStatement, ASTNode.CATCH_CLAUSE);
				if(parentNode != null) {
					collectResultOfTryStatements.add(tryStatement);
				}
			}
			return true;
		}
		
		public List<TryStatement> getResultTryStatementList() {
			return collectResultOfTryStatements;
		}
	}
	
	/**
	 * 
	 * find specified MethodDeclaration node by method name。<br />
	 * Known Issue: if there are two method with the same name, this feature will return the MethodDeclaration of the first method.
	 * @author charles
	 *
	 */
	class NameOfMethodDeclarationVisitor extends ASTVisitor {
		String methodName;
		MethodDeclaration foundNode;
		List<TryStatement> tryStatementList;
		
		public NameOfMethodDeclarationVisitor(String nodeName) {
			this.methodName = nodeName;
			foundNode = null;
			tryStatementList = new ArrayList<TryStatement>();
		}
		
		public boolean visit(MethodDeclaration node) {
			if(node.getName().toString().equals(methodName)) {
				foundNode = node;
				return true;
			}
			return false;
		}
		
		public boolean visit(TryStatement node) {
			tryStatementList.add(node);
			return false;
		}
		
		public MethodDeclaration getFoundMethodDeclaration() {
			return foundNode;
		}
		
		public List<TryStatement> getCollectedTryStatement() {
			return tryStatementList;
		}
	}
	
	/**
	 * use method name or part of code to find specified method invocation.
	 * return a node list due to this feature would find out so much corresponding result. 
	 * @author charles
	 *
	 */
	class CodeOfMethodInvocationVisitor extends ASTVisitor {
		String methodName;
		String codeName;
		List<MethodInvocation> foundNodeList;
		
		public CodeOfMethodInvocationVisitor(String methodName, String codeName) {
			this.methodName = methodName;
			this.codeName = codeName;
			foundNodeList = new ArrayList<MethodInvocation>();
		}
		
		public boolean visit(MethodDeclaration node) {
			if(node.getName().toString().equals(methodName)) {
				return true;
			}
			return false;
		}
		
		//Never lookup in the initializer
		@Override
		public boolean visit(Initializer node) {
			return false;
		}

		public boolean visit(MethodInvocation node) {
			if(node.toString().equals(codeName)) {
				foundNodeList.add(node);
			}
			return false;
		}
		
		public List<MethodInvocation> getFoundNodes() {
			return foundNodeList;
		}
	}
	
	class LineNumberOfASTNodeVisitor extends RLBaseVisitor {
		int lineNumber;
		ASTNode currentNode;
		CompilationUnit root;
		
		public LineNumberOfASTNodeVisitor(CompilationUnit unit, int lineNumber) {
			this.lineNumber = lineNumber;
			root = unit;
			currentNode = null;
		}
		
		public boolean visitNode(ASTNode node) {
			if(lineNumber == root.getLineNumber(node.getStartPosition())) {
				currentNode = node;
				return false;
			}
			return true;
		}
		
		public ASTNode getCurrentNode() {
			return currentNode;
		}
	}
}
