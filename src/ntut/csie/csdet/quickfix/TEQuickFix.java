package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.analyzer.ASTCatchCollect;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * add "throw checked exception" feature to quick fix of the marker
 * @author Shiau
 */
public class TEQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(TEQuickFix.class);

	private String label;

	private String badSmellType;

	private List<RLMessage> robustnessLevelAnnotationList = null;

	private String lineNumberOfQuickFixInvocation;

	private int amountOFDeletedStatement = 0;

	public TEQuickFix(String label) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			badSmellType = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			
			if(badSmellType != null && (badSmellType.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) || 
								  (badSmellType.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK))) {
				//rethrow exception when meet dummy handler
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				lineNumberOfQuickFixInvocation = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();

				boolean isok = this.findMethodNodeWillBeQuickFixed(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok) {
					robustnessLevelAnnotationList = getRobustnessLevelList();
					
					//add throw exception statement in method and then return index of catch clause
					int catchIdx = rethrowException(exception,Integer.parseInt(msgIdx));

					//adjust order of RL annotation TODO need to be fixed
					//new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
					//highlight specified line number (omit this feature temporarily)
					//selectSourceLine(marker, methodIdx, catchIdx);
				}
			}
		} catch (CoreException e) {
			logger.error("[TEQuickFix] EXCEPTION ",e);
		}
	}

	private List<RLMessage> getRobustnessLevelList() {
		if (methodNodeWillBeQuickFixed != null) {
			ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.javaFileWillBeQuickFixed, methodNodeWillBeQuickFixed.getStartPosition(), 0);
			methodNodeWillBeQuickFixed.accept(exVisitor);
			return exVisitor.getMethodRLAnnotationList();
		}
		return null;
	}
	
	/**
	 * rethrow checked exception in method
	 * @param exception
	 * @param msgIdx
	 * @return				
	 */
	private int rethrowException(String exception, int msgIdx) {
		AST ast = methodNodeWillBeQuickFixed.getAST();

		//add throw exception statement in catch clause
		//collect all catch clause in method
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		methodNodeWillBeQuickFixed.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();

		for (int i = 0; i < catchList.size(); i++) {
			if (catchList.get(i).getStartPosition() == Integer.parseInt(lineNumberOfQuickFixInvocation)) {
				addAnnotationRoot(exception, ast);
				addThrowExceptionStatement(catchList.get(i), ast);
				checkExceptionOnMethodSignature(ast, exception);
				this.applyChange();
				return i;
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	private void addAnnotationRoot(String exception,AST ast) {
		//establish the annotation like "@Robustness(value={@RTag(level=1, exception=java.lang.RuntimeException.class)})"
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);

		MethodDeclaration method = (MethodDeclaration) methodNodeWillBeQuickFixed;		
		
		if(robustnessLevelAnnotationList.size() == 0) {		
			rlary.expressions().add(getRLAnnotation(ast,1,exception));
		}else{
			for (RLMessage rlmsg : robustnessLevelAnnotationList) {
				//if there has been a duplicate annotation then ignore to add annotation in list.
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);

				if((!cut.equals(exception)) && (rlmsg.getRLData().getLevel() == 1)) {					
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exception));

			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//remove existing annotation
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		importRobustnessLevelLibrary();
	}
	
	/**
	 * generate robustness level information of robustness level annotation
	 * @param ast: AST Object
	 * @param robustnessLevelVal
	 * @param excptionType
	 * @return NormalAnnotation AST Node
	 */
	private NormalAnnotation getRLAnnotation(AST ast, int robustnessLevelVal,String excptionType) {
		//generate the annotation like "@Robustness(value={@RTag(level=1, exception=java.lang.RuntimeException.class)})"
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RTag"));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		//default level of throw statement is 1
		level.setValue(ast.newNumberLiteral(String.valueOf(robustnessLevelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		// default exception is RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excptionType)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}

	private void importRobustnessLevelLibrary() {
		List<ImportDeclaration> importList = this.javaFileWillBeQuickFixed.imports();
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;

		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName())) {
				isImportRobustnessClass = true;
			}
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName())) {
				isImportRLClass = true;
			}
		}

		AST rootAst = this.javaFileWillBeQuickFixed.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			this.javaFileWillBeQuickFixed.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			this.javaFileWillBeQuickFixed.imports().add(imp);
		}
	}

	private void addThrowExceptionStatement(CatchClause cc, AST ast) {
		//get exception variable from catch expression
		SingleVariableDeclaration svd = 
			(SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		ThrowStatement ts = ast.newThrowStatement();

		SimpleName name = ast.newSimpleName(svd.resolveBinding().getName());		
		
		ts.setExpression(name);

		List statement = cc.getBody().statements();

		amountOFDeletedStatement = statement.size();
		if(badSmellType.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {	
			deleteStatementWhichWillCauseDummtHandler(statement);
		}
		amountOFDeletedStatement -= statement.size();

		statement.add(ts);
	}

	private void deleteStatementWhichWillCauseDummtHandler(List<Statement> statementTemp) {
		if(statementTemp.size() != 0){
			for(int i=0;i<statementTemp.size();i++) {			
				if(statementTemp.get(i) instanceof ExpressionStatement ) {
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// remove System.out.print and printStackTrace
					if (statement.getExpression().toString().contains("System.out.print") ||
						statement.getExpression().toString().contains("printStackTrace")) {
						statementTemp.remove(i);
						deleteStatementWhichWillCauseDummtHandler(statementTemp);
					}
				}
			}
		}
	}

	private void checkExceptionOnMethodSignature(AST ast, String exception) {
		MethodDeclaration md = (MethodDeclaration)methodNodeWillBeQuickFixed;
		List thStat = md.thrownExceptions();
		boolean isExist = false;
		for(int i=0;i<thStat.size();i++) {
			if(thStat.get(i) instanceof SimpleName) {
				SimpleName sn = (SimpleName)thStat.get(i);
				if(sn.getIdentifier().equals(exception)) {
					isExist = true;
					break;
				}
			}
		}
		if (!isExist) {
			thStat.add(ast.newSimpleName(exception));
		}
	}
}
