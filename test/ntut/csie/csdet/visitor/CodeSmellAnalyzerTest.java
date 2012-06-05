package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CodeSmellAnalyzerTest {
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;
	
	public CodeSmellAnalyzerTest() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		// Ū�������ɮ׼˥����e
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, "test");
		
		jpm = new JavaProjectMaker("DummyHandlerTest");
		jpm.setJREDefaultContainer();
		// �s�W�����J��library
		jpm.addJarToBuildPath("lib\\log4j-1.2.15.jar");
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
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		jpm.deleteProject();
	}
	
	@Test
	public void testGetDummySetting() throws Exception {
		CodeSmellAnalyzer csVisitor = new CodeSmellAnalyzer(unit);
		Method getDummySettings = CodeSmellAnalyzer.class.getDeclaredMethod("getDummySettings");
		getDummySettings.setAccessible(true);
		getDummySettings.invoke(csVisitor);
		Field field = CodeSmellAnalyzer.class.getDeclaredField("libMap");
		field.setAccessible(true);
		TreeMap<String, Integer> libMap = (TreeMap<String, Integer>)field.get(csVisitor);
		assertEquals(5, libMap.size());
	}
	
	@Test
	public void testAddDummyMessage() throws Exception {
		Method addDummymessage = CodeSmellAnalyzer.class.getDeclaredMethod("addDummyMessage", CatchClause.class, ExpressionStatement.class);
		addDummymessage.setAccessible(true);
		// �ŧi�M������CatchClause��class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		unit.accept(catchCollector);
		// ���oCatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// �]�wDummyHandler������
		TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
		libMap.put("java.io.PrintStream.println", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("java.io.PrintStream.print", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("printStackTrace", ExpressionStatementAnalyzer.METHOD);
		libMap.put("org.apache.log4j", ExpressionStatementAnalyzer.LIBRARY);		
		// �إ�Dummy handler��T
		CodeSmellAnalyzer csVisitor = new CodeSmellAnalyzer(unit);
		for(int i = 0; i < catchList.size(); i++) {
			ExpressionStatementAnalyzer esVisitor = new ExpressionStatementAnalyzer(libMap);
			catchList.get(i).accept(esVisitor);
			if(esVisitor.getDummyHandlerList() != null) {
				for(int j = 0; j < esVisitor.getDummyHandlerList().size(); j++) {
					addDummymessage.invoke(csVisitor, catchList.get(i), esVisitor.getDummyHandlerList().get(j));
				}
			}
		}
		// ���Ҹ�T�O�_�ŦX�w��
		assertEquals(13, csVisitor.getDummyList().size());
		// ��method�u�إ�dummy message�A�Gignore message���|������F��
		assertEquals(0, csVisitor.getIgnoreExList().size());
	}
	
	@Test
	public void testJudgeDummyHandler() throws Exception {
		Method judgeDummyHandler = CodeSmellAnalyzer.class.getDeclaredMethod("judgeDummyHandler", CatchClause.class);
		judgeDummyHandler.setAccessible(true);
		// �ŧi�M������CatchClause��class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		unit.accept(catchCollector);
		// ���oCatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// �P�_�O�_��dummy handler
		CodeSmellAnalyzer csVisitor = new CodeSmellAnalyzer(unit);
		for(int i = 0; i < catchList.size(); i++) {
			judgeDummyHandler.invoke(csVisitor, catchList.get(i));
		}
		// ���Ҹ�T�O�_�ŦX�w��
		assertEquals(15, csVisitor.getDummyList().size());
		// ��method�u�إ�dummy message�A�Gignore message���|������F��
		assertEquals(0, csVisitor.getIgnoreExList().size());
	}
	
	@Test
	public void testJudgeIgnoreEx() throws Exception {
		Method judgeIgnoreEx = CodeSmellAnalyzer.class.getDeclaredMethod("judgeIgnoreEx", CatchClause.class);
		judgeIgnoreEx.setAccessible(true);
		// �ŧi�M������CatchClause��class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		unit.accept(catchCollector);
		// ���oCatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// �P�_�O�_��ignore or dummy handler
		CodeSmellAnalyzer csVisitor = new CodeSmellAnalyzer(unit);
		for(int i = 0; i < catchList.size(); i++) {
			judgeIgnoreEx.invoke(csVisitor, catchList.get(i));
		}
		// ���Ҹ�T�O�_�ŦX�w��
		assertEquals(15, csVisitor.getDummyList().size());
		assertEquals(1, csVisitor.getIgnoreExList().size());
	}
	
	@Test
	public void testVisitNode_IgnoreExList() {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit.accept(methodCollector);
		List<ASTNode> methodList = methodCollector.getMethodList();
		int ignoreEx = 0;
		for(ASTNode method : methodList){
			CodeSmellAnalyzer csVisitor = new CodeSmellAnalyzer(unit);
			method.accept(csVisitor);
			ignoreEx += csVisitor.getIgnoreExList().size();
		}
		assertEquals(1, ignoreEx);
	}

	@Test
	public void testVisitNode_DummyHandler() {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit.accept(methodCollector);
		List<ASTNode> methodList = methodCollector.getMethodList();
		int dummy = 0;
		for(ASTNode method : methodList){
			CodeSmellAnalyzer csVisitor = new CodeSmellAnalyzer(unit);
			method.accept(csVisitor);
			dummy += csVisitor.getDummyList().size();
		}
		assertEquals(15,dummy);
	}
	
	/**
	 * �إ�CSPreference.xml�ɮ�
	 */
	public void CreateDummyHandlerXML() {
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
