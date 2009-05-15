package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.visitor.ASTCatchCollect;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.RL;
import agile.exception.Robustness;


/**
 * �bMarker�W����Quick Fix���[�JThrow Checked Exception���\��
 * @author Shiau
 */
public class TEQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(TEQuickFix.class);

	private String label;
	//����code smell��type
	private String problem;

	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;
	
	//�O�_�n�[RL Annotation�A�_�h�w�s�b
	private boolean isAddAnnotation = true;
	
	boolean isImportRobustnessClass = false;
	boolean isImportRLClass = false;
	//���UQuickFix�Ӧ檺�{���_�l��m(Catch��m)
	private String srcPos;
	//�R����Statement�ƥ�
	private int delStatement = 0;

	private IOpenable actOpenable;
	
	public TEQuickFix(String label)
	{
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

				boolean isok = findDummyMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok)
					rethrowException(exception,Integer.parseInt(msgIdx));
			}
			
		} catch (CoreException e) {
			logger.error("[TEQuickFix] EXCEPTION ",e);
		}
	}
	
	private boolean findDummyMethod(IResource resource, int methodIdx) {
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
				}
				return true;
			}catch (Exception ex) {
				logger.error("[Find DH Method] EXCEPTION ",ex);
			}
		}
		return false;
	}

	/**
	 * �N��method Throw Checked Exception
	 * @param exception
	 */
	private void rethrowException(String exception, int msgIdx) {
		
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		//�ǳƦbCatch Caluse���[�Jthrow exception
		//������method�Ҧ���catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<ASTNode> catchList = catchCollector.getMethodList();
		
		for (ASTNode cc : catchList){
			//����Catch(�p�GCatch����m�P���UQuick���檺�_�l��m�ۦP)
			if (cc.getStartPosition() == Integer.parseInt(srcPos))
			{
				//�إ�RL Annotation
				addAnnotationRoot(exception,ast);
				//�bcatch clause���إ�throw statement
				addThrowStatement(cc, ast);
				//�ˬd�bmethod�e�����S��throw exception
				checkMethodThrow(ast,exception);
				//�g�^Edit��
				applyChange(cc);
				break;
			}
		}
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
			isAddAnnotation = false;
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
	 * �bcatch���W�[throw new RuntimeException(..)
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
	
	/**
	 * �ˬd�bmethod�e�����S��throw exception
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
		if(!isExist)
			thStat.add(ast.newSimpleName(exception));
	}

	/**
	 * �N�ҭn�ܧ󪺤��e�g�^Edit��
	 * @param node
	 */
	private void applyChange(ASTNode node) {
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
//			TextEdit edits = rewrite.rewriteAST(document,null);
			edits.apply(document);
			
			cu.getBuffer().setContents(document.get());
			
			//���o�ثe��EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;
			
			CatchClause cc = (CatchClause)node;
			List catchSt = cc.getBody().statements();
			if(catchSt != null){
				int numLine=0;
				//throw���e�S��statement���ignore
				if (catchSt.size()<2)
					//�[�bcatch����@��
					numLine = this.actRoot.getLineNumber(cc.getStartPosition());
				else
				{
					//���othrow���e�@��Statement
					ASTNode throwNode = (ASTNode)catchSt.get(catchSt.size()-2);

					//TODO throw�w��|�줣�ǡA1.throw�e�����ѡA2.throw�e�@����throw�������n�Q�R����statement
					//��Method�_�I��m���oMethod���ĴX���(�_�l��Ʊq0�}�l�A���O1�A�ҥH��1)
					numLine = this.actRoot.getLineNumber(throwNode.getStartPosition());
				}

				//�p�G��import Robustness��RL���ŧi��ƴN�[1
				if(!isImportRobustnessClass)
					numLine++;
				if(!isImportRLClass)
					numLine++;
				//�Y���[Annotation�h��ƥ[1
				if(isAddAnnotation)
					numLine++;

				//���o��ƪ����
				IRegion lineInfo = null;
				try {
					lineInfo = document.getLineInformation(numLine - delStatement);
				} catch (BadLocationException e) {
					logger.error("[BadLocation] EXCEPTION ",e);
				}
				//�ϥոӦ� �bQuick fix������,�i�H�N��Щw��bQuick Fix����
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			}
		} catch (BadLocationException e) {
			logger.error("[Rethrow Exception] EXCEPTION ",e);
		} catch (JavaModelException ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
		}
	}
}
