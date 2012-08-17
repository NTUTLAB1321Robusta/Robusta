package ntut.csie.csdet.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanupExample;
import ntut.csie.filemaker.exceptionBadSmells.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.ClassWithNotThrowingExceptionCloseable;
import ntut.csie.filemaker.exceptionBadSmells.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.UserDefinedCarelessCleanupWeather;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
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
	
	@Before
	public void setUp() throws Exception {
		String projectName = "CarelessCleanupExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(CarelessCleanupExample.class, "test");
		javaProjectMaker.createJavaFile(
				CarelessCleanupExample.class.getPackage().getName(),
				CarelessCleanupExample.class.getSimpleName() + ".java",
				"package " + CarelessCleanupExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassWithNotThrowingExceptionCloseable.class, "test");
		javaProjectMaker.createJavaFile(
				ClassWithNotThrowingExceptionCloseable.class.getPackage().getName(),
				ClassWithNotThrowingExceptionCloseable.class.getSimpleName() + ".java",
				"package " + ClassWithNotThrowingExceptionCloseable.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassImplementCloseable.class, "test");
		javaProjectMaker.createJavaFile(
				ClassImplementCloseable.class.getPackage().getName(),
				ClassImplementCloseable.class.getSimpleName() + ".java",
				"package " + ClassImplementCloseable.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		/* 測試使用者設定Pattern時候使用 */
		javaFile2String.read(UserDefinedCarelessCleanupWeather.class, "test");
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupWeather.class.getPackage().getName(),
				UserDefinedCarelessCleanupWeather.class.getSimpleName() + ".java",
				"package " + UserDefinedCarelessCleanupWeather.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(UserDefinedCarelessCleanupDog.class, "test");
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupDog.class.getPackage().getName(),
				UserDefinedCarelessCleanupDog.class.getSimpleName() + ".java",
				"package " + UserDefinedCarelessCleanupDog.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path ccExamplePath = new Path(
				projectName	+ "/src/ntut/csie/filemaker/exceptionBadSmells/CarelessCleanupExample.java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		
		CreateSettings();
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		
		ccuFix = new CCUQuickFix("");
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testFindMoveLine() throws Exception {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
		
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
		assertEquals(540, marker.getLineNumber());
	}
	
	@Test
	public void testMoveLineStatement() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
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
		assertEquals(	"@Robustness(value={@RL(level=1,exception=java.io.IOException.class)}) " +
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
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
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
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
		
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
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
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
	
	@Test
	public void testMoveInstance() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
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
		
		ExpressionStatement es = (ExpressionStatement)tryStat.getBody().statements().get(2);
		MethodInvocation mi = (MethodInvocation)es.getExpression();
		
		// check precondition
		assertEquals("closeStreamWithoutThrowingException(fi)", mi.toString());
		assertEquals(	"@Robustness(value={@RL(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInTry() throws IOException {\n" +
						"  try {\n" +
						"    FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"    closeStreamWithoutThrowingException(fi);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", currentMethodNode.get(ccuFix).toString());
		// test
		Method moveInstance = CCUQuickFix.class.getDeclaredMethod("moveInstance", TryStatement.class, MethodInvocation.class);
		moveInstance.setAccessible(true);
		moveInstance.invoke(ccuFix, tryStat, mi);
		// check postcondition
		// FIXME - 底下是預期的結果，由於是用ASTRewrite修改，沒有applyChange就不會修改原本內容，故要找出一個方法驗證結果
		assertEquals(	"@Robustness(value={@RL(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInTry() throws IOException {\n" +
						"  FileOutputStream fi=null;\n" +
						"  try {\n" +
						"    fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"    closeStreamWithoutThrowingException(fi);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", currentMethodNode.get(ccuFix).toString());
	}
	
	@Test
	public void testFindOutTheVariableInTry() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
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
		
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		Field moveLine = CCUQuickFix.class.getDeclaredField("moveLine");
		moveLine.setAccessible(true);
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));
		
		// check precondition
		assertEquals(	"@Robustness(value={@RL(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInTry() throws IOException {\n" +
						"  try {\n" +
						"    FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"    closeStreamWithoutThrowingException(fi);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", currentMethodNode.get(ccuFix).toString());
		// test
		Method findOutTheVariableInTry = CCUQuickFix.class.getDeclaredMethod("findOutTheVariableInTry");
		findOutTheVariableInTry.setAccessible(true);
		findOutTheVariableInTry.invoke(ccuFix);
		// check postcondition
		// FIXME - 底下是預期的結果，由於是用ASTRewrite修改，沒有applyChange就不會修改原本內容，故要找出一個方法驗證結果
		assertEquals(	"@Robustness(value={@RL(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInTry() throws IOException {\n" +
						"  FileOutputStream fi=null;\n" +
						"  try {\n" +
						"    fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"    closeStreamWithoutThrowingException(fi);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", currentMethodNode.get(ccuFix).toString());
	}
	
	@Test
	public void testMoveToFinallyBlock() throws Exception {
		Path path = new Path("CarelessCleanupExampleProject/src/ntut/csie/filemaker/exceptionBadSmells/CarelessCleanupExample.java");
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(path));
		
		Field actOpenable = BaseQuickFix.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		((IOpenable)javaElement).open(null);
		actOpenable.set(ccuFix, (IOpenable)javaElement);
		
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(ccuFix, compilationUnit);
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
		TryStatement tryStat = (TryStatement)((MethodDeclaration)methodList.get(29)).getBody().statements().get(0);
		
		Field tryStatement = CCUQuickFix.class.getDeclaredField("tryStatement");
		tryStatement.setAccessible(true);
		tryStatement.set(ccuFix, tryStat);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(ccuFix, methodList.get(29));
		
		Method findMoveLine = CCUQuickFix.class.getDeclaredMethod("findMoveLine", String.class);
		findMoveLine.setAccessible(true);
		Field moveLine = CCUQuickFix.class.getDeclaredField("moveLine");
		moveLine.setAccessible(true);
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));
		
		/** there is finally block in the try statement */
		// check precondition
		Field rewrite = CCUQuickFix.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		assertNull(rewrite.get(ccuFix));
		// test
		Method moveToFinallyBlock = CCUQuickFix.class.getDeclaredMethod("moveToFinallyBlock");
		moveToFinallyBlock.setAccessible(true);
		moveToFinallyBlock.invoke(ccuFix);
		
		// FIXME - 	原本構想是修改完ASTTree之後apply change，驗證修改結果是否正確，
		// 			目前卡在不知道怎樣模擬focus在當前的editor，所以apply change會拋例外。
		Method applyChange = BaseQuickFix.class.getDeclaredMethod("applyChange", ASTRewrite.class);
		applyChange.setAccessible(true);
		applyChange.invoke(ccuFix, rewrite.get(ccuFix));
		// check postcondition
//		assertEquals("", ((ASTRewrite)rewrite.get(ccuFix)).getAST().toString());
		
		/** there is no finally block in the try statement */
		tryStat = (TryStatement)((MethodDeclaration)methodList.get(30)).getBody().statements().get(1);
		tryStatement.set(ccuFix, tryStat);
		currentMethodNode.set(ccuFix, methodList.get(30));
		moveLine.set(ccuFix, findMoveLine.invoke(ccuFix, "0"));
		// test
		moveToFinallyBlock.invoke(ccuFix);
		// check postcondition
//		assertEquals("", ((ASTRewrite)rewrite.get(ccuFix)).getAST().toString());
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
