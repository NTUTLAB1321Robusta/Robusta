package ntut.csie.analyzer.careless;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SimpleName;

public class AssignmentCollectingVisitor extends ASTVisitor {

	SimpleName variable;
	private List<Assignment> variableAssignmentList;
	
	public AssignmentCollectingVisitor(Expression expression) {
		variable = (SimpleName)expression;
		variableAssignmentList = new ArrayList<Assignment>();
	}

	@Override
	public boolean visit(Assignment assignmentStatement) {
		if(assignmentStatement.getLeftHandSide().toString().equals(variable.toString()))
		{
			variableAssignmentList.add(assignmentStatement);
		}	
		return true;
	}
	
	public List<Assignment> getVariableAssignmentList()
	{
		return variableAssignmentList;
	}
}
