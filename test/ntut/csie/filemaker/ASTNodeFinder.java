package ntut.csie.filemaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

/**
 * �q���w���M�פ������O���oCompilationUnit
 * @author charles
 *
 */
public class ASTNodeFinder {
	public static CompilationUnit getCompilationUnit(Class<?> clazz, String projectName) {
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(clazz, projectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin
				.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
		CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		return compilationUnit;
	}

	/**
	 * �q���wclass�M��S�w�`�I
	 * �p�G�䤣��S�w�`�I�A�h�^��null�C
	 * @param className ���wclass�W��
	 * @param lineNumber ��class�渹
	 * @return
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public static ASTNode getNodeFromSpecifiedClass (Class<?> className, String projectName, int lineNumber) throws IOException, CoreException {
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(className, projectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
		CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		
		LineNumberOfASTNodeVisitor astNodeVisitor = new ASTNodeFinder().new LineNumberOfASTNodeVisitor(compilationUnit, lineNumber);
		
		compilationUnit.accept(astNodeVisitor);
		
		return astNodeVisitor.getCurrentNode();
	}
	
	/**
	 * �ھگS�w��Method Name��^MethodDeclaration Node <br />
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

	public static MethodDeclaration getMethodDeclarationNodeByName(
			CompilationUnit compilationUnit, String methodName) {
		NameOfMethodDeclarationVisitor nameOfMethodDeclarationVisitor = 
			new ASTNodeFinder().new NameOfMethodDeclarationVisitor(
				compilationUnit, methodName);
		compilationUnit.accept(nameOfMethodDeclarationVisitor);
		return nameOfMethodDeclarationVisitor.getFoundNode();
	}
	
	public static List<MethodInvocation> getMethodInvocationByMethodNameAndCode(
			Class<?> clazz, String projectName, String methodName, String code) {
		CompilationUnit compilationUnit = getCompilationUnit(clazz, projectName);
		return getMethodInvocationByMethodNameAndCode(compilationUnit, methodName, code);
	}
	
	public static List<MethodInvocation> getMethodInvocationByMethodNameAndCode(
			CompilationUnit compilationUnit, String methodName, String code) {
		CodeOfMethodInvocationVisitor codeOfMethodInvocationVisitor = 
			new ASTNodeFinder().new CodeOfMethodInvocationVisitor(
				compilationUnit, methodName, code);
		compilationUnit.accept(codeOfMethodInvocationVisitor);
		return codeOfMethodInvocationVisitor.getFoundNodes();
	}

	/**
	 * �Q��Method Name�����w��MethodDeclaration Node�C<br />
	 * Known Issue: �p�GClass���֦���ӨϥάۦP�W�٪�Method�A�u�|�^�ǳ̥���쪺���ӡC
	 * @author charles
	 *
	 */
	public class NameOfMethodDeclarationVisitor extends ASTVisitor {
		String methodName;
		MethodDeclaration foundNode;
		CompilationUnit astRoot;
		
		public NameOfMethodDeclarationVisitor(CompilationUnit compilationUnit, String nodeName) {
			astRoot = compilationUnit;
			this.methodName = nodeName;
			foundNode = null;
		}
		
		public boolean visit(MethodDeclaration node) {
			if(node.getName().toString().equals(methodName)) {
				foundNode = node;
			}
			return false;
		}
		
		public MethodDeclaration getFoundNode() {
			return foundNode;
		}
	}
	
	/**
	 * �Q��MethodName�P�{���X���q�����ݪ�MethodInvocation node�C
	 * �]���ŦX���{���X���q�i�঳�h�ӡA�ҥH�^�ǧ�쪺MethodInvocation Node�HList��ܡC
	 * �p�G�{���X���q�ҲŦX��Node���OMethodInvocation�A
	 * @author charles
	 *
	 */
	public class CodeOfMethodInvocationVisitor extends ASTVisitor {
		String methodName;
		String codeName;
		List<MethodInvocation> foundNodeList;
		CompilationUnit astRoot;
		
		public CodeOfMethodInvocationVisitor(CompilationUnit compilationUnit,
				String methodName, String codeName) {
			astRoot = compilationUnit;
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
