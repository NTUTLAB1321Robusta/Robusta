package ntut.csie.filemaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.common.RLBaseVisitor;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * 從指定的專案中的類別取得CompilationUnit
 * @author charles
 *
 */
public class ASTNodeFinder {
	public static CompilationUnit getCompilationUnit(Class<?> clazz, String projectName) {
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(clazz, projectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin
				.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		return compilationUnit;
	}

	/**
	 * 從指定class尋找特定節點
	 * 如果找不到特定節點，則回傳null。
	 * @param className 指定class名稱
	 * @param lineNumber 該class行號
	 * @return
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public static ASTNode getNodeFromSpecifiedClass (Class<?> className, String projectName, int lineNumber) throws IOException, CoreException {
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(className, projectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		
		LineNumberOfASTNodeVisitor astNodeVisitor = new ASTNodeFinder().new LineNumberOfASTNodeVisitor(compilationUnit, lineNumber);
		
		compilationUnit.accept(astNodeVisitor);
		
		return astNodeVisitor.getCurrentNode();
	}
	
	/**
	 * 根據特定的Method Name找回MethodDeclaration Node <br />
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
	 * 根據指定的Method Name，找出java檔中，第一個符合此Method Name的MethodDeclaration
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
	 * 根據指定的MethodName，找出這個Method所有的TryStatement。
	 * (巢狀的Try不會另外被抓出來)
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
	 * 從巢狀的TryStatement中取出內層的TryStatement
	 * @param outerTryStatment 有巢狀結構的TryStatement
	 * @param subNodeTypeOfOuterTryStatement 根據傳入ASTNode Type，決定要從TryBlock裡面去取TryStatement或是從 Catch Clause裡面去取 TryStatement
	 * @return
	 */
	public static List<TryStatement> getTryStatementInNestedTryStatement(TryStatement outerTryStatment, int subNodeTypeOfOuterTryStatement) {
		TryStatementInNestedTryStatementVisitor nestedTryStatementCollectorVisitor = new ASTNodeFinder().new TryStatementInNestedTryStatementVisitor(outerTryStatment, subNodeTypeOfOuterTryStatement);
		outerTryStatment.accept(nestedTryStatementCollectorVisitor);
		
		return nestedTryStatementCollectorVisitor.getResultTryStatementList();
	}
	
	/**
	 * 從巢狀的TryStatement中，蒐集TryStatement。<br />
	 * 可以選擇從Try Block裡面蒐集，也可以選擇從Catch Clause裡面蒐集。<br />
	 * lacks: 如果選擇從CatchClause裡面蒐集，會去尋找所有CatchClause裡面的TryStatement，而不能指定特定的CatchClause。
	 * lacks: 目前只有考慮巢狀結構只有一層的情況，再多可能會超乎預期。
	 * @author charles
	 *
	 */
	class TryStatementInNestedTryStatementVisitor extends ASTVisitor {
		TryStatement bigTryStatement;
		int subNodeTypeOfOuterTryStatement;
		List<TryStatement> collectResultOfTryStatements;
		
		/**
		 * 
		 * @param nestedTryStatement 具有巢狀結構的TryStatement
		 * @param subNodeTypeOfOuterTryStatement 你想要挖的TryStatement在Try Block裡面還是在Catch Clause裡面，請給定ASTNode Type。
		 */
		public TryStatementInNestedTryStatementVisitor(TryStatement nestedTryStatement, int subNodeTypeOfOuterTryStatement) {
			bigTryStatement = nestedTryStatement;
			this.subNodeTypeOfOuterTryStatement = subNodeTypeOfOuterTryStatement;
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
	 * 利用Method Name找到指定的MethodDeclaration Node。<br />
	 * Known Issue: 如果Class中擁有兩個使用相同名稱的Method，只會回傳最先找到的那個。
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
	 * 利用MethodName與程式碼片段找到所屬的MethodInvocation node。
	 * 因為符合的程式碼片段可能有多個，所以回傳找到的MethodInvocation Node以List表示。
	 * 如果程式碼片段所符合的Node不是MethodInvocation，
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
