package ntut.csie.robusta.codegen.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExtractMethodRefactoringTest {
	
	private String projectName = "TestProject";
	private CompilationUnit compilationUnit;
	private ExtractMethodRefactoringTestHelper helper;
	private TEFBExtractMethodRefactoring refactoring;
	
	@Before
	public void setUp() throws Exception {
		helper = new ExtractMethodRefactoringTestHelper(projectName);
		helper.InitailSettingForOnlyTEIFB();
		helper.loadClass(ThrowExceptionInFinallyBlockExampleForRefactoring.class);
		compilationUnit = helper.getCompilationUnit(ThrowExceptionInFinallyBlockExampleForRefactoring.class);
		List<MarkerInfo> list = helper.getTEIFBList(compilationUnit);
		ASTNode startNode = NodeFinder.perform(compilationUnit, list.get(0).getPosition(), 0);
		ExtractMethodAnalyzer analyzer = new ExtractMethodAnalyzer(startNode);
		//enclosing node: fileOutputStream.close()
		ASTNode enclosingNode =  analyzer.getEncloseRefactoringNode();
		refactoring = new TEFBExtractMethodRefactoring(compilationUnit, enclosingNode);
	}

	@After
	public void tearDown() throws Exception {
		helper.cleanUp();
	}
	
	@Test
	public void testCheckInitialConditions() throws Exception {
		//call checkInitialConditions to emulate the pick MarkerResolution action
		RefactoringStatus status = refactoring.checkInitialConditions(new NullProgressMonitor());
		assertTrue(status.getSeverity() == RefactoringStatus.OK);
	}
	
	@Test
	public void testCheckFinalConditions() throws Exception {
		//call checkInitialConditions to emulate the pick MarkerResolution action
		RefactoringStatus status = refactoring.checkInitialConditions(new NullProgressMonitor());
		assertTrue(status.getSeverity() == RefactoringStatus.OK);
		refactoring.checkFinalConditions(new NullProgressMonitor());
		assertTrue(status.getSeverity() == RefactoringStatus.OK);
	}

	@Test
	public void testGetSignature() throws Exception {
		//call checkInitialConditions to emulate the pick MarkerResolution action
		refactoring.checkInitialConditions(new NullProgressMonitor());
		String defualtPrivateSignature = "private void extracted(FileOutputStream fileOutputStream)";
		assertTrue(refactoring.getSignature().equals(defualtPrivateSignature));
	}

	@Test
	public void testSetNewMethodModifierType() throws Exception {
		//call checkInitialConditions to emulate the pick MarkerResolution action
		refactoring.checkInitialConditions(new NullProgressMonitor());
		
		String defualtPrivateSignature = "private void extracted(FileOutputStream fileOutputStream)";
		String publicSignature = "public void extracted(FileOutputStream fileOutputStream)";
		assertTrue(refactoring.getSignature().equals(defualtPrivateSignature));
		refactoring.setNewMethodModifierType("public");
		assertTrue(refactoring.getSignature().equals(publicSignature));
	}

	@Test
	public void testSetNewMethodName_ValidName() throws Exception {
		//call checkInitialConditions to emulate the pick MarkerResolution action
		refactoring.checkInitialConditions(new NullProgressMonitor());
		
		//For default it is "extracted"
		assertTrue(refactoring.getMethodName().equals("extracted"));
		RefactoringStatus  status = refactoring.setNewMethodName("newMethodName");
		//No Error
		assertTrue(status.getSeverity() == RefactoringStatus.OK);
		assertTrue(refactoring.getMethodName().equals("newMethodName"));
		
		status = refactoring.setNewMethodName("_");
		assertTrue(status.getSeverity() == RefactoringStatus.OK);
		
		status = refactoring.setNewMethodName("_1methodName");
		assertTrue(status.getSeverity() == RefactoringStatus.OK);
		
		status = refactoring.setNewMethodName("method_Name12");
		assertTrue(status.getSeverity() == RefactoringStatus.OK);
	}
	
	@Test
	public void testSetNewMethodName_InvalidNameWithNumber() throws Exception {
		//call checkInitialConditions to emulate the pick MarkerResolution action
		refactoring.checkInitialConditions(new NullProgressMonitor());
		
		//For default it is "extracted"
		assertTrue(refactoring.getMethodName().equals("extracted"));
		RefactoringStatus  status = refactoring.setNewMethodName("12InvalidNameWithNumber");
		assertTrue(status.getSeverity() == RefactoringStatus.FATAL);
	}
	
	@Test
	public void testSetNewMethodName_InvalidNameWithNonCharacter() throws Exception {
		//call checkInitialConditions to emulate the pick MarkerResolution action
		refactoring.checkInitialConditions(new NullProgressMonitor());
		
		//For default it is "extracted"
		assertTrue(refactoring.getMethodName().equals("extracted"));
		RefactoringStatus  status = refactoring.setNewMethodName("@Invalid_Name");
		assertTrue(status.getSeverity() == RefactoringStatus.FATAL);
		status = refactoring.setNewMethodName("Invalid%Name");
		assertTrue(status.getSeverity() == RefactoringStatus.FATAL);
	}
	
	@Test
	public void testApplyChange() throws Exception {
		//Before refactoring
		CompilationUnit beforeRefactoring = helper.getCompilationUnit(ThrowExceptionInFinallyBlockExampleForRefactoring.class);
		List<MarkerInfo> markerInfos = helper.getTEIFBList(beforeRefactoring);
		assertEquals(3, markerInfos.size());
		
		//call checkInitialConditions to emulate the pick MarkerResolution action
		refactoring.checkInitialConditions(new NullProgressMonitor());
		
		//Set params
		refactoring.setMethodName("extractedMethod");
		refactoring.setNewMethodModifierType("private");
		refactoring.setNewMethodLogType("e.printStackTrace();");
		
		//Perform change: apply the refactoring
		CompilationUnitChange change = (CompilationUnitChange)refactoring.createChange(new NullProgressMonitor());
		change.perform(new NullProgressMonitor());
		
		//After refactoring
		CompilationUnit afterRefactoring = helper.getCompilationUnit(ThrowExceptionInFinallyBlockExampleForRefactoring.class);
		markerInfos = helper.getTEIFBList(afterRefactoring);
		assertEquals(2, markerInfos.size());
	}
}
