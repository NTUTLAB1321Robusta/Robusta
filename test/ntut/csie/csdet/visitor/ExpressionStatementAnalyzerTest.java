package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;

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

public class ExpressionStatementAnalyzerTest {
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
	public void testAddDummyWarning() throws Exception {
		// �ŧi�M������CatchClause��class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		// �����Ҧ�CatchClause
		unit.accept(catchCollector);
		// ���oCatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// �]�wDummyHandler������
		TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
		libMap.put("java.io.PrintStream.println", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("java.io.PrintStream.print", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("printStackTrace", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("org.apache.log4j", ExpressionStatementAnalyzer.LIBRARY);		
		libMap.put("java.util.logging", ExpressionStatementAnalyzer.LIBRARY);
		
		ExpressionStatementAnalyzer esVisitor = new ExpressionStatementAnalyzer(libMap);
		Method addDummyWarning = ExpressionStatementAnalyzer.class.getDeclaredMethod("addDummyWarning", ASTNode.class);
		addDummyWarning.setAccessible(true);
		for(int i = 1; i < 20; i++) {
			// �qCatchClause node���U���oExpressionStatement node
			List<?> statements = ((CatchClause)catchList.get(i)).getBody().statements();
			// �@��Statement�̭��i�঳�Ƽƭ�ExpressionStatement
			for(int j = 0; j < statements.size(); j++) {
				// �p�Gtype�OExpressionStatement�~���B�z
				if(((ASTNode)statements.get(j)).getNodeType() == ASTNode.EXPRESSION_STATEMENT)
					// �M�ӧڭ̯u���ݭn���OExpressionStatement�U����Expression node
					addDummyWarning.invoke(esVisitor, ((ExpressionStatement)statements.get(j)).getExpression());
			}
		}
		// ���ҳQ�[�Jmaker�����X��Expression
		// �]���u��ExpressionStatement Node�A�Gthrow statement���|�P�_��A�|�h1��
		assertEquals(16 , esVisitor.getDummyHandlerList().size());
	}
	
	@Test
	public void testJudgeMethodInvocation() throws Exception {
		// �ŧi�M������CatchClause��class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		// �����Ҧ�CatchClause
		unit.accept(catchCollector);
		// ���oCatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// �]�wDummyHandler������
		TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
		
		/* �S���n������Library */
		ExpressionStatementAnalyzer esVisitor = new ExpressionStatementAnalyzer(libMap);
		Method judgeMethodInvocation = ExpressionStatementAnalyzer.class.getDeclaredMethod("judgeMethodInvocation", ASTNode.class);
		judgeMethodInvocation.setAccessible(true);
		// �qCatchClause node���U���oExpressionStatement node
		List<?> statements = ((CatchClause)catchList.get(0)).getBody().statements();
		assertTrue((Boolean)judgeMethodInvocation.invoke(esVisitor, ((ExpressionStatement)statements.get(0)).getExpression()));
		
		/* �u����Library */
		libMap.put("org.apache.log4j", ExpressionStatementAnalyzer.LIBRARY);		
		libMap.put("java.util.logging", ExpressionStatementAnalyzer.LIBRARY);
		esVisitor = new ExpressionStatementAnalyzer(libMap);
		for(int i = 1; i < 20; i++) {
			// �qCatchClause node���U���oExpressionStatement node
			statements = ((CatchClause)catchList.get(i)).getBody().statements();
			// �@��Statement�̭��i�঳�Ƽƭ�ExpressionStatement
			for(int j = 0; j < statements.size(); j++) {
				// �p�Gtype�OExpressionStatement�~���B�z
				if(((ASTNode)statements.get(j)).getNodeType() == ASTNode.EXPRESSION_STATEMENT)
					// �M�ӧڭ̯u���ݭn���OExpressionStatement�U����Expression node
					judgeMethodInvocation.invoke(esVisitor, ((ExpressionStatement)statements.get(j)).getExpression());
			}
		}
		// ���ҳQ�[�Jmaker�����X��Expression
		assertEquals(3 , esVisitor.getDummyHandlerList().size());
		
		/* �u����Method */
		libMap = new TreeMap<String, Integer>();
		libMap.put("printStackTrace", ExpressionStatementAnalyzer.METHOD);
		esVisitor = new ExpressionStatementAnalyzer(libMap);
		for(int i = 1; i < 20; i++) {
			// �qCatchClause node���U���oExpressionStatement node
			statements = ((CatchClause)catchList.get(i)).getBody().statements();
			// �@��Statement�̭��i�঳�Ƽƭ�ExpressionStatement
			for(int j = 0; j < statements.size(); j++) {
				// �p�Gtype�OExpressionStatement�~���B�z
				if(((ASTNode)statements.get(j)).getNodeType() == ASTNode.EXPRESSION_STATEMENT)
					// �M�ӧڭ̯u���ݭn���OExpressionStatement�U����Expression node
					judgeMethodInvocation.invoke(esVisitor, ((ExpressionStatement)statements.get(j)).getExpression());
			}
		}
		// ���ҳQ�[�Jmaker�����X��Expression
		assertEquals(6 , esVisitor.getDummyHandlerList().size());
		
		/* ����Library.Method���Φ� */
		libMap = new TreeMap<String, Integer>();
		libMap.put("java.io.PrintStream.println", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("java.io.PrintStream.print", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		esVisitor = new ExpressionStatementAnalyzer(libMap);
		for(int i = 1; i < 20; i++) {
			// �qCatchClause node���U���oExpressionStatement node
			statements = ((CatchClause)catchList.get(i)).getBody().statements();
			// �@��Statement�̭��i�঳�Ƽƭ�ExpressionStatement
			for(int j = 0; j < statements.size(); j++) {
				// �p�Gtype�OExpressionStatement�~���B�z
				if(((ASTNode)statements.get(j)).getNodeType() == ASTNode.EXPRESSION_STATEMENT)
					// �M�ӧڭ̯u���ݭn���OExpressionStatement�U����Expression node
					judgeMethodInvocation.invoke(esVisitor, ((ExpressionStatement)statements.get(j)).getExpression());
			}
		}
		// ���ҳQ�[�Jmaker�����X��Expression
		// �]���u��ExpressionStatement Node�A�Gthrow statement���|�P�_��A�|�h1��
		assertEquals(7 , esVisitor.getDummyHandlerList().size());
	}

	@Test
	public void testVisitNode() {
		// �ŧi�M������CatchClause��class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		// �����Ҧ�CatchClause
		unit.accept(catchCollector);
		// ���oCatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// �]�wDummyHandler������
		TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
		libMap.put("java.io.PrintStream.println", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("java.io.PrintStream.print", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("printStackTrace", ExpressionStatementAnalyzer.METHOD);
		libMap.put("org.apache.log4j", ExpressionStatementAnalyzer.LIBRARY);		
		libMap.put("java.util.logging", ExpressionStatementAnalyzer.LIBRARY);
		
		int dummy = 0;
		// �ˬd�C��CatchClause���O�_��Dummy bad smell
		for(ASTNode node : catchList){
			ExpressionStatementAnalyzer esVisitor = new ExpressionStatementAnalyzer(libMap);
			node.accept(esVisitor);
			if(esVisitor.getDummyHandlerList() != null)
				dummy += esVisitor.getDummyHandlerList().size();
		}
		// �����`�@���X��bad smell
		assertEquals(15, dummy);
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
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}
}
