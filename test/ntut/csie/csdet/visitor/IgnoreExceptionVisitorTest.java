package ntut.csie.csdet.visitor;


import static org.junit.Assert.assertEquals;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IgnoreExceptionVisitorTest {
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;
	IgnoreExceptionVisitor ignoreExceptionBSV;
	
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
		ignoreExceptionBSV = new IgnoreExceptionVisitor(unit);
	}

	@After
	public void tearDown() throws Exception {
		jpm.deleteProject();
	}

	@Test
	public void testVisitNode() {
		int ignore = 0;
		unit.accept(ignoreExceptionBSV);
		if(ignoreExceptionBSV.getIgnoreList() != null)
			ignore = ignoreExceptionBSV.getIgnoreList().size();
		
		// �����`�@���X��bad smell
		assertEquals(1, ignore);
	}
}
