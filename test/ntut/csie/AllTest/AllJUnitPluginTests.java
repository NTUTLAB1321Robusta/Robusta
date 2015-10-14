package ntut.csie.AllTest;

import ntut.csie.analyzer.ASTCatchCollectTest;
import ntut.csie.analyzer.BadSmellCollectorTest;
import ntut.csie.analyzer.SpareHandlerVisitorTest;
import ntut.csie.analyzer.SuppressWarningVisitorTest;
import ntut.csie.analyzer.TryStatementCounterVisitorTest;
import ntut.csie.analyzer.careless.CarelessCleanupDefinitionTest;
import ntut.csie.analyzer.careless.CarelessCleanupVisitorTest;
import ntut.csie.analyzer.careless.ClosingResourceBeginningPositionFinderTest;
import ntut.csie.analyzer.careless.MethodInvocationMayInterruptByExceptionCheckerTest;
import ntut.csie.analyzer.careless.closingmethod.CloseResourceMethodInvocationVisitorTest;
import ntut.csie.analyzer.dummy.DummyHandlerVisitorTest;
import ntut.csie.analyzer.empty.EmptyCatchBlockVisitorTest;
import ntut.csie.analyzer.nested.NestedTryStatementVisitorTest;
import ntut.csie.analyzer.over.OverLoggingVisitorTest;
import ntut.csie.analyzer.thrown.ThrownExceptionInFinallyBlockVisitorTest;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitorTest;
import ntut.csie.csdet.preference.RobustaSettingsTest;
import ntut.csie.csdet.quickfix.BaseQuickFixTest;
import ntut.csie.csdet.refactor.RethrowExRefactoringTest;
import ntut.csie.csdet.refactor.RetryRefactoringTest;
import ntut.csie.csdet.report.BadSmellDataEntityTest;
import ntut.csie.csdet.report.BadSmellDataStorageTest;
import ntut.csie.csdet.report.PastReportHistoryTest;
import ntut.csie.csdet.report.ReportBuilderIntergrationTest;
import ntut.csie.csdet.report.ReportBuilderTest;
import ntut.csie.csdet.report.ReportContentCreatorTest;
import ntut.csie.csdet.report.TrendReportDocumentTest;
import ntut.csie.filemaker.test.ASTNodeFinderTest;
import ntut.csie.rleht.builder.RLBuilderTest;
import ntut.csie.rleht.views.ExceptionAnalyzerTest;
import ntut.csie.robusta.agile.exception.EnableRLAnnotationTest;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitorTest;
import ntut.csie.robusta.codegen.ExpressionStatementStringFinderVisitorTest;
import ntut.csie.robusta.codegen.QuickFixCoreTest;
import ntut.csie.robusta.codegen.StatementFinderVisitorTest;
import ntut.csie.robusta.codegen.VariableDeclarationStatementFinderVisitorTest;
import ntut.csie.robusta.codegen.refactoring.ExtractMethodAnalyzerTest;
import ntut.csie.robusta.codegen.refactoring.TEFBExtractMethodRefactoringTest;
import ntut.csie.util.NodeUtilsTest;
import ntut.csie.util.RLAnnotationFileUtilTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ASTCatchCollectTest.class,
	BadSmellCollectorTest.class,
	SpareHandlerVisitorTest.class,
	SuppressWarningVisitorTest.class,
	TryStatementCounterVisitorTest.class,

	CarelessCleanupVisitorTest.class,
	ClosingResourceBeginningPositionFinderTest.class,
	MethodInvocationMayInterruptByExceptionCheckerTest.class,
	CloseResourceMethodInvocationVisitorTest.class,
	
	DummyHandlerVisitorTest.class,
	EmptyCatchBlockVisitorTest.class,
	NestedTryStatementVisitorTest.class,
	OverLoggingVisitorTest.class,
	ThrownExceptionInFinallyBlockVisitorTest.class,
	UnprotectedMainProgramVisitorTest.class,
	
	RobustaSettingsTest.class,
	
	BaseQuickFixTest.class,
	
	RethrowExRefactoringTest.class,
	RetryRefactoringTest.class,
	
	ReportBuilderIntergrationTest.class,
	ReportBuilderTest.class,
	
	ASTNodeFinderTest.class,
	RLBuilderTest.class,
	ExceptionAnalyzerTest.class,
	
	CatchClauseFinderVisitorTest.class,
	ExpressionStatementStringFinderVisitorTest.class,
	QuickFixCoreTest.class, //no method implemented 
	StatementFinderVisitorTest.class,
	VariableDeclarationStatementFinderVisitorTest.class,
	
	NodeUtilsTest.class,
	BadSmellDataStorageTest.class,
	ReportContentCreatorTest.class,
	ExtractMethodAnalyzerTest.class,
	TEFBExtractMethodRefactoringTest.class,
	
	BadSmellDataEntityTest.class,
	PastReportHistoryTest.class,
	TrendReportDocumentTest.class,
	
	EnableRLAnnotationTest.class,
	CarelessCleanupDefinitionTest.class,
	
	RLAnnotationFileUtilTest.class
})
public class AllJUnitPluginTests {
}
