package ntut.csie.AllTest;

import ntut.csie.csdet.preference.ReportDescriptionTest;
import ntut.csie.csdet.preference.RobustaSettingsTest;
import ntut.csie.csdet.quickfix.BaseQuickFixTest;
import ntut.csie.csdet.refactor.CarelessCleanupRefactorTest;
import ntut.csie.csdet.refactor.OverLoggingRefactorTest;
import ntut.csie.csdet.refactor.RethrowExRefactoringTest;
import ntut.csie.csdet.refactor.RetryRefactoringTest;
import ntut.csie.csdet.report.ReportBuilderIntergrationTest;
import ntut.csie.csdet.report.ReportBuilderTest;
import ntut.csie.csdet.views.CarelessCleanupPageTest;
import ntut.csie.csdet.visitor.ASTCatchCollectTest;
import ntut.csie.csdet.visitor.BadSmellCollectorTest;
import ntut.csie.csdet.visitor.CarelessCleanupVisitor2Test;
import ntut.csie.csdet.visitor.CarelessCleanupVisitorTest;
import ntut.csie.csdet.visitor.CloseResourceMethodInvocationVisitorTest;
import ntut.csie.csdet.visitor.DummyHandlerVisitorTest;
import ntut.csie.csdet.visitor.EmptyCatchBlockVisitorTest;
import ntut.csie.csdet.visitor.NestedTryStatementVisitorTest;
import ntut.csie.csdet.visitor.OverLoggingVisitorTest;
import ntut.csie.csdet.visitor.ThrownExceptionInFinallyBlockVisitorTest;
import ntut.csie.csdet.visitor.SpareHandlerVisitorTest;
import ntut.csie.csdet.visitor.SuppressWarningVisitorTest;
import ntut.csie.csdet.visitor.TryStatementCounterVisitorTest;
import ntut.csie.csdet.visitor.UnprotectedMainProgramVisitorTest;
import ntut.csie.csdet.visitor.aidvisitor.CarelessCleanupToleranceVisitorTest;
import ntut.csie.csdet.visitor.aidvisitor.ClassInstanceCreationVisitorTest;
import ntut.csie.csdet.visitor.aidvisitor.TryStatementExceptionsVisitorTest;
import ntut.csie.filemaker.test.ASTNodeFinderTest;
import ntut.csie.jdt.util.NodeUtilsTest;
import ntut.csie.rleht.builder.RLBuilderTest;
import ntut.csie.rleht.views.ExceptionAnalyzerTest;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitorTest;
import ntut.csie.robusta.codegen.ExpressionStatementStringFinderVisitorTest;
import ntut.csie.robusta.codegen.StatementFinderVisitorTest;
import ntut.csie.robusta.codegen.VariableDeclarationStatementFinderVisitorTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	BaseQuickFixTest.class,
	
	CarelessCleanupRefactorTest.class,
	OverLoggingRefactorTest.class,
	RethrowExRefactoringTest.class,
	RetryRefactoringTest.class,
	
	ReportBuilderIntergrationTest.class,
	ReportBuilderTest.class,
	
	CarelessCleanupPageTest.class,
	
	ASTCatchCollectTest.class,
	CarelessCleanupVisitor2Test.class,
	CarelessCleanupVisitorTest.class,
	DummyHandlerVisitorTest.class,
	EmptyCatchBlockVisitorTest.class,
	NestedTryStatementVisitorTest.class,
	OverLoggingVisitorTest.class,
	ThrownExceptionInFinallyBlockVisitorTest.class,
	SpareHandlerVisitorTest.class,
	SuppressWarningVisitorTest.class,
	TryStatementCounterVisitorTest.class,
	UnprotectedMainProgramVisitorTest.class,
	
	CarelessCleanupToleranceVisitorTest.class,
	ClassInstanceCreationVisitorTest.class,
	TryStatementExceptionsVisitorTest.class,
	
	ASTNodeFinderTest.class,
	
	NodeUtilsTest.class,
	
	RLBuilderTest.class,
	
	ExceptionAnalyzerTest.class,
	
	CatchClauseFinderVisitorTest.class,
	ExpressionStatementStringFinderVisitorTest.class,
	//QuickFixCoreTest.class,
	StatementFinderVisitorTest.class,
	VariableDeclarationStatementFinderVisitorTest.class,
	RobustaSettingsTest.class,
	ReportDescriptionTest.class,
	BadSmellCollectorTest.class,
	CloseResourceMethodInvocationVisitorTest.class
})
public class AllJUnitPluginTests {
}
