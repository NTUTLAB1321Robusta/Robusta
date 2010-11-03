package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.builder.RLOrderFix;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IType;
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
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.RL;
import agile.exception.Robustness;

/**
 * Rethrow Unchecked exception������ާ@���b�o��class��
 * @author chewei
 */

public class RethrowExRefactoring extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(RethrowExRefactoring.class);
	
	private IJavaProject project;
	
	//����code smell��type
	private String problem;
	//�ϥΪ̩ҿ�ܪ�Exception Type
	private IType exType;
	
	//�ϥΪ̩��I�諸Marker
	private IMarker marker;
	
	private IOpenable actOpenable;
	
	// user �Ҷ�g�n��X��Exception,�w�]�ORunTimeException
	private String exceptionType;
	
	private TextFileChange textFileChange;
	
	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;
	
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	private List<CSMessage> currentExList = null;
	
	String msgIdx;
	String methodIdx;
	int catchIdx = -1;
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
			//�h�ק�AST Tree
			collectChange(marker.getResource());
			//����check final condition
			RefactoringStatus status = new RefactoringStatus();		
			return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//����check initial condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm)
								throws CoreException, OperationCanceledException {
		// 2010.07.20 ���e���g�k�APreview��Token���|�ܦ�
		// ��n�ܧ󪺵��G�]��composite�ǥX�h
		//Change[] changes = new Change[] {textFileChange};
		//CompositeChange change = new CompositeChange("Rethrow Unchecked Exception", changes);

		String name = "Rethrow Unchecked Exception";
		ICompilationUnit unit = (ICompilationUnit) this.actOpenable;
		CompilationUnitChange result = new CompilationUnitChange(name, unit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

		// �N�קﵲ�G�]�m�bCompilationUnitChange
		TextEdit edits = textFileChange.getEdit();
		result.setEdit(edits);
		// �N�קﵲ�G�]��Group�A�|��ܦbPreview�W��`�I�C
		result.addTextEditGroup(new TextEditGroup("Rethrow Unchecked Exception", 
								new TextEdit[] {edits} ));

		return result;
	}

	@Override
	public String getName() {		
		return "Rethrow Unchecked Exception";
	}

	/**
	 * ��marker�Ƕi�ӨѦ�class�s���@��code smell��T
	 * @param marker
	 */
	public void setMarker(IMarker marker){
		this.marker = marker;
		this.project = JavaCore.create(marker.getResource().getProject());
	}
	
	/**
	 * parse AST Tree�è��o�n�ק諸method node
	 * @param resource
	 */
	private void collectChange(IResource resource) {
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);

			//���oMethod������T
			if (findMethod(resource))
				//�h�ק�AST Tree�����e
				rethrowException();
		} catch (CoreException e) {
			logger.error("[Find CS Method] EXCEPTION ",e);
		}
	}
	
	/**
	 * ���oMethod������T
	 * @param resource		�ӷ�
	 * @param methodIdx		Method��Index
	 * @return				�O�_���\
	 */
	private boolean findMethod(IResource resource) {
		//���o�n�ק諸CompilationUnit
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
				this.currentMethodNode = methodList.get(Integer.parseInt(methodIdx));
				if (currentMethodNode != null) {
					//���o�o��method��RL��T
					ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
					currentMethodNode.accept(exVisitor);
					currentMethodRLList = exVisitor.getMethodRLAnnotationList();

					CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.actRoot);
					currentMethodNode.accept(visitor);
					//�P�_�OIgnore Ex or Dummy handler�è��ocode smell��List
					if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)){
						currentExList = visitor.getIgnoreExList();	
					} else {
						currentExList = visitor.getDummyList();
					}
				}
				return true;
			} catch (Exception ex) {
				logger.error("[Find CS Method] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 *�إ�Throw Exception����T 
	 */
	private void rethrowException() {
		try {
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			
			//�ǳƦbCatch Clause���[�Jthrow exception
			//���oEH smell����T
			msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			CSMessage msg = currentExList.get(Integer.parseInt(msgIdx));
			//������method�Ҧ���catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			
			//�h���startPosition,��X�n�ק諸catch
			for (int i =0; i < catchList.size(); i++) {
				if(catchList.get(i).getStartPosition() == msg.getPosition()) {
					catchIdx = i;
					//�bcatch clause���إ�throw statement
					addThrowStatement(catchList.get(i), ast);
					//�إ�RL Annotation
					addAnnotationRoot(ast);
					//�[�J��import��Library(�J��RuntimeException�N���Υ[Library)
					if (!exceptionType.equals("RuntimeException")){
						addImportDeclaration();
						checkMethodThrow(ast);
						break;
					}
				}
			}
			//�g�^Edit��
			applyChange(msg);
		}catch (Exception ex) {
			logger.error("[Rethrow Unchecked Exception] EXCEPTION ",ex);
		}
	}
	
	/**
	 * �ˬd�bmethod�e�����S��throw exception
	 * @param ast
	 */
	private void checkMethodThrow(AST ast) {
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		List thStat = md.thrownExceptions();
		boolean isExist = false;
		for(int i=0;i<thStat.size();i++) {
			if(thStat.get(i) instanceof SimpleName){
				SimpleName sn = (SimpleName)thStat.get(i);
				if(sn.getIdentifier().equals(exceptionType)){
					isExist = true;
					break;
				}
			}
		}
		if(!isExist)
			thStat.add(ast.newSimpleName(this.exceptionType));
	}
	
	/**
	 * �bcatch���W�[throw new RuntimeException(..)
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(ASTNode cc,AST ast) {
		//���o��catch()����exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		CatchClause clause = (CatchClause)cc;
		//�ۦ�إߤ@��throw statement�[�J
		ThrowStatement ts = ast.newThrowStatement();
		//�Nthrow��variable�ǤJ
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		//throw new RuntimeException()
		cic.setType(ast.newSimpleType(ast.newSimpleName(exceptionType)));
		//�Nthrow new RuntimeException(ex)�A�����[�J�Ѽ� 
		cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
		
		//���oCatchClause�Ҧ���statement,�N����print�ҥ~��T���F�貾��
		List<Statement> statement = clause.getBody().statements();
		if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
			//���p�nfix��code smell�Odummy handler,�N�n��catch�����C�L��T�R��
			deleteStatement(statement);
		}
		//�N�s�إߪ��`�I�g�^
		ts.setExpression(cic);
		statement.add(ts);	
	}
	
	private void applyChange(CSMessage msg){		
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
		} catch (JavaModelException e) {
			logger.error("[Apply Change Rethrow Unchecked Exception] EXCEPTION ",e);
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
			rlary.expressions().add(getRLAnnotation(ast,1,exceptionType));
		}else{
		
			for (RLMessage rlmsg : currentMethodRLList) {
				//���ª�annotation�[�i�h
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);
				
				//�p�G����RL annotation���ƴN���[�i�h
				if((!cut.equals(exceptionType)) && (rlmsg.getRLData().getLevel() == 1)){					
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}	
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exceptionType));
			
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
		addImportRLDeclaration();
	}
	
	
	/**
	 * ����RL Annotation��RL���
	 * @param ast:AST Object
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

		//exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		//�w�]��RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);
	
		return rl;
	}
	
	
	
	/**
	 * �P�_�O�_�����[�J��Library,��throw RuntimeException�����p�n�ư�
	 * �]��throw RuntimeException����import Library
	 */
	@SuppressWarnings("unchecked")
	private void addImportDeclaration(){
		//�P�_�O�_��import library
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = this.actRoot.imports();
		for(ImportDeclaration id : importList){
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())){
				isImportLibrary = true;
			}
		}
		
		//���p�S��import�N�[�J��AST��
		AST rootAst = this.actRoot.getAST(); 
		if(!isImportLibrary){
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(exType.getFullyQualifiedName()));
			this.actRoot.imports().add(imp);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void addImportRLDeclaration() {
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
	 * ����user�ҭnthrow��exception type
	 * @param name : exception type
	 */
	public RefactoringStatus setExceptionName(String name){
		//���p�ϥΪ̨S����g����F��,��RefactoringStatus�]��Error
		if(name.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Please Choose an Exception Type");
		}else{
			//���p���g�N��L�s�U��
			this.exceptionType = name;
			return new RefactoringStatus();
		}		
	}
	
	/**
	 * ���oJavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}
	
	/**
	 * �x�s�nThrow��Exception��m(�nimport�ϥ�)
	 * @param type
	 */
	public void setExType(IType type){		
		this.exType = type;
	}
	
	/**
	 * �洫Annotation���ǡA�A�w��
	 */
	public void changeAnnotation() {
		if (methodIdx != null && msgIdx != null) {
			//�洫Annotation������
			new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
	
			//�w��
			selectSourceLine();
		}
	}
	
	/**
	 * ���oThrow Statement���
	 * @param catchIdx	catch��index
	 * @return			�ϥզ��
	 */
	private int getThrowStatementSourceLine(int catchIdx) {
		//�ϥզ��
		int selectLine = -1;

		if (catchIdx != -1) {
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			//���o���w��Catch
			CatchClause clause = (CatchClause) catchList.get(catchIdx);
			//�M��Throw statement�����
			List catchStatements = clause.getBody().statements();
			for (int i = 0; i < catchStatements.size(); i++) {
				if (catchStatements.get(i) instanceof ThrowStatement) {
					ThrowStatement statement = (ThrowStatement) catchStatements.get(i);
					selectLine = this.actRoot.getLineNumber(statement.getStartPosition()) -1;
					return selectLine;
				}
			}
		}
		return selectLine;
	}
	
	/**
	 * �ϥի��w���
	 * @param marker		���ϥ�Statement��Resource
	 * @param methodIdx		���ϥ�Statement��Method Index
	 * @param catchIdx		���ϥ�Statement��Catch Index
	 */
	private void selectSourceLine() {
		//���s���oMethod��T
		boolean isOK = findMethod(marker.getResource());
		if (isOK) {
			try {
				ICompilationUnit cu = (ICompilationUnit) actOpenable;
				Document document = new Document(cu.getBuffer().getContents());
				//���o�ثe��EditPart
				IEditorPart editorPart = EditorUtils.getActiveEditor();
				ITextEditor editor = (ITextEditor) editorPart;
	
				//���o�ϥ�Statement�����
				int selectLine = getThrowStatementSourceLine(catchIdx);
				//�Y�ϥզ�Ƭ�
				if (selectLine == -1) {
					//���oMethod���_�I��m
					int srcPos = currentMethodNode.getStartPosition();
					//��Method�_�I��m���oMethod���ĴX���(�_�l��Ʊq0�}�l�A���O1�A�ҥH��1)
					selectLine = this.actRoot.getLineNumber(srcPos)-1;
				}
				//���o�ϥզ�ƦbSourceCode����Ƹ��
				IRegion lineInfo = document.getLineInformation(selectLine);

				//�ϥոӦ� �bQuick fix������,�i�H�N��Щw��bQuick Fix����
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			} catch (JavaModelException e) {
				logger.error("[Rethrow checked Exception] EXCEPTION ",e);
			} catch (BadLocationException e) {
				logger.error("[BadLocation] EXCEPTION ",e);
			}
		}
	}
}
