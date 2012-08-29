package ntut.csie.filemaker;

import java.io.IOException;

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

/**
 * 從指定的專案中的類別取得CompilationUnit
 * @author charles
 *
 */
public class ASTNodeFinder {
	public static CompilationUnit getCompilationUnit(Class<?> clazz, String projectName) {
		String classCanonicalName = clazz.getCanonicalName();
		String classPath = PathUtils.dot2slash(classCanonicalName);
		Path path = new Path(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getName()
				+ "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ classPath	+ JavaProjectMaker.JAVA_FILE_EXTENSION);
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
		String classCanonicalName = className.getCanonicalName();
		String classPath = classCanonicalName.replace('.', '/');
		Path path = new Path(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getName() + "/src/" + classPath + ".java");
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
	public static MethodDeclaration getMethodDeclarationNodeByName(Class<?> clazz, String projectName, String methodName) {
		CompilationUnit compilationUnit = getCompilationUnit(clazz, projectName);
		return getMethodDeclarationNodeByName(compilationUnit, methodName);
	}
	
	public static MethodDeclaration getMethodDeclarationNodeByName(CompilationUnit compilationUnit, String methodName) {
		NameOfMethodDeclarationVisitor nameOfMethodDeclarationVisitor = new ASTNodeFinder().new NameOfMethodDeclarationVisitor(
				compilationUnit, methodName);
		compilationUnit.accept(nameOfMethodDeclarationVisitor);
		return nameOfMethodDeclarationVisitor.getFoundNode();		
	}
	
	/**
	 * 利用Method Name找到指定的MethodDeclaration Node。<br />
	 * Known Issue: 如果Class中擁有兩個使用相同名稱的Method，只會回傳最先找到的那個。
	 * @author charles
	 *
	 */
	public class NameOfMethodDeclarationVisitor extends ASTVisitor {
		String nodeName;
		MethodDeclaration foundNode;
		CompilationUnit astRoot;
		
		public NameOfMethodDeclarationVisitor(CompilationUnit compilationUnit, String nodeName) {
			astRoot = compilationUnit;
			this.nodeName = nodeName;
			foundNode = null;
		}
		
		public boolean visit(MethodDeclaration node) {
			if(node.getName().toString().equals(nodeName)) {
				foundNode = node;
			}
			return false;
		}
		
		public MethodDeclaration getFoundNode() {
			return foundNode;
		}
	}
	
	public class LineNumberOfASTNodeVisitor extends RLBaseVisitor {
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
