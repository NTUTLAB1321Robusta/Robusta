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
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.RuntimeEnvironmentProjectReader;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLMessage;

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
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DHQuickFixTest {
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;

	@Before
	public void setUp() throws Exception {
		// Ū�������ɮ׼˥����e
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, "test");
		
		jpm = new JavaProjectMaker("DummyHandlerTest");
		jpm.setJREDefaultContainer();
		// �s�W�����J��library
		jpm.addJarToBuildPath("lib\\log4j-1.2.15.jar");
		jpm.addJarToBuildPath("..\\SingleSharedLibrary\\common\\agile.rl.jar");
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		jpm.createJavaFile("ntut.csie.exceptionBadSmells", "DummyHandlerExample.java", "package ntut.csie.exceptionBadSmells;\n" + jfs.getFileContent());
		// �إ�XML
		CreateDummyHandlerXML();
		
		Path path = new Path("DummyHandlerTest\\src\\ntut\\csie\\exceptionBadSmells\\DummyHandlerExample.java");
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
		List<ASTNode> catchList = catchCollector.getMethodList();
		
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
		assertEquals(5, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(3).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(4).toString());
		// Import Robustness��RL���ŧi
		Method addImportDeclaration = DHQuickFix.class.getDeclaredMethod("addImportDeclaration");
		addImportDeclaration.setAccessible(true);
		addImportDeclaration.invoke(dhQF);
		// ���ҬO�_��import Robustness��RL���ŧi
		imports = unit.imports();
		assertEquals(7, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(3).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(4).toString());
		assertEquals("import agile.exception.Robustness;\n", imports.get(5).toString());
		assertEquals("import agile.exception.RL;\n", imports.get(6).toString());
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
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(jpm.getProjectName(), "ntut.csie.exceptionBadSmells", "DummyHandlerExample").getResource(), 6);
		Field currentMethodNodeField = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(dhQF);
		// ���ҬO�_���w�Q����method FIXME - ��X�Ӫ�method���F�{���X�~�A�Ů����Ӥ]�n�ۦP
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
//		Field actOpenable = BaseQuickFix.class.getDeclaredField("actOpenable");
//		actOpenable.setAccessible(true);
//		IOpenable act = (IOpenable)actOpenable.get(dhQF);
//		act.open(null);
//		dhQF.applyChange();
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
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(jpm.getProjectName(), "ntut.csie.exceptionBadSmells", "DummyHandlerExample").getResource(), 6);
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
		Method addThrowStatement = DHQuickFix.class.getDeclaredMethod("addThrowStatement", ASTNode.class, AST.class);
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
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(jpm.getProjectName(), "ntut.csie.exceptionBadSmells", "DummyHandlerExample").getResource(), 6);
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
		
		/* ����proglem��dummy handler�����p */
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
		IOpenable actO = (IOpenable)JavaCore.create(RuntimeEnvironmentProjectReader.getType(jpm.getProjectName(), "ntut.csie.exceptionBadSmells", "DummyHandlerExample").getResource());
		actO.open(null);
		actOpenable.set(dhQF, actO);
		
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		assertNull(actRoot.get(dhQF));
		actRoot.setAccessible(true);
		actRoot.set(dhQF, unit);
		assertNotNull(actRoot.get(dhQF));
		// ��w�nquick fix��method
		dhQF.findCurrentMethod(RuntimeEnvironmentProjectReader.getType(jpm.getProjectName(), "ntut.csie.exceptionBadSmells", "DummyHandlerExample").getResource(), 6);
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
		CodeSmellAnalyzer dhVisitor = new CodeSmellAnalyzer(unit);
		currentMethodNode.accept(dhVisitor);
		Field currentExList = DHQuickFix.class.getDeclaredField("currentExList");
		currentExList.setAccessible(true);
		currentExList.set(dhQF, dhVisitor.getDummyList());
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
	 * �إ�CSPreference.xml�ɮ�
	 */
	private void CreateDummyHandlerXML() {
		//����XML��root
		Element root = JDomUtil.createXMLContent();

		//�إ�Dummy Handler��Tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//���pe.printStackTrace���Q�Ŀ�_��
		rule.setAttribute(JDomUtil.e_printstacktrace,"Y");

		//���psystem.out.println���Q�Ŀ�_��
		rule.setAttribute(JDomUtil.systemout_print,"Y");
		
		rule.setAttribute(JDomUtil.apache_log4j,"Y");
		rule.setAttribute(JDomUtil.java_Logger,"Y");

		//��ϥΪ̦ۭq��Rule�s�JXML
		Element libRule = new Element("librule");
		
		//�N�s�ت�tag�[�i�h
		dummyHandler.addContent(rule);
		dummyHandler.addContent(libRule);

		if (root.getChild(JDomUtil.DummyHandlerTag) != null)
			root.removeChild(JDomUtil.DummyHandlerTag);

		root.addContent(dummyHandler);

		//�N�ɮ׼g�^
		String path = JDomUtil.getWorkspace() + File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}
}
