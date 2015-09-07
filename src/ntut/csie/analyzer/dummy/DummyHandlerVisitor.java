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

import org.eclipse.jdt.core.dom.ASTVisitor;
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

public class DummyHandlerVisitor extends ASTVisitor {
	private List<MarkerInfo> dummyHandlerList;
	// 儲存偵測"Library的Name"和"是否Library"
	// store使用者要偵測的library名稱，和"是否要偵測此library"
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

	public List<MarkerInfo> getDummyList() {
		return dummyHandlerList;
	}

	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		// 如果是Main Program，就不拜訪
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
		// 取得Method的Library名稱
		String libName = methodBinding.getDeclaringClass().getQualifiedName();
		// 取得Method的名稱
		String methodName = methodBinding.getName();

		// 如果該行有Array(如java.util.ArrayList<java.lang.Boolean>)，把<>與其內容都拿掉
		if (libName.indexOf("<") != -1)
			libName = libName.substring(0, libName.indexOf("<"));
			
		Iterator<String> libIt = libMap.keySet().iterator();
		// 判斷是否要偵測 且 此句也包含欲偵測Library
		while(libIt.hasNext()){
			String temp = libIt.next();
				
			// 只偵測Library
			if (libMap.get(temp) == UserDefinedConstraintsType.Library) {
				//若Library長度大於偵測長度，否則表不相同直接略過
				if (libName.length() >= temp.length()) {
					//比較前半段長度的名稱是否相同
					if (libName.substring(0, temp.length()).equals(temp))
						return true;
				}
			// 只偵測Method
			} else if (libMap.get(temp) == UserDefinedConstraintsType.Method) {
				if (methodName.equals(temp))
					return true;
			// 偵測Library.Method的形式
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

}
