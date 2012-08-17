package ntut.csie.csdet.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class DeclaredMethodAnalyzer extends ASTVisitor {
	boolean isDetected;

	public DeclaredMethodAnalyzer() {
		isDetected = false;
	}
	
	public boolean visit(MethodInvocation node) {
		if(!isDetected) {
			isDetected = node.getName().toString().equals("close");
		}
		return !isDetected;
	}
	
	public boolean BadSmellIsDetected() {
		return isDetected;
	}
}
