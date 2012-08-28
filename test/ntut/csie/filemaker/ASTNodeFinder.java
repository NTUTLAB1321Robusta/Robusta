package ntut.csie.filemaker;

import java.io.IOException;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ASTNodeFinder {
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
		
		ASTNodeVisitor astNodeVisitor = new ASTNodeFinder().new ASTNodeVisitor(compilationUnit, lineNumber);
		
		compilationUnit.accept(astNodeVisitor);
		
		return astNodeVisitor.getCurrentNode();
	}
	
	public class ASTNodeVisitor extends RLBaseVisitor {
		int lineNumber;
		ASTNode currentNode;
		CompilationUnit root;
		
		public ASTNodeVisitor(CompilationUnit unit, int lineNumber) {
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
