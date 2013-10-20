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
	public void testSetIgnoreExList() {
		int MAX = 10;
		List<MarkerInfo> ignoreExList = null;
		model.setIgnoreExList(ignoreExList, "ignoreMethod");
		assertEquals(0, model.getIgnoreSize());
		ignoreExList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			ignoreExList.add(new MarkerInfo("ignore", null, null, i, i*2, null));
		}
		model.setIgnoreExList(ignoreExList, "ignoreMethod");
		assertEquals(MAX, model.getIgnoreSize());
		assertEquals("ignore", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetDummyList() {
		int MAX = 10;
		List<MarkerInfo> dummyList = null;
		model.setIgnoreExList(dummyList, "dummyMethod");
		assertEquals(0, model.getIgnoreSize());
		dummyList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			dummyList.add(new MarkerInfo("dummy", null, null, i, i*2, null));
		}
		model.setIgnoreExList(dummyList, "dummyMethod");
		assertEquals(MAX, model.getIgnoreSize());
		assertEquals("dummy", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetNestedTryList() {
		int MAX = 10;
		List<MarkerInfo> nestedTryList = null;
		model.setIgnoreExList(nestedTryList, "nestedMethod");
		assertEquals(0, model.getIgnoreSize());
		nestedTryList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			nestedTryList.add(new MarkerInfo("nested", null, null, i, i*2, null));
		}
		model.setIgnoreExList(nestedTryList, "nestedMethod");
		assertEquals(MAX, model.getIgnoreSize());
		assertEquals("nested", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetUnprotectedMain() {
		int MAX = 10;
		List<MarkerInfo> unProtectedMain = null;
		model.setIgnoreExList(unProtectedMain, "mainMethod");
		assertEquals(0, model.getIgnoreSize());
		unProtectedMain = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			unProtectedMain.add(new MarkerInfo("main", null, null, i, i*2, null));
		}
		model.setIgnoreExList(unProtectedMain, "mainMethod");
		assertEquals(MAX, model.getIgnoreSize());
		assertEquals("main", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetOverLogging() {
		int MAX = 10;
		List<MarkerInfo> overLoggingList = null;
		model.setIgnoreExList(overLoggingList, "overMethod");
		assertEquals(0, model.getIgnoreSize());
		overLoggingList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			overLoggingList.add(new MarkerInfo("over", null, null, i, i*2, null));
		}
		model.setIgnoreExList(overLoggingList, "overMethod");
		assertEquals(MAX, model.getIgnoreSize());
		assertEquals("over", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetCarelessCleanUp() {
		int MAX = 10;
		List<MarkerInfo> carelessList = null;
		model.setIgnoreExList(carelessList, "cleanMethod");
		assertEquals(0, model.getIgnoreSize());
		carelessList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			carelessList.add(new MarkerInfo("clean", null, null, i, i*2, null));
		}
		model.setIgnoreExList(carelessList, "cleanMethod");
		assertEquals(MAX, model.getIgnoreSize());
		assertEquals("clean", model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
}
