package ntut.csie.csdet.refactor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.filemaker.exceptionBadSmells.NestedTryStatementExample;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class RetryRefactoringTest {
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit dummyAndIgnoredExampleUnit, nestedTryStatementUnit;
	String testProjectName;
	Path dummyAndIgnoredExamplePath, nestedTryStatementExamplePath;
	
	public RetryRefactoringTest() {
		testProjectName = "RetryRefactoringTestProject";
		dummyAndIgnoredExamplePath = new Path(testProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(DummyAndIgnoreExample.class.getName())
				+ JavaProjectMaker.JAVA_FILE_EXTENSION);
		nestedTryStatementExamplePath = new Path(testProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(NestedTryStatementExample.class.getName())
				+ JavaProjectMaker.JAVA_FILE_EXTENSION);
	}

	@Before
	public void setUp() throws Exception {
		// Ū�������ɮ׼˥����e
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		jpm = new JavaProjectMaker(testProjectName);
		jpm.setJREDefaultContainer();
		// �s�W�����J��library
		jpm.addJarFromProjectToBuildPath("lib/log4j-1.2.15.jar");
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		jpm.createJavaFile(DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName(),
				"package " + DummyAndIgnoreExample.class.getPackage().getName()
				+ ";\n" + jfs.getFileContent());
		// �إ�Nested try block example file
		jfs = new JavaFileToString();
		jfs.read(NestedTryStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		jpm.createJavaFile(NestedTryStatementExample.class.getPackage().getName(),
				NestedTryStatementExample.class.getSimpleName(),
				"package " + NestedTryStatementExample.class.getPackage().getName()
				+ ";\n" + jfs.getFileContent());
		
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyAndIgnoredExamplePath)));
		parser.setResolveBindings(true);
		// ���oAST
		dummyAndIgnoredExampleUnit = (CompilationUnit) parser.createAST(null); 
		dummyAndIgnoredExampleUnit.recordModifications();
		
		// Create the other AST to parse 
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(nestedTryStatementExamplePath)));
		parser.setResolveBindings(true);
		nestedTryStatementUnit = (CompilationUnit) parser.createAST(null); 
		nestedTryStatementUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		// �R���M��
		jpm.deleteProject();
	}
	
	@Test
	public void testSetExceptionName() throws Exception {
		RetryRefactoring refactoring = new RetryRefactoring(null, null, null, null);
		Field exceptionType = RetryRefactoring.class.getDeclaredField("exceptionType");
		exceptionType.setAccessible(true);
		assertNull(exceptionType.get(refactoring));
		/** �Y��J���Ŧr��ɡA�n�^�ǿ��~�����A��T */
		String name = "";
		RefactoringStatus result = refactoring.setExceptionName(name);
		assertEquals(	"<FATALERROR\n" +
						"\t\n" + 
						"FATALERROR: Some Field is empty\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n" +
						">", result.toString());
		assertNull(exceptionType.get(refactoring));
		/** �Y��J���T�A�h���A��OK */
		name = JavaProjectMaker.FOLDERNAME_TEST;
		result = refactoring.setExceptionName(name);
		assertEquals("<OK\n>", result.toString());
		assertEquals(JavaProjectMaker.FOLDERNAME_TEST, exceptionType.get(refactoring));
	}
	
	@Test
	public void testSetRetryVariable() throws Exception {
		RetryRefactoring refactoring = new RetryRefactoring(null, null, null, null);
		Field retry = RetryRefactoring.class.getDeclaredField("retry");
		retry.setAccessible(true);
		assertNull(retry.get(refactoring));
		/** �Y��J���Ŧr��ɡA�n�^�ǿ��~�����A��T */
		String name = "";
		RefactoringStatus result = refactoring.setRetryVariable(name);
		assertEquals(	"<FATALERROR\n" +
						"\t\n" + 
						"FATALERROR: Some Field is empty\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n" +
						">", result.toString());
		assertNull(retry.get(refactoring));
		/** �Y��J���T�A�h���A��OK */
		name = "test";
		result = refactoring.setRetryVariable(name);
		assertEquals("<OK\n>", result.toString());
		assertEquals("test", retry.get(refactoring));
	}
	
	@Test
	public void testSetMaxAttemptNum() throws Exception {
		RetryRefactoring refactoring = new RetryRefactoring(null, null, null, null);
		Field maxNum = RetryRefactoring.class.getDeclaredField("maxNum");
		maxNum.setAccessible(true);
		assertNull(maxNum.get(refactoring));
		/** �Y��J���Ŧr��ɡA�n�^�ǿ��~�����A��T */
		String num = "";
		RefactoringStatus result = refactoring.setMaxAttemptNum(num);
		assertEquals(	"<FATALERROR\n" +
						"\t\n" + 
						"FATALERROR: Some Field is empty\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n" +
						">", result.toString());
		assertNull(maxNum.get(refactoring));
		/** �Y��J���T�A�h���A��OK */
		num = "3";
		result = refactoring.setMaxAttemptNum(num);
		assertEquals("<OK\n>", result.toString());
		assertEquals("3", maxNum.get(refactoring));
	}
	
	@Test
	public void testSetMaxAttemptVariable() throws Exception {
		RetryRefactoring refactoring = new RetryRefactoring(null, null, null, null);
		Field maxAttempt = RetryRefactoring.class.getDeclaredField("maxAttempt");
		maxAttempt.setAccessible(true);
		assertNull(maxAttempt.get(refactoring));
		/** �Y��J���Ŧr��ɡA�n�^�ǿ��~�����A��T */
		String attempt = "";
		RefactoringStatus result = refactoring.setMaxAttemptVariable(attempt);
		assertEquals(	"<FATALERROR\n" +
						"\t\n" + 
						"FATALERROR: Some Field is empty\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n" +
						">", result.toString());
		assertNull(maxAttempt.get(refactoring));
		/** �Y��J���T�A�h���A��OK */
		attempt = "3";
		result = refactoring.setMaxAttemptVariable(attempt);
		assertEquals("<OK\n>", result.toString());
		assertEquals("3", maxAttempt.get(refactoring));
	}
	
	@Test
	public void testSetAttemptVariable() throws Exception {
		RetryRefactoring refactoring = new RetryRefactoring(null, null, null, null);
		Field attempt = RetryRefactoring.class.getDeclaredField("attempt");
		attempt.setAccessible(true);
		assertNull(attempt.get(refactoring));
		/** �Y��J���Ŧr��ɡA�n�^�ǿ��~�����A��T */
		String string = "";
		RefactoringStatus result = refactoring.setAttemptVariable(string);
		assertEquals(	"<FATALERROR\n" +
						"\t\n" + 
						"FATALERROR: Some Field is empty\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n" +
						">", result.toString());
		assertNull(attempt.get(refactoring));
		/** �Y��J���T�A�h���A��OK */
		string = "3";
		result = refactoring.setAttemptVariable(string);
		assertEquals("<OK\n>", result.toString());
		assertEquals("3", attempt.get(refactoring));
	}
	
	@Test
	public void testCheckInitialConditions() throws Exception {
		/* ����d�� > 0 */
		String retry_type = "Retry_with_original";
		Document document = new Document(
				"try {" + 
				"fis = new FileInputStream(\"\");" + 
				"fis.read();" + 
				"} catch (IOException e) {" + 
				"javaLog.log(Level.INFO, \"Just log it.\");	//	DummyHandler" + 
				"}");
		TextSelection textSelection = new TextSelection(document, 3069, 153);
		// ���o�M�פU��java��
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.open(null);
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyAndIgnoredExamplePath));
		// �]�w�ݴ��ؼ� RetryRefactoring
		IProgressMonitor pm = null;
		RetryRefactoring refactoring = new RetryRefactoring(javaProject,javaElement,textSelection,retry_type);
		Method checkInitialConditions = RetryRefactoring.class.getDeclaredMethod("checkInitialConditions", IProgressMonitor.class);
		checkInitialConditions.setAccessible(true);
		assertEquals("<OK\n>", ((RefactoringStatus)checkInitialConditions.invoke(refactoring, pm)).toString());
		
		/* ����d�� <= 0 */
		textSelection = new TextSelection(document, -1, 0);
		Field iTSelection = RetryRefactoring.class.getDeclaredField("iTSelection");
		iTSelection.setAccessible(true);
		iTSelection.set(refactoring, textSelection);
		assertEquals(	"<FATALERROR\n\t\n" +
						"FATALERROR: Selection Error, please retry again!!!\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" + 
						"Data: null\n" +
						">", ((RefactoringStatus)checkInitialConditions.invoke(refactoring, pm)).toString());
	}
	
	@Test
	public void testCreateChange() throws Exception {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyAndIgnoredExamplePath));
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, javaElement, null, null);
		// �]�w���իe�ݭn�Ψ쪺�ܼƤ��e
		ASTRewrite rewrite = ASTRewrite.create(dummyAndIgnoredExampleUnit.getAST());
		ICompilationUnit cu = (ICompilationUnit) javaElement;
		Document document = new Document(cu.getBuffer().getContents());	
		TextEdit edits = rewrite.rewriteAST(document,null);
		TextFileChange textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
		textFileChange.setEdit(edits);
		Field textFile = RetryRefactoring.class.getDeclaredField("textFileChange");
		textFile.setAccessible(true);
		textFile.set(retryRefactoring, textFileChange);
		// ������եؼ�
		Method createChange = RetryRefactoring.class.getDeclaredMethod("createChange", IProgressMonitor.class);
		createChange.setAccessible(true);
		IProgressMonitor pm = null;
		CompilationUnitChange result = (CompilationUnitChange)createChange.invoke(retryRefactoring, pm);
		// ���ҵ��G
		assertEquals("Introduce resourceful try clause", result.getName());
		assertEquals(TextFileChange.KEEP_SAVE_STATE, result.getSaveMode());
		assertEquals("{MultiTextEdit} [0,0] [undefined]", result.getEdit().toString());
	}
	
	
	@Test
	public void testAddImportRLDeclaration() throws Exception {
		// �]�w���ո��
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyAndIgnoredExamplePath));
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, javaElement, null, null);
		Field actRoot = RetryRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(retryRefactoring, dummyAndIgnoredExampleUnit);
		
		Field rewrite = RetryRefactoring.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		ASTRewrite rw = ASTRewrite.create(dummyAndIgnoredExampleUnit.getAST());
		rewrite.set(retryRefactoring, rw);
		// ���Ҫ�l���A
		List<?> importList = rw.getListRewrite(dummyAndIgnoredExampleUnit, CompilationUnit.IMPORTS_PROPERTY).getRewrittenList();
		assertEquals(6, importList.size());
		assertEquals("import java.io.FileInputStream;\n", importList.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", importList.get(1).toString());
		assertEquals("import java.io.IOException;\n", importList.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", importList.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", importList.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", importList.get(5).toString());
		
		// ������չ�H
		Method addImportRLDeclaration = RetryRefactoring.class.getDeclaredMethod("addImportRLDeclaration");
		addImportRLDeclaration.setAccessible(true);
		addImportRLDeclaration.invoke(retryRefactoring);
		
		// ���ҵ��G
		rw = (ASTRewrite)rewrite.get(retryRefactoring);
		importList = rw.getListRewrite(dummyAndIgnoredExampleUnit, CompilationUnit.IMPORTS_PROPERTY).getRewrittenList();
		assertEquals(8, importList.size());
		assertEquals("import java.io.FileInputStream;\n", importList.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", importList.get(1).toString());
		assertEquals("import java.io.IOException;\n", importList.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", importList.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", importList.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", importList.get(5).toString());
		assertEquals("import agile.exception.Robustness;\n", importList.get(6).toString());
		assertEquals("import agile.exception.RL;\n", importList.get(7).toString());
	}
	
	@Test
	public void testGetRLAnnotation() throws Exception {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyAndIgnoredExamplePath));
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, javaElement, null, null);
		
		Method getRLAnnotation = RetryRefactoring.class.getDeclaredMethod("getRLAnnotation", AST.class, int.class, String.class);
		getRLAnnotation.setAccessible(true);
		// ���ը����ҵ��G
		NormalAnnotation annotation = (NormalAnnotation)getRLAnnotation.invoke(retryRefactoring, dummyAndIgnoredExampleUnit.getAST(), 3, "RuntimeException");
		assertEquals("@RL(level=3,exception=RuntimeException.class)", annotation.toString());
	}
	
//	@Test
	public void testAddAnnotationRoot() throws Exception {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyAndIgnoredExamplePath));
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, javaElement, null, null);
		
		/* �]�w���հѼ� */
		Field exceptionType = RetryRefactoring.class.getDeclaredField("exceptionType");
		exceptionType.setAccessible(true);
		exceptionType.set(retryRefactoring, "RuntimeException");
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		dummyAndIgnoredExampleUnit.accept(methodCollector);
		
		Field currentMethodNode = RetryRefactoring.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		List<ASTNode> methodList = methodCollector.getMethodList();
		currentMethodNode.set(retryRefactoring, methodList.get(6));
		
		Field currentMethodRLList = RetryRefactoring.class.getDeclaredField("currentMethodRLList");
		currentMethodRLList.setAccessible(true);
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(dummyAndIgnoredExampleUnit, methodList.get(6).getStartPosition(), 0);
		methodList.get(6).accept(exVisitor);
		currentMethodRLList.set(retryRefactoring, exVisitor.getMethodRLAnnotationList());
		assertEquals(0, exVisitor.getMethodRLAnnotationList().size());
		
		Field rewrite = RetryRefactoring.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(retryRefactoring, ASTRewrite.create(dummyAndIgnoredExampleUnit.getAST()));
		
		Field actRoot = RetryRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(retryRefactoring, dummyAndIgnoredExampleUnit);
		
		/* �ˬdprecondition */
		List<?> modifiers = ((MethodDeclaration)methodList.get(6)).modifiers();
		assertEquals(1, modifiers.size());
		assertEquals("public", modifiers.get(0).toString());
		/* ���եؼ� */
		Method addAnnotationRoot = RetryRefactoring.class.getDeclaredMethod("addAnnotationRoot", AST.class);
		addAnnotationRoot.setAccessible(true);
		addAnnotationRoot.invoke(retryRefactoring, dummyAndIgnoredExampleUnit.getAST());
		/* ���ҵ��G */
		/*FIXME - �Qrefactoring��method�W�����ӥX�{RL annotation�Aclass�W�誺import���ӥX�{Robustness�MRL�~��*/
		modifiers = ((MethodDeclaration)methodList.get(6)).modifiers();
		assertEquals(1, modifiers.size());
		assertEquals("���Ӧ�6��imports�A�g�Lrefactoring�H��A���Ӧ�8��imports�~��", 8, ((CompilationUnit)actRoot.get(retryRefactoring)).imports().size());
	}
	
	@Test
	public void testAddImportDeclaration() throws Exception {
		/* �ѼƳ]�w */
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, null);
		Field actRoot = RetryRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(retryRefactoring, dummyAndIgnoredExampleUnit);
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IType exType = JavaCore.create(project).findType("java.io.IOException");
		retryRefactoring.setExType(exType);
		
		ASTRewrite rw = ASTRewrite.create(dummyAndIgnoredExampleUnit.getAST());
		Field rewrite = RetryRefactoring.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(retryRefactoring, rw);
		
		/** �����w�s�b��import�h������import */
		
		/* �ˬd�ثe���A */
		List<?> importList = rw.getListRewrite(dummyAndIgnoredExampleUnit, CompilationUnit.IMPORTS_PROPERTY).getRewrittenList();
		assertEquals(6, importList.size());
		
		/* ������չ�H */
		Method addImportDeclaration = RetryRefactoring.class.getDeclaredMethod("addImportDeclaration");
		addImportDeclaration.setAccessible(true);
		addImportDeclaration.invoke(retryRefactoring);
		
		/* ���ҵ��G */
		rw = (ASTRewrite)rewrite.get(retryRefactoring);
		importList = rw.getListRewrite(dummyAndIgnoredExampleUnit, CompilationUnit.IMPORTS_PROPERTY).getRewrittenList();
		assertEquals(6, importList.size());
		
		/** �����s��import�h����import */
		exType = JavaCore.create(project).findType("java.io.IOError");
		retryRefactoring.setExType(exType);
		addImportDeclaration.invoke(retryRefactoring);
		
		/* ���ҵ��G */
		rw = (ASTRewrite)rewrite.get(retryRefactoring);
		importList = rw.getListRewrite(dummyAndIgnoredExampleUnit, CompilationUnit.IMPORTS_PROPERTY).getRewrittenList();
		assertEquals(7, importList.size());
	}
	
	@Test
	public void testFindHomogeneousExType_InnerTryStatementHoldHigherLevelExceptionType() throws Exception {
		/* �]�w�Ѽ� */
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, null);
		Method findHomogeneousExType = RetryRefactoring.class.getDeclaredMethod("findHomogeneousExType", AST.class, SingleVariableDeclaration.class, Object.class);
		findHomogeneousExType.setAccessible(true);
		
		TryStatement tryStatement = ASTNodeFinder
				.getTryStatementNodeListByMethodDeclarationName(nestedTryStatementUnit, "nestedCatch_InnerCatchWithParentExceptionTypeOfOuter").get(0);
		
		TryStatement tryStatementInCatchClause = ASTNodeFinder.getTryStatementInNestedTryStatement(tryStatement, ASTNode.CATCH_CLAUSE).get(0);
		
		SingleVariableDeclaration svd1 = (SingleVariableDeclaration) ((CatchClause) tryStatement
				.catchClauses().get(0))
				.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		String lastExType = (String) findHomogeneousExType.invoke(retryRefactoring,
				nestedTryStatementUnit.getAST(), svd1, tryStatementInCatchClause);
		assertEquals("InterruptedIOException", lastExType);
	}

	@Test
	public void testFindHomogeneousExType_InnerTryStatementHoldLowerLevelExceptionType() throws Exception {
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, null);
		Method findHomogeneousExType = RetryRefactoring.class.getDeclaredMethod("findHomogeneousExType", AST.class, SingleVariableDeclaration.class, Object.class);
		findHomogeneousExType.setAccessible(true);
		
		TryStatement tryStatement = ASTNodeFinder
				.getTryStatementNodeListByMethodDeclarationName(nestedTryStatementUnit, "nestedCatch_InnerCatchWithChildExceptionTypeOfOuter").get(0);
		
		TryStatement tryStatementInCatchClause = ASTNodeFinder.getTryStatementInNestedTryStatement(tryStatement, ASTNode.CATCH_CLAUSE).get(0);
		
		SingleVariableDeclaration svd1 = (SingleVariableDeclaration) ((CatchClause) tryStatement
				.catchClauses().get(0))
				.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		String lastExType = (String) findHomogeneousExType.invoke(retryRefactoring,
				nestedTryStatementUnit.getAST(), svd1, tryStatementInCatchClause);
		assertEquals("IOException", lastExType);
	}
	
	@Test
	public void testFindHomogeneousExType_InnerTryStatementExceptionTypeAndOuterTryStatementExceptionWithoutParentChildRelations() throws Exception {
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, null);
		Method findHomogeneousExType = RetryRefactoring.class.getDeclaredMethod("findHomogeneousExType", AST.class, SingleVariableDeclaration.class, Object.class);
		findHomogeneousExType.setAccessible(true);
		
		TryStatement tryStatement = ASTNodeFinder
				.getTryStatementNodeListByMethodDeclarationName(nestedTryStatementUnit, "nestedCatch_ExceptionOfTwoCatchWithoutParentChildRelations").get(0);
		
		TryStatement tryStatementInCatchClause = ASTNodeFinder.getTryStatementInNestedTryStatement(tryStatement, ASTNode.CATCH_CLAUSE).get(0);
		
		SingleVariableDeclaration svd1 = (SingleVariableDeclaration) ((CatchClause) tryStatement
				.catchClauses().get(0))
				.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		String lastExType = (String) findHomogeneousExType.invoke(retryRefactoring,
				nestedTryStatementUnit.getAST(), svd1, tryStatementInCatchClause);
		assertEquals("Exception", lastExType);
	}

	@Test
	public void testAddNewVariable() throws Exception {
		// �]�w�Ѽ�
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, "Alt_Retry");
		retryRefactoring.setAttemptVariable("attempt");
		retryRefactoring.setMaxAttemptVariable("maxAttempt");
		retryRefactoring.setMaxAttemptNum("2");
		retryRefactoring.setRetryVariable("retry");
		ASTRewrite rw = ASTRewrite.create(nestedTryStatementUnit.getAST());
		Document document = new Document(
				"try {\n" + 
				"  throwSocketTimeoutException();\n" + 
				"}\n" +
				" catch (SocketTimeoutException e) {\n" + 
				"  try {\n" +
				"    throwInterruptedIOException();\n" +
				"  }\n" +
				" catch (  InterruptedIOException e1) {\n" +
				"    e1.printStackTrace();\n" +
				"  }\n" +
				"  e.printStackTrace();\n" +
				"}\n");
		// �����ϥծĪG
		TextSelection textSelection = new TextSelection(document, 1066, 231);
		NodeFinder nodeFinder = new NodeFinder(textSelection.getOffset(), textSelection.getLength());
		nestedTryStatementUnit.accept(nodeFinder);
		ASTNode selectNode = nodeFinder.getCoveringNode();
		ListRewrite listRewrite = rw.getListRewrite(selectNode, Block.STATEMENTS_PROPERTY);
		// ��X��������������unit�ӻ������Ӧ�m
		int replacePos = -1;
		for (int i = 0; i < listRewrite.getRewrittenList().size(); i++) {
			if (listRewrite.getRewrittenList().get(i).equals(selectNode)) {
				//���Try Statement�N��L����m�O���U��
				replacePos = i;
			}
		}
		
		// ������չ�H
		Method addNewVariable = RetryRefactoring.class.getDeclaredMethod("addNewVariable", AST.class, ListRewrite.class, int.class);
		addNewVariable.setAccessible(true);
		addNewVariable.invoke(retryRefactoring, nestedTryStatementUnit.getAST(), listRewrite, replacePos);
		
		// ���ҵ��G
		List<?> rewriteList = listRewrite.getRewrittenList();
		assertEquals(4, rewriteList.size());
		// CAUTION: �ڵo�{�o��listRewrite index�����ǥi��|�Q����
		assertEquals("int maxAttempt=2;\n", rewriteList.get(0).toString());
		assertEquals("boolean retry=false;\n", rewriteList.get(1).toString());
		assertEquals(document.get(), rewriteList.get(2).toString());
		assertEquals("int attempt=0;\n", rewriteList.get(3).toString());
	}

	@Test
	public void testAddDoWhile() throws Exception {
		// �]�w�Ѽ�
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, "Alt_Retry");
		retryRefactoring.setAttemptVariable("attempt");
		retryRefactoring.setMaxAttemptVariable("maxAttempt");
		retryRefactoring.setMaxAttemptNum("2");
		retryRefactoring.setRetryVariable("retry");
		
		// ������չ�H
		Method addDoWhile = RetryRefactoring.class.getDeclaredMethod("addDoWhile", AST.class);
		addDoWhile.setAccessible(true);
		DoStatement result = (DoStatement)addDoWhile.invoke(retryRefactoring, nestedTryStatementUnit.getAST());
		
		// ���ҵ��G
		assertEquals(	"do {\n" +
						"}\n" + 
						" while (attempt <= maxAttempt & retry);\n", result.toString());
	}
	
	@Test
	public void testAddTryClause() throws Exception {
		// �]�w�Ѽ�
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, "Alt_Retry");
		retryRefactoring.setAttemptVariable("attempt");
		retryRefactoring.setMaxAttemptVariable("maxAttempt");
		retryRefactoring.setMaxAttemptNum("2");
		retryRefactoring.setRetryVariable("retry");
		
		ASTRewrite rw = ASTRewrite.create(nestedTryStatementUnit.getAST());
		Field rewrite = RetryRefactoring.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(retryRefactoring, rw);
		
		Document document = new Document(
				"try {\n" + 
				"  throwSocketTimeoutException();\n" + 
				"}\n" +
				" catch (SocketTimeoutException e) {\n" + 
				"  try {\n" +
				"    throwInterruptedIOException();\n" +
				"  }\n" +
				" catch (  InterruptedIOException e1) {\n" +
				"    e1.printStackTrace();\n" +
				"  }\n" +
				"  e.printStackTrace();\n" +
				"}\n");
		// �����ϥծĪG
		TextSelection textSelection = new TextSelection(document, 1066, 231);
		NodeFinder nodeFinder = new NodeFinder(textSelection.getOffset(), textSelection.getLength());
		nestedTryStatementUnit.accept(nodeFinder);
		ASTNode selectNode = nodeFinder.getCoveringNode();
		ListRewrite listRewrite = rw.getListRewrite(selectNode, Block.STATEMENTS_PROPERTY);
		// ��X��������������unit�ӻ������Ӧ�m
		TryStatement original = null;
		for (int i = 0; i < listRewrite.getRewrittenList().size(); i++) {
			if (((ASTNode)listRewrite.getRewrittenList().get(i)).getParent().equals(selectNode))
				original = (TryStatement)listRewrite.getRewrittenList().get(i);
		}
		
		// �e�m�ʧ@�A���إ�do-while statement
		Method addDoWhile = RetryRefactoring.class.getDeclaredMethod("addDoWhile", AST.class);
		addDoWhile.setAccessible(true);
		DoStatement doWhile = (DoStatement)addDoWhile.invoke(retryRefactoring, nestedTryStatementUnit.getAST());

		// ������չ�H		
		Method addTryClause = RetryRefactoring.class.getDeclaredMethod("addTryClause", AST.class, DoStatement.class, TryStatement.class);
		addTryClause.setAccessible(true);
		TryStatement tryStatement = (TryStatement)addTryClause.invoke(retryRefactoring, nestedTryStatementUnit.getAST(), doWhile, original);
		
		// ���ҵ��G
		assertEquals(	"try {\n" +
						"  retry=false;\n" +
						"  if (attempt == 0) {\n" +
						"  }\n" +
						" else {\n" +
						"  }\n" +
						"}\n ", tryStatement.toString());
	}
	
	@Test
	public void testAddNoAltTryBlock() throws Exception {
		// �]�w�Ѽ�
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, "Alt_Retry");
		retryRefactoring.setAttemptVariable("attempt");
		retryRefactoring.setMaxAttemptVariable("maxAttempt");
		retryRefactoring.setMaxAttemptNum("2");
		retryRefactoring.setRetryVariable("retry");
		
		ASTRewrite rw = ASTRewrite.create(nestedTryStatementUnit.getAST());
		Field rewrite = RetryRefactoring.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(retryRefactoring, rw);
		
		Document document = new Document(
				"try {\n" + 
				"  throwSocketTimeoutException();\n" + 
				"}\n" +
				" catch (SocketTimeoutException e) {\n" + 
				"  try {\n" +
				"    throwInterruptedIOException();\n" +
				"  }\n" +
				" catch (  InterruptedIOException e1) {\n" +
				"    e1.printStackTrace();\n" +
				"  }\n" +
				"  e.printStackTrace();\n" +
				"}\n");
		// �����ϥծĪG
		TextSelection textSelection = new TextSelection(document, 1066, 231);
		NodeFinder nodeFinder = new NodeFinder(textSelection.getOffset(), textSelection.getLength());
		nestedTryStatementUnit.accept(nodeFinder);
		ASTNode selectNode = nodeFinder.getCoveringNode();
		ListRewrite listRewrite = rw.getListRewrite(selectNode, Block.STATEMENTS_PROPERTY);
		// ��X��������������unit�ӻ������Ӧ�m
		TryStatement original = null;
		for (int i = 0; i < listRewrite.getRewrittenList().size(); i++) {
			if (((ASTNode)listRewrite.getRewrittenList().get(i)).getParent().equals(selectNode))
				original = (TryStatement)listRewrite.getRewrittenList().get(i);
		}
		
		// �e�m�ʧ@�A���إ�do-while statement
		Method addDoWhile = RetryRefactoring.class.getDeclaredMethod("addDoWhile", AST.class);
		addDoWhile.setAccessible(true);
		DoStatement doWhile = (DoStatement)addDoWhile.invoke(retryRefactoring, nestedTryStatementUnit.getAST());
		
		// ������չ�H
		Method addNoAltTryBlock = RetryRefactoring.class.getDeclaredMethod("addNoAltTryBlock", AST.class, DoStatement.class, TryStatement.class);
		addNoAltTryBlock.setAccessible(true);
		TryStatement tryStatement = (TryStatement)addNoAltTryBlock.invoke(retryRefactoring, nestedTryStatementUnit.getAST(), doWhile, original);
		
		// ���ҵ��G
		assertEquals(	"try {\n" +
						"  retry=false;\n" +
						"}\n ", tryStatement.toString());
	}
	
	@Test
	public void testAddCatchBlock() throws Exception {
		// �]�w�Ѽ�
		RetryRefactoring retryRefactoring = new RetryRefactoring(null, null, null, "Alt_Retry");
		retryRefactoring.setAttemptVariable("attempt");
		retryRefactoring.setMaxAttemptVariable("maxAttempt");
		retryRefactoring.setMaxAttemptNum("2");
		retryRefactoring.setRetryVariable("retry");
		
		ASTRewrite rw = ASTRewrite.create(nestedTryStatementUnit.getAST());
		Field rewrite = RetryRefactoring.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(retryRefactoring, rw);
		
		Field exceptionType = RetryRefactoring.class.getDeclaredField("exceptionType");
		exceptionType.setAccessible(true);
		exceptionType.set(retryRefactoring, "PropertyVetoException");
		
		Field actRoot = RetryRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(retryRefactoring, nestedTryStatementUnit);
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		nestedTryStatementUnit.accept(methodCollector);
		
		Field currentMethodNode = RetryRefactoring.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		List<ASTNode> methodList = methodCollector.getMethodList();
		currentMethodNode.set(retryRefactoring, methodList.get(0));
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IType exType = JavaCore.create(project).findType("java.beans.PropertyVetoException");
		retryRefactoring.setExType(exType);
		
		Document document = new Document(
				"try {\n" + 
				"  throwSocketTimeoutException();\n" + 
				"}\n" +
				" catch (SocketTimeoutException e) {\n" + 
				"  try {\n" +
				"    throwInterruptedIOException();\n" +
				"  }\n" +
				" catch (  InterruptedIOException e1) {\n" +
				"    e1.printStackTrace();\n" +
				"  }\n" +
				"  e.printStackTrace();\n" +
				"}\n");
		// �����ϥծĪG
		TextSelection textSelection = new TextSelection(document, 1066, 231);
		NodeFinder nodeFinder = new NodeFinder(textSelection.getOffset(), textSelection.getLength());
		nestedTryStatementUnit.accept(nodeFinder);
		ASTNode selectNode = nodeFinder.getCoveringNode();
		ListRewrite listRewrite = rw.getListRewrite(selectNode, Block.STATEMENTS_PROPERTY);
		// ��X��������������unit�ӻ������Ӧ�m
		TryStatement original = null;
		for (int i = 0; i < listRewrite.getRewrittenList().size(); i++) {
			if (((ASTNode)listRewrite.getRewrittenList().get(i)).getParent().equals(selectNode))
				original = (TryStatement)listRewrite.getRewrittenList().get(i);
		}
		
		// �e�m�ʧ@
		Method addDoWhile = RetryRefactoring.class.getDeclaredMethod("addDoWhile", AST.class);
		addDoWhile.setAccessible(true);
		DoStatement doWhile = (DoStatement)addDoWhile.invoke(retryRefactoring, nestedTryStatementUnit.getAST());
		
		Method addNoAltTryBlock = RetryRefactoring.class.getDeclaredMethod("addNoAltTryBlock", AST.class, DoStatement.class, TryStatement.class);
		addNoAltTryBlock.setAccessible(true);
		TryStatement tryStatement = (TryStatement)addNoAltTryBlock.invoke(retryRefactoring, nestedTryStatementUnit.getAST(), doWhile, original);
		// �ˬd��l���A
		assertEquals(	"try {\n" +
						"  retry=false;\n" +
						"}\n ", tryStatement.toString());
		// ������չ�H
		Method addCatchBlock = RetryRefactoring.class.getDeclaredMethod("addCatchBlock", AST.class, TryStatement.class, TryStatement.class);
		addCatchBlock.setAccessible(true);
		addCatchBlock.invoke(retryRefactoring, nestedTryStatementUnit.getAST(), original, tryStatement);
		// ���ҵ��G
		assertEquals(	"try {\n" +
						"  retry=false;\n" +
						"}\n" +
						" catch (InterruptedIOException e) {\n" +
						"  attempt++;\n" +
						"  retry=true;\n" +
						"  if (attempt > maxAttempt) {\n" +
						"    throw new PropertyVetoException(e);\n" +
						"  }\n" +
						"}\n", tryStatement.toString());
	}
	
//	@Test
	public void testIntroduceTryClause() {
		fail("not implement �]��applyChange�ثe�Q���X����");
	}
	
//	@Test
	public void testCollectChange() throws Exception {
		String retry_type = "Retry_with_original";
		/* �ϥΪ̿�����T�A�]�t�ť� */
		Document document = new Document(
				"try {" + 
				"fis = new FileInputStream(\"\");" + 
				"fis.read();" + 
				"} catch (IOException e) {" + 
				"javaLog.log(Level.INFO, \"Just log it.\");	//	DummyHandler" + 
				"}");
		TextSelection textSelection = new TextSelection(document, 3392, 168);
		// ���o�M�פU��java��
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.open(null);
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyAndIgnoredExamplePath));
		// �]�w�ݴ��ؼ� RetryRefactoring
		RetryRefactoring refactoring = new RetryRefactoring(javaProject,javaElement,textSelection,retry_type);
		refactoring.setAttemptVariable("attempt");
		refactoring.setMaxAttemptVariable("maxAttempt");
		refactoring.setMaxAttemptNum("2");
		refactoring.setRetryVariable("retry");
		refactoring.setExType(null);
		refactoring.setExceptionName("RuntimeException");
		Method collectChange = RetryRefactoring.class.getDeclaredMethod("collectChange", RefactoringStatus.class);
		collectChange.setAccessible(true);
		RefactoringStatus status = new RefactoringStatus();
		collectChange.invoke(refactoring, status);
		// ���G��OK��ܨϥΪ̿�����T
		assertEquals("<OK\n>", status.toString());
		
		/* �ϥΪ̿�����T�A���]�t�ť� */
		document = new Document(
				"try {" + 
				"fis = new FileInputStream(\"\");" + 
				"fis.read();" + 
				"} catch (IOException e) {" + 
				"javaLog.log(Level.INFO, \"Just log it.\");	//	DummyHandler" + 
				"}");
		textSelection = new TextSelection(document, 3075, 151);
		refactoring = new RetryRefactoring(javaProject,javaElement,textSelection,retry_type);
		refactoring.setAttemptVariable("attempt");
		refactoring.setMaxAttemptVariable("maxAttempt");
		refactoring.setMaxAttemptNum("2");
		refactoring.setRetryVariable("retry");
		refactoring.setExType(null);
		refactoring.setExceptionName("RuntimeException");
		
		collectChange.invoke(refactoring, status);
		// ���G��OK��ܨϥΪ̿�����T
		assertEquals("<OK\n>", status.toString());
		
		/* �ϥΪ̦h��A����d�� */
		document = new Document(
				"FileInputStream fis = null;" + 
				"try {" + 
				"fis = new FileInputStream(\"\");" + 
				"fis.read();" + 
				"} catch (IOException e) {" + 
				"javaLog.log(Level.INFO, \"Just log it.\");	//	DummyHandler" + 
				"}");
		textSelection = new TextSelection(document, 3040, 182);
		refactoring = new RetryRefactoring(javaProject,javaElement,textSelection,retry_type);
		refactoring.setAttemptVariable("attempt");
		refactoring.setMaxAttemptVariable("maxAttempt");
		refactoring.setMaxAttemptNum("2");
		refactoring.setRetryVariable("retry");
		refactoring.setExType(null);
		refactoring.setExceptionName("RuntimeException");
		
		collectChange.invoke(refactoring, status);
		// �d�ݪ��A�A���ҵ��G
		assertEquals(	"<FATALERROR\n\t\n" +
						"FATALERROR: Selection Error, please retry again!!!\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" + 
						"Data: null\n" +
						">", status.toString());
		
		/* �ϥΪֿ̤�A����d�� */
		document = new Document(
				"fis = new FileInputStream(\"\");" + 
				"fis.read();" + 
				"} catch (IOException e) {" + 
				"javaLog.log(Level.INFO, \"Just log it.\");	//	DummyHandler" + 
				"}");
		textSelection = new TextSelection(document, 3081, 141);
		refactoring = new RetryRefactoring(javaProject,javaElement,textSelection,retry_type);
		refactoring.setAttemptVariable("attempt");
		refactoring.setMaxAttemptVariable("maxAttempt");
		refactoring.setMaxAttemptNum("2");
		refactoring.setRetryVariable("retry");
		refactoring.setExType(null);
		refactoring.setExceptionName("RuntimeException");
		
		collectChange.invoke(refactoring, status);
		// �d�ݪ��A�A���ҵ��G
		assertEquals(	"<FATALERROR\n\t\n" +
						"FATALERROR: Selection Error, please retry again!!!\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" + 
						"Data: null\n\t\n" +
						"FATALERROR: Selection Error, please retry again!!!\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" + 
						"Data: null\n" +
						">", status.toString());
	}
	
//	@Test
	public void testApplyChange() {
		fail("not implement");
	}
}
