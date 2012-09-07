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
 * �q���w���M�פ������O���oCompilationUnit
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
		String classCanonicalName = className.getCanonicalName();
		String classPath = classCanonicalName.replace('.', '/');
		Path path = new Path(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getName() + "/src/" + classPath + ".java");
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

	/**
	 * �ھګ��w��Method Name�A��Xjava�ɤ��A�Ĥ@�ӲŦX��Method Name��MethodDeclaration
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
	 * �ھګ��w��MethodName�A��X�o��Method�Ҧ���TryStatement�C
	 * (�_����Try���|�t�~�Q��X��)
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
	 * �q�_����TryStatement�����X���h��TryStatement
	 * @param outerTryStatment ���_�����c��TryStatement
	 * @param subNodeTypeOfOuterTryStatement �ھڶǤJASTNode Type�A�M�w�n�qTryBlock�̭��h��TryStatement�άO�q Catch Clause�̭��h�� TryStatement
	 * @return
	 */
	public static List<TryStatement> getTryStatementInNestedTryStatement(TryStatement outerTryStatment, int subNodeTypeOfOuterTryStatement) {
		TryStatementInNestedTryStatementVisitor nestedTryStatementCollectorVisitor = new ASTNodeFinder().new TryStatementInNestedTryStatementVisitor(outerTryStatment, subNodeTypeOfOuterTryStatement);
		outerTryStatment.accept(nestedTryStatementCollectorVisitor);
		
		return nestedTryStatementCollectorVisitor.getResultTryStatementList();
	}
	
	/**
	 * �q�_����TryStatement���A�`��TryStatement�C<br />
	 * �i�H��ܱqTry Block�̭��`���A�]�i�H��ܱqCatch Clause�̭��`���C<br />
	 * lacks: �p�G��ܱqCatchClause�̭��`���A�|�h�M��Ҧ�CatchClause�̭���TryStatement�A�Ӥ�����w�S�w��CatchClause�C
	 * lacks: �ثe�u���Ҽ{�_�����c�u���@�h�����p�A�A�h�i��|�W�G�w���C
	 * @author charles
	 *
	 */
	class TryStatementInNestedTryStatementVisitor extends ASTVisitor {
		TryStatement bigTryStatement;
		int subNodeTypeOfOuterTryStatement;
		List<TryStatement> collectResultOfTryStatements;
		
		/**
		 * 
		 * @param nestedTryStatement �㦳�_�����c��TryStatement
		 * @param subNodeTypeOfOuterTryStatement �A�Q�n����TryStatement�bTry Block�̭��٬O�bCatch Clause�̭��A�е��wASTNode Type�C
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
	 * �Q��Method Name�����w��MethodDeclaration Node�C<br />
	 * Known Issue: �p�GClass���֦���ӨϥάۦP�W�٪�Method�A�u�|�^�ǳ̥���쪺���ӡC
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
	 * �Q��MethodName�P�{���X���q�����ݪ�MethodInvocation node�C
	 * �]���ŦX���{���X���q�i�঳�h�ӡA�ҥH�^�ǧ�쪺MethodInvocation Node�HList��ܡC
	 * �p�G�{���X���q�ҲŦX��Node���OMethodInvocation�A
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
