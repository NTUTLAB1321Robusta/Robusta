package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.MainAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution;

public class UMQuickFix implements IMarkerResolution{

	private String label;
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	private IOpenable actOpenable;
	
	private List<CSMessage> currentExList = null;

	
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
				if(isok)
					addBigOuterTry(Integer.parseInt(msgIdx));
			}
		} catch (CoreException e) {
		
			e.printStackTrace();
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
					return true;
				}
				
			}catch(Exception ex){
				ex.printStackTrace();
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
		actRoot.recordModifications();
		
		//���omain function block���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		List statement = md.getBody().statements();
		
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

		if(isTryExist){
			 /*------------------------------------------------------------------------*
	        -  ���pMain function���w�g��try block�F,���N�n�h�T�{try block�O���main���@�}�l
	            �٬O����,�Ϊ̬O�b�̫᭱,�è̾ڳo�T�ر��p�N�{���X����i�htry block�̭�  
	        *-------------------------------------------------------------------------*/	
			moveTryBlock(ast,statement,pos);
		}else{
			addNewTryBlock(ast,statement);
		}		
		applyChange();
	}
	
	/**
	 * �s�W�@��Try block�϶�,�ñN�btry���~���{���[�J��Try block��
	 * @param ast
	 * @param statement
	 */
	private void addNewTryBlock(AST ast,List statement){
		 /*------------------------------------------------------------------------*
        -  ���pMain function���S��try block�F,���N�ۤv�W�[�@��try block,�A��main���Ҧ���
            �{����������itry block��
        *-------------------------------------------------------------------------*/
		TryStatement ts = ast.newTryStatement();
		//��try �[�J�@��Catch clause
		List catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		//�إ�catch��type�� catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		catchStatement.add(cc);
		
		//���o���إߪ�try block����statement,�ñN�btry���~���{��������try block����
		List tryStatement = ASTNode.copySubtrees(ast, statement);	

		Block block = ts.getBody();
		for(int i=0;i<tryStatement.size();i++){
			//�N�C�@�檺�{���[�J��try block��
			block.statements().add(i, tryStatement.get(i));
		}
		//�̫��쥻�btry block���~���{����������,�]�����w�g�ƻs�itry block���F
		statement.clear();
		statement.add(ts);
		

	}
	
	/**
	 * ���p���main function�N��try block,�����{���å�����try block�����ܴN����o��method
	 * @param ast
	 * @param statement
	 * @param pos
	 */
	private void moveTryBlock(AST ast,List statement,int pos){
		List copy = ASTNode.copySubtrees(ast, statement);
		//�إ߷s��try 
		TryStatement ts = ast.newTryStatement(); 		
		Block block = ts.getBody();
		List tryStatement = block.statements();
		List catchStatement = ts.catchClauses();
		//�إ߷s��catch
		CatchClause cc = ast.newCatchClause();		
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		//�N�s�ت�catch�[��try block��
		catchStatement.add(cc);
		
		//���o�ncopy��try
		TryStatement original = (TryStatement)copy.get(pos);
		List originalBlock = original.getBody().statements();
		statement.remove(pos);
		//���o�ncopy��catch
		List catchBlock = original.catchClauses();
		//���o�ncopy��finally
		Block FinalBlock = original.getFinally();
		if(pos == 0){
			System.out.println("�iEnter pos=0�j");
			//���s�W�쥻try�A���������e
			for(int i=0;i<originalBlock.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalBlock.get(i)));
			}
			//�A�N���Ӧbtry���~���{���]�ƻs�@����try���A����
			for(int i=0;i<statement.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) statement.get(i)));
			}
			
			//�A�Ncatch���������e�]�ƻs�L��
			for(int i=0;i<catchBlock.size();i++){
				System.out.println("�iCatch Block�j===>"+catchBlock.get(i).toString());
				CatchClause cx =(CatchClause)catchBlock.get(i);
				List cxList = cx.getBody().statements();
				for(int x=0;x<cxList.size();x++){
					System.out.println("�iBlock Content�j===>"+cxList.get(x).toString());
					cc.getBody().statements().add(ASTNode.copySubtree(cx.getBody().getAST(), (ASTNode) cxList.get(x)));	
				}				
			}
			
			//�P�_�쥻main����try�O�_��finally block,�����ܴN�s�W�@��finally�`�I
			if(FinalBlock != null){
				Block realBlock = (Block) ASTNode.copySubtree(ast, FinalBlock);	
				ts.setFinally(realBlock);
			}
			
		}else if(pos == (copy.size()-1)){
			System.out.println("�iEnter pos=size-1�j");
			//���]main function�̫�@�ӵ{���Otry block
			//���Ntry���e���{����copy��s��try block��
			for(int i=0;i<statement.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) statement.get(i)));
			}
			
			//�N�쥻��try block�����e�٭�
			for(int i=0;i<originalBlock.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalBlock.get(i)));
			}
			
			//�A�Ncatch���������e�]�ƻs�L��
			for(int i=0;i<catchBlock.size();i++){
				System.out.println("�iCatch Block�j===>"+catchBlock.get(i).toString());
				CatchClause cx =(CatchClause)catchBlock.get(i);
				List cxList = cx.getBody().statements();
				for(int x=0;x<cxList.size();x++){
					System.out.println("�iBlock Content�j===>"+cxList.get(x).toString());
					cc.getBody().statements().add(ASTNode.copySubtree(cx.getBody().getAST(), (ASTNode) cxList.get(x)));	
				}				
			}
			
			//�P�_�쥻main����try�O�_��finally block,�����ܴN�s�W�@��finally�`�I
			if(FinalBlock != null){
				Block realBlock = (Block) ASTNode.copySubtree(ast, FinalBlock);	
				ts.setFinally(realBlock);
			}

		}else{

			//��Try block���e���{��copy���
			for(int i=0;i<=statement.size()-pos;i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) statement.get(i)));
			}
			//�N�쥻��try block�����e�٭�
			for(int i=0;i<originalBlock.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalBlock.get(i)));
			}

			//�A�Ncatch���������e�]�ƻs�L��
			for(int i=0;i<catchBlock.size();i++){
				System.out.println("�iCatch Block�j===>"+catchBlock.get(i).toString());
				CatchClause cx =(CatchClause)catchBlock.get(i);
				List cxList = cx.getBody().statements();
				for(int x=0;x<cxList.size();x++){
					System.out.println("�iBlock Content�j===>"+cxList.get(x).toString());
					cc.getBody().statements().add(ASTNode.copySubtree(cx.getBody().getAST(), (ASTNode) cxList.get(x)));	
				}			
			}
			
			//�P�_�쥻main����try�O�_��finally block,�����ܴN�s�W�@��finally�`�I
			if(FinalBlock != null){
				Block realBlock = (Block) ASTNode.copySubtree(ast, FinalBlock);	
				ts.setFinally(realBlock);
			}
			//�NTry block���᪺���e�~��ƻs
			for(int i=statement.size()-pos+1;i<statement.size();i++){
//				System.out.println("Content==>"+statement.get(i).toString());
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) statement.get(i)));
			}
		}
		//�N�쥻main�����F�賣�M����
		statement.clear();
		//��s�إߪ�try�[�J
		statement.add(ts);
	}
	
	
	/**
	 * �N�n�ܧ󪺸�Ƽg�^��Document��
	 */
	private void applyChange(){
		//�g�^Edit��
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
//			ICompilationUnit cu = (ICompilationUnit) actRoot.getJavaElement();
			Document document = new Document(cu.getBuffer().getContents());

			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);

			cu.getBuffer().setContents(document.get());

		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
