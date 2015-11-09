package ntut.csie.analyzer.over;

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
import ntut.csie.util.MethodDeclarationCollectorVisitor;
import ntut.csie.util.MethodInvocationCollectorVisitor;
import ntut.csie.util.NodeUtils;
import ntut.csie.util.TryStatementCollectorVisitor;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

public class OverLoggingVisitor extends AbstractBadSmellVisitor {
	private CompilationUnit root;
	private List<MarkerInfo> overLoggingList = new ArrayList<MarkerInfo>();
	//store user define rule for logging 
	private TreeMap<String, UserDefinedConstraintsType> libMap = new TreeMap<String, UserDefinedConstraintsType>();
	// configure of bad smell setting
	private SmellSettings smellSettings;

	public OverLoggingVisitor(CompilationUnit root) {
		this.root = root;
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_OVERLOGGING);
	}
	
	public boolean visit(MethodInvocation node) {
		// check if the method invocation is in a catch block
		ASTNode catchClause = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
		if(catchClause == null)
			return false;
		
		if(!isLoggingStatement(node))
			return false;
		
		if(catchClause instanceof CatchClause) {
			String exceptionCatched = ((CatchClause) catchClause).getException().getType().toString();
			List<Statement> statementsInCatch = ((CatchClause) catchClause).getBody().statements();
			for(Statement s: statementsInCatch) {
				if(s instanceof ThrowStatement) {
					// we found a potential over logger
					ASTNode methodDeclaration = NodeUtils.getSpecifiedParentNode(node, ASTNode.METHOD_DECLARATION);
					if(methodDeclaration == null) 
						return false;
					if(((MethodDeclaration) methodDeclaration).resolveBinding() == null)
						return false;	
					
					Expression expression = ((ThrowStatement) s).getExpression();
					if(isLoggingAgainInExceptionStack(node, isRethrowWithNewException(expression)? getExceptionType(expression) : exceptionCatched))
						addOverLoggingMarkerInfo(node);
				}	
			}
		}
		
		return true;
	}

	
	private String getExceptionType(Expression expression) {
		return ((SimpleType)((ClassInstanceCreation)expression).getType()).getName().toString();
	}

	private boolean isRethrowWithNewException(Expression expression) {
		return (expression instanceof ClassInstanceCreation)? true : false;
	}

	private boolean isLoggingAgainInExceptionStack(ASTNode nodeInCatch, String exceptionType) {
		IMethod method = (IMethod) getIMethodDeclaration(nodeInCatch);
		MethodDeclaration calleeMethodDeclaration = (MethodDeclaration) NodeUtils.getSpecifiedParentNode(nodeInCatch, ASTNode.METHOD_DECLARATION);
		// get callers for the method
		IMember[] methodArray = new IMember[] {method};
		MethodWrapper[] currentMW = CallHierarchy.getDefault().getCallerRoots(methodArray);
		if (currentMW.length != 1)
			return false;
		MethodWrapper[] calls = currentMW[0].getCalls(new NullProgressMonitor());
		
		// check each caller for logging in a catch block
		if(calls.length == 0)
			return false;
		
		for(MethodWrapper mw : calls) {
			if (mw.getMember() instanceof IMethod) {
				IMethod callerMethod = (IMethod) mw.getMember();
				
				// to avoid recursion
				if (callerMethod.equals(method))
					continue;	
				
				MethodDeclaration callerMethodDeclaration = getMethodNode(callerMethod);
				if(callerMethodDeclaration == null)
					continue;
				
				TryStatement tryStatement = getTryStatementContainingTargetMethodInvocation(callerMethodDeclaration, calleeMethodDeclaration);
				if(tryStatement == null)
					continue;
				
				CatchClause  catchClause = getCatchClauseForException(tryStatement, exceptionType);
				if(catchClause == null)
					continue;
				
				if(hasLoggingStatement(catchClause))
					return true;
				
				String exceptionCatched = ((CatchClause) catchClause).getException().getType().toString();
				List<Statement> statementsInCatch = ((CatchClause) catchClause).getBody().statements();
				for(Statement s: statementsInCatch) {
					if(s instanceof ThrowStatement) {
						// no logging but re-throw
						Expression expression = ((ThrowStatement) s).getExpression();
						return isLoggingAgainInExceptionStack(s, isRethrowWithNewException(expression)? getExceptionType(expression) : exceptionCatched);
					}	
				}
			}
		}
		// no caller has logging for the corresponding exception
		return false;
	}

	private boolean hasLoggingStatement(CatchClause catchClause) {
		MethodInvocationCollectorVisitor micVisitor = new MethodInvocationCollectorVisitor();
		catchClause.accept(micVisitor);
		List<MethodInvocation> methodInvocationInCatch = micVisitor.getMethodInvocations();
		for(MethodInvocation mi : methodInvocationInCatch) {
			if(isLoggingStatement(mi))
				return true;
		}
		return false;
	}

	private CatchClause getCatchClauseForException(TryStatement tryStatement, String exceptionType) {
		List<CatchClause> catchClauses = tryStatement.catchClauses();
		for(CatchClause cc : catchClauses) {
			if(cc.getException().getType().toString().equals(exceptionType)) {
				return cc;
			}
		}
		return null;
	}

	private TryStatement getTryStatementContainingTargetMethodInvocation(MethodDeclaration methodDeclaration, MethodDeclaration calleeMethodDeclaration) {
		TryStatementCollectorVisitor tscVisitor = new TryStatementCollectorVisitor();
		methodDeclaration.accept(tscVisitor);
		List<TryStatement> tryStatementsInCallerMethod = tscVisitor.getTryStatements();
		
		for(TryStatement ts : tryStatementsInCallerMethod) {
			List<Statement> statementsInTry = ts.getBody().statements();
			for(Statement s : statementsInTry) {
				MethodInvocationCollectorVisitor micVisitor = new MethodInvocationCollectorVisitor();
				s.accept(micVisitor);
				List<MethodInvocation> methodInvocations = micVisitor.getMethodInvocations();
				for(MethodInvocation mi : methodInvocations) {
					if(compare(mi, calleeMethodDeclaration)) {
						return ts;
					}
				}
			}
		}
		return null;
	}

	private boolean compare(MethodInvocation mi, MethodDeclaration calleeMethodDeclaration) {
		String miName = mi.getName().toString();
		String mdName = calleeMethodDeclaration.resolveBinding().getName();
		if(!mi.getName().toString().equals(calleeMethodDeclaration.resolveBinding().getName()))
			return false;
		
		List<Expression> miArgument = mi.arguments();
		List<SingleVariableDeclaration> loggingMethodParameter = calleeMethodDeclaration.parameters();
		if(!(miArgument.size() == loggingMethodParameter.size()))
			return false;
			
		for(int i = 0; i < mi.arguments().size(); i++) {
			if(!miArgument.get(i).resolveTypeBinding().getName().equals(((SimpleType)loggingMethodParameter.get(i).getType()).getName()))
				return false;
		}
		
		return true;
	}

	private MethodDeclaration getMethodNode(IMethod callerMethod) {
		final ICompilationUnit cu = callerMethod.getCompilationUnit();
		final ASTParser astParser = ASTParser.newParser(AST.JLS3);
	    astParser.setSource(cu);
	    astParser.setKind(ASTParser.K_COMPILATION_UNIT);
	    astParser.setResolveBindings(true);
	    astParser.setBindingsRecovery(true);
	    final ASTNode rootNode = astParser.createAST( null );
	    final CompilationUnit compilationUnitNode = (CompilationUnit) rootNode;
	    
	    MethodDeclarationCollectorVisitor mdcVisitor = new MethodDeclarationCollectorVisitor();
	    compilationUnitNode.accept(mdcVisitor);
	    List<MethodDeclaration> methodDeclarations = mdcVisitor.getMethods();
	    
	    for(MethodDeclaration md : methodDeclarations) {
	    	if(md.resolveBinding() == null)
	    		return null;
	    	if(md.resolveBinding().getJavaElement().equals(callerMethod)) {
	    		return md;
	    	}
	    }
		return null;
	}

	private IJavaElement getIMethodDeclaration(ASTNode node) {
		ASTNode methodDeclaration = NodeUtils.getSpecifiedParentNode(node, ASTNode.METHOD_DECLARATION);
		if(methodDeclaration == null) 
			return null;
		if(((MethodDeclaration) methodDeclaration).resolveBinding() == null)
			return null;	
		
		return ((MethodDeclaration) methodDeclaration).resolveBinding().getJavaElement();
	}
	
	private MethodDeclaration getMethodDeclaration(MethodInvocation node) {
		return (MethodDeclaration)NodeUtils.getSpecifiedParentNode(node, ASTNode.METHOD_DECLARATION);
	}
	
	/**
	 * store over logging which is detected
	 */
	private void addOverLoggingMarkerInfo(ASTNode node) {
		ASTNode compilationUnit = NodeUtils.getSpecifiedParentNode(node, ASTNode.COMPILATION_UNIT);
		
		if(compilationUnit == null)
			return;
		//only store marker in specified compilationUnit
		if(compilationUnit.toString().equals(root.toString())) {
			ASTNode parent = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
			CatchClause cc = (CatchClause)parent;
			SingleVariableDeclaration svd = cc.getException();
			
			ArrayList<AnnotationInfo> annotationList = new ArrayList<AnnotationInfo>(2);
			AnnotationInfo ai = new AnnotationInfo(root.getLineNumber(node.getStartPosition()), 
					node.getStartPosition(), 
					node.getLength(), 
					"Multiple Logging for an exception instance");
			annotationList.add(ai);
			
			MarkerInfo marker = new MarkerInfo(	
					RLMarkerAttribute.CS_OVER_LOGGING, 
					svd.resolveBinding().getType(), 
					((CompilationUnit)node.getRoot()).getJavaElement().getElementName(), // class name
					cc.toString(),										
					cc.getStartPosition(), 
					root.getLineNumber(node.getStartPosition()), 
					svd.getType().toString(),
					annotationList);
			overLoggingList.add(marker);
		}
	}

	private boolean isLoggingStatement(MethodInvocation node) {
		if(libMap.isEmpty()) {
			return false;
		}
		
		if(node.resolveMethodBinding() == null)
			return false;
		
		if(node.resolveMethodBinding().getDeclaringClass() == null)
			return false;
		
		String classNameInFullyQualifiedForm = node.resolveMethodBinding().getDeclaringClass().getQualifiedName();
		Iterator<String> iterator = libMap.keySet().iterator();
		while(iterator.hasNext()) {
			String userSettingRule = iterator.next();
			if(classNameInFullyQualifiedForm.equals(userSettingRule))
				return true;
			
			if(classNameInFullyQualifiedForm.length() >= userSettingRule.length() &&
				classNameInFullyQualifiedForm.substring(0, userSettingRule.length()).equals(userSettingRule))
				return true;
		}
		
		return false;
	}

	public List<MarkerInfo> getOverLoggingList() {
		return overLoggingList;
	}

	@Override
	public List<MarkerInfo> getBadSmellCollected() {
		return getOverLoggingList();
	}
}
