package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSQuickFix  implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(CSQuickFix.class);
	
	private String label;
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	private IOpenable actOpenable;
	
//	private ASTRewrite rewrite;
	
	private List<CSMessage> currentExList = null;
	
	
	public CSQuickFix(String label){
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
			
			if (problem != null && problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)){
//							||problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER))) {
			   //如果碰到ignore Exception,則將exception rethrow
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				// 找出要被Quick fix的method
				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));

				if(isok){					
					rethrowException(exception,Integer.parseInt(msgIdx));
				}
			}
			
		} catch (CoreException e) {
			logger.error("[Ignore Ex QuickFix] EXCEPTION ",e);
			e.printStackTrace();
		}
	}
	
	/**
	 * 找出要被Quick fix的method node(Ignore Exception)
	 * @param resource 要被修改的source
	 * @param methodIdx method node
	 * @return
	 */
	private boolean findMethod(IResource resource, int methodIdx){
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
					CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.actRoot);
					currentMethodNode.accept(visitor);
					currentExList = visitor.getIgnoreExList();
				}
				
				return true;
			
			}catch (Exception ex) {
				logger.error("[Find CS Method] EXCEPTION ",ex);
				ex.printStackTrace();
			}
		}
		return false;
	}
	

	
	/**
	 * 將該method rethrow unchecked exception
	 * @param exception
	 */
	private void rethrowException(String exception,int msgIdx){
		try {
		
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			
			//準備在Catch Caluse中加入throw exception
			//取得Code smell的資訊
			CSMessage msg = currentExList.get(msgIdx);
			//收集該method所有的catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			//去比對startPosition,找出要修改的節點
			
			for (ASTNode cc : catchList){
				if(cc.getStartPosition() == msg.getPosition()){
					SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
					.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
//					System.out.println("【Quick fixCatch Clause】=====>"+cc.toString());	
//					System.out.println("Variable=====>"+svd.resolveBinding().getName());
					
					CatchClause clause = (CatchClause)cc;
					//自行建立一個throw statement加入
					ThrowStatement ts = ast.newThrowStatement();
					//將throw的variable傳入
					ClassInstanceCreation cic = ast.newClassInstanceCreation();
					//throw new RuntimeException()
					cic.setType(ast.newSimpleType(ast.newSimpleName("RuntimeException")));
					//將throw new RuntimeException(ex)括號中加入參數 
					cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
//					Expression expression = ts.getAST().newSimpleName(svd.resolveBinding().getName()) ;
					
					//取得CatchClause所有的statement
					List<Statement> statement = clause.getBody().statements();
					//將資料寫回
					ts.setExpression(cic);
					statement.add(ts);
					
				}
			}

			//寫回Edit中
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);
			
			cu.getBuffer().setContents(document.get());
			//取得目前的EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;

			//利用document取得定位點
			int offset = document.getLineOffset(msg.getLineNumber());
			//在Quick fix完之後,可以將游標定位在Quick Fix那行
			//TODO 可以將Fix的那行給highlight起來,但要先取得length,暫時先把長度固定
			EditorUtils.selectInEditor(editor,offset,34);
		}catch (Exception ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
			ex.printStackTrace();
		}
		
	}

}
