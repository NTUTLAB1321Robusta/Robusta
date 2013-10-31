package ntut.csie.csdet.report;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.filemaker.JavaProjectMaker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClassModelTest {
	ClassModel model;
	
	@Before
	public void setUp() throws Exception {
		assertNull(model);
		model = new ClassModel();
		assertNotNull(model);
	}
	
	@After
	public void tearDown() throws Exception {
		
	}
	
	@Test
	public void testSetClassName() {
		model.setClassName(null);
		assertEquals("", model.getClassName());
		
		model.setClassName(ClassModel.class.getSimpleName());
		assertEquals("ClassModel", model.getClassName());
	}
	
	@Test
	public void testSetClassPath() {
		model.setClassName(ClassModel.class.getSimpleName());
		model.setClassPath(JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ ClassModel.class.getPackage().getName().replace(".", "/"));
		assertEquals(JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ ClassModel.class.getName().replace(".", "/"),
				model.getClassPath());
	}
	
	@Test
	public void testSetEmptyCatchList() {
		int MAX = 10;
		List<MarkerInfo> emptyCatchList = null;
		model.setEmptyCatchList(emptyCatchList, "ignoreMethod");
		assertEquals(0, model.getEmptySize());
		emptyCatchList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			emptyCatchList.add(new MarkerInfo("ignore", null, null, i, i*2, null));
		}
		model.setEmptyCatchList(emptyCatchList, "ignoreMethod");
		assertEquals(MAX, model.getEmptySize());
		assertEquals("ignore", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetDummyList() {
		int MAX = 10;
		List<MarkerInfo> dummyList = null;
		model.setEmptyCatchList(dummyList, "dummyMethod");
		assertEquals(0, model.getEmptySize());
		dummyList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			dummyList.add(new MarkerInfo("dummy", null, null, i, i*2, null));
		}
		model.setEmptyCatchList(dummyList, "dummyMethod");
		assertEquals(MAX, model.getEmptySize());
		assertEquals("dummy", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetNestedTryList() {
		int MAX = 10;
		List<MarkerInfo> nestedTryList = null;
		model.setEmptyCatchList(nestedTryList, "nestedMethod");
		assertEquals(0, model.getEmptySize());
		nestedTryList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			nestedTryList.add(new MarkerInfo("nested", null, null, i, i*2, null));
		}
		model.setEmptyCatchList(nestedTryList, "nestedMethod");
		assertEquals(MAX, model.getEmptySize());
		assertEquals("nested", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetUnprotectedMain() {
		int MAX = 10;
		List<MarkerInfo> unProtectedMain = null;
		model.setEmptyCatchList(unProtectedMain, "mainMethod");
		assertEquals(0, model.getEmptySize());
		unProtectedMain = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			unProtectedMain.add(new MarkerInfo("main", null, null, i, i*2, null));
		}
		model.setEmptyCatchList(unProtectedMain, "mainMethod");
		assertEquals(MAX, model.getEmptySize());
		assertEquals("main", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetOverLogging() {
		int MAX = 10;
		List<MarkerInfo> overLoggingList = null;
		model.setEmptyCatchList(overLoggingList, "overMethod");
		assertEquals(0, model.getEmptySize());
		overLoggingList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			overLoggingList.add(new MarkerInfo("over", null, null, i, i*2, null));
		}
		model.setEmptyCatchList(overLoggingList, "overMethod");
		assertEquals(MAX, model.getEmptySize());
		assertEquals("over", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetCarelessCleanUp() {
		int MAX = 10;
		List<MarkerInfo> carelessList = null;
		model.setEmptyCatchList(carelessList, "cleanMethod");
		assertEquals(0, model.getEmptySize());
		carelessList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			carelessList.add(new MarkerInfo("clean", null, null, i, i*2, null));
		}
		model.setEmptyCatchList(carelessList, "cleanMethod");
		assertEquals(MAX, model.getEmptySize());
		assertEquals("clean", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
}
