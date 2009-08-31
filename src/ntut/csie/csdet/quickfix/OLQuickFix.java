package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.OverLoggingDetector;
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
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
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
 * �bMarker�W����Quick Fix���A�[�J�R����Statement���\��
 * @author Shiau
 */
public class OLQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(OLQuickFix.class);
	
	//����code smell��type
	private String problem;
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	private IOpenable actOpenable;

	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;

	//�����ҧ�쪺code smell list
	private List<CSMessage> overLoggingList = null;
	
	//�ϥժ����
	int selectLine = -1;

	private String label;
	
	public OLQuickFix(String label){
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
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_OVER_LOGGING))) {				
				//���oMarker����T
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				//�Y���o��Method����T�A�R�NMaker���o��R��
				boolean isok = findLoggingMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok)
					deleteMessage(Integer.parseInt(msgIdx));
			}
		} catch (CoreException e) {
			logger.error("[OLQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * ���oMethod������T
	 * @param resource
	 * @param methodIdx
	 * @return
	 */
	private boolean findLoggingMethod(IResource resource, int methodIdx){
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
					//�M���method����OverLogging
					OverLoggingDetector loggingDetector = new OverLoggingDetector(this.actRoot, currentMethodNode);
					loggingDetector.detect();
					//���o�M�פ�OverLogging
					overLoggingList = loggingDetector.getOverLoggingList();
				}				
				return true;			
			}catch (Exception ex) {
				logger.error("[Find OL Method] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 * �R��Message
	 * @param exception
	 */
	private void deleteMessage(int msgIdx){
		try {
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			//���oEH smell����T
			CSMessage msg = overLoggingList.get(msgIdx);

			//������method�Ҧ���catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();

			//�h���startPosition,��X�n�ק諸�`�I
			for (ASTNode cc : catchList){
					if(cc.getStartPosition() == msg.getPosition()){
					//�R��Logging Statement
					deleteCatchStatement(cc, msg);
					//�g�^Edit��
					applyChange(cc);
					break;
				}
			}
		} catch (Exception ex) {
			logger.error("[Delete Message] EXCEPTION ",ex);
		}
		
	}
	
	/**
	 * �R����Marker��Logging�ʧ@
	 * @param cc
	 * @param msg 
	 */
	private void deleteCatchStatement(ASTNode cc, CSMessage msg){
		CatchClause clause = (CatchClause)cc;
		//���oCatchClause�Ҧ���statement,�N����print�ҥ~��T���F�貾��
		List statementList = clause.getBody().statements();

		if(statementList.size() != 0){
			for(int i=0;i<statementList.size();i++){			
				if(statementList.get(i) instanceof ExpressionStatement ){
					ExpressionStatement statement = (ExpressionStatement) statementList.get(i);

					int tempLine = this.actRoot.getLineNumber(statement.getStartPosition());
					//�Y����ܪ���ơA�h�R������
					if (tempLine == msg.getLineNumber()) {
						selectLine = msg.getLineNumber()-1;
						statementList.remove(i);
					}
				}
			}
		}
	}
	
	/**
	 * �N�ҭn�ܧ󪺤��e�g�^Edit��
	 */
	private void applyChange(ASTNode node){
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
			
			//�]�w�n�ϥժ����
			setSelectLine(document, node);
		} catch (BadLocationException e) {
			logger.error("[Delete Statement Exception] EXCEPTION ",e);
		} catch (JavaModelException ex) {
			logger.error("[Delete Statement Exception] EXCEPTION ",ex);
		}	
	}

	/**
	 * ��Щw��
	 * @param document
	 * @param node
	 */
	private void setSelectLine(Document document, ASTNode node) {
		//���o�ثe��EditPart
		IEditorPart editorPart = EditorUtils.getActiveEditor();
		ITextEditor editor = (ITextEditor) editorPart;

		//�Y�ϥզ�Ƭ�
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

		//�ϥոӦ� �bQuick fix������,�i�H�N��Щw��bQuick Fix����
		editor.selectAndReveal(lineInfo.getOffset(), 0);
	}
}
