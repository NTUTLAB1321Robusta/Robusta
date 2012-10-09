package ntut.csie.csdet.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassWithNotThrowingExceptionCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupWeather;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CCUQuickFixTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	CCUQuickFix ccuFix;
	ASTMethodCollector methodCollector;
	List<?> methodList;
	
	public CCUQuickFixTest() {}
	
	@Before
	public void setUp() throws Exception {
		String projectName = "CarelessCleanupExampleProject";
		
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		javaFile2String.read(CarelessCleanupExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CarelessCleanupExample.class.getPackage().getName(),
				CarelessCleanupExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CarelessCleanupExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassWithNotThrowingExceptionCloseable.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassWithNotThrowingExceptionCloseable.class.getPackage().getName(),
				ClassWithNotThrowingExceptionCloseable.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassWithNotThrowingExceptionCloseable.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassImplementCloseable.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassImplementCloseable.class.getPackage().getName(),
				ClassImplementCloseable.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassImplementCloseable.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassImplementCloseableWithoutThrowException.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassImplementCloseableWithoutThrowException.class.getPackage().getName(),
				ClassImplementCloseableWithoutThrowException.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassImplementCloseableWithoutThrowException.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		/* ���ըϥΪ̳]�wPattern�ɭԨϥ� */
		javaFile2String.read(UserDefinedCarelessCleanupWeather.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupWeather.class.getPackage().getName(),
				UserDefinedCarelessCleanupWeather.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefinedCarelessCleanupWeather.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(UserDefinedCarelessCleanupDog.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupDog.class.getPackage().getName(),
				UserDefinedCarelessCleanupDog.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefinedCarelessCleanupDog.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(CarelessCleanupExample.class, projectName));

		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		
		CreateSettings();
		// ���oAST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		
		ccuFix = new CCUQuickFix("");
		
		methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		methodList = methodCollector.getMethodList();
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
	public void testFindMoveLine() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, methodList.get(29));
		
		Field smellMessage = CCUQuickFix.class.getDeclaredField("smellMessage");
		smellMessage.setAccessible(true);
		
		// check precondition
		assertNull(smellMessage.get(ccuFix));
		
		// test
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		
		// check postcondition
		assertEquals("closeStreamWithoutThrowingException(fi)", findMoveLine.invoke(ccuFix, "0"));
		MarkerInfo marker = (MarkerInfo)smellMessage.get(ccuFix);
		assertEquals(520, marker.getLineNumber());
	}
	
	@Test
	public void testMoveLineStatement() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		MethodDeclaration md = (MethodDeclaration)methodList.get(29);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, methodList.get(29));
		
		// check precondition
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		Field moveLine = CCUQuickFix.class.getDeclaredField("moveLine");
		moveLine.setAccessible(true);
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));
		assertEquals("closeStreamWithoutThrowingException(fi)", moveLine.get(ccuFix));
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) " +
						"public void uy_closeStreaminOuterMethodInTry() throws IOException {\n" +
						"  try {\n" +
						"    FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"    closeStreamWithoutThrowingException(fi);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", methodList.get(29).toString());
		// verify if target is found
		Method moveLineStatement = CCUQuickFix.class.getDeclaredMethod("moveLineStatement", List.class);
		moveLineStatement.setAccessible(true);
		assertEquals("closeStreamWithoutThrowingException(fi);\n", moveLineStatement.invoke(ccuFix, ((TryStatement)md.getBody().statements().get(0)).getBody().statements()).toString());
		// verify if target is not found 
		moveLine.set(ccuFix, "close(fi)");
		assertNull(moveLineStatement.invoke(ccuFix, ((TryStatement)md.getBody().statements().get(0)).getBody().statements()));
	}
	
	@Test
	public void testContainTargetLine() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		MethodDeclaration md = (MethodDeclaration)methodList.get(29);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, methodList.get(29));
		
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		Field moveLine = CCUQuickFix.class.getDeclaredField("moveLine");
		moveLine.setAccessible(true);
		
		Field tryIndex = CCUQuickFix.class.getDeclaredField("tryIndex");
		tryIndex.setAccessible(true);
		
		Field tryStatement = CCUQuickFix.class.getDeclaredField("tryStatement");
		tryStatement.setAccessible(true);
		// check precondition
		assertEquals(-1, tryIndex.get(ccuFix));
		assertNull(tryStatement.get(ccuFix));
		// test
		Method containTargetLine = CCUQuickFix.class.getDeclaredMethod("containTargetLine", TryStatement.class, List.class, int.class);
		containTargetLine.setAccessible(true);
		// verify if there don't contain targer line
		moveLine.set(ccuFix, "close(fi)");
		TryStatement tryStat = (TryStatement)md.getBody().statements().get(0);
		assertFalse((Boolean)containTargetLine.invoke(ccuFix, tryStat, tryStat.getBody().statements(), 1));
		assertNull(tryStatement.get(ccuFix));
		assertEquals(-1, tryIndex.get(ccuFix));
		// verify if there contains target line
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));
		assertTrue((Boolean)containTargetLine.invoke(ccuFix, tryStat, tryStat.getBody().statements(), 1));
		assertEquals(	"try {\n" +
						"  FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"  fi.write(1);\n" +
						"  closeStreamWithoutThrowingException(fi);\n" +
						"}\n" +
						" catch (FileNotFoundException e) {\n" +
						"  throw e;\n" +
						"}\n", tryStatement.get(ccuFix).toString());
		assertEquals(1, tryIndex.get(ccuFix));
	}
	
	@Test
	public void testFindTryStatement() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, methodList.get(29));
		
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		Field moveLine = CCUQuickFix.class.getDeclaredField("moveLine");
		moveLine.setAccessible(true);
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));
		
		Field tryIndex = CCUQuickFix.class.getDeclaredField("tryIndex");
		tryIndex.setAccessible(true);
		
		Field tryStatement = CCUQuickFix.class.getDeclaredField("tryStatement");
		tryStatement.setAccessible(true);
	
		// check precondition
		assertEquals(-1, tryIndex.get(ccuFix));
		assertNull(tryStatement.get(ccuFix));
		
		/** found in try block */
		// test
		Method findTryStatement = CCUQuickFix.class.getDeclaredMethod("findTryStatement");
		findTryStatement.setAccessible(true);
		findTryStatement.invoke(ccuFix);
		
		// check postcondition
		assertEquals(0, tryIndex.get(ccuFix));
		assertEquals(	"try {\n" +
						"  FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"  fi.write(1);\n" +
						"  closeStreamWithoutThrowingException(fi);\n" +
						"}\n" +
						" catch (FileNotFoundException e) {\n" +
						"  throw e;\n" +
						"}\n", tryStatement.get(ccuFix).toString());
		
		/** found in catch block */
		// test
		currentMethodNode.set(ccuFix, methodList.get(30));
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));
		findTryStatement.invoke(ccuFix);
		
		// check postcondition
		assertEquals(1, tryIndex.get(ccuFix));
		assertEquals(	"try {\n" +
						"  fi=new FileOutputStream(\"\");\n" +
						"  fi.write(1);\n" +
						"}\n" +
						" catch (FileNotFoundException e) {\n" +
						"  closeStreamWithoutThrowingException(fi);\n" +
						"  throw e;\n" +
						"}\n" +
						" finally {\n" +
						"}\n", tryStatement.get(ccuFix).toString());
		
		/** FIXME - found in finally block ?*/ 
	}
	
	@Test
	public void testIsVariableDeclareInTry() throws Exception {
		TryStatement tryStat = (TryStatement)((MethodDeclaration)methodList.get(30)).getBody().statements().get(1);
		
		Field tryStatement = CCUQuickFix.class.getDeclaredField("tryStatement");
		tryStatement.setAccessible(true);
		tryStatement.set(ccuFix, tryStat);
		
		/** it's not in the try block */
		// check precondition
		assertEquals(	"try {\n" +
						"  fi=new FileOutputStream(\"\");\n" +
						"  fi.write(1);\n" +
						"}\n" +
						" catch (FileNotFoundException e) {\n" +
						"  closeStreamWithoutThrowingException(fi);\n" +
						"  throw e;\n" +
						"}\n" +
						" finally {\n" +
						"}\n", tryStatement.get(ccuFix).toString());
		// test
		Method isVariableDeclareInTry = CCUQuickFix.class.getDeclaredMethod("isVariableDeclareInTry", String.class);
		isVariableDeclareInTry.setAccessible(true);
		// check postcondition
		assertFalse((Boolean)isVariableDeclareInTry.invoke(ccuFix, "fi"));
		
		/** it's in the try block */
		tryStat = (TryStatement)((MethodDeclaration)methodList.get(29)).getBody().statements().get(0);
		tryStatement.set(ccuFix, tryStat);
		// check precondition
		assertEquals(	"try {\n" +
						"  FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"  fi.write(1);\n" +
						"  closeStreamWithoutThrowingException(fi);\n" +
						"}\n" +
						" catch (FileNotFoundException e) {\n" +
						"  throw e;\n" +
						"}\n", tryStatement.get(ccuFix).toString());
		// check postcondition
		assertTrue((Boolean)isVariableDeclareInTry.invoke(ccuFix, "fi"));
	}
	
//	@Test
	public void testMoveInstance() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		Field rewrite = CCUQuickFix.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(ccuFix, ASTRewrite.create(compilationUnit.getAST()));
		
		TryStatement tryStat = (TryStatement)((MethodDeclaration)methodList.get(29)).getBody().statements().get(0);
		
		Field tryStatement = CCUQuickFix.class.getDeclaredField("tryStatement");
		tryStatement.setAccessible(true);
		tryStatement.set(ccuFix, tryStat);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, methodList.get(29));
		
		Field tryIndex = CCUQuickFix.class.getDeclaredField("tryIndex");
		tryIndex.setAccessible(true);
		tryIndex.set(ccuFix, 0);
		
		ExpressionStatement eStatement = (ExpressionStatement)tryStat.getBody().statements().get(2);
		MethodInvocation mInvocation = (MethodInvocation) eStatement.getExpression();
		
		// check precondition
		assertEquals("closeStreamWithoutThrowingException(fi)", mInvocation.toString());
		assertEquals(	"@Robustness(value={@Tag(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInTry() throws IOException {\n" +
						"  try {\n" +
						"    FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"    closeStreamWithoutThrowingException(fi);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", currentMethodNode.get(ccuFix).toString());
		// test the method
		Method moveInstance = CCUQuickFix.class.getDeclaredMethod("moveInstance", TryStatement.class, MethodInvocation.class);
		moveInstance.setAccessible(true);
		moveInstance.invoke(ccuFix, tryStat, mInvocation);
		// check postcondition
		assertEquals(
				"���U�O�w�������G�A�L�k����focus�b��e��editor�A�ҥHapply change�|�ߥX�ҥ~" +
				"�ثe�u�O��ASTRewrite�ק�A�S���I�sapplyChange�h�ק�쥻���e�A�٨S��X�@�Ӥ�k���ҵ��G",
				"@Robustness(value={@Tag(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInTry() throws IOException {\n"
						+ "  FileOutputStream fi=null;\n"
						+ "  try {\n"
						+ "    fi=new FileOutputStream(\"\");\n"
						+ "    fi.write(1);\n"
						+ "    closeStreamWithoutThrowingException(fi);\n"
						+ "  }\n"
						+ " catch (  FileNotFoundException e) {\n"
						+ "    throw e;\n" + "  }\n" + "}\n", currentMethodNode.get(ccuFix).toString());
	}
	
//	@Test
	public void testFindOutTheVariableInTryWithArgument() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		MethodDeclaration mDeclaration = (MethodDeclaration) methodList.get(29);
		TryStatement tryStat = (TryStatement) mDeclaration.getBody().statements().get(0);
		
		Field tryStatement = CCUQuickFix.class.getDeclaredField("tryStatement");
		tryStatement.setAccessible(true);
		tryStatement.set(ccuFix, tryStat);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, mDeclaration);
		
		Field tryIndex = CCUQuickFix.class.getDeclaredField("tryIndex");
		tryIndex.setAccessible(true);
		tryIndex.set(ccuFix, 0);
		
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		Field moveLine = CCUQuickFix.class.getDeclaredField("moveLine");
		moveLine.setAccessible(true);
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));
		
		// check precondition
		assertEquals(
				"@Robustness(value={@Tag(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInTry() throws IOException {\n"
						+ "  try {\n"
						+ "    FileOutputStream fi=new FileOutputStream(\"\");\n"
						+ "    fi.write(1);\n"
						+ "    closeStreamWithoutThrowingException(fi);\n"
						+ "  }\n"
						+ " catch (  FileNotFoundException e) {\n"
						+ "    throw e;\n" + "  }\n" + "}\n", currentMethodNode.get(ccuFix).toString());
		
		// test
		Method findOutTheVariableInTry = CCUQuickFix.class.getDeclaredMethod("findOutTheVariableInTry");
		findOutTheVariableInTry.setAccessible(true);
		findOutTheVariableInTry.invoke(ccuFix);
		
		// check postcondition
		assertEquals(
				"���U�O�w�������G�A�L�k����focus�b��e��editor�A�ҥHapply change�|�ߥX�ҥ~" +
				"�ثe�u�O��ASTRewrite�ק�A�S���I�sapplyChange�h�ק�쥻���e�A�٨S��X�@�Ӥ�k���ҵ��G",
				"@Robustness(value={@Tag(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInTry() throws IOException {\n"
						+ "  FileOutputStream fi=null;\n"
						+ "  try {\n"
						+ "    fi=new FileOutputStream(\"\");\n"
						+ "    fi.write(1);\n"
						+ "    closeStreamWithoutThrowingException(fi);\n"
						+ "  }\n"
						+ " catch (  FileNotFoundException e) {\n"
						+ "    throw e;\n" + "  }\n" + "}\n", currentMethodNode.get(ccuFix).toString());
	}

	/**
	 * Test the case if there is close invocation in try block,
	 * and test the case if finally block exists.
	 */
//	@Test
	public void testMoveToFinallyBlockWithCloseInTryAndFinallyDoExist() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);

		Field rewrite = CCUQuickFix.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(ccuFix, ASTRewrite.create(compilationUnit.getAST()));
		
		MethodDeclaration closeStreamInTryBlockWithBF = (MethodDeclaration) methodList.get(4);
		TryStatement tryStat = (TryStatement)(closeStreamInTryBlockWithBF.getBody().statements().get(1));
		
		Field tryStatement = CCUQuickFix.class.getDeclaredField("tryStatement");
		tryStatement.setAccessible(true);
		tryStatement.set(ccuFix, tryStat);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, closeStreamInTryBlockWithBF);
		
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		Field moveLine = CCUQuickFix.class.getDeclaredField("moveLine");
		moveLine.setAccessible(true);
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));

		ExpressionStatement eStatement = (ExpressionStatement) tryStat.getBody().statements().get(2);
		MethodInvocation mInvocation = (MethodInvocation) eStatement.getExpression();
		
		// check precondition
		assertEquals("fileOutputStream.close()", mInvocation.toString());
		assertEquals(
				"/** \n"
						+ " * �|�QCarelessCleanupVisitor�bfileOutputStream.close();�[�Wmark\n"
						+ " * @param context\n"
						+ " * @param outputFile\n"
						+ " */\n"
						+ "@Robustness(value={@Tag(level=1,exception=java.lang.RuntimeException.class)}) public void y_closeStreamInTryBlockWithBlankFinally(byte[] context,File outputFile){\n"
						+ "  FileOutputStream fileOutputStream=null;\n"
						+ "  try {\n"
						+ "    fileOutputStream=new FileOutputStream(outputFile);\n"
						+ "    fileOutputStream.write(context);\n"
						+ "    fileOutputStream.close();\n" + "  }\n"
						+ " catch (  FileNotFoundException e) {\n"
						+ "    throw new RuntimeException(e);\n" + "  }\n"
						+ "catch (  IOException e) {\n"
						+ "    throw new RuntimeException(e);\n" + "  }\n"
						+ " finally {\n" + "  }\n" + "}\n", currentMethodNode.get(ccuFix).toString());
		
		// test
		Method moveToFinallyBlock = CCUQuickFix.class.getDeclaredMethod("moveToFinallyBlock");
		moveToFinallyBlock.setAccessible(true);
		moveToFinallyBlock.invoke(ccuFix);

		// check precondition
		assertEquals("fileOutputStream.close()", mInvocation.toString());
		assertEquals(
				"���U�O�w�������G�A�L�k����focus�b��e��editor�A�ҥHapply change�|�ߥX�ҥ~" +
				"�ثe�u�O��ASTRewrite�ק�A�S���I�sapplyChange�h�ק�쥻���e�A�٨S��X�@�Ӥ�k���ҵ��G",
				"/** \n"
						+ " * �|�QCarelessCleanupVisitor�bfileOutputStream.close();�[�Wmark\n"
						+ " * @param context\n"
						+ " * @param outputFile\n"
						+ " */\n"
						+ "@Robustness(value={@Tag(level=1,exception=java.lang.RuntimeException.class)}) public void y_closeStreamInTryBlockWithBlankFinally(byte[] context,File outputFile){\n"
						+ "  FileOutputStream fileOutputStream=null;\n"
						+ "  try {\n"
						+ "    fileOutputStream=new FileOutputStream(outputFile);\n"
						+ "    fileOutputStream.write(context);\n" + "  }\n"
						+ " catch (  FileNotFoundException e) {\n"
						+ "    throw new RuntimeException(e);\n" + "  }\n"
						+ "catch (  IOException e) {\n"
						+ "    throw new RuntimeException(e);\n" + "  }\n"
						+ " finally {\n" + "    fileOutputStream.close();\n" +  "  }\n" + "}\n", currentMethodNode.get(ccuFix).toString());
	}

	/**
	 * Test the case if the close invocation in try block is implemented of Closable,
	 * but the close method doesn't throw exception
	 */
//	@Test
	public void testMoveToFinallyBlockWithCloseImplementClosableWithoutThrowException() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);

		Field rewrite = CCUQuickFix.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(ccuFix, ASTRewrite.create(compilationUnit.getAST()));
		
		MethodDeclaration closeStreamInTryBlockWithBF = (MethodDeclaration) methodList.get(32);
		TryStatement tryStat = (TryStatement)(closeStreamInTryBlockWithBF.getBody().statements().get(1));
		
		Field tryStatement = CCUQuickFix.class.getDeclaredField("tryStatement");
		tryStatement.setAccessible(true);
		tryStatement.set(ccuFix, tryStat);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, closeStreamInTryBlockWithBF);
		
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		Field moveLine = CCUQuickFix.class.getDeclaredField("moveLine");
		moveLine.setAccessible(true);
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));

		ExpressionStatement eStatement = (ExpressionStatement) tryStat.getBody().statements().get(1);
		MethodInvocation mInvocation = (MethodInvocation) eStatement.getExpression();
		
		// check precondition
		assertEquals("anInstance.close()", mInvocation.toString());
		assertEquals(
				"/** \n"
				+ " * �Y.close() method���|��X�ҥ~�A���i�H����quick fix���finally block��\n"
				+ " * @throws IOException\n"
				+ " */\n"
				+ "@Robustness(value={@Tag(level=1,exception=java.io.IOException.class)}) public void theCloseImplementClosableWillNotThrowException() throws IOException {\n"
				+ "  ClassImplementCloseableWithoutThrowException anInstance=null;\n"
				+ "  try {\n"
				+ "    anInstance=new ClassImplementCloseableWithoutThrowException();\n"
				+ "    anInstance.close();\n" + "  }\n"
				+ "  finally {\n" + "  }\n" + "}\n", currentMethodNode.get(ccuFix).toString());
		
		// test
		Method moveToFinallyBlock = CCUQuickFix.class.getDeclaredMethod("moveToFinallyBlock");
		moveToFinallyBlock.setAccessible(true);
		moveToFinallyBlock.invoke(ccuFix);

		// check precondition
		assertEquals("anInstance.close()", mInvocation.toString());
		assertEquals(
				"�o�Ӧa�観�ӿ��~�n�B�z�G\n"
						+ "    ���U�O�w�������G�A���]�L�k����focus�b��e��editor�A�ҥHapply change�|�ߥX�ҥ~\n"
						+ "    �ثe�u�O��ASTRewrite�ק�A�S���I�sapplyChange�h�ק�쥻���e�A�٨S��X�@�Ӥ�k���ҵ��G",
				"/** \n"
						+ " * �Y.close() method���|��X�ҥ~�A���i�H����quick fix���finally block��\n"
						+ " * @throws IOException\n"
						+ " */\n"
						+ "@Robustness(value={@Tag(level=1,exception=java.io.IOException.class)}) public void theCloseImplementClosableWillNotThrowException() throws IOException {\n"
						+ "  ClassImplementCloseableWithoutThrowException anInstance=null;\n"
						+ "  try {\n"
						+ "    anInstance=new ClassImplementCloseableWithoutThrowException();\n"
						+ "  }\n" + "  finally {\n"
						+ "    anInstance.close();\n" + "  }\n" + "}\n", currentMethodNode.get(ccuFix).toString());
	}
	
	/**
	 * Test the case if there is close invocation in catch block,
	 * and test the case if finally block doesn't exist.
	*/
//	@Test
	public void testMoveToFinallyBlockWithCloseInCatchAndFinallyNotExist() throws Exception {
		fail("���յ{���PtestMoveToFinallyBlockWithCloseInTryAndFinallyDoExist()��������\n���P�˷|�J��L�k����focus�b��e��editor�A�ҥHapply change�|�ߥX�ҥ~�����p");
	}

	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
