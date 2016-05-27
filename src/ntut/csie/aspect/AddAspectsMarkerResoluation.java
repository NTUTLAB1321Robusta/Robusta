package ntut.csie.aspect;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

public class AddAspectsMarkerResoluation implements IMarkerResolution,
		IMarkerResolution2 {
	private String label;
	private String description = "Add a aspectJ file to expose influence of bad smell!";
	private QuickFixCore quickFixCore;
	private CompilationUnit compilationUnit;

	public AddAspectsMarkerResoluation(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return label;
	}

	@Override
	public void run(IMarker marker) {
		MethodDeclaration methodDeclaration = getMethodDeclaration(marker);
		System.out.println("CompilationUnit " + compilationUnit.toString());
		System.out.println("methodDeclaration" + methodDeclaration.toString());

		// 取得壞味道行數
		int badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);

		List<TryStatement> tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclaration);
		// 比對壞味道行數及trystatement所在位置，鎖定該被inject的try statement
		TryStatement tryStatementWillBeInject = getTargetTryStetment(tryStatements, badSmellLineNumber);
		// 取得會被try block接注的例外型別
		System.out.println("tryStatementWillBeInject "
				+ tryStatementWillBeInject);
		List<CatchClause> catchClauses = tryStatementWillBeInject
				.catchClauses();
		String exceptionType = getExceptionTypeOfCatchClauseWhichHasBadSmell(
				badSmellLineNumber, catchClauses);
		System.out.println("exceptionType " + exceptionType);
		MethodInvocation methodWhichWillThrowSpecificException = getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput(
				exceptionType, tryStatementWillBeInject);
		// 搜尋tru block拿出第一個會丟出例外的node
		String methodReturnType = getMethodReturnType(methodWhichWillThrowSpecificException);

		FindTheFirstExpressionOfMethodInvocationVisitor theFirstExpressionVisitor = new FindTheFirstExpressionOfMethodInvocationVisitor();
		methodWhichWillThrowSpecificException.accept(theFirstExpressionVisitor);
		String methodSimpleName = theFirstExpressionVisitor.getTheFirstExpression();

		System.out.println("methodDeclaration name"
				+ methodDeclaration.getName());
		System.out.println("methodDeclaration return type"
				+ methodDeclaration.getName().resolveTypeBinding());
		System.out.println("methodDeclaration parent"
				+ ((TypeDeclaration) methodDeclaration.getParent())
						.resolveBinding().getName());

		// 以這個node為主角，開始inject例外
	}

	private MethodDeclaration getMethodDeclaration(IMarker marker) {
		String methodIdx = "";
		try {
			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		quickFixCore.setJavaFileModifiable(marker.getResource());
		compilationUnit = quickFixCore.getCompilationUnit();
		return QuickFixUtils.getMethodDeclaration(compilationUnit,Integer.parseInt(methodIdx));
	}
	
	

	private List<TryStatement> getAllTryStatementOfMethodDeclaration(
			MethodDeclaration methodDeclaration) {
		// 取出在method中所有的try block
		FindAllTryStatementVisitor visitor = new FindAllTryStatementVisitor();
		methodDeclaration.accept(visitor);
		return visitor.getTryStatementsList();
	}

	private int getBadSmellLineNumberFromMarker(IMarker marker) {
		int badSmellLineNumber = 0;
		try {
			badSmellLineNumber = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return badSmellLineNumber;
	}

	private String getMethodReturnType(
			MethodInvocation methodWhichWillThrowSpecificException) {
		ITypeBinding returnType = methodWhichWillThrowSpecificException
				.resolveTypeBinding();
		return returnType.getName().toString();
	}

	private MethodInvocation getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput(
			String exceptionType, TryStatement tryStatementWillBeInject) {
		Block body = tryStatementWillBeInject.getBody();
		FindStatementWhichWillThrowSpecificExceptionVisitor visitor = new FindStatementWhichWillThrowSpecificExceptionVisitor(
				exceptionType);
		body.accept(visitor);
		return visitor.getMethodInvocationWhichWiThrowException();
	}

	private String getExceptionTypeOfCatchClauseWhichHasBadSmell(
			int badSmellLineNumber, List<CatchClause> catchClauses) {
		for (CatchClause catchBlock : catchClauses) {
			int catchClauseLineNumber = compilationUnit.getLineNumber(catchBlock.getStartPosition());
			if (badSmellLineNumber == catchClauseLineNumber) {
				return catchBlock.getException().getType().toString();
			}
		}
		return null;
	}
	
	public void setCompilationUnitForTesting(CompilationUnit compilationUnit){
		this.compilationUnit = compilationUnit;
	}

	private TryStatement getTargetTryStetment(List<TryStatement> tryStatements,
			int badSmellLineNumber) {
		TryStatement candidate = null;
		for (TryStatement tryStatement : tryStatements) {
			int lineNumberOfTryStatement = compilationUnit
					.getLineNumber(tryStatement.getStartPosition());
			if (lineNumberOfTryStatement < badSmellLineNumber) {
				candidate = tryStatement;
			} else {
				break;
			}
		}
		return candidate;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_FIX);
	}

}
