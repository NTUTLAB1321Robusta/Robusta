package ntut.csie.AllTest;

import ntut.csie.csdet.data.MarkerInfoTest;
import ntut.csie.csdet.report.BadSmellDataEntityTest;
import ntut.csie.csdet.report.BadSmellDataStorageTest;
import ntut.csie.csdet.report.ClassModelTest;
import ntut.csie.csdet.report.PastReportHistoryTest;
import ntut.csie.csdet.report.ReportBuilderIntergrationTest;
import ntut.csie.csdet.report.ReportBuilderTest;
import ntut.csie.csdet.report.ReportContentCreatorTest;
import ntut.csie.csdet.report.TrendReportDocumentTest;
import ntut.csie.robusta.codegen.refactoring.ExtractMethodAnalyzerTest;
import ntut.csie.robusta.codegen.refactoring.TEFBExtractMethodRefactoringTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	MarkerInfoTest.class,
	ClassModelTest.class,
	TEFBExtractMethodRefactoringTest.class,
	ExtractMethodAnalyzerTest.class,
	BadSmellDataStorageTest.class,
	ReportBuilderTest.class,
	ReportBuilderIntergrationTest.class,
	ReportContentCreatorTest.class,
	BadSmellDataEntityTest.class,
	PastReportHistoryTest.class,
	TrendReportDocumentTest.class
})

public class AllOOADTests {
}