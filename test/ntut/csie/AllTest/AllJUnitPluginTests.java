package ntut.csie.AllTest;

import ntut.csie.csdet.quickfix.BaseQuickFixTest;
import ntut.csie.csdet.refactor.CarelessCleanUpRefactorTest;
import ntut.csie.csdet.refactor.OverLoggingRefactorTest;
import ntut.csie.csdet.refactor.RethrowExRefactoringTest;
import ntut.csie.csdet.refactor.RetryRefactoringTest;
import ntut.csie.csdet.report.ReportBuilderTest;
import ntut.csie.csdet.visitor.CarelessCleanupVisitor2Test;
import ntut.csie.csdet.visitor.DummyHandlerVisitorTest;
import ntut.csie.csdet.visitor.IgnoreExceptionVisitorTest;
import ntut.csie.csdet.visitor.NestedTryStatementVisitorTest;
import ntut.csie.csdet.visitor.OverLoggingVisitorTest;
import ntut.csie.csdet.visitor.OverwrittenLeadExceptionVisitorTest;
import ntut.csie.csdet.visitor.SpareHandlerVisitorTest;
import ntut.csie.csdet.visitor.SuppressWarningVisitorTest;
import ntut.csie.csdet.visitor.TryStatementCounterVisitorTest;
import ntut.csie.csdet.visitor.UnprotectedMainProgramVisitorTest;
import ntut.csie.csdet.visitor.aidvisitor.CarelessCleanupToleranceVisitorTest;
import ntut.csie.csdet.visitor.aidvisitor.CarelessClenupRaisedExceptionNotInTryCausedVisitorTest;
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
	CarelessCleanUpRefactorTest.class,
	RethrowExRefactoringTest.class,
	RetryRefactoringTest.class,
	OverLoggingRefactorTest.class,
	
	ReportBuilderTest.class,
	
	CarelessCleanupVisitor2Test.class,
	DummyHandlerVisitorTest.class,
	IgnoreExceptionVisitorTest.class,
	NestedTryStatementVisitorTest.class,
	OverLoggingVisitorTest.class,
	OverwrittenLeadExceptionVisitorTest.class,
	SpareHandlerVisitorTest.class,
	UnprotectedMainProgramVisitorTest.class,
	TryStatementCounterVisitorTest.class,
	SuppressWarningVisitorTest.class,
	
	CarelessCleanupToleranceVisitorTest.class,
	CarelessClenupRaisedExceptionNotInTryCausedVisitorTest.class,
	ClassInstanceCreationVisitorTest.class,
	TryStatementExceptionsVisitorTest.class,
	
	ASTNodeFinderTest.class,
	
	NodeUtilsTest.class,
	
	RLBuilderTest.class,
	ExceptionAnalyzerTest.class,
	CatchClauseFinderVisitorTest.class,
	ExpressionStatementStringFinderVisitorTest.class,
	StatementFinderVisitorTest.class,
	VariableDeclarationStatementFinderVisitorTest.class
})
public class AllJUnitPluginTests {
}
