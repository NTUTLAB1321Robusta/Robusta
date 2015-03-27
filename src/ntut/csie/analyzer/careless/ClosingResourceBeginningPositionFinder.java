package ntut.csie.analyzer.careless;

import java.util.List;

import ntut.csie.util.BoundaryChecker;
import ntut.csie.util.NodeUtils;
import ntut.csie.util.exception.CloseMethodArgumentNotAllInSimpleNameFormException;
import ntut.csie.util.exception.CloseMethodInvocationHasNoExpressionException;
import ntut.csie.util.exception.ClosingResourceBeginningPositionException;
import ntut.csie.util.exception.ExpressionIsNotInSimpleNameFormException;
import ntut.csie.util.exception.NoAssignmentStatementInDetectionRangeException;
import ntut.csie.util.exception.NoAssignmentStatementOrDeclarationInDetectionRangeException;
import ntut.csie.util.exception.RetrieveVariableDeclarationPointFailException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

public class ClosingResourceBeginningPositionFinder {

	private int methodDeclarationPosition;
	BoundaryChecker boundaryChecker;
	private int detectionStartPoint;
	MethodInvocation closeMethodInvocation;
	
	public int findPosition(MethodInvocation methodInvocation) {
		// initialized
		methodDeclarationPosition = getStartPositionOfMethodDeclarationBody(methodInvocation);
		boundaryChecker = new BoundaryChecker(methodDeclarationPosition, methodInvocation.getStartPosition());
		closeMethodInvocation = methodInvocation;
		return getDetectionStartPositionForGivenCloseMethodInvocation(methodInvocation);
	}

	private int getStartPositionOfMethodDeclarationBody(
			MethodInvocation methodInvocation) {
		MethodDeclaration methodDeclaration = NodeUtils
				.getParentMethodDeclaration(methodInvocation);
		return methodDeclaration.getBody().getStartPosition();
	}

	/**
	 * 
	 */
	private int getDetectionStartPositionForGivenCloseMethodInvocation(MethodInvocation methodInvocation) {
		try {
			if (methodInvocation.arguments().isEmpty()) {
				setStartPositionByTheExpressionOfMethodInvocation(methodInvocation);
			} else {
				setStartPositionByTheFurthestArgument(methodInvocation);
			}	
		} catch (ClosingResourceBeginningPositionException e){
			setDetectionStartPointToMethodDeclaration();
		} 
		/* the catch above is equivalent to the catches below
		 *  
		} catch (CloseMethodInvocationHasNoExpressionException e){
			setDetectionStartPointToMethodDeclaration();
		} catch (ExpressionIsNotInSimpleNameFormException e){
			setDetectionStartPointToMethodDeclaration();
		} catch (CloseMethodArgumentNotAllInSimpleNameFormException e){
			setDetectionStartPointToMethodDeclaration();
		} catch (NoAssignmentStatementOrDeclarationInDetectionRangeException e){
			setDetectionStartPointToMethodDeclaration();
		}
		*/
		return detectionStartPoint;
	}

	/**
	 *  Among the arguments find the furthest beginningPosition 
	 *  if any of the argument is not  in simple name form or 
	 *  any of the argument's assignment cannot be found between the range it's 
	 *  supposed to be in, an exception would be raised
	 */
	private void setStartPositionByTheFurthestArgument(MethodInvocation methodInvocation) throws CloseMethodArgumentNotAllInSimpleNameFormException{
		detectionStartPoint = methodInvocation.getStartPosition();

		List<Expression> arguments = methodInvocation.arguments();
		for (Expression eachArgument : arguments) {
			if (!(eachArgument instanceof SimpleName))
				throw new CloseMethodArgumentNotAllInSimpleNameFormException();
			try {
				int lastAssignmentPosition = getLastAssignmentPosition(eachArgument);
				updateDetectionStartPoint(lastAssignmentPosition);	
			} catch (NoAssignmentStatementInDetectionRangeException e){
				detectionStartPoint = getVariableDeclarationPosition(eachArgument);
				if(!(boundaryChecker.isInClosedInterval(detectionStartPoint)))
					throw new NoAssignmentStatementOrDeclarationInDetectionRangeException();
			}
		}
	}

	private void setDetectionStartPointToMethodDeclaration() {
		detectionStartPoint = methodDeclarationPosition;
	}

	private void updateDetectionStartPoint(int newPosition) {
		if (newPosition < detectionStartPoint) {
			detectionStartPoint = newPosition;
		}
	}

	/**
	 *  If the method invocation is not in "instance.close()" form or the instance has no assignment between the range
	 *  it's supposed to be in, an exception would be raised
	 */
	private void setStartPositionByTheExpressionOfMethodInvocation(MethodInvocation methodInvocation) throws CloseMethodInvocationHasNoExpressionException, ExpressionIsNotInSimpleNameFormException, NoAssignmentStatementOrDeclarationInDetectionRangeException, RetrieveVariableDeclarationPointFailException{
		Expression expression = methodInvocation.getExpression();
		// close()
		if(expression == null)
			throw new CloseMethodInvocationHasNoExpressionException();
		// obj.getInstance().close() or something else
		if(!(expression instanceof SimpleName))
			throw new ExpressionIsNotInSimpleNameFormException();
	
		try {
			detectionStartPoint = getLastAssignmentPosition(expression);			
		} catch (NoAssignmentStatementInDetectionRangeException e){
			detectionStartPoint = getVariableDeclarationPosition(expression);
			if(!(boundaryChecker.isInClosedInterval(detectionStartPoint)))
				throw new NoAssignmentStatementOrDeclarationInDetectionRangeException();
		}
	}

	private int getLastAssignmentPosition(Expression expression){
		ASTNode lastAssignment = getLastAssignment(expression);
		return lastAssignment.getStartPosition();
	}
	
	private ASTNode getLastAssignment(Expression expression){
		CompilationUnit root = (CompilationUnit)expression.getRoot();
		AssignmentCollectingVisitor assignmentCollectingVisitor = new AssignmentCollectingVisitor(expression);
		root.accept(assignmentCollectingVisitor);
		List<Assignment> assignmentList = assignmentCollectingVisitor.getVariableAssignmentList();
		return getAssignmentWithLargestLineNumber(assignmentList);
	}
	
	private ASTNode getAssignmentWithLargestLineNumber(List<Assignment> assignmentList) throws NoAssignmentStatementInDetectionRangeException{
		ASTNode lastAssignment = null;
		int lastAssignmentPosition = 0;
		for(Assignment assignment : assignmentList)
		{
			// instance = (condition)? instanceA : instance B
			if(isRightHandSideConditionalExpression(assignment))
				continue;
			if(!boundaryChecker.isInClosedInterval(assignment.getStartPosition()))
				continue;
			if(!(assignment.getStartPosition() >= lastAssignmentPosition))
				continue;
			if(isAssignmentInIfOrSwitchBlock(assignment) && !isSiblingOf(assignment, closeMethodInvocation))
				continue;
			
			lastAssignmentPosition = assignment.getStartPosition();
			lastAssignment = assignment;
		}
		if(lastAssignment == null)
			throw new NoAssignmentStatementInDetectionRangeException();
		return lastAssignment;
	}

	private boolean isRightHandSideConditionalExpression(Assignment assignment){
		return (assignment.getRightHandSide().getNodeType() == ASTNode.CONDITIONAL_EXPRESSION);
	}
	
	private boolean isAssignmentInIfOrSwitchBlock(Assignment assignemtn){
		return (isAssignmentInIfBlock(assignemtn) || isAssignmentInSwitchBlock(assignemtn));
	}
	private boolean isAssignmentInIfBlock(Assignment assignment){
		ASTNode node = assignment;
		boolean encounterIfBlockWhenTracing = false;
		do{
			node = node.getParent();
			if((node.getNodeType()== ASTNode.IF_STATEMENT))
				encounterIfBlockWhenTracing = true;
		}while(boundaryChecker.isInClosedInterval(node));
		
		return encounterIfBlockWhenTracing;
	}
	
	private boolean isAssignmentInSwitchBlock(Assignment assignment){
		ASTNode node = assignment;
		boolean encounterIfBlockWhenTracing = false;
		do{
			node = node.getParent();
			if((node.getNodeType()== ASTNode.SWITCH_STATEMENT))
				encounterIfBlockWhenTracing = true;
		}while(boundaryChecker.isInClosedInterval(node));
		
		return encounterIfBlockWhenTracing;
	}
	
	private boolean isSiblingOf(Assignment assignment, MethodInvocation closeMethodInvocation){
		return assignment.getParent().getParent().equals(closeMethodInvocation.getParent().getParent())? true : false;
	}
	
	private int getVariableDeclarationPosition(Expression expression) {
		ASTNode variableDeclaration = getVariableDeclaration(expression);
		
		if(variableDeclaration == null)
			throw new RetrieveVariableDeclarationPointFailException();
		
		return variableDeclaration.getStartPosition();
	}

	private ASTNode getVariableDeclaration(Expression expression) {
		SimpleName variableName = NodeUtils
				.getSimpleNameFromExpression(expression);
		
		CompilationUnit root = (CompilationUnit) expression.getRoot();
		return root.findDeclaringNode(variableName.resolveBinding());
	}
}
