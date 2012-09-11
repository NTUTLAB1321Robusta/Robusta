package ntut.csie.csdet.data;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import junit.framework.TestCase;

public class MarkerInfoTest extends TestCase {
	private MarkerInfo markerInfo;
	
	protected void setUp() throws Exception {
		super.setUp();
		markerInfo = new MarkerInfo(RLMarkerAttribute.CS_INGNORE_EXCEPTION,
				null, "catch(Exception e){}", 155, 77, "Exception");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetCodeSmellType() {
		assertEquals(RLMarkerAttribute.CS_INGNORE_EXCEPTION, markerInfo.getCodeSmellType());
	}

	public void testGetLineNumber() {
		assertEquals(77,markerInfo.getLineNumber());
	}

	public void testGetExceptionType() {
		assertEquals("Exception", markerInfo.getExceptionType());
	}

	public void testGetStatement() {
		assertEquals("catch(Exception e){}", markerInfo.getStatement());
	}

	public void testGetPosition() {
		assertEquals(155,markerInfo.getPosition());
	}

}
