package ntut.csie.AllTest;

import ntut.csie.csdet.quickfix.DHQuickFixTest;
import ntut.csie.csdet.refactor.RetryRefactoringTest;
import ntut.csie.csdet.report.ReportBuilderTest;
import ntut.csie.csdet.visitor.DummyHandlerVisitorTest;
import ntut.csie.csdet.visitor.ExpressionStatementAnalyzerTest;
import ntut.csie.csdet.visitor.IgnoreExceptionVisitorTest;
import ntut.csie.csdet.visitor.SpareHandlerVisitorTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	DHQuickFixTest.class,
	RetryRefactoringTest.class,
	DummyHandlerVisitorTest.class,
	ExpressionStatementAnalyzerTest.class,
	IgnoreExceptionVisitorTest.class,
	SpareHandlerVisitorTest.class,
	ReportBuilderTest.class
})
public class AllTestsJUnit4 {
}
