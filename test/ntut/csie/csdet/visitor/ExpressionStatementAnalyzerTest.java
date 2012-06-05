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
		// 讀取測試檔案樣本內容
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, "test");
		
		jpm = new JavaProjectMaker("DummyHandlerTest");
		jpm.setJREDefaultContainer();
		// 新增欲載入的library
		jpm.addJarToBuildPath("lib\\log4j-1.2.15.jar");
		// 根據測試檔案樣本內容建立新的檔案
		jpm.createJavaFile("ntut.csie.exceptionBadSmells", "DummyHandlerExample.java", "package ntut.csie.exceptionBadSmells;\n" + jfs.getFileContent());
		// 建立XML
		CreateDummyHandlerXML();
		
		Path path = new Path("DummyHandlerTest\\src\\ntut\\csie\\exceptionBadSmells\\DummyHandlerExample.java");
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
	public void testAddDummyWarning() throws Exception {
		// 宣告專門收集CatchClause的class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		// 收集所有CatchClause
		unit.accept(catchCollector);
		// 取得CatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// 設定DummyHandler的條件
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
			// 從CatchClause node往下取得ExpressionStatement node
			List<?> statements = ((CatchClause)catchList.get(i)).getBody().statements();
			// 一個Statement裡面可能有複數個ExpressionStatement
			for(int j = 0; j < statements.size(); j++) {
				// 如果type是ExpressionStatement才做處理
				if(((ASTNode)statements.get(j)).getNodeType() == ASTNode.EXPRESSION_STATEMENT)
					// 然而我們真正需要的是ExpressionStatement下面的Expression node
					addDummyWarning.invoke(esVisitor, ((ExpressionStatement)statements.get(j)).getExpression());
			}
		}
		// 驗證被加入maker的有幾個Expression
		// 因為只餵ExpressionStatement Node，故throw statement不會判斷到，會多1個
		assertEquals(16 , esVisitor.getDummyHandlerList().size());
	}
	
	@Test
	public void testJudgeMethodInvocation() throws Exception {
		// 宣告專門收集CatchClause的class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		// 收集所有CatchClause
		unit.accept(catchCollector);
		// 取得CatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// 設定DummyHandler的條件
		TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
		
		/* 沒有要偵測的Library */
		ExpressionStatementAnalyzer esVisitor = new ExpressionStatementAnalyzer(libMap);
		Method judgeMethodInvocation = ExpressionStatementAnalyzer.class.getDeclaredMethod("judgeMethodInvocation", ASTNode.class);
		judgeMethodInvocation.setAccessible(true);
		// 從CatchClause node往下取得ExpressionStatement node
		List<?> statements = ((CatchClause)catchList.get(0)).getBody().statements();
		assertTrue((Boolean)judgeMethodInvocation.invoke(esVisitor, ((ExpressionStatement)statements.get(0)).getExpression()));
		
		/* 只偵測Library */
		libMap.put("org.apache.log4j", ExpressionStatementAnalyzer.LIBRARY);		
		libMap.put("java.util.logging", ExpressionStatementAnalyzer.LIBRARY);
		esVisitor = new ExpressionStatementAnalyzer(libMap);
		for(int i = 1; i < 20; i++) {
			// 從CatchClause node往下取得ExpressionStatement node
			statements = ((CatchClause)catchList.get(i)).getBody().statements();
			// 一個Statement裡面可能有複數個ExpressionStatement
			for(int j = 0; j < statements.size(); j++) {
				// 如果type是ExpressionStatement才做處理
				if(((ASTNode)statements.get(j)).getNodeType() == ASTNode.EXPRESSION_STATEMENT)
					// 然而我們真正需要的是ExpressionStatement下面的Expression node
					judgeMethodInvocation.invoke(esVisitor, ((ExpressionStatement)statements.get(j)).getExpression());
			}
		}
		// 驗證被加入maker的有幾個Expression
		assertEquals(3 , esVisitor.getDummyHandlerList().size());
		
		/* 只偵測Method */
		libMap = new TreeMap<String, Integer>();
		libMap.put("printStackTrace", ExpressionStatementAnalyzer.METHOD);
		esVisitor = new ExpressionStatementAnalyzer(libMap);
		for(int i = 1; i < 20; i++) {
			// 從CatchClause node往下取得ExpressionStatement node
			statements = ((CatchClause)catchList.get(i)).getBody().statements();
			// 一個Statement裡面可能有複數個ExpressionStatement
			for(int j = 0; j < statements.size(); j++) {
				// 如果type是ExpressionStatement才做處理
				if(((ASTNode)statements.get(j)).getNodeType() == ASTNode.EXPRESSION_STATEMENT)
					// 然而我們真正需要的是ExpressionStatement下面的Expression node
					judgeMethodInvocation.invoke(esVisitor, ((ExpressionStatement)statements.get(j)).getExpression());
			}
		}
		// 驗證被加入maker的有幾個Expression
		assertEquals(6 , esVisitor.getDummyHandlerList().size());
		
		/* 偵測Library.Method的形式 */
		libMap = new TreeMap<String, Integer>();
		libMap.put("java.io.PrintStream.println", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("java.io.PrintStream.print", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		esVisitor = new ExpressionStatementAnalyzer(libMap);
		for(int i = 1; i < 20; i++) {
			// 從CatchClause node往下取得ExpressionStatement node
			statements = ((CatchClause)catchList.get(i)).getBody().statements();
			// 一個Statement裡面可能有複數個ExpressionStatement
			for(int j = 0; j < statements.size(); j++) {
				// 如果type是ExpressionStatement才做處理
				if(((ASTNode)statements.get(j)).getNodeType() == ASTNode.EXPRESSION_STATEMENT)
					// 然而我們真正需要的是ExpressionStatement下面的Expression node
					judgeMethodInvocation.invoke(esVisitor, ((ExpressionStatement)statements.get(j)).getExpression());
			}
		}
		// 驗證被加入maker的有幾個Expression
		// 因為只餵ExpressionStatement Node，故throw statement不會判斷到，會多1個
		assertEquals(7 , esVisitor.getDummyHandlerList().size());
	}

	@Test
	public void testVisitNode() {
		// 宣告專門收集CatchClause的class
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		// 收集所有CatchClause
		unit.accept(catchCollector);
		// 取得CatchClause list
		List<ASTNode> catchList = catchCollector.getMethodList();
		// 設定DummyHandler的條件
		TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
		libMap.put("java.io.PrintStream.println", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("java.io.PrintStream.print", ExpressionStatementAnalyzer.LIBRARY_METHOD);
		libMap.put("printStackTrace", ExpressionStatementAnalyzer.METHOD);
		libMap.put("org.apache.log4j", ExpressionStatementAnalyzer.LIBRARY);		
		libMap.put("java.util.logging", ExpressionStatementAnalyzer.LIBRARY);
		
		int dummy = 0;
		// 檢查每個CatchClause中是否有Dummy bad smell
		for(ASTNode node : catchList){
			ExpressionStatementAnalyzer esVisitor = new ExpressionStatementAnalyzer(libMap);
			node.accept(esVisitor);
			if(esVisitor.getDummyHandlerList() != null)
				dummy += esVisitor.getDummyHandlerList().size();
		}
		// 驗證總共抓到幾個bad smell
		assertEquals(15, dummy);
	}

	/**
	 * 建立CSPreference.xml檔案
	 */
	private void CreateDummyHandlerXML() {
		//取的XML的root
		Element root = JDomUtil.createXMLContent();

		//建立Dummy Handler的Tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//假如e.printStackTrace有被勾選起來
		rule.setAttribute(JDomUtil.e_printstacktrace,"Y");

		//假如system.out.println有被勾選起來
		rule.setAttribute(JDomUtil.systemout_print,"Y");
		
		rule.setAttribute(JDomUtil.apache_log4j,"Y");
		rule.setAttribute(JDomUtil.java_Logger,"Y");

		//把使用者自訂的Rule存入XML
		Element libRule = new Element("librule");
		
		//將新建的tag加進去
		dummyHandler.addContent(rule);
		dummyHandler.addContent(libRule);

		if (root.getChild(JDomUtil.DummyHandlerTag) != null)
			root.removeChild(JDomUtil.DummyHandlerTag);

		root.addContent(dummyHandler);

		//將檔案寫回
		String path = JDomUtil.getWorkspace()+File.separator+"CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}
}
