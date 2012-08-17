package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanupExample;
import ntut.csie.filemaker.exceptionBadSmells.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.ClassWithNotThrowingExceptionCloseable;
import ntut.csie.filemaker.exceptionBadSmells.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.UserDefinedCarelessCleanupWeather;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanupVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	CarelessCleanupVisitor carelessCleanupVisitor;
	SmellSettings smellSettings;

	@Before
	public void setUp() throws Exception {
		String projectName = "CarelessCleanupExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
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
		
		/* ���ըϥΪ̳]�wPattern�ɭԨϥ� */
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
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		// �إ�XML
		CreateSettings();
		// ���oAST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		carelessCleanupVisitor = new CarelessCleanupVisitor(compilationUnit);
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
	}
	
	@Test
	public void testGetCarelessCleanupListWithoutExtraRules() {
		/* ���s���ͳ]�w�� */
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// �]��Setup�ɭԤw�g���X�@�Ӧ�ExtraRule���]�w�ɡA�{�b�n�S�a��ExtraRule�������Ӵ��աC
		smellSettings.removeExtraRule(
			SmellSettings.SMELL_CARELESSCLEANUP,
			SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		// ���s����Visitor�A�ϱo�]�w�Ȧ��s�JCarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor(compilationUnit);
		
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				23, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupList() {
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				25, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupListWithUserDefiendLibs() throws Exception {	
		// ���ͳ]�w��
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupWeather.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		// ���s����Visitor�A�ϱo�]�w�Ȧ��s�JCarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor(compilationUnit);
		
		// �������G
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				28, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupListWithUserDefiendFullQualifiedMethods() throws Exception {	
		// ���ͳ]�w��
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupWeather.class.getName() + ".bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		// ���s����Visitor�A�ϱo�]�w�Ȧ��s�JCarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor(compilationUnit);
		
		// �������G
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				26, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupListWithUserDefiendMethods() throws Exception {	
		// ���ͳ]�w��
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		// ���s����Visitor�A�ϱo�]�w�Ȧ��s�JCarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor(compilationUnit);
		
		// �������G
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				27, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupListWithUserDefiendOnlyMethods() throws Exception {	
		// ���ͳ]�w��
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern("rain", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		// ���s����Visitor�A�ϱo�]�w�Ȧ��s�JCarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor(compilationUnit);
		
		// �������G
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				26, carelessCleanupSmellCount);
	}
	
	/**
	 * �����Ҧ�badSmell���e�H�Φ渹
	 * @param badSmellList
	 * @return
	 */
	private String colloectBadSmellListContent(List<MarkerInfo> badSmellList) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for (int i = 0; i < badSmellList.size(); i++) {
			MarkerInfo m = badSmellList.get(i);
			sb.append(m.getLineNumber()).append("\t").append(m.getStatement()).append("\n");
		}
		return sb.toString();
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
