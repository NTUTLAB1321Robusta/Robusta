package ntut.csie.analyzer.dummy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.preference.SmellSettings.UserDefinedConstraintsType;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.marker.AnnotationInfo;
import ntut.csie.util.AbstractBadSmellVisitor;

import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class DummyHandlerVisitor extends AbstractBadSmellVisitor {
	private List<MarkerInfo> dummyHandlerList;
	//store library name and token of whether detect this library from user setting
	private TreeMap<String, UserDefinedConstraintsType> libMap;
	private boolean isDetectingDummyHandlerSmell;
	private CompilationUnit root;
	
	public DummyHandlerVisitor(CompilationUnit root) {
		super();
		dummyHandlerList = new ArrayList<MarkerInfo>();
		this.root = root;
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER);
		isDetectingDummyHandlerSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_DUMMYHANDLER);
	}

	public List<MarkerInfo> getDummyHandlerList() {
		return dummyHandlerList;
	}
	/**
	 * According to configure that visitor decide whether visit all class ASTnode structure or not.
	 */
	public boolean visit(MethodDeclaration node) {
		// don't visit main(){}
		if(node.getName().toString().equals("main")) {
			return false;
		}
		return isDetectingDummyHandlerSmell;
	}

	public boolean visit(Initializer node) {
		return isDetectingDummyHandlerSmell;
	}

	public boolean visit(CatchClause catchClause) {
		List<Statement> statements = catchClause.getBody().statements();
		if (statements.isEmpty()) {
			// It's empty catch, do nothing
			return false;
		}
		
		Iterator<Statement> iterator = statements.iterator();
		while (iterator.hasNext()) {
			if (!isPrintingOrLoggingStatement(iterator.next())) {
				// Something else in this catch, continue deeper
				return true;
			}
		}
		/*
		 * All statements in this catch are printing/logging, this catch is a
		 * dummy handler.
		 */
		addSmellInfo(catchClause);
		return false;
	}
	
	/**
	 * Is it a statement has a expression invoking printing or logging?
	 */
	private boolean isPrintingOrLoggingStatement(Statement statement) {
		if (statement instanceof ExpressionStatement) {
			Expression expression = ((ExpressionStatement) statement).getExpression();
			return isPrintingOrLoggingExpression(expression);
		}
		return false;
	}

	/**
	 * Is it an expression invoking printing or logging?
	 */
	private boolean isPrintingOrLoggingExpression(Expression expression) {
		if (expression instanceof MethodInvocation) {
			return isPrintOrLog((MethodInvocation) expression);
		} else if (expression instanceof SuperMethodInvocation) {
			return isPrintOrLog((SuperMethodInvocation) expression);
		} else {
			return false;
		}
	}
	
	private boolean isPrintOrLog(MethodInvocation node) {
		return isPrintOrLog(node.resolveMethodBinding());
	}

	private boolean isPrintOrLog(SuperMethodInvocation node) {
		return isPrintOrLog(node.resolveMethodBinding());
	}

	private boolean isPrintOrLog(IMethodBinding methodBinding) {
		if(methodBinding == null)
			return false;
		
		String libName = methodBinding.getDeclaringClass().getQualifiedName();
		
		String methodName = methodBinding.getName();

		//remove Greater than marker, Less than marker, and other content between these two marker from library name
		if (libName.indexOf("<") != -1)
			libName = libName.substring(0, libName.indexOf("<"));
			
		Iterator<String> libIt = libMap.keySet().iterator();
		while(libIt.hasNext()){
			String temp = libIt.next();
			
			if (libMap.get(temp) == UserDefinedConstraintsType.Library) {
				
				if (libName.length() >= temp.length()) {
					
					if (libName.substring(0, temp.length()).equals(temp))
						return true;
				}
			
			} else if (libMap.get(temp) == UserDefinedConstraintsType.Method) {
				if (methodName.equals(temp))
					return true;
			
			} else if (libMap.get(temp) == UserDefinedConstraintsType.FullQulifiedMethod) {
				int pos = temp.lastIndexOf(".");
				if (libName.equals(temp.substring(0, pos)) &&
					methodName.equals(temp.substring(pos + 1))) {
					return true;
				}
			}
		}
		return false;
	}

	private void addSmellInfo(CatchClause node) {
		SingleVariableDeclaration svd = node.getException();
		
		ArrayList<AnnotationInfo> annotationList = new ArrayList<AnnotationInfo>(2);
		AnnotationInfo ai = new AnnotationInfo(root.getLineNumber(node.getStartPosition()), 
				node.getStartPosition(),
				node.getLength(), 
				"Not handling exception: only logging its info!");
		annotationList.add(ai);
		
		MarkerInfo markerInfo = new MarkerInfo(
				RLMarkerAttribute.CS_DUMMY_HANDLER,
				svd.resolveBinding().getType(), 
				((CompilationUnit)node.getRoot()).getJavaElement().getElementName(), // class name
				node.toString(),
				node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()),
				svd.getType().toString(), 
				annotationList);
		dummyHandlerList.add(markerInfo);
	}

	@Override
	public List<MarkerInfo> getBadSmellCollected() {
		return getDummyHandlerList();
	}

}
