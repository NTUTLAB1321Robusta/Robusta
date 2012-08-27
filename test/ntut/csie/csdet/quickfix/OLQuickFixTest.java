package ntut.csie.csdet.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.OverLoggingExample;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OLQuickFixTest {
	JavaProjectMaker javaProjectMaker;
	String javaProjectName;
	JavaFileToString javaFile2String;
	String javaFileName;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	OLQuickFix olQuickFix;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	MethodDeclaration mDeclaration;
	MarkerInfo marker;

	public OLQuickFixTest() {
		javaProjectName = "OverLoggingExampleProject";
		javaFileName = OverLoggingExample.class.getPackage().getName();
	}
	
	@Before
	public void setUp() throws Exception {
		// �ǳƴ����ɮ׼˥����e
		javaProjectMaker = new JavaProjectMaker(javaProjectName);
		javaProjectMaker.setJREDefaultContainer();

		// �إ߷s���ɮ�OverLoggingExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(OverLoggingExample.class, "test");
		javaProjectMaker.createJavaFile(javaFileName, "OverLoggingExample.java", 
				"package " + javaFileName + ";\n" + javaFile2String.getFileContent());

		Path olExamplePath = new Path(
				javaProjectName	+ "/src/ntut/csie/filemaker/exceptionBadSmells/OverLoggingExample.java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(olExamplePath)));
		parser.setResolveBindings(true);
		// �Ŀ�ç����]�w
		CreateSettings();
		// ���oAST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		olQuickFix = new OLQuickFix("");
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// �p�Gxml�ɮצs�b�A�h�R����
		if(xmlFile.exists()) {
			assertTrue(xmlFile.delete());
		}
		// �R���M��
		javaProjectMaker.deleteProject();
	}

	@Test
	public void test() {
	}

	// TODO ���ե��g
	@Test
	public void testFindLoggingList() {
	}

	@Test
	public void testDeleteMessageWithIfIsTrue() throws Exception {
		initializeMethodCollectList();

		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(olQuickFix, methodCollectList.get(0));
		
		Field smellMessage = OLQuickFix.class.getDeclaredField("overLoggingList");
		smellMessage.setAccessible(true);
		List<MarkerInfo> overLoggingList = new ArrayList<MarkerInfo>();
		smellMessage.set(olQuickFix, overLoggingList);

		Method deleteMessage = OLQuickFix.class.getDeclaredMethod("deleteMessage", int.class);
		deleteMessage.setAccessible(true);

		// ���ʹ��ե� argument
		mDeclaration = getMethodDeclarationByName(methodCollectList, "theFirstOrderInTheSameClass");
		TryStatement tStatement = (TryStatement) mDeclaration.getBody().statements().get(0);
		CatchClause cClause = (CatchClause) tStatement.catchClauses().get(0);
		ExpressionStatement eStatement = (ExpressionStatement) cClause.getBody().statements().get(0);

		SingleVariableDeclaration singleVariableDeclaration = cClause.getException();
		marker = new MarkerInfo(RLMarkerAttribute.CS_OVER_LOGGING,
				singleVariableDeclaration.resolveBinding().getType(),
				cClause.toString(), cClause.getStartPosition(),
				compilationUnit.getLineNumber(eStatement.getStartPosition()),
				singleVariableDeclaration.getType().toString());

		overLoggingList.add(marker);
		
		// do method and check postcondition
		// FIXME �i�Jif�I�s��applyChange�ɷ|�ߥX�ҥ~�A���״_���D
		deleteMessage.invoke(olQuickFix, 0);
	}

	@Test
	public void testDeleteMessageWithIfIsFalse() throws Exception {
		initializeMethodCollectList();

		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(olQuickFix, methodCollectList.get(0));
		
		Field smellMessage = OLQuickFix.class.getDeclaredField("overLoggingList");
		smellMessage.setAccessible(true);
		List<MarkerInfo> overLoggingList = new ArrayList<MarkerInfo>();
		smellMessage.set(olQuickFix, overLoggingList);

		Method deleteMessage = OLQuickFix.class.getDeclaredMethod("deleteMessage", int.class);
		deleteMessage.setAccessible(true);

		// ���ʹ��ե� argument
		mDeclaration = getMethodDeclarationByName(methodCollectList, "theFirstOrderInTheSameClass");
		TryStatement tStatement = (TryStatement) mDeclaration.getBody().statements().get(0);
		CatchClause cClause = (CatchClause) tStatement.catchClauses().get(0);
		ExpressionStatement eStatement = (ExpressionStatement) cClause.getBody().statements().get(0);

		SingleVariableDeclaration singleVariableDeclaration = cClause.getException();
		marker = new MarkerInfo(RLMarkerAttribute.CS_OVER_LOGGING,
				singleVariableDeclaration.resolveBinding().getType(),
				cClause.toString(), cClause.getStartPosition() + 1,
				compilationUnit.getLineNumber(eStatement.getStartPosition()),
				singleVariableDeclaration.getType().toString());

		overLoggingList.add(marker);
		
		// do method and check postcondition
		deleteMessage.invoke(olQuickFix, 0);
	}

	@Test
	public void testDeleteMessageWithException() throws Exception {
		Method deleteMessage = OLQuickFix.class.getDeclaredMethod("deleteMessage", int.class);
		deleteMessage.setAccessible(true);

		deleteMessage.invoke(olQuickFix, 1);
	}
	
	@Test
	public void testDeleteCatchStatementWithRightArgument() throws Exception {
		initializeMethodCollectList();
		
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);

		Method deleteCatchStatement = OLQuickFix.class.getDeclaredMethod("deleteCatchStatement", ASTNode.class, MarkerInfo.class);
		deleteCatchStatement.setAccessible(true);
		
		// ���ʹ��ե� argument
		mDeclaration = getMethodDeclarationByName(methodCollectList, "theFirstOrderInTheSameClass");
		TryStatement tStatement = (TryStatement) mDeclaration.getBody().statements().get(0);
		CatchClause cClause = (CatchClause) tStatement.catchClauses().get(0);
		List<ASTNode> statementList = cClause.getBody().statements();
		ExpressionStatement eStatement = (ExpressionStatement) statementList.get(0);

		SingleVariableDeclaration singleVariableDeclaration = cClause.getException();
		marker = new MarkerInfo(RLMarkerAttribute.CS_OVER_LOGGING,
				singleVariableDeclaration.resolveBinding().getType(),
				cClause.toString(), cClause.getStartPosition(),
				compilationUnit.getLineNumber(eStatement.getStartPosition()),
				singleVariableDeclaration.getType().toString());

		// check precondition
		assertEquals(2, statementList.size());
		
		// do method and check postcondition
		deleteCatchStatement.invoke(olQuickFix, cClause, marker);
		assertEquals(1, statementList.size());
	}

	@Test
	public void testDeleteCatchStatementWithWrongArgument() throws Exception {
		initializeMethodCollectList();
		
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);
		
		Method deleteCatchStatement = OLQuickFix.class.getDeclaredMethod("deleteCatchStatement", ASTNode.class, MarkerInfo.class);
		deleteCatchStatement.setAccessible(true);
		
		// ���ʹ��ե� argument
		mDeclaration = getMethodDeclarationByName(methodCollectList, "theFirstOrderInTheSameClass");
		TryStatement tStatement = (TryStatement) mDeclaration.getBody().statements().get(0);
		CatchClause cClause = (CatchClause) tStatement.catchClauses().get(0);
		List<ASTNode> statementList = cClause.getBody().statements();
		ExpressionStatement eStatement = (ExpressionStatement) statementList.get(0);

		SingleVariableDeclaration singleVariableDeclaration = cClause.getException();
		marker = new MarkerInfo(RLMarkerAttribute.CS_OVER_LOGGING,
				singleVariableDeclaration.resolveBinding().getType(),
				cClause.toString(), cClause.getStartPosition(),
				compilationUnit.getLineNumber(eStatement.getStartPosition()) + 1,  // �G�N���������
				singleVariableDeclaration.getType().toString());

		// check precondition
		assertEquals(2, statementList.size());
		
		// do method and check postcondition
		deleteCatchStatement.invoke(olQuickFix, cClause, marker);
		assertEquals(2, statementList.size());
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private MethodDeclaration getMethodDeclarationByName(
			List<ASTNode> methodDeclarationList, String methodName) {
		for (int i = 0; i < methodDeclarationList.size(); i++) {
			mDeclaration = (MethodDeclaration) methodDeclarationList.get(i);
			if (methodName.equals(mDeclaration.getName().toString())) {
				return mDeclaration;
			}
		}
		return null;
	}
	
	// ���ͤ@�Ӧ����Ҧ�Method��Collector�ꦨList
	private void initializeMethodCollectList() {
		methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		methodCollectList = methodCollector.getMethodList();
	}
}
