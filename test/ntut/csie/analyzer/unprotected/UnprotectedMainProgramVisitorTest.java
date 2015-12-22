package ntut.csie.analyzer.unprotected;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UnprotectedMainProgramVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit unit1, unit2, unit3, unit4, unit5, unit6, unit7, unit8, unit9;
	SmellSettings smellSettings;
	UnprotectedMainProgramVisitor mainVisitor;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "UnprotectedMainProgramTest";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packageAgileExceptionClassesToJarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();

		// unit1
		javaFile2String.read(UnprotectedMainProgramExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramExample.class.getPackage().getName()
				, UnprotectedMainProgramExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit2
		javaFile2String.read(UnprotectedMainProgramWithCatchThrowableExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker
				.createJavaFile(
						UnprotectedMainProgramWithCatchThrowableExample.class.getPackage().getName(),
						UnprotectedMainProgramWithCatchThrowableExample.class.getSimpleName()
						+ JavaProjectMaker.JAVA_FILE_EXTENSION,	"package "
						+ UnprotectedMainProgramWithCatchThrowableExample.class.getPackage().getName() + ";\n"
						+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit3
		javaFile2String.read(UnprotectedMainProgramWithoutStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutStatementExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutStatementExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramWithoutStatementExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit4
		javaFile2String.read(UnprotectedMainProgramWithoutTryExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutTryExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramWithoutTryExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit5
		javaFile2String.read(UnprotectedmainProgramWithTryAtFirstStatement.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedmainProgramWithTryAtFirstStatement.class.getPackage().getName()
				, UnprotectedmainProgramWithTryAtFirstStatement.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedmainProgramWithTryAtFirstStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit6
		javaFile2String.read(UnprotectedMainProgramWithTryAtMiddleStatement.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTryAtMiddleStatement.class.getPackage().getName()
				, UnprotectedMainProgramWithTryAtMiddleStatement.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramWithTryAtMiddleStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit7
		javaFile2String.read(UnprotectedMainProgramWithTryAtLastStatement.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTryAtLastStatement.class.getPackage().getName()
				, UnprotectedMainProgramWithTryAtLastStatement.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramWithTryAtLastStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit8
		javaFile2String.read(UnprotectedMainProgramWithTry.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTry.class.getPackage().getName()
				, UnprotectedMainProgramWithTry.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramWithTry.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit9
		javaFile2String.read(UnprotectedMainProgramWithoutCatchRightExceptionExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutCatchRightExceptionExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutCatchRightExceptionExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramWithoutCatchRightExceptionExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		CreateSettings();
		/** unit1 */ 
		Path path1 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramExample.class, testProjectName));
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path1)));
		parser.setResolveBindings(true);

		unit1 = (CompilationUnit) parser.createAST(null); 
		unit1.recordModifications();
		/** unit2 */
		Path path2 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithCatchThrowableExample.class, testProjectName));
		
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path2)));
		parser.setResolveBindings(true);

		unit2 = (CompilationUnit) parser.createAST(null); 
		unit2.recordModifications();
		/** unit3 */
		Path path3 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithoutStatementExample.class, testProjectName));
		
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path3)));
		parser.setResolveBindings(true);
		
		unit3 = (CompilationUnit) parser.createAST(null); 
		unit3.recordModifications();
		/** unit4 */
		Path path4 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithoutTryExample.class, testProjectName));
		
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path4)));
		parser.setResolveBindings(true);

		unit4 = (CompilationUnit) parser.createAST(null);
		unit4.recordModifications();
		/** unit5 */
		Path path5 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedmainProgramWithTryAtFirstStatement.class, testProjectName));
		
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path5)));
		parser.setResolveBindings(true);
		
		unit5 = (CompilationUnit) parser.createAST(null);
		unit5.recordModifications();
		/** unit6 */
		Path path6 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithTryAtMiddleStatement.class, testProjectName));
		
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path6)));
		parser.setResolveBindings(true);
		
		unit6 = (CompilationUnit) parser.createAST(null);
		unit6.recordModifications();
		/** unit7 */
		Path path7 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithTryAtLastStatement.class, testProjectName));
		
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path7)));
		parser.setResolveBindings(true);
		
		unit7 = (CompilationUnit) parser.createAST(null);
		unit7.recordModifications();
		/** unit8 */
		Path path8 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithTry.class, testProjectName));
		
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path8)));
		parser.setResolveBindings(true);
		
		unit8 = (CompilationUnit) parser.createAST(null);
		unit8.recordModifications();
		/** unit9 */
		Path path9 = new Path(PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithoutCatchRightExceptionExample.class, testProjectName));
		
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path9)));
		parser.setResolveBindings(true);
		
		unit9 = (CompilationUnit) parser.createAST(null);
		unit9.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testProcessMainFunction() throws Exception {
		// case 1 : correct example
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit1.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit1);
		List<MethodDeclaration> list = methodCollector.getMethodList();	
		MethodDeclaration md = list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// test target
		Method processMainFunction = UnprotectedMainProgramVisitor.class.getDeclaredMethod("containUnprotectedStatement", List.class);
		processMainFunction.setAccessible(true);
		// check postcondition
		assertFalse((Boolean)processMainFunction.invoke(mainVisitor, md.getBody().statements()));
		
		// case 2 : main body with try block but no catch Exception.class
		methodCollector = new ASTMethodCollector();
		unit9.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit9);
		list = methodCollector.getMethodList();
		md = list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// test target & check postcondition
		assertTrue((Boolean)processMainFunction.invoke(mainVisitor, md.getBody().statements()));
		
		// case 3 : main body is empty
		methodCollector = new ASTMethodCollector();
		unit3.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit3);
		list = methodCollector.getMethodList();
		md = list.get(0);
		// check precondition
		assertEquals(0, md.getBody().statements().size());
		// test target & check postcondition
		assertFalse((Boolean)processMainFunction.invoke(mainVisitor, md.getBody().statements()));
		
		// case 4 : main body without try block
		methodCollector = new ASTMethodCollector();
		unit4.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit4);
		list = methodCollector.getMethodList();
		md = list.get(0);
		// check precondition
		assertEquals(2, md.getBody().statements().size());
		// test target & check postcondition
		assertTrue((Boolean)processMainFunction.invoke(mainVisitor, md.getBody().statements()));
	}
	
	@Test
	public void testGetNumber() throws Exception {
		// case 1 : annotation above method  
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit1.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit1);
		List<MethodDeclaration> list = methodCollector.getMethodList();
		MethodDeclaration md = list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		//if there is an annotation on main function, the start position of the method will start at annotation.
		//so the marker need to be added at next line.
		assertEquals(7, unit1.getLineNumber(md.getStartPosition()));
		// test target
		Method getLineNumber = UnprotectedMainProgramVisitor.class.getDeclaredMethod("getLineNumber", MethodDeclaration.class);
		getLineNumber.setAccessible(true);
		// check postcondition
		assertEquals(8, getLineNumber.invoke(mainVisitor, md));
		
		// case 2 : method without annotation
		methodCollector = new ASTMethodCollector();
		unit2.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit2);
		list = methodCollector.getMethodList();
		md = list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// due to there is no annotation on main function, so the marker will be added at the first line of main function.
		assertEquals(4, unit2.getLineNumber(md.getStartPosition()));
		// test target & check postcondition
		assertEquals(4, getLineNumber.invoke(mainVisitor, md));
	}
	
	@Test
	public void testVisit() {
		// case 1 : give the main function
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit4.accept(methodCollector);
		List<MethodDeclaration> list = methodCollector.getMethodList();
		MethodDeclaration md = list.get(0);
		// test target & check postcondition
		mainVisitor = new UnprotectedMainProgramVisitor(unit4);
		assertFalse(mainVisitor.visit(md));
		
		// case 2 : give the other function
		methodCollector = new ASTMethodCollector();
		unit1.accept(methodCollector);
		list = methodCollector.getMethodList();
		md = list.get(0);
		// test target & check postcondition
		assertTrue(mainVisitor.visit(md));
	}
	
	@Test
	public void testUnprotectedMainProgramExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit1);
		unit1.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithoutCatchExceptionExample_doNotDetect() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		mainVisitor = new UnprotectedMainProgramVisitor(unit2);
		unit2.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithCatchRuntimeExceptionExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit2);
		unit2.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithoutCatchExceptionExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit9);
		unit9.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithoutStatementExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit3);
		unit3.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithoutTryExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit4);
		unit4.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedmainProgramWithTryAtFirstStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit5);
		unit5.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithTryAtLastStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit6);
		unit6.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithTryAtMiddleStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit7);
		unit7.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithTry() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit8);
		unit8.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
