package ntut.csie.csdet.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingJavaLogExample;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.util.PathUtils;

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
	String javaProjectName;
	String javaPackageName;
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	OLQuickFix olQuickFix;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	MethodDeclaration mDeclaration;
	MarkerInfo marker;

	public OLQuickFixTest() {
		javaProjectName = "OverLoggingExampleProject";
		javaPackageName = OverLoggingJavaLogExample.class.getPackage().getName();
	}
	
	@Before
	public void setUp() throws Exception {
		// 準備測試檔案樣本內容
		javaProjectMaker = new JavaProjectMaker(javaProjectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// 建立新的檔案OverLoggingJavaLogExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(OverLoggingJavaLogExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(javaPackageName,
				OverLoggingJavaLogExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + javaPackageName + ";\n" + javaFile2String.getFileContent());

		Path olExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(OverLoggingJavaLogExample.class, javaProjectName));
		
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(olExamplePath)));
		parser.setResolveBindings(true);
		// 勾選並完成設定
		CreateSettings();
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		olQuickFix = new OLQuickFix("");
		
		// 產生收集所有Method的Collector串成List
		methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		methodCollectList = methodCollector.getMethodList();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists()) {
			assertTrue(xmlFile.delete());
		}
		// 刪除專案
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testRun() {
		fail("There was an excepsion when method \"deleteMessage\" invoke the method \"applyChange\"");
	}

	
	/**
	 * 測試theSecondOrderInTheSameClassWithJavaLog()
	 * 此method的call chain有over logging，且此method的log非最上層
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testFindLoggingListReturnNonEmptyList() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(olQuickFix, methodCollectList.get(1));

		Method findLoggingList = OLQuickFix.class.getDeclaredMethod("findLoggingList");
		findLoggingList.setAccessible(true);

		// 初始化成null
		List<MarkerInfo> overLoggingList = null;
		// 執行method後list長度應為1
		overLoggingList = (List<MarkerInfo>) findLoggingList.invoke(olQuickFix);
		assertEquals(1, overLoggingList.size());
	}

	/**
	 * 測試theThirdOrderInTheSameClassWithJavaLog()
	 * 此method的call chain有over logging，但此method並沒有log的動作，故不會被標記
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testFindLoggingListReturnEmptyList() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(olQuickFix, methodCollectList.get(2));
		
		Method findLoggingList = OLQuickFix.class.getDeclaredMethod("findLoggingList");
		findLoggingList.setAccessible(true);

		// 初始化成null
		List<MarkerInfo> overLoggingList = null;
		// 執行method後list長度應為0
		overLoggingList = (List<MarkerInfo>) findLoggingList.invoke(olQuickFix);
		assertEquals(0, overLoggingList.size());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testFindLoggingListReturnNull() throws Exception {
		Method findLoggingList = OLQuickFix.class.getDeclaredMethod("findLoggingList");
		findLoggingList.setAccessible(true);
		
		// 初始化成空鏈結
		List<MarkerInfo> overLoggingList = new ArrayList<MarkerInfo>();
		// 執行method後應被設為null
		overLoggingList = (List<MarkerInfo>) findLoggingList.invoke(olQuickFix);
		assertEquals(null, overLoggingList);
	}
	
	@Test
	public void testDeleteMessageWithIfIsTrue() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(olQuickFix, methodCollectList.get(1));
		
		Field smellMessage = OLQuickFix.class.getDeclaredField("overLoggingList");
		smellMessage.setAccessible(true);
		List<MarkerInfo> overLoggingList = new ArrayList<MarkerInfo>();
		smellMessage.set(olQuickFix, overLoggingList);

		Method deleteMessage = OLQuickFix.class.getDeclaredMethod("deleteMessage", int.class);
		deleteMessage.setAccessible(true);

		// 產生測試用 argument
		mDeclaration = getMethodDeclarationByName(methodCollectList, "theSecondOrderInTheSameClassWithJavaLog");
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
		
		// check precondition
		@SuppressWarnings("unchecked")
		List<ASTNode> statementList = cClause.getBody().statements();
		assertEquals(2, statementList.size());

		// do method and check postcondition
		// FIXME deleteMessage呼叫applyChange時會拋出例外，console會出現例外訊息
		deleteMessage.invoke(olQuickFix, 0);
		assertEquals(1, statementList.size());
	}

	@Test
	public void testDeleteMessageWithIfIsFalse() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(olQuickFix, methodCollectList.get(1));
		
		Field smellMessage = OLQuickFix.class.getDeclaredField("overLoggingList");
		smellMessage.setAccessible(true);
		List<MarkerInfo> overLoggingList = new ArrayList<MarkerInfo>();
		smellMessage.set(olQuickFix, overLoggingList);

		Method deleteMessage = OLQuickFix.class.getDeclaredMethod("deleteMessage", int.class);
		deleteMessage.setAccessible(true);

		// 產生測試用 argument
		mDeclaration = getMethodDeclarationByName(methodCollectList, "theSecondOrderInTheSameClassWithJavaLog");
		TryStatement tStatement = (TryStatement) mDeclaration.getBody().statements().get(0);
		CatchClause cClause = (CatchClause) tStatement.catchClauses().get(0);
		ExpressionStatement eStatement = (ExpressionStatement) cClause.getBody().statements().get(0);

		SingleVariableDeclaration singleVariableDeclaration = cClause.getException();
		marker = new MarkerInfo(RLMarkerAttribute.CS_OVER_LOGGING,
				singleVariableDeclaration.resolveBinding().getType(),
				cClause.toString(), cClause.getStartPosition() + 1,  // 故意給錯的位置
				compilationUnit.getLineNumber(eStatement.getStartPosition()),
				singleVariableDeclaration.getType().toString());

		overLoggingList.add(marker);

		// check precondition
		@SuppressWarnings("unchecked")
		List<ASTNode> statementList = cClause.getBody().statements();
		assertEquals(2, statementList.size());

		// do method and check postcondition
		deleteMessage.invoke(olQuickFix, 0);
		assertEquals(2, statementList.size());
	}
	
	@Test
	public void testDeleteCatchStatementWithRightArgument() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);

		Method deleteCatchStatement = OLQuickFix.class.getDeclaredMethod("deleteCatchStatement", CatchClause.class, MarkerInfo.class);
		deleteCatchStatement.setAccessible(true);
		
		// 產生測試用 argument
		mDeclaration = getMethodDeclarationByName(methodCollectList, "theSecondOrderInTheSameClassWithJavaLog");
		TryStatement tStatement = (TryStatement) mDeclaration.getBody().statements().get(0);
		CatchClause cClause = (CatchClause) tStatement.catchClauses().get(0);
		@SuppressWarnings("unchecked")
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
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(olQuickFix, compilationUnit);
		
		Method deleteCatchStatement = OLQuickFix.class.getDeclaredMethod("deleteCatchStatement", CatchClause.class, MarkerInfo.class);
		deleteCatchStatement.setAccessible(true);
		
		// 產生測試用 argument
		mDeclaration = getMethodDeclarationByName(methodCollectList, "theSecondOrderInTheSameClassWithJavaLog");
		TryStatement tStatement = (TryStatement) mDeclaration.getBody().statements().get(0);
		CatchClause cClause = (CatchClause) tStatement.catchClauses().get(0);
		@SuppressWarnings("unchecked")
		List<ASTNode> statementList = cClause.getBody().statements();
		ExpressionStatement eStatement = (ExpressionStatement) statementList.get(0);

		SingleVariableDeclaration singleVariableDeclaration = cClause.getException();
		marker = new MarkerInfo(RLMarkerAttribute.CS_OVER_LOGGING,
				singleVariableDeclaration.resolveBinding().getType(),
				cClause.toString(), cClause.getStartPosition(),
				compilationUnit.getLineNumber(eStatement.getStartPosition()) + 1,  // 故意給錯的行數
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
}
