package ntut.csie.AllTest;

import ntut.csie.csdet.data.CSMessageTest;
import ntut.csie.csdet.data.MarkerInfoTest;
import ntut.csie.csdet.preference.SmellSettingsTest;
import ntut.csie.csdet.report.ClassModelTest;
import ntut.csie.jdt.util.ClazzTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	CSMessageTest.class,
	MarkerInfoTest.class,
	
	SmellSettingsTest.class,

	ClassModelTest.class,
	
	ClazzTest.class
})
public class AllJUnitTests {
}
