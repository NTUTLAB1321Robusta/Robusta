package ntut.csie.csdet.visitor;

import junit.framework.TestCase;
import ntut.csie.csdet.fixture.FixtureManager;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class MainAnalyzerTest extends TestCase {
	FixtureManager fm;
	IType type;
	MainAnalyzer mainAnalyzer;
	
	protected void setUp() throws Exception {
		super.setUp();
		fm = new FixtureManager();
		fm.createProject("MM");
		type = fm.getProject().getIType("a.b.c.MainFixture");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		fm.dispose();
	}

	public void testGetUnprotedMainList() {
		IResource resource =  type.getResource();
		IJavaElement javaElement = JavaCore.create(resource);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource((ICompilationUnit)javaElement);
		parser.setResolveBindings(true);
		CompilationUnit unit = (CompilationUnit) parser.createAST(null);
		mainAnalyzer = new MainAnalyzer(unit);
		unit.accept(mainAnalyzer);
		assertEquals(1,mainAnalyzer.getUnprotedMainList().size());
	}

}
