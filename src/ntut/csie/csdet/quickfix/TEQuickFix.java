package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
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

import agile.exception.RL;
import agile.exception.Robustness;


/**
 * �bMarker�W����Quick Fix���[�J����Throw Checked Exception���\��
 * @author Shiau
 */
public class TEQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(TEQuickFix.class);

	private String label;
	//����code smell��type
	private String problem;

	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;

	//���UQuickFix�Ӧ檺�{���_�l��m(Catch��m)
	private String srcPos;
	//�R����Statement�ƥ�
	private int delStatement = 0;

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
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) || 
								  (problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION))){
				//�p�G�I��dummy handler,�h�Nexception rethrow
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				//�x�s���UQuickFix�Ӧ檺�{���_�l��m
				this.srcPos = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();

				boolean isok = this.findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok) {
					currentMethodRLList = findRLList();
					
					//�NMethod�[�JThrow Exception�A�æ^��Catch��Index
					int catchIdx = rethrowException(exception,Integer.parseInt(msgIdx));

					//�վ�RL Annotation���� TODO �ݭץ�
					//new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
					//�ϥի��w��� (�Ȯɤ��ݭn�ϥզ��)
					//selectSourceLine(marker, methodIdx, catchIdx);
				}
			}
		} catch (CoreException e) {
			logger.error("[TEQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * ���oRL Annotation List
	 * @param resource		�ӷ�
	 * @param methodIdx		Method��Index
	 * @return				�O�_���\
	 */
	private List<RLMessage> findRLList() {
		if (currentMethodNode != null) {
			//���o�o��method��RL��T
			ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
			currentMethodNode.accept(exVisitor);
			return exVisitor.getMethodRLAnnotationList();
		}
		return null;
	}
	
	/**
	 * �N��method Throw Checked Exception
	 * @param exception
	 * @param msgIdx
	 * @return				
	 */
	private int rethrowException(String exception, int msgIdx) {
		
//		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		//�ǳƦbCatch Caluse���[�Jthrow exception
		//������method�Ҧ���catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<ASTNode> catchList = catchCollector.getMethodList();

		for (int i = 0; i < catchList.size(); i++) {
			//����Catch(�p�GCatch����m�P���UQuick���檺�_�l��m�ۦP)
			if (catchList.get(i).getStartPosition() == Integer.parseInt(srcPos)) {
				//�إ�RL Annotation
				addAnnotationRoot(exception,ast);
				//�bcatch clause���إ�throw statement
				addThrowStatement(catchList.get(i), ast);
				//�ˬd�bmethod�e�����S��throw exception
				checkMethodThrow(ast,exception);
				//�g�^Edit��
				this.applyChange();
				return i;
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	private void addAnnotationRoot(String exception,AST ast) {
		//�n�إ�@Robustness(value={@RL(level=1, exception=java.lang.RuntimeException.class)})�o�˪�Annotation
		//�إ�Annotation root
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);

		MethodDeclaration method = (MethodDeclaration) currentMethodNode;		
		
		if(currentMethodRLList.size() == 0){		
			rlary.expressions().add(getRLAnnotation(ast,1,exception));
		}else{
			for (RLMessage rlmsg : currentMethodRLList) {
				//���ª�annotation�[�i�h
				//�P�_�p�G�J�쭫�ƪ��N���n�[annotation
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);

				if((!cut.equals(exception)) && (rlmsg.getRLData().getLevel() == 1)){					
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exception));

			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//����¦���annotation��N������
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		//�NRL��library�[�i��
		addImportDeclaration();
	}
	
	/**
	 * ����RL Annotation��RL���
	 * @param ast: AST Object
	 * @param levelVal:�j���׵���
	 * @param exClass:�ҥ~���O
	 * @return NormalAnnotation AST Node
	 */
	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//�n�إ�@Robustness(value={@RL(level=1, exception=java.lang.RuntimeException.class)})�o�˪�Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RL"));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		//throw statement �w�]level = 1
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		// �w�]��RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}

	private void addImportDeclaration() {
		// �P�_�O�_�w�gImport Robustness��RL���ŧi
		List<ImportDeclaration> importList = this.actRoot.imports();
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

		AST rootAst = this.actRoot.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			this.actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RL.class.getName()));
			this.actRoot.imports().add(imp);
		}
	}

	/**
	 * �bcatch���W�[throw checked exception
	 * 
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(ASTNode cc, AST ast) {
		//���o��catch()����exception variable
		SingleVariableDeclaration svd = 
			(SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		CatchClause clause = (CatchClause)cc;

		//�ۦ�إߤ@��throw statement�[�J
		ThrowStatement ts = ast.newThrowStatement();

		//���oCatch��Exception���ܼ�
		SimpleName name = ast.newSimpleName(svd.resolveBinding().getName());		
		
		//�[��throw statement
		ts.setExpression(name);

		//���oCatchClause�Ҧ���statement,�N����print�ҥ~��T���F�貾��
		List statement = clause.getBody().statements();

		delStatement = statement.size();
		if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)){	
			//���p�nfix��code smell�Odummy handler,�N�n��catch�����C�L��T�R��
			deleteStatement(statement);
		}
		delStatement -= statement.size();

		//�N�s�إߪ��`�I�g�^
		statement.add(ts);
	}

	/**
	 * �bRethrow���e,���N������print�r�곣�M����
	 */
	private void deleteStatement(List<Statement> statementTemp){
		// �qCatch Clause�̭���R��ر���
		if(statementTemp.size() != 0){
			for(int i=0;i<statementTemp.size();i++){			
				if(statementTemp.get(i) instanceof ExpressionStatement ){
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// �J��System.out.print or printStackTrace�N��Lremove��
					if (statement.getExpression().toString().contains("System.out.print") ||
						statement.getExpression().toString().contains("printStackTrace")) {

						statementTemp.remove(i);
						//����������ArrayList����m�|���s�վ�L,�ҥH�Q�λ��^���~�򩹤U��ŦX������ò���
						deleteStatement(statementTemp);
					}
				}
			}
		}
	}

	/**
	 * �ˬd�bmethod�e�����S��throw exception
	 * 
	 * @param ast
	 * @param exception 
	 */
	private void checkMethodThrow(AST ast, String exception){
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		List thStat = md.thrownExceptions();
		boolean isExist = false;
		for(int i=0;i<thStat.size();i++){
			if(thStat.get(i) instanceof SimpleName){
				SimpleName sn = (SimpleName)thStat.get(i);
				if(sn.getIdentifier().equals(exception)){
					isExist = true;
					break;
				}
			}
		}
		if (!isExist) {
			thStat.add(ast.newSimpleName(exception));
		}
	}

//	/**
//	 * �ϥի��w���
//	 * @param marker		���ϥ�Statement��Resource
//	 * @param methodIdx		���ϥ�Statement��Method Index
//	 * @param catchIdx		���ϥ�Statement��Catch Index
//	 */
//	private void selectSourceLine(IMarker marker, String methodIdx, int catchIdx) {
//		//���s���oMethod��T
//		boolean isOK = this.findCurrentMethod(marker.getResource(),Integer.parseInt(methodIdx));
//		if (isOK) {
//			try {
//				ICompilationUnit cu = (ICompilationUnit) actOpenable;
//				Document document = new Document(cu.getBuffer().getContents());
//				//���o�ثe��EditPart
//				IEditorPart editorPart = EditorUtils.getActiveEditor();
//				ITextEditor editor = (ITextEditor) editorPart;
//
//				//���o�ϥ�Statement�����
//				int selectLine = getThrowStatementSourceLine(catchIdx);
//				//�Y�ϥզ�Ƭ�
//				if (selectLine == -1) {
//					//���oMethod���_�I��m
//					int srcPos = currentMethodNode.getStartPosition();
//					//��Method�_�I��m���oMethod���ĴX���(�_�l��Ʊq0�}�l�A���O1�A�ҥH��1)
//					selectLine = this.actRoot.getLineNumber(srcPos)-1;
//				}
//				//���o�ϥզ�ƦbSourceCode����Ƹ��
//				IRegion lineInfo = document.getLineInformation(selectLine);
//
//				//�ϥոӦ� �bQuick fix������,�i�H�N��Щw��bQuick Fix����
//				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
//			} catch (JavaModelException e) {
//				logger.error("[Rethrow checked Exception] EXCEPTION ",e);
//			} catch (BadLocationException e) {
//				logger.error("[BadLocation] EXCEPTION ",e);
//			}
//		}
//	}
	
//	/**
//	 * ���oThrow Statement���
//	 * @param catchIdx	catch��index
//	 * @return			�ϥզ��
//	 */
//	private int getThrowStatementSourceLine(int catchIdx) {
//		//�ϥզ��
//		int selectLine = -1;
//
//		if (catchIdx != -1) {
//			ASTCatchCollect catchCollector = new ASTCatchCollect();
//			currentMethodNode.accept(catchCollector);
//			List<ASTNode> catchList = catchCollector.getMethodList();
//			//���o���w��Catch
//			CatchClause clause = (CatchClause) catchList.get(catchIdx);
//			//�M��Throw statement�����
//			List catchStatements = clause.getBody().statements();
//			for (int i = 0; i < catchStatements.size(); i++) {
//				if (catchStatements.get(i) instanceof ThrowStatement) {
//					ThrowStatement statement = (ThrowStatement) catchStatements.get(i);
//					selectLine = this.actRoot.getLineNumber(statement.getStartPosition()) -1;
//				}
//			}
//		}
//		return selectLine;
//	}
}
