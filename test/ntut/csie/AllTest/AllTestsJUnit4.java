package ntut.csie.AllTest;

import ntut.csie.csdet.data.CSMessageTest;
import ntut.csie.csdet.data.MarkerInfoTest;
import ntut.csie.csdet.preference.JDomUtilTest;
import ntut.csie.csdet.preference.SmellSettingsTest;
import ntut.csie.csdet.quickfix.BaseQuickFixTest;
import ntut.csie.csdet.quickfix.CCUQuickFixTest;
import ntut.csie.csdet.quickfix.DHQuickFixTest;
import ntut.csie.csdet.quickfix.OLQuickFixTest;
import ntut.csie.csdet.quickfix.UMQuickFixTest;
import ntut.csie.csdet.refactor.CarelessCleanUpRefactorTest;
import ntut.csie.csdet.refactor.RethrowExRefactoringTest;
import ntut.csie.csdet.refactor.RetryRefactoringTest;
import ntut.csie.csdet.report.ClassModelTest;
import ntut.csie.csdet.report.ReportBuilderTest;
import ntut.csie.csdet.views.CarelessCleanUpPageTest;
import ntut.csie.csdet.visitor.ASTCatchCollectTest;
import ntut.csie.csdet.visitor.CarelessCleanupVisitorTest;
import ntut.csie.csdet.visitor.DummyHandlerVisitorTest;
import ntut.csie.csdet.visitor.ExpressionStatementAnalyzerTest;
import ntut.csie.csdet.visitor.IgnoreExceptionVisitorTest;
import ntut.csie.csdet.visitor.NestedTryStatementVisitorTest;
import ntut.csie.csdet.visitor.SpareHandlerVisitorTest;
import ntut.csie.csdet.visitor.UnprotectedMainProgramVisitorTest;
import ntut.csie.filemaker.test.ASTNodeFinderTest;
import ntut.csie.jdt.util.ClazzTest;
import ntut.csie.jdt.util.NodeUtilsTest;
import ntut.csie.rleht.builder.RLBuilderTest;
import ntut.csie.rleht.views.ExceptionAnalyzerTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	CSMessageTest.class,
	MarkerInfoTest.class,
	
	JDomUtilTest.class,
	SmellSettingsTest.class,

	BaseQuickFixTest.class,
	CCUQuickFixTest.class,
	DHQuickFixTest.class,
	OLQuickFixTest.class,
	UMQuickFixTest.class,
	
	CarelessCleanUpRefactorTest.class,
	RethrowExRefactoringTest.class,
	RetryRefactoringTest.class,
	
	ClassModelTest.class,
	ReportBuilderTest.class,
	
	CarelessCleanUpPageTest.class,
	
	ASTCatchCollectTest.class,
	CarelessCleanupVisitorTest.class,
	DummyHandlerVisitorTest.class,
	ExpressionStatementAnalyzerTest.class,
	IgnoreExceptionVisitorTest.class,
	NestedTryStatementVisitorTest.class,
	SpareHandlerVisitorTest.class,
	UnprotectedMainProgramVisitorTest.class,
	
	ASTNodeFinderTest.class,
	
	ClazzTest.class,
	NodeUtilsTest.class,
	
	RLBuilderTest.class,
	ExceptionAnalyzerTest.class
})
public class AllTestsJUnit4 {
}
