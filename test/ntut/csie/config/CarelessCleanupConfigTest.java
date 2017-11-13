package ntut.csie.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.careless.CarelessCleanupVisitor;
import ntut.csie.aspect.AddAspectsMarkerResoluationForCarelessCleanup;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.failFastUT.CarelessCleanup.carelessCleanupExample;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.MethodInvocationCollectorVisitor;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanupConfigTest {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private CarelessCleanupVisitor CarelessCleanupVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> markerInfos;

	private AddAspectsMarkerResoluationForCarelessCleanup CarelessCleanupResoluation;
	private IMarker marker;
	private Path CarelessCleanupExamplePath;

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		markerInfos = visitCompilationUnitCarelessCleanupAndGetSmellList();
		setUpMethodIndexOfMarkerInfo();
		CarelessCleanupResoluation = new AddAspectsMarkerResoluationForCarelessCleanup(
				"test");
		CarelessCleanupExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject"
						+ "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE
						+ "/"
						+ PathUtils.dot2slash(carelessCleanupExample.class
								.getName())
						+ JavaProjectMaker.JAVA_FILE_EXTENSION);
	}

	private List<MarkerInfo> visitCompilationUnitCarelessCleanupAndGetSmellList()
			throws JavaModelException {
		CarelessCleanupVisitor = new CarelessCleanupVisitor(compilationUnit,
				true);
		compilationUnit.accept(CarelessCleanupVisitor);
		return CarelessCleanupVisitor.getCarelessCleanupList();
	}

	private void setUpTestingEnvironment() throws Exception, JavaModelException {
		environmentBuilder = new TestEnvironmentBuilder(
				"AddAspectsMarkerResoluationExampleProject");
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(carelessCleanupExample.class);
		compilationUnit = environmentBuilder
				.getCompilationUnit(carelessCleanupExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);

		for (MarkerInfo m : markerInfos) {
			int methodIdx = -1;
			for (MethodDeclaration declaration : methodCollector
					.getMethodList()) {
				methodIdx++;
				MethodInvocationCollectorVisitor methodInvocationCollector = new MethodInvocationCollectorVisitor();
				declaration.accept(methodInvocationCollector);
				for (MethodInvocation invocation : methodInvocationCollector
						.getMethodInvocations()) {
					if (m.getLineNumber() == compilationUnit
							.getLineNumber(invocation.getStartPosition())) {
						m.setMethodIndex(methodIdx);
						break;
					}

				}
			}
		}
	}

	private IMarker getSpecificMarkerByMarkerInfoIndex(int index, Path filePath) {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin
				.getWorkspace().getRoot().getFile(filePath));
		IMarker tempMarker = null;
		try {
			tempMarker = javaElement.getResource().createMarker("test.test");
			tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX,
					Integer.toString(markerInfos.get(index).getMethodIndex()));
			tempMarker.setAttribute(IMarker.LINE_NUMBER, new Integer(
					markerInfos.get(index).getLineNumber()));
			tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE,
					markerInfos.get(index).getCodeSmellType());

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("throw exception");
		}
		return tempMarker;
	}

	@After
	public void tearDown() throws Exception {
		 environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testGetCollectBadSmellMethodsFirst() {
		List<String> expected = new ArrayList<String>();
		expected.add("read");
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				CarelessCleanupExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		List<String> actual = builder.getCollectBadSmellMethods();
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testGetCollectBadSmellMethodsSecond() {
		List<String> expected = new ArrayList<String>();
		expected.add("read");
		marker = getSpecificMarkerByMarkerInfoIndex(1,
				CarelessCleanupExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		List<String> actual = builder.getCollectBadSmellMethods();
		Assert.assertEquals(expected, actual);
	}
	
	
	@Test
	public void testGetCollectBadSmellExceptionTypesFirst() {
		List<String> expected = new ArrayList<String>();
		expected.add("IOException");
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				CarelessCleanupExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		List<String> actual = builder.getCollectBadSmellExceptionTypes();
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testGetCollectBadSmellExceptionTypesSecond() {
		List<String> expected = new ArrayList<String>();
		expected.add("IOException");
		marker = getSpecificMarkerByMarkerInfoIndex(1,
				CarelessCleanupExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		List<String> actual = builder.getCollectBadSmellExceptionTypes();
		Assert.assertEquals(expected, actual);
	}

}
