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
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	//�s��ثe�ҭnfix��method node
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
			   //�p�G�I��ignore Exception,�h�Nexception rethrow
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				// ��X�n�QQuick fix��method
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
	 * ��X�n�QQuick fix��method node(Ignore Exception)
	 * @param resource �n�Q�ק諸source
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
				
				//���o��class�Ҧ���method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();
				
				//���o�ثe�n�Q�ק諸method node
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
			//������method�Ҧ���catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			//�h���startPosition,��X�n�ק諸�`�I
			
			for (ASTNode cc : catchList){
				if(cc.getStartPosition() == msg.getPosition()){
					SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
					.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
//					System.out.println("�iQuick fixCatch Clause�j=====>"+cc.toString());	
//					System.out.println("Variable=====>"+svd.resolveBinding().getName());
					
					CatchClause clause = (CatchClause)cc;
					//�ۦ�إߤ@��throw statement�[�J
					ThrowStatement ts = ast.newThrowStatement();
					//�Nthrow��variable�ǤJ
					ClassInstanceCreation cic = ast.newClassInstanceCreation();
					//throw new RuntimeException()
					cic.setType(ast.newSimpleType(ast.newSimpleName("RuntimeException")));
					//�Nthrow new RuntimeException(ex)�A�����[�J�Ѽ� 
					cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
//					Expression expression = ts.getAST().newSimpleName(svd.resolveBinding().getName()) ;
					
					//���oCatchClause�Ҧ���statement
					List<Statement> statement = clause.getBody().statements();
					//�N��Ƽg�^
					ts.setExpression(cic);
					statement.add(ts);
					
				}
			}

			//�g�^Edit��
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);
			
			cu.getBuffer().setContents(document.get());
			//���o�ثe��EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;

			//�Q��document���o�w���I
			int offset = document.getLineOffset(msg.getLineNumber());
			//�bQuick fix������,�i�H�N��Щw��bQuick Fix����
			//TODO �i�H�NFix�����浹highlight�_��,���n�����olength,�Ȯɥ�����שT�w
			EditorUtils.selectInEditor(editor,offset,34);
		}catch (Exception ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
			ex.printStackTrace();
		}
		
	}

}
