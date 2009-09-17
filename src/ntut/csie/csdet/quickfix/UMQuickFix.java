package ntut.csie.csdet.quickfix;

import java.util.List;

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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
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
import agile.exception.Smell;
import agile.exception.SuppressSmell;

public class UMQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(UMQuickFix.class);
	
	private String label;
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;
	
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	private IOpenable actOpenable;
	
//	private String exType = "Exception";
	private String exType = "java.lang.Exception";
	
	private ASTRewrite rewrite;
	
	public UMQuickFix(String label){
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)){
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				//String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				boolean isok = unprotectedMain(marker.getResource(), Integer.parseInt(methodIdx));
				//����X�n�Q�ק諸main function,�p�G�s�b�N�}�l�i��fix
				if(isok) {
					addBigOuterTry(Integer.parseInt(msgIdx));
					//�ϥ�Annotation
					selectSourceLine(marker, methodIdx);
				}
			}
		} catch (CoreException e) {		
			logger.error("[UMQuickFix] EXCEPTION ",e);
		}		
	}
	
	/**
	 * ��X�n�Q�ק諸main function
	 * @param resource
	 * @param methodIdx
	 * @return
	 */
	private boolean unprotectedMain(IResource resource, int methodIdx){
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
					return true;
				}
				
			}catch(Exception ex){
				ex.printStackTrace();
				logger.error("[UMQuickFix] EXCEPTION ",ex);
			}
		}		
		return false;
	}
	
	/**
	 * �w��Code smell�i��ק�,�W�[�@��Big try block
	 * @param msgIdx
	 */
	private void addBigOuterTry(int msgIdx){
		AST ast = actRoot.getAST();
		rewrite = ASTRewrite.create(actRoot.getAST());
		//actRoot.recordModifications();
		
		//���omain function block���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List statement = mdBlock.statements();

		boolean isTryExist = false;
		int pos = -1;

		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				//���pMain function����try�N�Хܬ�true
				pos = i; //try block����m
				isTryExist = true;
				break;
			}
		}
		
		ListRewrite listRewrite = rewrite.getListRewrite(mdBlock,Block.STATEMENTS_PROPERTY);
		
		if(isTryExist){
			/*------------------------------------------------------------------------*
			-	���pMain function���w�g��try block�F,���N�n�h�T�{try block�O���main���@�}�l
				�٬O����,�Ϊ̬O�b�̫᭱,�è̾ڳo�T�ر��p�N�{���X����i�htry block�̭�  
	        *-------------------------------------------------------------------------*/	
			moveTryBlock(ast,statement,pos,listRewrite);
			
		}else{
			/*------------------------------------------------------------------------*
			-	���pMain function���S��try block�F,���N�ۤv�W�[�@��try block,�A��main���Ҧ���
				�{����������itry block��
	        *-------------------------------------------------------------------------*/
			addNewTryBlock(ast,listRewrite);
			
		}	
		addAnnotationRoot(ast);		
		applyChange();
	}
	
	/**
	 * �s�W�@��Try block,�ç�������{���X�[�i�hTry��
	 * @param ast
	 * @param listRewrite
	 */
	private void addNewTryBlock(AST ast,ListRewrite listRewrite){
		TryStatement ts = ast.newTryStatement();
		
		//��try �[�J�@��Catch clause
		List catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = rewrite.getListRewrite(cc.getBody(),Block.STATEMENTS_PROPERTY);
		
		//�إ�catch��type�� catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		
		//�bCatch���[�Jtodo������
		StringBuffer comment = new StringBuffer();
		comment.append("//TODO: handle exception");
		ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);
		
		//�N�쥻�btry block���~���{�������ʶi��
		ListRewrite tryStatement = rewrite.getListRewrite(ts.getBody(),Block.STATEMENTS_PROPERTY);
		int listSize = listRewrite.getRewrittenList().size();
		tryStatement.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
									(ASTNode) listRewrite.getRewrittenList().get(listSize-1)), null);
		//�N�s�W��try statement�[�i��
		listRewrite.insertLast(ts, null);		
	}
	
	/**	
	 * �s�W�@��Try block�϶�,�ñN�btry���~���{���[�J��Try block��
	 * @param ast
	 * @param statement
	 * @param pos : try����m
	 * @param listRewrite
	 */
	private void moveTryBlock(AST ast,List statement,int pos,ListRewrite listRewrite){
		TryStatement original = (TryStatement)statement.get(pos);
		ListRewrite originalRewrite = rewrite.getListRewrite(original.getBody(),Block.STATEMENTS_PROPERTY);
	
		//���pTry block�����٦��{���X,�N�ƻs�i�htry block����
		int totalSize = statement.size();
		if(pos == 0){
			//���ptry block�b�̤@�}�l
			if(totalSize > 1){
				//�Ntry block���᪺�{���Xmove��try��
				originalRewrite.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(1), 
						(ASTNode) listRewrite.getRewrittenList().get(totalSize-1)), null);
			}			
		}else if(pos == listRewrite.getRewrittenList().size()-1){
			//���ptry block�b����	
			if(pos > 0 ){
				//�Ntry block���e���{���Xmove��try��
				originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
						(ASTNode) listRewrite.getRewrittenList().get(pos-1)), null);
			}
		}else{
			//���ptry block�b����
			//���Ntry block���e���{���Xmove��try��
			originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
					(ASTNode) listRewrite.getRewrittenList().get(pos-1)), null);
			//�Ntry block���᪺�{���Xmove��try��
			//�ѩ�b��try���e���F�貾�i�s��try block����,list����m�|���,try����m�|�]��̫e��,�ҥH�q1�}�l�ƻs			
			totalSize = totalSize - pos;
			originalRewrite.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(1), 
					(ASTNode) listRewrite.getRewrittenList().get(totalSize-1)), null);
		}		
		addCatchBody(ast,original);
	}
	
	/**
	 * �p�G�S��catch(Exception e)���ܭn�[�i�h
	 * @param ast
	 * @param original
	 */
	private void addCatchBody(AST ast,TryStatement original){
		//�Q�Φ��ܼƨӧP�_�쥻main����catch exception���A�O�_��catch(Exception e...)
		boolean isException = false;
		List catchStatement = original.catchClauses();
		for(int i=0;i<catchStatement.size();i++){
			CatchClause temp = (CatchClause)catchStatement.get(i);
			SingleVariableDeclaration svd = (SingleVariableDeclaration) temp
			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			if(svd.getType().toString().equals("Exception")){
				//���p�����ŦX�����A,�N���ܼƳ]��true
				isException = true;			
				//�bCatch���[�Jtodo������
				StringBuffer comment = new StringBuffer();
				comment.append("//TODO: handle exception");
				ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
				ListRewrite todoRewrite = rewrite.getListRewrite(temp.getBody(),Block.STATEMENTS_PROPERTY);
				todoRewrite.insertLast(placeHolder, null);
			}
		}
		
		if(!isException){
			//�إ߷s��catch(Exception ex)	
			ListRewrite catchRewrite = rewrite.getListRewrite(original, TryStatement.CATCH_CLAUSES_PROPERTY);
			CatchClause cc = ast.newCatchClause();		
			SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
			sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
			sv.setName(ast.newSimpleName("exxxxx"));
			cc.setException(sv);
			catchRewrite.insertLast(cc, null);
			//�bCatch���[�Jtodo������
			StringBuffer comment = new StringBuffer();
			comment.append("//TODO: handle exception");
			ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
			ListRewrite todoRewrite = rewrite.getListRewrite(cc.getBody(),Block.STATEMENTS_PROPERTY);
			todoRewrite.insertLast(placeHolder, null);
		}
	}

	private void addAnnotationRoot(AST ast){
		//�n�إ�@Robustness(value={@RL(level=1, exception=java.lang.Exception.class)})�o�˪�Annotation
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
				if((!rlmsg.getRLData().getExceptionType().toString().equals(exType)) && (rlmsg.getRLData().getLevel() == 1)){	
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}				
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exType));
			
			ListRewrite listRewrite = rewrite.getListRewrite(method, method.getModifiersProperty());
			List<IExtendedModifier> modifiers = listRewrite.getRewrittenList();
			for(IExtendedModifier mdf : modifiers){
				if(mdf.isAnnotation() && mdf.toString().indexOf("Robustness") != -1){
					listRewrite.remove((ASTNode)mdf, null);
				}
			}
		}
		
		if (rlary.expressions().size() > 0) {
			ListRewrite listRewrite = rewrite.getListRewrite(method, method.getModifiersProperty());
			listRewrite.insertAt(root, 0, null);
		}
		//�NRL��library�[�i��
		addImportDeclaration();
	}
	
	/**
	 * ����RL Annotation��RL���
	 * @param ast:AST Object
	 * @param levelVal:�j���׵���
	 * @param exClass:�ҥ~���O
	 * @return NormalAnnotation AST Node
	 */
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
		//�w�]��Exception
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}
	
	private void addImportDeclaration() {
		// �P�_�O�_�w�gImport Robustness��RL���ŧi
		ListRewrite listRewrite = rewrite.getListRewrite(this.actRoot, this.actRoot.IMPORTS_PROPERTY);
		List<ImportDeclaration> importList = listRewrite.getRewrittenList();
		
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
			listRewrite.insertLast(imp, null);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RL.class.getName()));
			listRewrite.insertLast(imp, null);
		}
	}
	
	/**
	 * �N�n�ܧ󪺸�Ƽg�^��Document��
	 */
	private void applyChange(){
		//�g�^Edit��
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = rewrite.rewriteAST(document,null);
			edits.apply(document);
			cu.getBuffer().setContents(document.get());
		}catch (Exception ex) {
			logger.error("[UMQuickFix] EXCEPTION ",ex);
		}
	}
	
	/**
	 * �ϥ�Annotation����
	 * @param document
	 */
	private void selectSourceLine(IMarker marker, String methodIdx) {
		//���s���oMethod��T
		boolean isOK = unprotectedMain(marker.getResource(), Integer.parseInt(methodIdx));
		if (isOK) {
			try {
				ICompilationUnit cu = (ICompilationUnit) actOpenable;
				Document document = new Document(cu.getBuffer().getContents());
				//���o�ثe��EditPart
				IEditorPart editorPart = EditorUtils.getActiveEditor();
				ITextEditor editor = (ITextEditor) editorPart;
		
				//���oMethod���_�I��m
				int srcPos = currentMethodNode.getStartPosition();
				//��Method�_�I��m���oMethod���ĴX��ơA��ư_�l��m�q0�}�l(���O1)�A�ҥH��1
				//�S�Ĥ@��"���w"�ORobustness�A�ҥH���U�@��
				int selectLine = this.actRoot.getLineNumber(srcPos);

				//���o��ƪ����
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
