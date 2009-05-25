package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.common.ErrorLog;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLChecker;
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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
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

public class RLQuickFix implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(RLQuickFix.class);
	
	// �ثemethod��Exception��T
	private List<RLMessage> currentMethodExList = null;

	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;

	// �ثe��Method AST Node
	private ASTNode currentMethodNode = null;

	private CompilationUnit actRoot;

	private IOpenable actOpenable;

	private String label;

	private int levelForUpdate;
	
	//�O�_�w�s�bRobustness��RL���ŧi
	boolean isImportRobustnessClass = false;
	boolean isImportRLClass = false;
	
	public RLQuickFix(String label) {
		this.label = label;
	}

	public RLQuickFix(String label, int level) {
		this.label = label;
		this.levelForUpdate = level;
	}

	public String getLabel() {
		return label;
	}

	public void run(IMarker marker) {
		try {
	
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);			
			if (problem != null && problem.equals(RLMarkerAttribute.ERR_RL_LEVEL)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if (isok) {
					this.updateRLAnnotation(false, Integer.parseInt(msgIdx), levelForUpdate);
				}
			}
			else if (problem != null && problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));

				if (isok) {
					this.addOrRemoveRLAnnotation(true, Integer.parseInt(msgIdx));
				}

			}
			//RL���ǹ��
			else if (problem != null && problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE))
			{
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				findMethod(marker.getResource(), Integer.parseInt(methodIdx));

				//��currentMethodRLList�s��Array + 1(�[1���ƧǮɥi�վ㪺�Ŷ�)
				RLMessage[] newRLList = new RLMessage[currentMethodRLList.size()+1];
				//currentMethodRLList��JArray��
				for (int i=0;i < currentMethodRLList.size();i++)
					newRLList[i] = currentMethodRLList.get(i);
				//�b�s�W�վ�Ŷ���J��ơA�ϥ�����Null�A�~���|�X��
				newRLList[currentMethodRLList.size()] = newRLList[0];

				//��RL List���s�ƧǡA�Y��m���T�⵲�G�s��currentMethodRLList��
				permutation(newRLList,1);

				//��s������ RL Annotation List
				updateRLAnnotation(true, Integer.parseInt(msgIdx), levelForUpdate);
				
			}
		}
		catch (CoreException e) {
			ErrorLog.getInstance().logError("[RLQuickFix]", e);
		}

	}

	/**
	 * ��RL List���s�ƧǡA�Y��m���T�⵲�G�s��currentMethodRLList��
	 * @param newRLList
	 * 				�s�ƧǪ�RL Annotation List	
	 * @param i
	 * 				�ƦC�s�զ�m
	 */
	private void permutation(RLMessage[] newRLList, int i) {
		//�ƦC��
		if(i < newRLList.length - 1) {
    		for(int j = i; j <= newRLList.length - 1; j++) {
    			RLMessage tmp = newRLList[j];
                //����ӰϬq�̥k��Ʀr�̥ܳ���
                for(int k = j; k > i; k--)
                	newRLList[k] = newRLList[k-1];
                newRLList[i] = tmp;
                permutation(newRLList, i+1);
                //�٭�
                for(int k = i; k < j; k++)
                	newRLList[k] = newRLList[k+1];
                newRLList[j] = tmp;
        	}
   		//�ƦC����
    	} else {
        	//�YRL Annotation List���ǥ������T�A�⥦�O����currentMethodRLList��
        	if (!isAllRLListCorrect(newRLList)) {
        		currentMethodRLList.clear();
        		for(int j = 1; j <= newRLList.length - 1; j++)
        			currentMethodRLList.add(newRLList[j]);
        	}
        }
    }
	
	/**
	 * �P�_�Ҧ���RL List��m�O�_���T
	 * 
	 * @param newRLList
	 * 		�s��m��RL Annotation List
	 * @return
	 * 		RL List���ǬO�_�������T
	 */
	private boolean isAllRLListCorrect(RLMessage[] newRLList)
	{
		int msg1 = 0;
		for (int i=1;i<newRLList.length;i++) {
			msg1++;
			int k = 1;
			for (int j=1;j<newRLList.length;j++) {
				if (msg1 >= k++)
					continue;

				//�P�_�����O�O�_�b�l���O�e
				boolean isErr = isParentFrontSon(newRLList[j].getTypeBinding(),newRLList[i].getTypeBinding().getQualifiedName());
				if (isErr)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * �P�_�����O�O�_�b�l���O�e
	 * 
	 * @param typeBinding
	 * 			�����O
	 * @param typeName
	 * 			�l���O�W��
	 * @return
	 * 			�����O�O�_�b�l���O�e
	 */
    public boolean isParentFrontSon(ITypeBinding typeBinding,String typeName)
	{
		if (typeBinding == null || typeName == null)
			return false;

		String qname = typeBinding.getQualifiedName();
		if (qname.equals(typeName))
			return true;

		// �P�_�����O
		ITypeBinding superClass = typeBinding.getSuperclass();
		if (superClass != null) {
			if (superClass.getQualifiedName().equals(typeName))
				return true;
		}

		// �P�_����
		ITypeBinding[] interfaceType = typeBinding.getInterfaces();
		if (interfaceType != null && interfaceType.length > 0) {
			for (int i = 0, size = interfaceType.length; i < size; i++) {
				if (interfaceType[i].getQualifiedName().equals(typeName))
					return true;
			}
		}
		return false;
	}

	private boolean findMethod(IResource resource, int methodIdx) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {

			try {
				IJavaElement javaElement = JavaCore.create(resource);

				if (javaElement instanceof IOpenable) {
					this.actOpenable = (IOpenable) javaElement;
				}

				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);

				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.actRoot = (CompilationUnit) parser.createAST(null);
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();

				ASTNode method = methodList.get(methodIdx);
				if (method != null) {
					ExceptionAnalyzer visitor = new ExceptionAnalyzer(this.actRoot, method.getStartPosition(), 0);
					method.accept(visitor);
					currentMethodNode = visitor.getCurrentMethodNode();
					currentMethodRLList = visitor.getMethodRLAnnotationList();

					if (currentMethodNode != null) {
						RLChecker checker = new RLChecker();
						currentMethodExList = checker.check(visitor);
						return true;
					}
				}
			}
			catch (Exception ex) {
				logger.error("[findMethod] EXCEPTION ",ex);
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
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

	/***************************************************************************
	 * �NRL Annotation�T���W�[����wMethod�W
	 * 
	 * @param msg
	 *            �ثe�ƥ�Ҧb��RLMessage����T��
	 */
	@SuppressWarnings("unchecked")
	private void addOrRemoveRLAnnotation(boolean add, int pos) {

		RLMessage msg = this.currentMethodExList.get(pos);

		try {

			actRoot.recordModifications();

			AST ast = currentMethodNode.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);
			
			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			if (add) {
				addImportDeclaration();

				// �W�[�{�b�ҿ�Exception��@RL Annotation
				rlary.expressions().add(
						getRLAnnotation(ast, msg.getRLData().getLevel() <= 0 ? RL.LEVEL_1_ERR_REPORTING : msg
								.getRLData().getLevel(), msg.getRLData().getExceptionType()));
			}

			// �[�J�¦���@RL Annotation
			int idx = 0;
			for (RLMessage rlmsg : currentMethodRLList) {
				if (add) {
					// �s�W
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
				}
				else {
					// ����
					if (idx++ != pos) {
						rlary.expressions()
								.add(
										getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData()
												.getExceptionType()));
					}
				}
			}

			MethodDeclaration method = (MethodDeclaration) currentMethodNode;

			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}

			if (rlary.expressions().size() > 0) {
				method.modifiers().add(0, root);
			}

			applyChange();
		}
		catch (Exception ex) {
			logger.error("[addOrRemoveRLAnnotation] EXCEPTION ",ex);
		}
	}

	/**
	 * ��sRL Annotation
	 * 
	 * @param isAllUpdate
	 * 		�O�_��s������Annotation
	 * @param pos
	 * @param level
	 */
	@SuppressWarnings("unchecked")
	private void updateRLAnnotation(boolean isAllUpdate, int pos, int level) {
		try {

			actRoot.recordModifications();

			AST ast = currentMethodNode.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);

			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			//�Y������s�A��ܬ�currentMethodRLList�ƧǡA�ҥH�[�JAnnotation Library�ŧi
			if (isAllUpdate)
				addImportDeclaration();
			
			int msgIdx = 0;
			for (RLMessage rlmsg : currentMethodRLList) {
				//�YisAllUpdate��ܬ�RL Annotation�ƧǡA�ƧǤ��Χ��level�A�ҥH�ϥ����i�J
				if (msgIdx++ == pos && !isAllUpdate) {
					rlary.expressions().add(getRLAnnotation(ast, level, rlmsg.getRLData().getExceptionType()));
				}
				else {
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
				}
			}

			MethodDeclaration method = (MethodDeclaration) currentMethodNode;

			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//����¦���annotation��N������
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}

			if (rlary.expressions().size() > 0) {
				//�N�s�إߪ�annotation root�[�i�h
				method.modifiers().add(0, root);
			}

			applyChange();
		}
		catch (Exception ex) {
			logger.error("[updateRLAnnotation] EXCEPTION ",ex);
		}
	}

	/**
	 * ����RL Annotation��RL���
	 * 
	 * @param ast
	 *            AST Object
	 * @param levelVal
	 *            �j���׵���
	 * @param exClass
	 *            �ҥ~���O
	 * @return NormalAnnotation AST Node
	 */
	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal, String exClass) {
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RL"));

		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));

		rl.values().add(level);

		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		exclass.setType(ast.newSimpleType(ast.newName(exClass)));
		exception.setValue(exclass);

		rl.values().add(exception);
		return rl;
	}

	/**
	 * �N�n�ܧ󪺸�Ƽg�^��Document��
	 */
	private void applyChange()
	{
		//�g�^Edit��
		try{
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);
			cu.getBuffer().setContents(document.get());
			
			setCursorLocation(document);
		}
		catch(Exception ex){
			logger.error("[RLQuickFix] EXCEPTION ",ex);
		}
	}

	/**
	 * ��Щw��(�w���RL Annotation����)
	 * @param document
	 */
	private void setCursorLocation(Document document) {
		//���o�ثe��EditPart
		IEditorPart editorPart = EditorUtils.getActiveEditor();
		ITextEditor editor = (ITextEditor) editorPart;

		//���oMethod���_�I��m
		int srcPos = currentMethodNode.getStartPosition();
		//��Method�_�I��m���oMethod���ĴX���(�_�l��Ʊq0�}�l�A���O1�A�ҥH��1)
		int numLine = this.actRoot.getLineNumber(srcPos)-1;

		//�p�G��import Robustness��RL���ŧi��ƴN�[1
		if(!isImportRobustnessClass)
			numLine++;
		if(!isImportRLClass)
			numLine++;

		//���o��ƪ����
		IRegion lineInfo = null;
		try {
			lineInfo = document.getLineInformation(numLine);
		} catch (BadLocationException e) {
			logger.error("[BadLocation] EXCEPTION ",e);
		}
		
		//�ϥոӦ� �bQuick fix������,�i�H�N��Щw��bQuick Fix����
		editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
	}
}
