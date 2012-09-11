package ntut.csie.csdet.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.RuntimeEnvironmentProjectReader;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.Statement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DHQuickFixTest {
	String projectNameString;
	String packageNameString;
	String classSimpleNameString;
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;
	SmellSettings smellSettings;
	
	public DHQuickFixTest() {
		projectNameString = "DummyHandlerTest";
		packageNameString = DummyAndIgnoreExample.class.getPackage().getName();
		classSimpleNameString = DummyAndIgnoreExample.class.getSimpleName();
	}
	
	@Before
	public void setUp() throws Exception {
		// 讀取測試檔案樣本內容
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		jpm = new JavaProjectMaker(projectNameString);
		jpm.setJREDefaultContainer();
		// 新增欲載入的library
		jpm.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		jpm.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		jpm.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.FOLDERNAME_LIB_JAR + JavaProjectMaker.FOLDERNAME_LIB_JAR);
		// 根據測試檔案樣本內容建立新的檔案
		jpm.createJavaFile(packageNameString,
				classSimpleNameString + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + packageNameString + ";\n" + jfs.getFileContent());
		// 建立XML
		CreateSettings();
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, projectNameString));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		unit = (CompilationUnit) parser.createAST(null); 
		unit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		jpm.deleteProject();
	}
	
	@Test
	public void testDeleteStatement() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??"); 
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		unit.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();
		
		Method deleteStatement = DHQuickFix.class.getDeclaredMethod("deleteStatement", List.class);
		deleteStatement.setAccessible(true);
		
		for(int i = 0; i < catchList.size(); i++) {
			List<Statement> statements = ((CatchClause)catchList.get(i)).getBody().statements();
			deleteStatement.invoke(dhQF, statements);
			// 經過deleteStatement method之後，如果還有剩下statement的話，可能是一些其他邏輯
			if(statements.size() > 0) {
				for(int j = 0; j < statements.size(); j++) {
					// 判斷ExpressionStatement不應該為System.out.print、printStackTrace、System.err.print
					if(statements.get(j).getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
						assertFalse(((ExpressionStatement)statements.get(j)).getExpression().toString().contains("System.out.print"));
						assertFalse(((ExpressionStatement)statements.get(j)).getExpression().toString().contains("printStackTrace"));
						assertFalse(((ExpressionStatement)statements.get(j)).getExpression().toString().contains("System.err.print"));
					}
				}
			}
		}
	}
	
	@Test
	public void testAddImportDeclaration() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??");
		/* 設定actRoot成員變數，此為BaseQuickFix的成員變數
		 * 設定actRoot內容的地點是在BaseQuickFix中的method
		 * 故需要自行設定
		 */
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		assertNull(actRoot.get(dhQF));
		actRoot.setAccessible(true);
		actRoot.set(dhQF, unit);
		assertNotNull(actRoot.get(dhQF));
		// 檢查原本import的classes
		List<?> imports = unit.imports();
		assertEquals(6, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		// Import Robustness及RL的宣告
		Method addImportDeclaration = DHQuickFix.class.getDeclaredMethod("addImportDeclaration");
		addImportDeclaration.setAccessible(true);
		addImportDeclaration.invoke(dhQF);
		// 驗證是否有import Robustness及RL的宣告
		imports = unit.imports();
		assertEquals(8, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		assertEquals("import agile.exception.Robustness;\n", imports.get(6).toString());
		assertEquals("import agile.exception.RL;\n", imports.get(7).toString());
	}
	
	@Test
	public void testGetRLAnnotation() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??");
		Method getRLAnnotation = DHQuickFix.class.getDeclaredMethod("getRLAnnotation", AST.class, int.class, String.class);
		getRLAnnotation.setAccessible(true);
		NormalAnnotation annotation = (NormalAnnotation)getRLAnnotation.invoke(dhQF, unit.getAST(), 1, "RuntimeException");
		assertEquals("@RL(level=1,exception=RuntimeException.class)", annotation.toString());
	}
	
	@Test
	public void testAddAnnotationRoot() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??");
		/* 選定要quick fix的method */
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// 驗證是否抓到預想中的method - 抓出來的method除了程式碼外，空格應該也要相同
		assertEquals(	"public void true_systemErrPrint(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    System.err.println(e);\n" +
						"  }\n" +
						"}\n", currentMethodNode.toString());
		
		/* 建立空的RL list，測試RL是否會加上去 */
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(unit, currentMethodNode.getStartPosition(), 0);
		currentMethodNode.accept(exVisitor);
		List<RLMessage> currentMethodRLList = exVisitor.getMethodRLAnnotationList();
		Field currentMethodRLListField = DHQuickFix.class.getDeclaredField("currentMethodRLList");
		currentMethodRLListField.setAccessible(true);
		currentMethodRLListField.set(dhQF, currentMethodRLList);
		// 驗證一開始沒有任何RL
		assertEquals(0, currentMethodRLList.size());
		// 新增RuntimeException RL
		Method addAnnotationRoot = DHQuickFix.class.getDeclaredMethod("addAnnotationRoot", AST.class);
		addAnnotationRoot.setAccessible(true);
		addAnnotationRoot.invoke(dhQF, currentMethodNode.getAST());
		List<IExtendedModifier> modifiers = ((MethodDeclaration)currentMethodNodeField.get(dhQF)).modifiers();
		assertEquals(2, modifiers.size());
		assertEquals("@Robustness(value={@RL(level=1,exception=RuntimeException.class)})", modifiers.get(0).toString());
		assertEquals("public", modifiers.get(1).toString());

		/* FIXME -  問題在於我們建立出來的Compilation Unit對他修改了現在抓取到的Method，
		 * 			可是ExceptionAnalyzer還是只能對原來的cu做visitor的動作
		 */
		/* 建立有內容的RL list，測試RL加上去時，是否會出現重複的RL annotation */
		// 上面已加入RuntimeException RL，再讓它加一次
		currentMethodNode.accept(exVisitor);
		currentMethodRLList = exVisitor.getMethodRLAnnotationList();
		currentMethodRLListField.set(dhQF, currentMethodRLList);
		// 驗證已經存在的RL
		assertEquals(1, currentMethodRLList.size());
		// 新增RuntimeException RL
		addAnnotationRoot.invoke(dhQF, currentMethodNode.getAST());
		modifiers = ((MethodDeclaration)currentMethodNodeField.get(dhQF)).modifiers();
		assertEquals(2, modifiers.size());
		assertEquals("@Robustness(value={@RL(level=1,exception=RuntimeException.class)})", modifiers.get(0).toString());
		assertEquals("public", modifiers.get(1).toString());
	}
	
	@Test
	public void testAddThrowStatement() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??");
		/* 選定要quick fix的method */
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// 驗證是否抓到預想中的method
		assertEquals(	"public void true_systemErrPrint(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    System.err.println(e);\n" +
						"  }\n" +
						"}\n", currentMethodNode.toString());
		/* 設定此quick fix的method問題為Dummy handler*/
		Field problem = DHQuickFix.class.getDeclaredField("problem");
		problem.setAccessible(true);
		problem.set(dhQF, "Dummy_Handler");
		
		/* add throw statement */
		Method addThrowStatement = DHQuickFix.class.getDeclaredMethod("addThrowStatement", CatchClause.class, AST.class);
		addThrowStatement.setAccessible(true);
		// 取得該method的catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		addThrowStatement.invoke(dhQF, catchCollector.getMethodList().get(0), currentMethodNode.getAST());
		// 驗證是否加入throw statement以及刪除System.err.println(e)
		assertEquals(	"public void true_systemErrPrint(){\n" +
				"  FileInputStream fis=null;\n" +
				"  try {\n" +
				"    fis=new FileInputStream(\"\");\n" +
				"    fis.read();\n" +
				"  }\n" +
				" catch (  IOException e) {\n" +
				"    throw new RuntimeException(e);\n" +
				"  }\n" +
				"}\n", currentMethodNode.toString());
	}
	
	@Test
	public void testFindEHSmellList() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??");
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		assertNull(actRoot.get(dhQF));
		actRoot.setAccessible(true);
		actRoot.set(dhQF, unit);
		assertNotNull(actRoot.get(dhQF));
		
		/* 測試尚未抓取current method node時的情況 */
		Method findEHSmellList = DHQuickFix.class.getDeclaredMethod("findEHSmellList", String.class);
		findEHSmellList.setAccessible(true);
		assertNull(findEHSmellList.invoke(dhQF, "Dummy_Handler"));
		
		/* 選定要quick fix的method */
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// 驗證是否抓到預想中的method
		assertEquals(	"public void true_systemErrPrint(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    System.err.println(e);\n" +
						"  }\n" +
						"}\n", currentMethodNode.toString());
		
		/* 測試problem為dummy handler的情況 */
		List<MarkerInfo> dummyList = (List)findEHSmellList.invoke(dhQF, "Dummy_Handler");
		assertEquals(1, dummyList.size());
		assertEquals("Dummy_Handler", dummyList.get(0).getCodeSmellType());
		
		/* 測試proglem為ignore checked exception的情況 */
		List<MarkerInfo> ignoreList = (List)findEHSmellList.invoke(dhQF, "Ignore_Checked_Exception");
		assertEquals(0, ignoreList.size());
	}
	
	@Test
	public void testRethrowException() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??");
		/* 設定必要參數 */
		Field actOpenable = BaseQuickFix.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		IOpenable actO = (IOpenable)JavaCore.create(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource());
		actO.open(null);
		actOpenable.set(dhQF, actO);
		
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		assertNull(actRoot.get(dhQF));
		actRoot.setAccessible(true);
		actRoot.set(dhQF, unit);
		assertNotNull(actRoot.get(dhQF));
		// 選定要quick fix的method
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// 驗證是否抓到預想中的method
		assertEquals(	"public void true_systemErrPrint(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    System.err.println(e);\n" +
						"  }\n" +
						"}\n", currentMethodNode.toString());
		// 設定此quick fix的method問題為Dummy handler
		Field problem = DHQuickFix.class.getDeclaredField("problem");
		problem.setAccessible(true);
		problem.set(dhQF, "Dummy_Handler");
		
		Field currentMethodRLList = DHQuickFix.class.getDeclaredField("currentMethodRLList");
		currentMethodRLList.setAccessible(true);
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(unit, currentMethodNode.getStartPosition(), 0);
		currentMethodNode.accept(exVisitor);
		currentMethodRLList.set(dhQF, exVisitor.getMethodRLAnnotationList());
		
//		DummyHandlerVisitor dhVisitor = new DummyHandlerVisitor(unit);
		/* FIXME - how to focus editor */
//		IDE.openEditor(JavaPlugin.getActivePage(), (IFile)((ICompilationUnit)actO).getResource());
		Method rethrowException = DHQuickFix.class.getDeclaredMethod("rethrowException", int.class);
		rethrowException.setAccessible(true);
		assertEquals(0, rethrowException.invoke(dhQF, 0));
	}

	@Test
	public void testQucikFix_RethrowUncheckedException() {
		fail("not implement");
	}
	
	@Test
	public void testQucikFix_ThrowCheckedException() {
		fail("not implement");
	}
	
	/**
	 * 建立xml檔案
	 */
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
