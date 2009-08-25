package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.CarelessCleanUpAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.EditorUtils;

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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
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
/**
 * ���ѵ�Careless CleanUp���Ѫk
 * @author chenyimin
 */
public class CCUQuickFix implements IMarkerResolution{
	
	private static Logger logger = LoggerFactory.getLogger(CCUQuickFix.class);
	private String label;
	private IOpenable actOpenable;
	private ASTRewrite rewrite;
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	//����code smell��type
	private String problem;

	//�ϥժ����
	int selectLine = -1;

	//Method Index
	private String methodIdx;
	
	//Smell Index
	private String msgIdx;
	
	//���ק諸�{���X��T
	private String moveLine;
	
	@Override
	public String getLabel() {
		return label;
	}
	public CCUQuickFix(String label){
		this.label = label;
	}
	@Override
	public void run(IMarker marker) {
		try{
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP))){
				methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				//���o�ثe�n�Q�ק諸method node
				findCurrentMethodNode(marker.getResource());
				//���o�n�Q�ק諸�{���X��T
				moveLine=getMoveLine();
				//�Ytry Statement�̤w�g��Finally Block,�N�����N�Ӧ�{���X����Finally Block��
				//�_�h���إ�Finally Block��,�A����Finally Block
				if(hasFinallyBlock()){
					moveToFinallyBlock();
				}else{
					addNewFinallyBlock();
					findCurrentMethodNode(marker.getResource());
					moveToFinallyBlock();
					}
				//�N�n�ܧ󪺸�Ƽg�^
				Document document = applyChange();
				findCurrentMethodNode(marker.getResource());
				//�ϥճQ�ܧ󪺵{���X
				selectLine(document);
			}
		}catch(CoreException e){
			logger.error("[CCUQuickFix] EXCEPTION ",e);
		}finally{
		}
	}
	/**
	 * ���o�ثe�n�Q�ק諸method node
	 * @param resource
	 */
	private void findCurrentMethodNode(IResource resource){
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
			}catch(Exception ex){
				ex.printStackTrace();
				logger.error("[CCUQuickFix] EXCEPTION ",ex);
			}
		}
	}
	/**
	 * ���o���ק諸�{���X��T
	 * @return String
	 */
	private String getMoveLine(){
		CarelessCleanUpAnalyzer ccVisitor=new CarelessCleanUpAnalyzer(this.actRoot); 
		currentMethodNode.accept(ccVisitor);
		List<CSMessage> ccList=ccVisitor.getCarelessCleanUpList();
		CSMessage csMsg=ccList.get(Integer.parseInt(msgIdx));
		return csMsg.getStatement();
	}
	/**
	 * �P�_Try Statment�O�_��Finally Block
	 * @return boolean
	 */
	private boolean hasFinallyBlock(){
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		
		boolean isFinallyExist=false;
		
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement trystat=(TryStatement) statement.get(i);
				Block finallyBlock=trystat.getFinally();
				if(finallyBlock!=null){
					//���p��Finally Block�N�Хܬ�true
					isFinallyExist = true;
					break;
				}
			}
		}
		return isFinallyExist;
	}
	/**
	 * �bTry Statement�̫إ�Finally Block
	 */
	private void addNewFinallyBlock(){
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();
		
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement ts = (TryStatement)statement.get(i);
				Block block=ast.newBlock();
				ts.setFinally(block);
				applyChange(ts);
				break;
			}
		}

	}
	/**
	 * �N���ק諸�{���X����Finally Block��
	 */
	private void moveToFinallyBlock(){
		rewrite = ASTRewrite.create(actRoot.getAST());
		
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement ts = (TryStatement)statement.get(i);
				Block finallyBlock=ts.getFinally();
				ListRewrite tsRewrite = rewrite.getListRewrite(ts.getBody(),Block.STATEMENTS_PROPERTY);
				List<?> tsList=tsRewrite.getOriginalList();
				//���Try Statement�̬O�_�������ʪ��{���X,�Y���h����
				for(int j=0;j<tsList.size();j++){
					String temp=tsList.get(j).toString();
					if(temp.contains(moveLine)){
						tsRewrite.remove((ASTNode) tsRewrite.getRewrittenList().get(j), null);
					}
				}
				//���Catch Clauses�̬O�_�������ʪ��{���X,�Y���h����
				List<?> ccList=ts.catchClauses();
				for(int j=0;j<ccList.size();j++){
					CatchClause cc=(CatchClause) ccList.get(j);
					ListRewrite ccRewrite = rewrite.getListRewrite(cc.getBody(),Block.STATEMENTS_PROPERTY);
					List<?> ccbody=ccRewrite.getOriginalList();
					for(int k=0;k<ccbody.size();k++){
						String ccStat=ccbody.get(k).toString();
						if(ccStat.contains(moveLine)){
							ccRewrite.remove((ASTNode) ccRewrite.getRewrittenList().get(k), null);
						}
					}
					
				}
				//�bFinally Block�̥[�J�����ʪ��{���X
				StringBuffer strBuf = new StringBuffer();
				strBuf.append(moveLine);
				if(!moveLine.contains(";")){
					strBuf.append(";");
				}			
				ASTNode placeHolder = rewrite.createStringPlaceholder(strBuf.toString(), ASTNode.EXPRESSION_STATEMENT);
				ListRewrite moveRewrite = rewrite.getListRewrite(finallyBlock,Block.STATEMENTS_PROPERTY);
				moveRewrite.insertLast(placeHolder, null);
				break;
			}
		}
		
			
	}
	/**
	 * �N�n�ܧ󪺤��e�g�^Edit��
	 * @param ASTNode
	 */
	private void applyChange(ASTNode node) {
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);
			
			cu.getBuffer().setContents(document.get());

		} catch (BadLocationException e) {
			logger.error("[Rethrow checked Exception] EXCEPTION ",e);
		} catch (JavaModelException ex) {
			logger.error("[Rethrow checked Exception] EXCEPTION ",ex);
		}
	}
	
	
	/**
	 * �N�n�ܧ󪺸�Ƽg�^��Document��
	 */
	private Document applyChange(){
		//�g�^Edit��
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = rewrite.rewriteAST(document,null);
			edits.apply(document);
			cu.getBuffer().setContents(document.get());
			return document;
		}catch (Exception ex) {
			logger.error("[CCUQuickFix] EXCEPTION ",ex);
		}
		return null;
	}
	/**
	 * �ϥճQ�ܧ󪺵{���X
	 * @param Document 
	 */
	private void selectLine(Document document) {
		//���o�ثe��EditPart
		IEditorPart editorPart = EditorUtils.getActiveEditor();
		ITextEditor editor = (ITextEditor) editorPart;
		
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> mdstatement = mdBlock.statements();
		//���Finally Block�̪��{���X,�w��b�Q���ʹL�Ӫ�����{���X
		for(int i=0;i<mdstatement.size();i++){
			if(mdstatement.get(i) instanceof TryStatement){
				TryStatement ts = (TryStatement) mdstatement.get(i);
				Block finallyBlock=ts.getFinally();
				List<?> finallystat = finallyBlock.statements();
				for(int j=0;j<finallystat.size();j++){
					Statement stat=(Statement)finallystat.get(j);
					String temp=stat.toString();
					if(temp.contains(moveLine)){
						//�_�l��Ʊq0�}�l�A���O1�A�ҥH��1
						selectLine = this.actRoot.getLineNumber(stat.getStartPosition())-1;
					}
				}
			}
		}
		
		if (selectLine == -1) {
			//���oMethod���_�I��m
			int srcPos = currentMethodNode.getStartPosition();

			//��Method�_�I��m���oMethod���ĴX���(�_�l��Ʊq0�}�l�A���O1�A�ҥH��1)
			selectLine = this.actRoot.getLineNumber(srcPos)-1;
		}

		//���ϥժ���Ƹ��
		IRegion lineInfo = null;
		try {
			//���o��ƪ����
			lineInfo = document.getLineInformation(selectLine);
		} catch (BadLocationException e) {
			logger.error("[BadLocation] EXCEPTION ",e);
		}

		//�ϥոӦ�,�bQuick fix������,�i�H�N��Щw��bQuick Fix����
		editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
	}
}