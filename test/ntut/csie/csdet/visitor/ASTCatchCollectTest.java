package ntut.csie.csdet.visitor;

import java.util.List;

import junit.framework.TestCase;
import ntut.csie.csdet.fixture.FixtureManager;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
/**
 * 測試去讀取一個MM.java的檔案中catch的數量
 * @author chewei
 */

public class ASTCatchCollectTest extends TestCase {
	FixtureManager fm;
	IType type;
	ASTCatchCollect catchCollector;
	
	protected void setUp() throws Exception {
		super.setUp();
		fm = new FixtureManager();
		fm.createProject("MM");
		type = fm.getProject().getIType("a.b.c.MM");
		catchCollector = new ASTCatchCollect();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		fm.dispose();
	}

	public void testASTCatchCollect() {
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
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		int catchSize = 0;
		for(MethodDeclaration method : methodList){
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			method.accept(catchCollector);
			catchSize += catchCollector.getMethodList().size();
		}
		assertEquals(6,catchSize);
	}

}
