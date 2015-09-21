package ntut.csie.analyzer;

import org.eclipse.jdt.core.dom.CompilationUnit;
import ntut.csie.analyzer.careless.CarelessCleanupVisitor;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.empty.EmptyCatchBlockVisitor;
import ntut.csie.analyzer.nested.NestedTryStatementVisitor;
import ntut.csie.analyzer.over.OverLoggingVisitor;
import ntut.csie.analyzer.thrown.ExceptionThrownFromFinallyBlockVisitor;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitor;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.util.AbstractBadSmellVisitor;

public class BadSmellVisitorFactory {

	public static AbstractBadSmellVisitor createVisitor(String smellType,
			CompilationUnit root, boolean detectCCOutsideTryStatement) {
		if (smellType.equals(SmellSettings.SMELL_EMPTYCATCHBLOCK))
			return new EmptyCatchBlockVisitor(root);
		else if (smellType.equals(SmellSettings.SMELL_DUMMYHANDLER))
			return new DummyHandlerVisitor(root);
		else if (smellType.equals(SmellSettings.SMELL_NESTEDTRYSTATEMENT))
			return new NestedTryStatementVisitor(root);
		else if (smellType.equals(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM))
			return new UnprotectedMainProgramVisitor(root);
		else if (smellType.equals(SmellSettings.SMELL_OVERLOGGING))
			return new OverLoggingVisitor(root);
		else if (smellType.equals(SmellSettings.SMELL_CARELESSCLEANUP))
			return new CarelessCleanupVisitor(root, detectCCOutsideTryStatement);
		else if (smellType
				.equals(SmellSettings.SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK))
			return new ExceptionThrownFromFinallyBlockVisitor(root);
		else
			throw new IllegalArgumentException("Invalid smell type: "
					+ smellType);
	}
}
