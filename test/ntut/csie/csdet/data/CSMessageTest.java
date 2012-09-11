package ntut.csie.csdet.data;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import junit.framework.TestCase;

public class CSMessageTest extends TestCase {
	private CSMessage cm;
	
	protected void setUp() throws Exception {
		super.setUp();
		cm = new CSMessage(RLMarkerAttribute.CS_INGNORE_EXCEPTION, null,
				"catch(Exception e){}", 155, 77, "Exception");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetCodeSmellType() {
		assertEquals(RLMarkerAttribute.CS_INGNORE_EXCEPTION,cm.getCodeSmellType());
	}

	public void testGetLineNumber() {
		assertEquals(77,cm.getLineNumber());
	}

	public void testGetExceptionType() {
		assertEquals("Exception",cm.getExceptionType());
	}

	public void testGetStatement() {
		assertEquals("catch(Exception e){}",cm.getStatement());
	}

	public void testGetPosition() {
		assertEquals(155,cm.getPosition());
	}

}
