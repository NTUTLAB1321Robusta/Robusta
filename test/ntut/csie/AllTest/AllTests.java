package ntut.csie.AllTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for ntut.csie.AllTest");
		//$JUnit-BEGIN$
		suite.addTest(new TestSuite(ntut.csie.csdet.preference.JDomUtilTest.class));
		suite.addTest(new TestSuite(ntut.csie.csdet.visitor.ASTCatchCollectTest.class));
		//$JUnit-END$
		return suite;
	}

}
