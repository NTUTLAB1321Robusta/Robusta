package ntut.csie.AllTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for ntut.csie.AllTest");
		//$JUnit-BEGIN$
		suite.addTest(new TestSuite(ntut.csie.csdet.data.CSMessageTest.class));
		suite.addTest(new TestSuite(ntut.csie.csdet.preference.JDomUtilTest.class));
		suite.addTest(new TestSuite(ntut.csie.csdet.visitor.ASTCatchCollectTest.class));
		suite.addTest(new TestSuite(ntut.csie.csdet.visitor.MainAnalyzerTest.class));
		//$JUnit-END$
		return suite;
	}

}
