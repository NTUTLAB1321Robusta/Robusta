package ntut.csie.csdet.report;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;

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
		List<MarkerInfo> ignoreExList = null;
		model.addSmellList(ignoreExList);
		assertEquals(0, model.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		ignoreExList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			ignoreExList.add(new MarkerInfo(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK, null, null, i, i*2, null, "ignoreMethod", null));
		}
		model.addSmellList(ignoreExList);
		assertEquals(MAX, model.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK, model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetDummyList() {
		int MAX = 10;
		List<MarkerInfo> dummyList = null;
		model.addSmellList(dummyList);
		assertEquals(0, model.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		dummyList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			dummyList.add(new MarkerInfo(RLMarkerAttribute.CS_DUMMY_HANDLER, null, null, i, i*2, null, "dummyMethod", null));
		}
		model.addSmellList(dummyList);
		assertEquals(MAX, model.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(RLMarkerAttribute.CS_DUMMY_HANDLER, model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetNestedTryList() {
		int MAX = 10;
		List<MarkerInfo> nestedTryList = null;
		model.addSmellList(nestedTryList);
		assertEquals(0, model.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		nestedTryList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			nestedTryList.add(new MarkerInfo(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT, null, null, i, i*2, null, "nested", null));
		}
		model.addSmellList(nestedTryList);
		assertEquals(MAX, model.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT, model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetUnprotectedMain() {
		int MAX = 10;
		List<MarkerInfo> unProtectedMain = null;
		model.addSmellList(unProtectedMain);
		assertEquals(0, model.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		unProtectedMain = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			unProtectedMain.add(new MarkerInfo(RLMarkerAttribute.CS_UNPROTECTED_MAIN, null, null, i, i*2, null, "main", null));
		}
		model.addSmellList(unProtectedMain);
		assertEquals(MAX, model.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(RLMarkerAttribute.CS_UNPROTECTED_MAIN, model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetOverLogging() {
		int MAX = 10;
		List<MarkerInfo> overLoggingList = null;
		model.addSmellList(overLoggingList);
		assertEquals(0, model.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		overLoggingList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			overLoggingList.add(new MarkerInfo(RLMarkerAttribute.CS_OVER_LOGGING, null, null, i, i*2, null, "over", null));
		}
		model.addSmellList(overLoggingList);
		assertEquals(MAX, model.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(RLMarkerAttribute.CS_OVER_LOGGING, model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
	
	@Test
	public void testSetCarelessCleanup() {
		int MAX = 10;
		List<MarkerInfo> carelessList = null;
		model.addSmellList(carelessList);
		assertEquals(0, model.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		carelessList = new ArrayList<MarkerInfo>();
		for(int i = 0; i < MAX; i++) {
			carelessList.add(new MarkerInfo(RLMarkerAttribute.CS_CARELESS_CLEANUP, null, null, i, i*2, null, "clean", null));
		}
		model.addSmellList(carelessList);
		assertEquals(MAX, model.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(RLMarkerAttribute.CS_CARELESS_CLEANUP, model.getSmellType(2));
		assertEquals(8, model.getSmellLine(4));
	}
}
