package ntut.csie.csdet.visitor.aidvisitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.CarelessCleanupVisitor2;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;

/**
 * �ˬd�S�w��Block�̥��b����Node�|�ߥX�ҥ~�C
 * 
 * �`�N�G���O�u��Finally���`�I�s��Block�C
 * @author charles
 *
 */
public class CarelessCleanupOnlyInFinallyVisitor extends ASTVisitor {
	private CompilationUnit root;
	private boolean isExceptionRisable;
	private List<MethodInvocation> carelessCleanupNodes;
	
	public CarelessCleanupOnlyInFinallyVisitor(CompilationUnit compilationUnit) {
		root = compilationUnit;
		isExceptionRisable = false;
		carelessCleanupNodes = new ArrayList<MethodInvocation>();
	}
	
	public boolean visit(ThrowStatement node) {
		isExceptionRisable = true;
		return true;
	}
	
	public boolean visit(MethodInvocation node) {	
		boolean userDefinedLibResult = false;
		boolean userDefinedResult = false;
		boolean userDefinedExtraRule = false;
		boolean defaultResult = false;
		
		UserDefinedMethodAnalyzer userDefinedMethodAnalyzer = new UserDefinedMethodAnalyzer(SmellSettings.SMELL_CARELESSCLEANUP);
		if(userDefinedMethodAnalyzer.analyzeLibrary(node)) {
			userDefinedLibResult = true;
		}
		
		if(userDefinedMethodAnalyzer.analyzeMethods(node)) {
			userDefinedResult = true;
		}
		
		if(userDefinedMethodAnalyzer.analyzeExtraRule(node, root)) {
			userDefinedExtraRule = true;
		}
		
		if(userDefinedMethodAnalyzer.getEnable()) {
			defaultResult = CarelessCleanupVisitor2.isNodeACloseCodeAndImplementatedCloseable(node);
		}
		
		if(userDefinedLibResult || userDefinedResult || userDefinedExtraRule || defaultResult) {
			// �p�G�e���w�g���{���X�|�o�ͨҥ~�A�h�o��������y���{���X�N�Ocareless cleanup
			if(isExceptionRisable) {
				carelessCleanupNodes.add(node);
			}
		}
		
		if (node.resolveMethodBinding().getExceptionTypes().length != 0) {
			isExceptionRisable = true;
		}
		
		return true;
	}
	
	public List<MethodInvocation> getCarelessCleanupNodes() {
		return carelessCleanupNodes;
	}
}
