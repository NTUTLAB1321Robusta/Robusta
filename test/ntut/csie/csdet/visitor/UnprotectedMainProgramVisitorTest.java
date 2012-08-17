package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTry;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTryAtLastStatement;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTryAtMiddleStatement;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutCatchExceptionExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutStatementExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedmainProgramWithTryAtFirstStatement;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UnprotectedMainProgramVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit unit1, unit2, unit3, unit4, unit5, unit6, unit7, unit8;
	SmellSettings smellSettings;
	UnprotectedMainProgramVisitor mainVisitor;
	
	@Before
	public void setUp() throws Exception {
		String projectName = "UnprotectedMainProgramTest";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.addAgileExceptionClasses();
		javaProjectMaker.addJarFromTestProjectToBuildPath("/lib/RL.jar");
		javaProjectMaker.setJREDefaultContainer();
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		// unit1
		javaFile2String.read(UnprotectedMainProgramExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramExample.class.getPackage().getName()
				, UnprotectedMainProgramExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit2
		javaFile2String.read(UnprotectedMainProgramWithoutCatchExceptionExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutCatchExceptionExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutCatchExceptionExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithoutCatchExceptionExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit3
		javaFile2String.read(UnprotectedMainProgramWithoutStatementExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutStatementExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutStatementExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithoutStatementExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit4
		javaFile2String.read(UnprotectedMainProgramWithoutTryExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutTryExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithoutTryExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit5
		javaFile2String.read(UnprotectedmainProgramWithTryAtFirstStatement.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedmainProgramWithTryAtFirstStatement.class.getPackage().getName()
				, UnprotectedmainProgramWithTryAtFirstStatement.class.getSimpleName() + ".java"
				, "package " + UnprotectedmainProgramWithTryAtFirstStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit6
		javaFile2String.read(UnprotectedMainProgramWithTryAtMiddleStatement.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTryAtMiddleStatement.class.getPackage().getName()
				, UnprotectedMainProgramWithTryAtMiddleStatement.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithTryAtMiddleStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit7
		javaFile2String.read(UnprotectedMainProgramWithTryAtLastStatement.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTryAtLastStatement.class.getPackage().getName()
				, UnprotectedMainProgramWithTryAtLastStatement.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithTryAtLastStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit8
		javaFile2String.read(UnprotectedMainProgramWithTry.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTry.class.getPackage().getName()
				, UnprotectedMainProgramWithTry.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithTry.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// �إ�XML
		CreateSettings();
		/** unit1 */ 
		Path path1 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramExample.class.getSimpleName() + ".java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path1)));
		parser.setResolveBindings(true);
		// ���oAST
		unit1 = (CompilationUnit) parser.createAST(null); 
		unit1.recordModifications();
		/** unit2 */
		Path path2 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithoutCatchExceptionExample.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path2)));
		parser.setResolveBindings(true);
		// ���oAST
		unit2 = (CompilationUnit) parser.createAST(null); 
		unit2.recordModifications();
		/** unit3 */
		Path path3 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithoutStatementExample.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path3)));
		parser.setResolveBindings(true);
		// ���oAST
		unit3 = (CompilationUnit) parser.createAST(null); 
		unit3.recordModifications();
		/** unit4 */
		Path path4 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path4)));
		parser.setResolveBindings(true);
		// ���oAST
		unit4 = (CompilationUnit) parser.createAST(null);
		unit4.recordModifications();
		/** unit5 */
		Path path5 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedmainProgramWithTryAtFirstStatement.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path5)));
		parser.setResolveBindings(true);
		// ���oAST
		unit5 = (CompilationUnit) parser.createAST(null);
		unit5.recordModifications();
		/** unit6 */
		Path path6 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithTryAtMiddleStatement.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path6)));
		parser.setResolveBindings(true);
		// ���oAST
		unit6 = (CompilationUnit) parser.createAST(null);
		unit6.recordModifications();
		/** unit7 */
		Path path7 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithTryAtLastStatement.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path7)));
		parser.setResolveBindings(true);
		// ���oAST
		unit7 = (CompilationUnit) parser.createAST(null);
		unit7.recordModifications();
		/** unit8 */
		Path path8 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithTry.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path8)));
		parser.setResolveBindings(true);
		// ���oAST
		unit8 = (CompilationUnit) parser.createAST(null);
		unit8.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// �p�Gxml�ɮצs�b�A�h�R����
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// �R���M��
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testProcessMainFunction() throws Exception {
		// case 1 : correct example
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit1.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit1);
		List<ASTNode> list = methodCollector.getMethodList();	
		MethodDeclaration md = (MethodDeclaration)list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// test target
		Method processMainFunction = UnprotectedMainProgramVisitor.class.getDeclaredMethod("processMainFunction", List.class);
		processMainFunction.setAccessible(true);
		// check postcondition
		assertFalse((Boolean)processMainFunction.invoke(mainVisitor, md.getBody().statements()));
		
		// case 2 : main body with try block but not catch Exception.class
		methodCollector = new ASTMethodCollector();
		unit2.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit2);
		list = methodCollector.getMethodList();
		md = (MethodDeclaration)list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// test target & check postcondition
		assertTrue((Boolean)processMainFunction.invoke(mainVisitor, md.getBody().statements()));
		
		// case 3 : main body is empty
		methodCollector = new ASTMethodCollector();
		unit3.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit3);
		list = methodCollector.getMethodList();
		md = (MethodDeclaration)list.get(0);
		// check precondition
		assertEquals(0, md.getBody().statements().size());
		// test target & check postcondition
		assertFalse((Boolean)processMainFunction.invoke(mainVisitor, md.getBody().statements()));
		
		// case 4 : main body without try block
		methodCollector = new ASTMethodCollector();
		unit4.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit4);
		list = methodCollector.getMethodList();
		md = (MethodDeclaration)list.get(0);
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
		List<ASTNode> list = methodCollector.getMethodList();
		MethodDeclaration md = (MethodDeclaration)list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// �p�Gmain function�W����annotation���ܡA��method���_�l��m�|�qannotation�}�l��_
		// �Gmarker�����[�b�U�@��
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
		md = (MethodDeclaration)list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// �]��main function�W���S��annotation�A�ҥHmain function�b�ĴX��Amarker�N�[�b�ĴX��
		assertEquals(4, unit2.getLineNumber(md.getStartPosition()));
		// test target & check postcondition
		assertEquals(4, getLineNumber.invoke(mainVisitor, md));
	}
	
	@Test
	public void testVisit() {
		// case 1 : give the main function
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit4.accept(methodCollector);
		List<ASTNode> list = methodCollector.getMethodList();
		MethodDeclaration md = (MethodDeclaration)list.get(0);
		// test target & check postcondition
		mainVisitor = new UnprotectedMainProgramVisitor(unit4);
		assertFalse(mainVisitor.visit(md));
		
		// case 2 : give the other function
		methodCollector = new ASTMethodCollector();
		unit1.accept(methodCollector);
		list = methodCollector.getMethodList();
		md = (MethodDeclaration)list.get(0);
		// test target & check postcondition
		assertTrue(mainVisitor.visit(md));
	}
	
	@Test
	public void testUnprotectedMainProgramExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit1);
		unit1.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithoutCatchExceptionExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit2);
		unit2.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithoutStatementExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit3);
		unit3.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithoutTryExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit4);
		unit4.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotedMainList().size());
	}
	
	@Test
	public void testUnprotectedmainProgramWithTryAtFirstStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit5);
		unit5.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithTryAtLastStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit6);
		unit6.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithTryAtMiddleStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit7);
		unit7.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotedMainList().size());
	}
	
	@Test
	public void testUnprotectedMainProgramWithTry() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit8);
		unit8.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotedMainList().size());
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, "true");
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
