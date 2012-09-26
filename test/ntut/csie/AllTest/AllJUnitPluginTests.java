package ntut.csie.AllTest;

import ntut.csie.csdet.quickfix.BaseQuickFixTest;
import ntut.csie.csdet.quickfix.CCUQuickFixTest;
import ntut.csie.csdet.quickfix.DHQuickFixTest;
import ntut.csie.csdet.quickfix.OLQuickFixTest;
import ntut.csie.csdet.quickfix.UMQuickFixTest;
import ntut.csie.csdet.refactor.CarelessCleanUpRefactorTest;
import ntut.csie.csdet.refactor.OverLoggingRefactorTest;
import ntut.csie.csdet.refactor.RethrowExRefactoringTest;
import ntut.csie.csdet.refactor.RetryRefactoringTest;
import ntut.csie.csdet.report.ReportBuilderTest;
import ntut.csie.csdet.visitor.CarelessCleanupVisitorTest;
import ntut.csie.csdet.visitor.DummyHandlerVisitorTest;
import ntut.csie.csdet.visitor.IgnoreExceptionVisitorTest;
import ntut.csie.csdet.visitor.NestedTryStatementVisitorTest;
import ntut.csie.csdet.visitor.OverLoggingVisitorTest;
import ntut.csie.csdet.visitor.SpareHandlerVisitorTest;
import ntut.csie.csdet.visitor.TryStatementCounterVisitorTest;
import ntut.csie.csdet.visitor.UnprotectedMainProgramVisitorTest;
import ntut.csie.filemaker.test.ASTNodeFinderTest;
import ntut.csie.jdt.util.NodeUtilsTest;
import ntut.csie.rleht.builder.RLBuilderTest;
import ntut.csie.rleht.views.ExceptionAnalyzerTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	BaseQuickFixTest.class,
	CCUQuickFixTest.class,
	DHQuickFixTest.class,
	OLQuickFixTest.class,
	UMQuickFixTest.class,
	
	CarelessCleanUpRefactorTest.class,
	RethrowExRefactoringTest.class,
	RetryRefactoringTest.class,
	OverLoggingRefactorTest.class,
	
	ReportBuilderTest.class,
	
	CarelessCleanupVisitorTest.class,
	DummyHandlerVisitorTest.class,
	IgnoreExceptionVisitorTest.class,
	NestedTryStatementVisitorTest.class,
	OverLoggingVisitorTest.class,
	SpareHandlerVisitorTest.class,
	UnprotectedMainProgramVisitorTest.class,
	TryStatementCounterVisitorTest.class,
	
	ASTNodeFinderTest.class,
	
	NodeUtilsTest.class,
	
	RLBuilderTest.class,
	ExceptionAnalyzerTest.class
})
public class AllJUnitPluginTests {
}
