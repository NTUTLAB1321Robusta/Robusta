package ntut.csie.csdet.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.DummyHandlerVisitor;
import ntut.csie.csdet.visitor.SuppressWarningVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.filemaker.exceptionBadSmells.UserDefineDummyHandlerFish;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReportBuilderTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	ReportBuilder reportBuilder;
	IProject project;
	String projectName;
	SmellSettings smellSettings;
	
	public ReportBuilderTest() {
		projectName = "DummyHandlerTest";
	}

	@Before
	public void setUp() throws Exception {
		
		// 讀取測試檔案樣本內容
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// 新增欲載入的library
		javaProjectMaker.addJarFromProjectToBuildPath("lib\\log4j-1.2.15.jar");
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		
		// 根據測試檔案樣本內容建立新的檔案
		javaFileToString.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ DummyAndIgnoreExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UserDefineDummyHandlerFish.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		CreateSettings();
		
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		reportBuilder = new ReportBuilder(project, new ReportModel());

		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, projectName));
		
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
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
	public void testCountFileLOC() throws Exception {
		
		/** 正確路徑下的class file */
		Method countFileLOC = ReportBuilder.class.getDeclaredMethod("countFileLOC", String.class);
		countFileLOC.setAccessible(true);
		// 檢查測試專案檔案的行數
		assertEquals(299, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, projectName)));
		/** 路徑不正確或者不存在的class file */
		assertEquals(0, countFileLOC.invoke(reportBuilder, "not/exist/example.java"));
	}
	
	@Test
	public void testGetSourcePaths() throws Exception {
		IJavaProject javaPrj = JavaCore.create(project);
		IPackageFragmentRoot[] roots = javaPrj.getAllPackageFragmentRoots();
		
		// 檢查precondition
		assertEquals(13, roots.length);
		for(int i = 0; i < roots.length; i++) {
			if(i == roots.length - 1)
				assertEquals(JavaProjectMaker.FOLDERNAME_SOURCE, roots[i].getElementName());
			else
				assertTrue(roots[i].getPath().toString().endsWith(".jar"));
		}
		// 執行測試對象
		Method getSourcePaths = ReportBuilder.class.getDeclaredMethod("getSourcePaths", IJavaProject.class);
		getSourcePaths.setAccessible(true);
		List<IPackageFragmentRoot> srcPaths = (List)getSourcePaths.invoke(reportBuilder, javaPrj);
		// 驗證結果，排除jar檔路徑，只要src資訊
		assertEquals(1, srcPaths.size());
		assertEquals("F/DummyHandlerTest/src", srcPaths.get(0).getUnderlyingResource().toString());
	}
	
	@Test
	public void testGetTrimFolderName() throws Exception {
		Method getTrimFolderName = ReportBuilder.class.getDeclaredMethod("getTrimFolderName", String.class);
		getTrimFolderName.setAccessible(true);
		assertEquals(DummyAndIgnoreExample.class.getPackage().getName(), (String)getTrimFolderName.invoke(reportBuilder, "EH_LEFTsrcEH_RIGHT" + DummyAndIgnoreExample.class.getPackage().getName()));
	}
	
	@Test
	public void testIsConformFolderFormat() throws Exception {
		Method isConformFolderFormat = ReportBuilder.class.getDeclaredMethod("isConformFolderFormat", String.class);
		isConformFolderFormat.setAccessible(true);
		assertTrue((Boolean)isConformFolderFormat.invoke(reportBuilder, "EH_LEFTsrcEH_RIGHT" + DummyAndIgnoreExample.class.getPackage().getName()));
		assertFalse((Boolean)isConformFolderFormat.invoke(reportBuilder, DummyAndIgnoreExample.class.getPackage().getName()));
		assertFalse((Boolean)isConformFolderFormat.invoke(reportBuilder, "srcEH_RIGHT" + DummyAndIgnoreExample.class.getPackage().getName()));
		assertFalse((Boolean)isConformFolderFormat.invoke(reportBuilder, "EH_LEFT" + DummyAndIgnoreExample.class.getPackage().getName()));
		assertFalse((Boolean)isConformFolderFormat.invoke(reportBuilder, "EH_RIGHTsrcEH_LEFTntut.csie.exceptionBadSmells" + DummyAndIgnoreExample.class.getPackage().getName()));
	}
	
	@Test
	public void testGetFolderName() throws Exception {
		Method getFolderName = ReportBuilder.class.getDeclaredMethod("getFolderName", String.class);
		getFolderName.setAccessible(true);
		assertEquals(JavaProjectMaker.FOLDERNAME_SOURCE, getFolderName.invoke(reportBuilder, "EH_LEFTsrcEH_RIGHT" + DummyAndIgnoreExample.class.getPackage().getName()));
		assertEquals(JavaProjectMaker.FOLDERNAME_SOURCE, getFolderName.invoke(reportBuilder, "EH_LEFTsrcEH_RIGHTntut.csie.*"));
		assertEquals(JavaProjectMaker.FOLDERNAME_SOURCE, getFolderName.invoke(reportBuilder, "EH_LEFTsrcEH_RIGHT"));
	}
	
	@Test
	public void testIsConformMultiPackageFormat() throws Exception {
		Method isConformMultiPackageFormat = ReportBuilder.class.getDeclaredMethod("isConformMultiPackageFormat", IPackageFragment.class, String.class);
		isConformMultiPackageFormat.setAccessible(true);
		
		IJavaProject javaPrj = JavaCore.create(project);
		List<IPackageFragmentRoot> root = reportBuilder.getSourcePaths(javaPrj);
		
		for(int i = 0; i < root.size(); i++) {
			IJavaElement[] packages = root.get(i).getChildren();
			for (int j = 0; j < packages.length; j++) {
				if (packages[j].getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
					IPackageFragment iPackageFgt = (IPackageFragment) packages[j];
					// i = 2 此時才會找到ntut.csie這個package
					if(j >= 2) {
						assertTrue((Boolean)isConformMultiPackageFormat.invoke(reportBuilder, iPackageFgt, "ntut.csie.EH_STAR"));
						assertTrue((Boolean)isConformMultiPackageFormat.invoke(reportBuilder, iPackageFgt, "EH_LEFTsrcEH_RIGHTntut.csie.EH_STAR"));
					}
					else {
						assertFalse((Boolean)isConformMultiPackageFormat.invoke(reportBuilder, iPackageFgt, "ntut.csie.EH_STAR"));
						assertFalse((Boolean)isConformMultiPackageFormat.invoke(reportBuilder, iPackageFgt, "EH_LEFTsrcEH_RIGHTntut.csie.EH_STAR"));
					}
					assertFalse((Boolean)isConformMultiPackageFormat.invoke(reportBuilder, iPackageFgt, "EH_LEFTsrcEH_RIGHTntut.EH_STAR.csie"));
				}
			}
		}
	}
	
	@Test
	public void testDetermineRecord() throws Exception {
		Method determineRecord = ReportBuilder.class.getDeclaredMethod("determineRecord", String.class, IPackageFragment.class);
		determineRecord.setAccessible(true);
		Field isAllPackage = ReportBuilder.class.getDeclaredField("isAllPackage");
		isAllPackage.setAccessible(true);
		isAllPackage.set(reportBuilder, true);
		
		IJavaProject javaPrj = JavaCore.create(project);
		List<IPackageFragmentRoot> root = reportBuilder.getSourcePaths(javaPrj);
		
		/** 如果isAllPackage為true，則每個都要記錄 */
		for(int i = 0; i < root.size(); i++) {
			String folderName = root.get(i).getElementName();
			IJavaElement[] packages = root.get(i).getChildren();
			for (int j = 0; j < packages.length; j++) {
				if (packages[j].getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
					IPackageFragment iPackageFgt = (IPackageFragment) packages[j];
					assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
				}
			}
		}
		
		/** 如果有使用filter，則只有在符合filter條件的才要記錄 */
		isAllPackage.set(reportBuilder, false);
		// 設定filter rule
		ArrayList<ArrayList<String>> ruleList = new ArrayList<ArrayList<String>>();
		ArrayList<String> tempList = new ArrayList<String>();
		tempList.add("EH_LEFTsrcEH_RIGHT");
		ruleList.add(tempList);
		tempList = new ArrayList<String>();
		tempList.add("EH_LEFTsrcEH_RIGHTntut.EH_STAR");
		ruleList.add(tempList);
		tempList = new ArrayList<String>();
		tempList.add("EH_LEFTsrcEH_RIGHTntut.csie.EH_STAR");
		ruleList.add(tempList);
		tempList = new ArrayList<String>();
		tempList.add("EH_LEFTsrcEH_RIGHTntut.csie.filemaker.EH_STAR");
		ruleList.add(tempList);
		tempList = new ArrayList<String>();
		tempList.add("EH_LEFTsrcEH_RIGHT" + DummyAndIgnoreExample.class.getPackage().getName());
		ruleList.add(tempList);
		tempList = new ArrayList<String>();
		tempList.add("ntut.EH_STAR");
		ruleList.add(tempList);
		tempList = new ArrayList<String>();
		tempList.add("ntut.csie.EH_STAR");
		tempList = new ArrayList<String>();
		tempList.add("ntut.csie.filemaker.EH_STAR");
		ruleList.add(tempList);
		tempList = new ArrayList<String>();
		tempList.add(DummyAndIgnoreExample.class.getPackage().getName());
		ruleList.add(tempList);
		
		Field filterRuleList = ReportBuilder.class.getDeclaredField("filterRuleList");
		filterRuleList.setAccessible(true);
		
		for(int k = 0; k < ruleList.size(); k++) {
			filterRuleList.set(reportBuilder, ruleList.get(k));
			for(int i = 0; i < root.size(); i++) {
				String folderName = root.get(i).getElementName();
				IJavaElement[] packages = root.get(i).getChildren();
				for (int j = 0; j < packages.length; j++) {
					if (packages[j].getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment iPackageFgt = (IPackageFragment) packages[j];
						if(k == 0) {
							if(j == 0)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
						else if(k == 1) {
							if(j == 0)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
						else if(k == 2) {
							if(j == 0)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
						else if(k == 3) {
							if(j == 0)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
						else if(k == 4) {
							if(j == 0)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
						else if(k == 5) {
							if(j == 0)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
						else if(k == 6) {
							if(j == 0)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
						else if(k == 7) {
							if(j == 0)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
						else if(k == 8) {
							if(j == 0)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 1)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 2)
								assertFalse((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 3)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
							else if(j == 4)
								assertTrue((Boolean)determineRecord.invoke(reportBuilder, folderName, iPackageFgt));
						}
					}
				}
			}
		}
	}
	
	@Test
	public void testInputSuppressData() throws Exception {
		Method inputSuppressData = ReportBuilder.class.getDeclaredMethod("inputSuppressData", List.class, TreeMap.class, TreeMap.class);
		inputSuppressData.setAccessible(true);
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		// 取得專案中所有的method
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		for(int i = 0; i < methodList.size(); i++) {
			SuppressWarningVisitor visitor = new SuppressWarningVisitor(compilationUnit);
			methodList.get(i).accept(visitor);
			List<SSMessage> suppressSmellList = visitor.getSuppressWarningList();
			TreeMap<String, Boolean> detMethodSmell = new TreeMap<String, Boolean>();
			TreeMap<String, List<Integer>> detCatchSmell = new TreeMap<String, List<Integer>>();
			inputSuppressData.invoke(reportBuilder, suppressSmellList, detMethodSmell, detCatchSmell);
			// FIXME - 由於目前測試範本沒有suppress marker範例，故suppressSmellList為0
			assertEquals(0, suppressSmellList.size());
			assertEquals(7, detMethodSmell.size());
			assertEquals(3, detCatchSmell.size());
		}
	}
	
	@Test
	public void testSuppressMarker() throws Exception {
		Method suppressMarker = ReportBuilder.class.getDeclaredMethod("suppressMarker", List.class, int.class);
		suppressMarker.setAccessible(true);
		
		List<Integer> smellPosList = new ArrayList<Integer>();
		for(int i = 0; i < 10; i+=2) {
			smellPosList.add(i);
		}
		for(int i = 0; i < 10; i++) {
			if(i%2 == 0)
				assertTrue((Boolean)suppressMarker.invoke(reportBuilder, smellPosList, i));
			else
				assertFalse((Boolean)suppressMarker.invoke(reportBuilder, smellPosList, i));
		}
	}
	
	@Test
	public void testCheckCatchSmell() throws Exception {
		Method checkCatchSmell = ReportBuilder.class.getDeclaredMethod("checkCatchSmell", List.class, List.class);
		checkCatchSmell.setAccessible(true);
		
		DummyHandlerVisitor dhVisitor = new DummyHandlerVisitor(compilationUnit);
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		// 取得專案中所有的method
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		methodList.get(10).accept(dhVisitor);
		/* FIXME - 暫時轉換用，等全部都換成MarkerInfo就不需要這個LOOP */
		List<MarkerInfo> dhList = dhVisitor.getDummyList();
		List<MarkerInfo> tempList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < dhList.size(); i++) {
			MarkerInfo message = new MarkerInfo(dhList.get(i).getCodeSmellType(), dhList.get(i).getTypeBinding(), dhList.get(i).getStatement(), dhList.get(i).getPosition(), dhList.get(i).getLineNumber(), dhList.get(i).getExceptionType());
			tempList.add(message);
		}
		
		List<MarkerInfo> result = null;
		result = (List<MarkerInfo>)checkCatchSmell.invoke(reportBuilder, tempList, null);
		assertEquals(2, result.size());
		
		List<Integer> posList = new ArrayList<Integer>();
		result = (List<MarkerInfo>)checkCatchSmell.invoke(reportBuilder, tempList, posList);
		assertEquals(2, result.size());
	}
	
	@Test
	public void testSetSmellInfo() throws Exception {
		Method setSmellInfo = ReportBuilder.class.getDeclaredMethod("setSmellInfo", ICompilationUnit.class, boolean.class, PackageModel.class, String.class);
		setSmellInfo.setAccessible(true);
		
		Field model = ReportBuilder.class.getDeclaredField("model");
		model.setAccessible(true);
		ReportModel reportModel = (ReportModel)model.get(reportBuilder); 
		
		// 取得目前專案
		IJavaProject javaPrj = JavaCore.create(project);
		List<IPackageFragmentRoot> root = reportBuilder.getSourcePaths(javaPrj);
		for(int i = 0; i < root.size(); i++) {
			// 取得Root底下的所有Package
			IJavaElement[] packages = root.get(i).getChildren();
			for(IJavaElement iJavaElement : packages) {
				if (iJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
					IPackageFragment iPackageFgt = (IPackageFragment) iJavaElement;
					// 取得Package底下的class
					ICompilationUnit[] compilationUnits = iPackageFgt.getCompilationUnits();
					for(int j = 0; j < compilationUnits.length; j++) {
						PackageModel newPackageModel = reportModel.addSmellList(iPackageFgt.getElementName());
						setSmellInfo.invoke(reportBuilder, compilationUnits[j], true, newPackageModel, iPackageFgt.getPath().toString());
					}
				}
			}
		}
		assertEquals(25, reportModel.getTryCounter());
		assertEquals(25, reportModel.getCatchCounter());
		assertEquals(2, reportModel.getFinallyCounter());
		assertEquals(0, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(19, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(0, reportModel.getNestedTryTotalSize());
		assertEquals(0, reportModel.getOverLoggingTotalSize());
	}
	
	@Test
	public void testAnalysisProject() throws Exception {
		Method analysisProject = ReportBuilder.class.getDeclaredMethod("analysisProject", IProject.class);
		analysisProject.setAccessible(true);
		
		Method getFilterSettings = ReportBuilder.class.getDeclaredMethod("getFilterSettings");
		getFilterSettings.setAccessible(true);
		getFilterSettings.invoke(reportBuilder);
		
		Field model = ReportBuilder.class.getDeclaredField("model");
		model.setAccessible(true);
		ReportModel reportModel = (ReportModel)model.get(reportBuilder);
		reportModel.setBuildTime();
		analysisProject.invoke(reportBuilder, project);
		
		assertTrue(reportModel.getProjectPath().contains("junit-workspace/DummyHandlerTest/_Report"));
		assertEquals(1, reportModel.getPackagesSize());
	}
	
	@Test
	public void testGetFilterSettings() throws Exception {
		/** 沒有xml，則預設偵測全部 */
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		
		Method getFilterSettings = ReportBuilder.class.getDeclaredMethod("getFilterSettings");
		getFilterSettings.setAccessible(true);
		getFilterSettings.invoke(reportBuilder);
		
		Field isAllPackage = ReportBuilder.class.getDeclaredField("isAllPackage");
		isAllPackage.setAccessible(true);
		assertTrue((Boolean)isAllPackage.get(reportBuilder));
		
		Field filterRuleList = ReportBuilder.class.getDeclaredField("filterRuleList");
		filterRuleList.setAccessible(true);
		List<String> ruleList = (List)filterRuleList.get(reportBuilder);
		assertEquals(0, ruleList.size());
		
		/** xml存在，且裡面有設定，則依據內容的設定來偵測 */
		ForTestGetFilterSettings();
		
		getFilterSettings.invoke(reportBuilder);
		assertFalse((Boolean)isAllPackage.get(reportBuilder));
		assertEquals(3, ruleList.size());
		assertEquals(JavaProjectMaker.FOLDERNAME_SOURCE, ruleList.get(0));
		assertEquals("ntut.csie.EH_STAR", ruleList.get(1));
		assertEquals(DummyAndIgnoreExample.class.getPackage().getName(), ruleList.get(2));
	}
	
//	@Test
	public void testRun() throws Exception {
		reportBuilder.run();
		// 如果不停1秒，跑test suite會掛掉(紅燈)，認為可能是eclipse自己的thread的問題
		Thread.sleep(1000);
		fail("結果未驗證");
	}

	private void ForTestGetFilterSettings() {
		//取的XML的root
		Element root = JDomUtil.createXMLContent();

		// 建立report filter的tag
		Element filter = new Element(JDomUtil.EHSmellFilterTaq);
		Element filterRules = new Element("filter");
		
		filterRules.setAttribute("IsAllPackage", "false");
		filterRules.setAttribute(JavaProjectMaker.FOLDERNAME_SOURCE, "true");
		filterRules.setAttribute("ntut.csie.EH_STAR", "true");
		filterRules.setAttribute(DummyAndIgnoreExample.class.getPackage().getName(), "true");
		
		filter.addContent(filterRules);

		if (root.getChild(JDomUtil.DummyHandlerTag) != null)
			root.removeChild(JDomUtil.DummyHandlerTag);

		root.addContent(filter);

		//將檔案寫回
		String path = JDomUtil.getWorkspace() + File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}
	
	/**
	 * 建立CSPreference.xml檔案
	 */
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}