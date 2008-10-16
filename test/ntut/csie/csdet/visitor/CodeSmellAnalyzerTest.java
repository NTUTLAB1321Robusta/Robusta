package ntut.csie.csdet.visitor;

import java.util.List;

import ntut.csie.csdet.fixture.FixtureManager;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import junit.framework.TestCase;

/**
 * 測試找出Ignore Exception的code smell個數
 * @author chewei
 */

public class CodeSmellAnalyzerTest extends TestCase {
	FixtureManager fm;
	IType type;
	
	
	protected void setUp() throws Exception {
		super.setUp();
		fm = new FixtureManager();
		fm.createProject("MM");
		type = fm.getProject().getIType("a.b.c.MM");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		fm.dispose();
	}

	public void testGetIgnoreExList() {
		IResource resource =  type.getResource();
		IJavaElement javaElement = JavaCore.create(resource);
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource((ICompilationUnit)javaElement);
		parser.setResolveBindings(true);
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		CompilationUnit unit = (CompilationUnit) parser.createAST(null);
		unit.accept(methodCollector);
		List<ASTNode> methodList = methodCollector.getMethodList();
		int ignoreEx = 0;
		for(ASTNode method : methodList){
			CodeSmellAnalyzer csVisitor = new CodeSmellAnalyzer(unit);
			method.accept(csVisitor);
			ignoreEx += csVisitor.getIgnoreExList().size();
		}
		assertEquals(4,ignoreEx);
	}	

}
