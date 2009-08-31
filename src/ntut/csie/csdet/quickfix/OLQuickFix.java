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
 * 在Marker上面的Quick Fix中，加入刪除此Statement的功能
 * @author Shiau
 */
public class OLQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(OLQuickFix.class);
	
	//紀錄code smell的type
	private String problem;
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	private IOpenable actOpenable;

	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;

	//紀錄所找到的code smell list
	private List<CSMessage> overLoggingList = null;
	
	//反白的行數
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
				//取得Marker的資訊
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				//若取得到Method的資訊，刪將Maker的這行刪除
				boolean isok = findLoggingMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok)
					deleteMessage(Integer.parseInt(msgIdx));
			}
		} catch (CoreException e) {
			logger.error("[OLQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * 取得Method相關資訊
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

				//取得該class所有的method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();
				
				//取得目前要被修改的method node
				this.currentMethodNode = methodList.get(methodIdx);
				if(currentMethodNode != null){					
					//尋找該method內的OverLogging
					OverLoggingDetector loggingDetector = new OverLoggingDetector(this.actRoot, currentMethodNode);
					loggingDetector.detect();
					//取得專案中OverLogging
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
	 * 刪除Message
	 * @param exception
	 */
	private void deleteMessage(int msgIdx){
		try {
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			//取得EH smell的資訊
			CSMessage msg = overLoggingList.get(msgIdx);

			//收集該method所有的catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();

			//去比對startPosition,找出要修改的節點
			for (ASTNode cc : catchList){
					if(cc.getStartPosition() == msg.getPosition()){
					//刪除Logging Statement
					deleteCatchStatement(cc, msg);
					//寫回Edit中
					applyChange(cc);
					break;
				}
			}
		} catch (Exception ex) {
			logger.error("[Delete Message] EXCEPTION ",ex);
		}
		
	}
	
	/**
	 * 刪除此Marker的Logging動作
	 * @param cc
	 * @param msg 
	 */
	private void deleteCatchStatement(ASTNode cc, CSMessage msg){
		CatchClause clause = (CatchClause)cc;
		//取得CatchClause所有的statement,將相關print例外資訊的東西移除
		List statementList = clause.getBody().statements();

		if(statementList.size() != 0){
			for(int i=0;i<statementList.size();i++){			
				if(statementList.get(i) instanceof ExpressionStatement ){
					ExpressionStatement statement = (ExpressionStatement) statementList.get(i);

					int tempLine = this.actRoot.getLineNumber(statement.getStartPosition());
					//若為選擇的行數，則刪除此行
					if (tempLine == msg.getLineNumber()) {
						selectLine = msg.getLineNumber()-1;
						statementList.remove(i);
					}
				}
			}
		}
	}
	
	/**
	 * 將所要變更的內容寫回Edit中
	 */
	private void applyChange(ASTNode node){
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
//			TextEdit edits = rewrite.rewriteAST(document,null);
			edits.apply(document);
			
			cu.getBuffer().setContents(document.get());
			
			//取得目前的EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;
			
			//設定要反白的行數
			setSelectLine(document, node);
		} catch (BadLocationException e) {
			logger.error("[Delete Statement Exception] EXCEPTION ",e);
		} catch (JavaModelException ex) {
			logger.error("[Delete Statement Exception] EXCEPTION ",ex);
		}	
	}

	/**
	 * 游標定位
	 * @param document
	 * @param node
	 */
	private void setSelectLine(Document document, ASTNode node) {
		//取得目前的EditPart
		IEditorPart editorPart = EditorUtils.getActiveEditor();
		ITextEditor editor = (ITextEditor) editorPart;

		//若反白行數為
		if (selectLine == -1) {
			//取得Method的起點位置
			int srcPos = currentMethodNode.getStartPosition();
			//用Method起點位置取得Method位於第幾行數(起始行數從0開始，不是1，所以減1)
			selectLine = this.actRoot.getLineNumber(srcPos)-1;
		}

		//欲反白的行數資料
		IRegion lineInfo = null;
		try {
			//取得行數的資料
			lineInfo = document.getLineInformation(selectLine);
		} catch (BadLocationException e) {
			logger.error("[BadLocation] EXCEPTION ",e);
		}

		//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
		editor.selectAndReveal(lineInfo.getOffset(), 0);
	}
}
