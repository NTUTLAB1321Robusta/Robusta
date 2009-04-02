package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.RL;
import agile.exception.Robustness;

/**
 * ���ѵ�Ignore Ex�PDummy handler���Ѫk
 * @author chewei
 */

public class DHQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(DHQuickFix.class);
	
	private String label;
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	private IOpenable actOpenable;
	
	//�����ҧ�쪺code smell list
	private List<CSMessage> currentExList = null;
	
	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;
	
	//����code smell��type
	private String problem;
	
	private String exType = "RuntimeException";
	
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
					(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION))){
				//�p�G�I��dummy handler,�h�Nexception rethrow
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				boolean isok = findDummyMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok)
					rethrowException(exception,Integer.parseInt(msgIdx));
			}
			
		} catch (CoreException e) {
			logger.error("[DHQuickFix] EXCEPTION ",e);
		}
		
	}
	
	private boolean findDummyMethod(IResource resource, int methodIdx){
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				
				if (javaElement instanceof IOpenable) {
					this.actOpenable = (IOpenable) javaElement;
				}
				
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
	
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.actRoot = (CompilationUnit) parser.createAST(null);
				
				//���o��class�Ҧ���method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();
				
				//���o�ثe�n�Q�ק諸method node
				this.currentMethodNode = methodList.get(methodIdx);
				if(currentMethodNode != null){
					//���o�o��method��RL��T
					ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
					currentMethodNode.accept(exVisitor);
					currentMethodRLList = exVisitor.getMethodRLAnnotationList();
					//��X�o��method��code smell
					CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.actRoot);
					currentMethodNode.accept(visitor);
					if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)){
						currentExList = visitor.getIgnoreExList();	
					}else{
						currentExList = visitor.getDummyList();
					}
				}				
				return true;			
			}catch (Exception ex) {
				logger.error("[Find DH Method] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 * �N��method rethrow unchecked exception
	 * @param exception
	 */
	private void rethrowException(String exception,int msgIdx){
		try {
		
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
		
			//�ǳƦbCatch Caluse���[�Jthrow exception
			//���oCode smell����T
			CSMessage msg = currentExList.get(msgIdx);
			System.out.println("�iMsg Idx exception type�j===>"+msg.getExceptionType());
			//������method�Ҧ���catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			
			//�h���startPosition,��X�n�ק諸�`�I			
			for (ASTNode cc : catchList){
				System.out.println("�iAll Catch Line num�j===>"+this.actRoot.getLineNumber(cc.getStartPosition()));
				if(cc.getStartPosition() == msg.getPosition()){					
					//�إ�RL Annotation
					addAnnotationRoot(ast);					
					//�bcatch clause���إ�throw statement
					addThrowStatement(cc, ast);
					break;
				}
			}

			//�g�^Edit��
			applyChange(msg);
		}catch (Exception ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
		}
		
	}
	
	/**
	 * �bcatch���W�[throw new RuntimeException(..)
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(ASTNode cc,AST ast){
		//���o��catch()����exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		CatchClause clause = (CatchClause)cc;
		//�ۦ�إߤ@��throw statement�[�J
		ThrowStatement ts = ast.newThrowStatement();
		//�Nthrow��variable�ǤJ
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		//throw new RuntimeException()
		cic.setType(ast.newSimpleType(ast.newSimpleName(exType)));
		//�Nthrow new RuntimeException(ex)�A�����[�J�Ѽ� 
		cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
		
		//���oCatchClause�Ҧ���statement,�N����print�ҥ~��T���F�貾��
		List<Statement> statement = clause.getBody().statements();
		if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)){	
			//���p�nfix��code smell�Odummy handler,�N�n��catch�����C�L��T�R��
			deleteStatement(statement);
		}
		//�N�s�إߪ��`�I�g�^
		ts.setExpression(cic);
		statement.add(ts);	
	}
	
	
	@SuppressWarnings("unchecked")
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
		if(currentMethodRLList.size() == 0){		
			rlary.expressions().add(getRLAnnotation(ast,1,exType));
		}else{
			for (RLMessage rlmsg : currentMethodRLList) {
				//���ª�annotation�[�i�h
				//�P�_�p�G�J�쭫�ƪ��N���n�[annotation
				if((!rlmsg.getRLData().getExceptionType().toString().contains(exType)) && (rlmsg.getRLData().getLevel() != 1)){					
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
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
	 * �N�ҭn�ܧ󪺤��e�g�^Edit��
	 * @param msg
	 */
	private void applyChange(CSMessage msg){
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);
			
			cu.getBuffer().setContents(document.get());
			
			//���o�ثe��EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;
	
			//�Q��document���o�w���I(�n�[1�O�]�����쪺����O��marker����)
			int offset = document.getLineOffset(msg.getLineNumber());
			//�bQuick fix������,�i�H�N��Щw��bQuick Fix����
			//TODO �i�H�NFix�����浹highlight�_��,���n�����olength,�Ȯɥ�����שT�w
			EditorUtils.selectInEditor(editor,offset,40);
		} catch (BadLocationException e) {
			logger.error("[Rethrow Exception] EXCEPTION ",e);
		} catch (JavaModelException ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
		}	
	}
	
	@SuppressWarnings("unchecked")
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
	 * �bRethrow���e,���N������print�r�곣�M����
	 */
	private void deleteStatement(List<Statement> statementTemp){
		// �qCatch Clause�̭���R��ر���
		if(statementTemp.size() != 0){
			for(int i=0;i<statementTemp.size();i++){			
				if(statementTemp.get(i) instanceof ExpressionStatement ){
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// �J��System.out.print or printStackTrace�N��Lremove��
					if(statement.getExpression().toString().contains("System.out.print")||
							statement.getExpression().toString().contains("printStackTrace")){	
							statementTemp.remove(i);
							//����������ArrayList����m�|���s�վ�L,�ҥH�Q�λ��^���~�򩹤U��ŦX������ò���
							deleteStatement(statementTemp);						
					}	
				}		
			}
		}
	}
}
