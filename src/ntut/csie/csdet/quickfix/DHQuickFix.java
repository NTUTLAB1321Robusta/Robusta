package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
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
 * ���ѵ�Ignore checked Exception�PDummy handler���Ѫk
 * @author chewei
 */
public class DHQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(DHQuickFix.class);
	
	private String label;

	// �����ҧ�쪺code smell list
	private List<CSMessage> currentExList = null;
	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;

	// ����code smell��type
	private String problem;
	// �ץ��᪺Exception���A
	private String exType = "RuntimeException";

	//�R����Statement�ƥ�
	private int delStatement = 0;
	// �ϥժ����
	//int selectLine = -1;
	
	public DHQuickFix(String label){
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
								  (problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION))) {
				//�p�G�I��dummy handler,�h�Nexception rethrow
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				
				boolean isok = this.findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok) {
					currentExList = findEHSmellList(problem);
					// �ˬd�O�_�i���oEH Smell List
					if (currentExList == null)
						return;
					//�NMethod�[�JThrow Exception�A�æ^��Catch��Index
					int catchIdx = rethrowException(Integer.parseInt(msgIdx));

					// �վ�RL Annotation���� TODO �ݭץ�
					//new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
					// ���o�ϥժ���� (�Ȯɤ��ݭn�ϥզ��)
					//selectSourceLine(marker, methodIdx, catchIdx);
				}
			}
		} catch (CoreException e) {
			logger.error("[DHQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * ���oMethod������T
	 * @param resource		�ӷ�
	 * @param methodIdx		Method��Index
	 * @return				�O�_���\
	 */
	private List<CSMessage> findEHSmellList(String problem) {
		if (currentMethodNode != null) {
			// ���o�o��method��RL��T
			ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
			currentMethodNode.accept(exVisitor);
			currentMethodRLList = exVisitor.getMethodRLAnnotationList();

			// ��X�o��method��code smell
			CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.actRoot);
			currentMethodNode.accept(visitor);
			if (problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
				return visitor.getIgnoreExList();
			} else {
				return visitor.getDummyList();
			}
		}
		return null;
	}

	/**
	 * �N��method rethrow unchecked exception
	 * @param msgIdx	marker��Index
	 * @return			marker���Catch Index
	 */
	private int rethrowException(int msgIdx){
		try {
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
		
			// �ǳƦbCatch Clause���[�Jthrow exception
			// ���oEH smell����T
			CSMessage msg = currentExList.get(msgIdx);

			//������method�Ҧ���catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			
			// �h���startPosition,��X�n�ק諸�`�I
			for (int i = 0; i < catchList.size(); i++) {
				if (catchList.get(i).getStartPosition() == msg.getPosition()) {
					// �إ�RL Annotation
					addAnnotationRoot(ast);
					// �bcatch clause���إ�throw statement
					addThrowStatement(catchList.get(i), ast);
					// �g�^Edit��
					this.applyChange();

					return i;
				}
			}
		}catch (Exception ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
		}
		return -1;
	}
	
	/**
	 * �bcatch���W�[throw new RuntimeException(..)
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(ASTNode cc, AST ast) {
		// ���o��catch()����exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		CatchClause clause = (CatchClause) cc;
		// �ۦ�إߤ@��throw statement�[�J
		ThrowStatement ts = ast.newThrowStatement();
		
		// �Nthrow��variable�ǤJ
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		// throw new RuntimeException()
		cic.setType(ast.newSimpleType(ast.newSimpleName(exType)));
		// �Nthrow new RuntimeException(ex)�A�����[�J�Ѽ�
		cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));

		// ���oCatchClause�Ҧ���statement,�N����print�ҥ~��T���F�貾��
		List statement = clause.getBody().statements();

		delStatement = statement.size();
		if (problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
			// ���p�nfix��code smell�Odummy handler,�N�n��catch�����C�L��T�R��
			deleteStatement(statement);
		}
		delStatement -= statement.size();
		// �N�s�إߪ��`�I�g�^
		ts.setExpression(cic);
		statement.add(ts);
	}
	
	/**
	 * ����Annotation
	 * @param ast
	 */
	private void addAnnotationRoot(AST ast){
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
		if (currentMethodRLList.size() == 0) {
			rlary.expressions().add(getRLAnnotation(ast,1,exType));
		} else {
			for (RLMessage rlmsg : currentMethodRLList) {
				//���ª�annotation�[�i�h
				//�P�_�p�G�J�쭫�ƪ��N���n�[annotation
				
				if((!rlmsg.getRLData().getExceptionType().toString().contains(exType)) && (rlmsg.getRLData().getLevel() == 1)){					
					rlary.expressions().add(getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exType));
			
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
	
	/**
	 * ����import Robustness Library
	 */
	private void addImportDeclaration() {
		// �P�_�O�_�w�gImport Robustness��RL���ŧi
		List<ImportDeclaration> importList = this.actRoot.imports();
		//�O�_�w�s�bRobustness��RL���ŧi
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
	 * �bRethrow���e,���N������print�r�곣�M����
	 * @param statementTemp
	 */
	private void deleteStatement(List<Statement> statementTemp){
		// �qCatch Clause�̭���R��ر���
		if(statementTemp.size() != 0){
			for (int i = 0; i < statementTemp.size(); i++) {
				if (statementTemp.get(i) instanceof ExpressionStatement) {
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// �J��System.out.print or printStackTrace�N��Lremove��
					if (statement.getExpression().toString().contains("System.out.print") ||
						statement.getExpression().toString().contains("printStackTrace")) {

						statementTemp.remove(i);
						// ����������ArrayList����m�|���s�վ�L,�ҥH�Q�λ��^���~�򩹤U��ŦX������ò���
						deleteStatement(statementTemp);
					}
				}
			}
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
//					return selectLine;
//				}
//			}
//		}
//		return selectLine;
//	}
}
