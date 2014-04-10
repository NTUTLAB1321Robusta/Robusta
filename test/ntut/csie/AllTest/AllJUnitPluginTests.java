package ntut.csie.AllTest;

import ntut.csie.analyzer.ASTCatchCollectTest;
import ntut.csie.analyzer.BadSmellCollectorTest;
import ntut.csie.analyzer.SpareHandlerVisitorTest;
import ntut.csie.analyzer.SuppressWarningVisitorTest;
import ntut.csie.analyzer.TryStatementCounterVisitorTest;
import ntut.csie.analyzer.careless.CarelessCleanupVisitorTest;
import ntut.csie.analyzer.careless.ClosingResourceBeginningPositionFinderTest;
import ntut.csie.analyzer.careless.MethodInvocationMayInterruptByExceptionCheckerTest;
import ntut.csie.analyzer.careless.StatementsInBlockCollectingVisitorTest;
import ntut.csie.analyzer.careless.closingmethod.CloseResourceMethodInvocationVisitorTest;
import ntut.csie.analyzer.dummy.DummyHandlerVisitorTest;
import ntut.csie.analyzer.empty.EmptyCatchBlockVisitorTest;
import ntut.csie.analyzer.nested.NestedTryStatementVisitorTest;
import ntut.csie.analyzer.over.OverLoggingVisitorTest;
import ntut.csie.analyzer.thrown.ThrownExceptionInFinallyBlockVisitorTest;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitorTest;
import ntut.csie.csdet.preference.DetectedFileTest;
import ntut.csie.csdet.preference.ReportDescriptionTest;
import ntut.csie.csdet.preference.RobustaSettingsTest;
import ntut.csie.csdet.quickfix.BaseQuickFixTest;
import ntut.csie.csdet.refactor.OverLoggingRefactorTest;
import ntut.csie.csdet.refactor.RethrowExRefactoringTest;
import ntut.csie.csdet.refactor.RetryRefactoringTest;
import ntut.csie.csdet.report.ReportBuilderIntergrationTest;
import ntut.csie.csdet.report.ReportBuilderTest;
import ntut.csie.filemaker.test.ASTNodeFinderTest;
import ntut.csie.rleht.builder.RLBuilderTest;
import ntut.csie.rleht.views.ExceptionAnalyzerTest;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitorTest;
import ntut.csie.robusta.codegen.ExpressionStatementStringFinderVisitorTest;
import ntut.csie.robusta.codegen.QuickFixCoreTest;
import ntut.csie.robusta.codegen.StatementFinderVisitorTest;
import ntut.csie.robusta.codegen.VariableDeclarationStatementFinderVisitorTest;
import ntut.csie.util.NodeUtilsTest;

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
	StatementsInBlockCollectingVisitorTest.class,
	MethodInvocationMayInterruptByExceptionCheckerTest.class,
	CloseResourceMethodInvocationVisitorTest.class,
	
	DummyHandlerVisitorTest.class,
	EmptyCatchBlockVisitorTest.class,
	NestedTryStatementVisitorTest.class,
	OverLoggingVisitorTest.class,
	ThrownExceptionInFinallyBlockVisitorTest.class,
	UnprotectedMainProgramVisitorTest.class,
	
	DetectedFileTest.class,
	ReportDescriptionTest.class,
	RobustaSettingsTest.class,
	
	BaseQuickFixTest.class,
	
	OverLoggingRefactorTest.class,
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
})
public class AllJUnitPluginTests {
}
