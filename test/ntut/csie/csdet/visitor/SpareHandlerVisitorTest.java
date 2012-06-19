package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SpareHandlerVisitorTest {
	
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;

	public SpareHandlerVisitorTest() {
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
	public void testProcessTryStatement() throws Exception {
		// �����Ҧ�trystatement
		ASTTryCollect tryCollector = new ASTTryCollect();
		unit.accept(tryCollector);
		
		int index = 4;
		List<ASTNode> tryList = tryCollector.getMethodList();
		Method processTryStatement = SpareHandlerVisitor.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		processTryStatement.setAccessible(true);
		
		/* �M��n�Qrefactor���`�I */
		for(int i = 0; i < tryList.size(); i++) {
			SpareHandlerVisitor shVisitor = new SpareHandlerVisitor(tryList.get(index));
			processTryStatement.invoke(shVisitor, tryList.get(i));
			if(i == index)	// ���Ӹ`�I
				assertTrue(shVisitor.getResult());
			else			// �����
				assertFalse(shVisitor.getResult());
		}
	}
	
	@Test
	public void testVisitNode() throws Exception {
		// �����Ҧ�trystatement
		ASTTryCollect tryCollector = new ASTTryCollect();
		unit.accept(tryCollector);
		SpareHandlerVisitor shVisitor = new SpareHandlerVisitor(tryCollector.getMethodList().get(4));
		
		unit.accept(shVisitor);
		assertTrue(shVisitor.getResult());
	}
	
	
	/**
	 * ����TryStatement nodes 
	 * @author Crimson
	 */
	public class ASTTryCollect extends RLBaseVisitor {
		private List<ASTNode> methodList;
		public ASTTryCollect() {
			super(true);
			methodList = new ArrayList<ASTNode>();
		}
		
		protected boolean visitNode(ASTNode node) {
			try {
				switch (node.getNodeType()) {
				case ASTNode.TRY_STATEMENT:
					this.methodList.add(node);
					return true;
				default:
					return true;

				}
			} catch (Exception e) {
				return false;
			}
		}
		
		public List<ASTNode> getMethodList() {
			return methodList;
		}
	}
}
