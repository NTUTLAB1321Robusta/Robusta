package ntut.csie.csdet.visitor.aidvisitor;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.sampleCode4VisitorTest.TryStatementExceptionsSampleCode;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TryStatementExceptionsVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	TryStatementExceptionsVisitor tryStatementExceptionVisitor;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "TryStatementExceptionsSampleCodeProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(TryStatementExceptionsSampleCode.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				TryStatementExceptionsSampleCode.class.getPackage().getName(),
				TryStatementExceptionsSampleCode.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + TryStatementExceptionsSampleCode.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(TryStatementExceptionsSampleCode.class, testProjectName));
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);

		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public final void testGetTotalExceptionStrings_RuntimeException_NestedInFinally() {
		/*
		try {
			fis = new FileInputStream(path);
			while(fis.available() != 0) {
				sb.append(fis.read());
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		 */
		TryStatement tryStatement = (TryStatement)NodeFinder.perform(compilationUnit, 818, 391);
		tryStatementExceptionVisitor = new TryStatementExceptionsVisitor(tryStatement);
		tryStatement.accept(tryStatementExceptionVisitor);
		assertEquals(1, tryStatementExceptionVisitor.getTotalExceptionStrings().length);
	}
	
	@Test
	public final void testGetTotalExceptionStrings_FileNotFoundException_IOException() {
		/*
		try {
			fos = new FileOutputStream(outputPath);
			fos.write(content);
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		 */
		TryStatement tryStatement = (TryStatement)NodeFinder.perform(compilationUnit, 1621, 309);
		tryStatementExceptionVisitor = new TryStatementExceptionsVisitor(tryStatement);
		tryStatement.accept(tryStatementExceptionVisitor);
		String[] totalExceptions = tryStatementExceptionVisitor.getTotalExceptionStrings();
		assertEquals(2, totalExceptions.length);
		int typeCount = 0;
		for (String anException : totalExceptions) {
			if(anException.equals(FileNotFoundException.class.getName())) {
				typeCount++;
			} else if (anException.equals(IOException.class.getName())) {
				typeCount++;
			}
		}
		assertEquals(2, typeCount);
	}
	
	@Test
	public final void testGetTotalExceptionStrings_IOException_TryFinally() {
		/*
		try {
			fos.write(10);
		} finally {
			fos.write(20);
		}
		 */
		TryStatement tryStatement = (TryStatement)NodeFinder.perform(compilationUnit, 2128, 63);
		tryStatementExceptionVisitor = new TryStatementExceptionsVisitor(tryStatement);
		tryStatement.accept(tryStatementExceptionVisitor);
		String[] totalExceptions = tryStatementExceptionVisitor.getTotalExceptionStrings();
		assertEquals(1, totalExceptions.length);
		assertEquals(IOException.class.getName(), totalExceptions[0]);
	}
	
	@Test
	public final void testGetTotalExceptionStrings_NestedInTry() {
		/*
		try {
			for(int i = 0; i<2; i++) {
				try {
					is = new FileInputStream(firstChoosenPath);
				} catch (FileNotFoundException e) {
					is = new FileInputStream(defaultPath);
					is.read();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		 */
		TryStatement tryStatement = (TryStatement)NodeFinder.perform(compilationUnit, 2403, 271);
		tryStatementExceptionVisitor = new TryStatementExceptionsVisitor(tryStatement);
		tryStatement.accept(tryStatementExceptionVisitor);
		String[] totalExceptions = tryStatementExceptionVisitor.getTotalExceptionStrings();
		assertEquals(0, totalExceptions.length);
	}
	
	@Test
	public final void testGetTotalExceptionStrings_ComlicatedNestedInTry() {
		// public void complicatedNestedTryStatement()
		TryStatement tryStatement = (TryStatement)NodeFinder.perform(compilationUnit, 2759, 965);
		tryStatementExceptionVisitor = new TryStatementExceptionsVisitor(tryStatement);
		tryStatement.accept(tryStatementExceptionVisitor);
		String[] totalExceptions = tryStatementExceptionVisitor.getTotalExceptionStrings();
		assertEquals(0, totalExceptions.length);
	}
	
	@Test
	public final void testGetTotalExceptionStrings_ComlicatedNestedInTry_ThrowsExceptions() {
		// public void complicatedNestedTryStatement()
		TryStatement tryStatement = (TryStatement)NodeFinder.perform(compilationUnit, 3883, 819);
		tryStatementExceptionVisitor = new TryStatementExceptionsVisitor(tryStatement);
		tryStatement.accept(tryStatementExceptionVisitor);
		String[] totalExceptions = tryStatementExceptionVisitor.getTotalExceptionStrings();
		assertEquals(showAllExceptions(totalExceptions), 3, totalExceptions.length);
	}

	@Test
	public final void testGetTotalExceptionStrings_ComlicatedNestedInTry_() {
		// public void complicatedNestedTryStatement()
		TryStatement tryStatement = (TryStatement)NodeFinder.perform(compilationUnit, 4231, 438);
		tryStatementExceptionVisitor = new TryStatementExceptionsVisitor(tryStatement);
		tryStatement.accept(tryStatementExceptionVisitor);
		String[] totalExceptions = tryStatementExceptionVisitor.getTotalExceptionStrings();
		assertEquals(showAllExceptions(totalExceptions), 2, totalExceptions.length);
		int typeCount = 0;
		for (String anException : totalExceptions) {
			if(anException.equals(FileNotFoundException.class.getName())) {
				typeCount++;
			} else if (anException.equals(RuntimeException.class.getName())) {
				typeCount++;
			}
		}
		assertEquals(2, typeCount);
	}
	
	private String showAllExceptions(String[] exceptions) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for(String e : exceptions) {
			sb.append(e);
			sb.append("\n");
		}
		return sb.toString();
	}
}
