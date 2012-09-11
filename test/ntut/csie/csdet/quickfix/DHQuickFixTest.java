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
		// Ū�������ɮ׼˥����e
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		jpm = new JavaProjectMaker(projectNameString);
		jpm.setJREDefaultContainer();
		// �s�W�����J��library
		jpm.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		jpm.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		jpm.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.FOLDERNAME_LIB_JAR + JavaProjectMaker.FOLDERNAME_LIB_JAR);
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		jpm.createJavaFile(packageNameString,
				classSimpleNameString + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + packageNameString + ";\n" + jfs.getFileContent());
		// �إ�XML
		CreateSettings();
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, projectNameString));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
		unit = (CompilationUnit) parser.createAST(null); 
		unit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// �p�Gxml�ɮצs�b�A�h�R����
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// �R���M��
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
			// �g�LdeleteStatement method����A�p�G�٦��ѤUstatement���ܡA�i��O�@�Ǩ�L�޿�
			if(statements.size() > 0) {
				for(int j = 0; j < statements.size(); j++) {
					// �P�_ExpressionStatement�����Ӭ�System.out.print�BprintStackTrace�BSystem.err.print
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
		/* �]�wactRoot�����ܼơA����BaseQuickFix�������ܼ�
		 * �]�wactRoot���e���a�I�O�bBaseQuickFix����method
		 * �G�ݭn�ۦ�]�w
		 */
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		assertNull(actRoot.get(dhQF));
		actRoot.setAccessible(true);
		actRoot.set(dhQF, unit);
		assertNotNull(actRoot.get(dhQF));
		// �ˬd�쥻import��classes
		List<?> imports = unit.imports();
		assertEquals(6, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		// Import Robustness��RL���ŧi
		Method addImportDeclaration = DHQuickFix.class.getDeclaredMethod("addImportDeclaration");
		addImportDeclaration.setAccessible(true);
		addImportDeclaration.invoke(dhQF);
		// ���ҬO�_��import Robustness��RL���ŧi
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
		/* ��w�nquick fix��method */
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// ���ҬO�_���w�Q����method - ��X�Ӫ�method���F�{���X�~�A�Ů����Ӥ]�n�ۦP
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
		
		/* �إߪŪ�RL list�A����RL�O�_�|�[�W�h */
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(unit, currentMethodNode.getStartPosition(), 0);
		currentMethodNode.accept(exVisitor);
		List<RLMessage> currentMethodRLList = exVisitor.getMethodRLAnnotationList();
		Field currentMethodRLListField = DHQuickFix.class.getDeclaredField("currentMethodRLList");
		currentMethodRLListField.setAccessible(true);
		currentMethodRLListField.set(dhQF, currentMethodRLList);
		// ���Ҥ@�}�l�S������RL
		assertEquals(0, currentMethodRLList.size());
		// �s�WRuntimeException RL
		Method addAnnotationRoot = DHQuickFix.class.getDeclaredMethod("addAnnotationRoot", AST.class);
		addAnnotationRoot.setAccessible(true);
		addAnnotationRoot.invoke(dhQF, currentMethodNode.getAST());
		List<IExtendedModifier> modifiers = ((MethodDeclaration)currentMethodNodeField.get(dhQF)).modifiers();
		assertEquals(2, modifiers.size());
		assertEquals("@Robustness(value={@RL(level=1,exception=RuntimeException.class)})", modifiers.get(0).toString());
		assertEquals("public", modifiers.get(1).toString());

		/* FIXME -  ���D�b��ڭ̫إߥX�Ӫ�Compilation Unit��L�ק�F�{�b����쪺Method�A
		 * 			�i�OExceptionAnalyzer�٬O�u����Ӫ�cu��visitor���ʧ@
		 */
		/* �إߦ����e��RL list�A����RL�[�W�h�ɡA�O�_�|�X�{���ƪ�RL annotation */
		// �W���w�[�JRuntimeException RL�A�A�����[�@��
		currentMethodNode.accept(exVisitor);
		currentMethodRLList = exVisitor.getMethodRLAnnotationList();
		currentMethodRLListField.set(dhQF, currentMethodRLList);
		// ���Ҥw�g�s�b��RL
		assertEquals(1, currentMethodRLList.size());
		// �s�WRuntimeException RL
		addAnnotationRoot.invoke(dhQF, currentMethodNode.getAST());
		modifiers = ((MethodDeclaration)currentMethodNodeField.get(dhQF)).modifiers();
		assertEquals(2, modifiers.size());
		assertEquals("@Robustness(value={@RL(level=1,exception=RuntimeException.class)})", modifiers.get(0).toString());
		assertEquals("public", modifiers.get(1).toString());
	}
	
	@Test
	public void testAddThrowStatement() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??");
		/* ��w�nquick fix��method */
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// ���ҬO�_���w�Q����method
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
		/* �]�w��quick fix��method���D��Dummy handler*/
		Field problem = DHQuickFix.class.getDeclaredField("problem");
		problem.setAccessible(true);
		problem.set(dhQF, "Dummy_Handler");
		
		/* add throw statement */
		Method addThrowStatement = DHQuickFix.class.getDeclaredMethod("addThrowStatement", CatchClause.class, AST.class);
		addThrowStatement.setAccessible(true);
		// ���o��method��catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		addThrowStatement.invoke(dhQF, catchCollector.getMethodList().get(0), currentMethodNode.getAST());
		// ���ҬO�_�[�Jthrow statement�H�ΧR��System.err.println(e)
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
		
		/* ���թ|�����current method node�ɪ����p */
		Method findEHSmellList = DHQuickFix.class.getDeclaredMethod("findEHSmellList", String.class);
		findEHSmellList.setAccessible(true);
		assertNull(findEHSmellList.invoke(dhQF, "Dummy_Handler"));
		
		/* ��w�nquick fix��method */
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// ���ҬO�_���w�Q����method
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
		
		/* ����problem��dummy handler�����p */
		List<MarkerInfo> dummyList = (List)findEHSmellList.invoke(dhQF, "Dummy_Handler");
		assertEquals(1, dummyList.size());
		assertEquals("Dummy_Handler", dummyList.get(0).getCodeSmellType());
		
		/* ����proglem��ignore checked exception�����p */
		List<MarkerInfo> ignoreList = (List)findEHSmellList.invoke(dhQF, "Ignore_Checked_Exception");
		assertEquals(0, ignoreList.size());
	}
	
	@Test
	public void testRethrowException() throws Exception {
		DHQuickFix dhQF = new DHQuickFix("??");
		/* �]�w���n�Ѽ� */
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
		// ��w�nquick fix��method
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(projectNameString, packageNameString, classSimpleNameString).getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// ���ҬO�_���w�Q����method
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
		// �]�w��quick fix��method���D��Dummy handler
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
	 * �إ�xml�ɮ�
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
