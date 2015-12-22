package ntut.csie.analyzer;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.SSMessage;
import ntut.csie.robusta.agile.exception.SuppressSmell;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class SuppressWarningVisitor extends ASTVisitor {
	private List<SSMessage> suppressList;
	private CompilationUnit root;
	
	public SuppressWarningVisitor(CompilationUnit root) {
		suppressList = new ArrayList<SSMessage>();
		this.root = root;
	}
	
	public boolean visit(MethodDeclaration node) {
		if(node.resolveBinding() == null)
			return false;
		
		IAnnotationBinding[] annoBinding = node.resolveBinding().getAnnotations();
		
		for (int i = 0, size = annoBinding.length; i < size; i++) {
			//get suppress smell information on method signature
			if (annoBinding[i].getAnnotationType().getQualifiedName().equals(SuppressSmell.class.getName()))
				addSuppressWarning(node, annoBinding[i].getAllMemberValuePairs(), -1);
		}
		return true;
	}
	
	public boolean visit(CatchClause node) {
		// get catch index from catch clauses list
		int index = -1;
		TryStatement ts = (TryStatement)NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		List<?> cc = ts.catchClauses();
		for(int i = 0; i < cc.size(); i++) {
			if(cc.get(i) == node) {
				index = i;
				break;
			}
		}
		// inform whether there is a suppress warning on catch statement
		SingleVariableDeclaration svd = (SingleVariableDeclaration) node.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		List<?> modifyList = svd.modifiers();
		for (int j = 0; j < modifyList.size(); j++) {
			if (modifyList.get(j) instanceof Annotation) {
				Annotation annotation = (Annotation) modifyList.get(j);
				IAnnotationBinding iab  = annotation.resolveAnnotationBinding();
				//inform whether annotation type is a suppress smell
				if (iab.getAnnotationType().getQualifiedName().equals(SuppressSmell.class.getName()))
					addSuppressWarning(node, iab.getAllMemberValuePairs(), index);
			}
		}
		return true;
	}
	
	private void addSuppressWarning(ASTNode node, IMemberValuePairBinding[] mvpb, int index) {
		SSMessage ssmsg = null;
		if(index == -1) // suppress warning on method
			ssmsg = new SSMessage(node.getStartPosition(), root.getLineNumber(node.getStartPosition()));
		else			// suppress warning on catch
			ssmsg = new SSMessage(node.getStartPosition(), root.getLineNumber(node.getStartPosition()), index);
		
		if (mvpb[0].getValue() instanceof String) {
			ssmsg.addSmellList((String) mvpb[0].getValue());
		//check whether annotation content is an array
		} else if (mvpb[0].getValue() instanceof Object[]) {
			Object[] values = (Object[]) mvpb[0].getValue();
			for (Object obj : values) {
				if (obj instanceof String)
					ssmsg.addSmellList((String) obj);
			}
		}
		suppressList.add(ssmsg);
	}
	
	public List<SSMessage> getSuppressWarningList() {
		return suppressList;
	}
}
