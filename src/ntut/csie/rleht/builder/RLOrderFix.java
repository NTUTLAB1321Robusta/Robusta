package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLOrderFix {
	private static Logger logger = LoggerFactory.getLogger(RLOrderFix.class);

	private CompilationUnit actRoot;
	
	private IOpenable actOpenable;
	//�ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;
	//�ثe��Method AST Node
	private ASTNode currentMethodNode = null;

	/**
	 * �i��վ�RL Annotation����
	 * @param resource		
	 * @param methodIdx		
	 * @param msgIdx		
	 * @param selectLine	���ϥժ����
	 */
	public void run(IResource resource, String methodIdx, String msgIdx)
	{		
		//���ocurrentMethodRLList
		findMethod(resource, Integer.parseInt(methodIdx));

		//�P�_currentMethodRLList�O�_�ݭn���m
		boolean isError = checkCurrentMethodRLList();

		//�Y�ݭn���m�A�h�i�歫�m�@�~
		if (isError)
		{			
			//��currentMethodRLList�s��Array + 1(�[1���ƧǮɥi�վ㪺�Ŷ�)
			RLMessage[] newRLList = new RLMessage[currentMethodRLList.size()+1];
			//currentMethodRLList��JArray��
			for (int i=0;i < currentMethodRLList.size();i++)
				newRLList[i] = currentMethodRLList.get(i);
			//�b�s�W�վ�Ŷ���J��ơA�ϥ�����Null�A�~���|�X��
			newRLList[currentMethodRLList.size()] = newRLList[0];

			//��RL List���s�ƧǡA�Y��m���T�⵲�G�s��currentMethodRLList��
			permutation(newRLList,1);
		}

		//��s������ RL Annotation List
		updateRLAnnotation(Integer.parseInt(msgIdx));
	}
	
	/**
	 * ���ocurrentMethodRLList
	 * 
	 * @param resource
	 * @param methodIdx
	 * @return
	 */
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
				}
			}
			catch (Exception ex) {
				logger.error("[findMethod] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 * �P�_currentMethodRLList�O�_�ݭn���m
	 * 
	 * @return	�O�_�ݭn���m
	 */
	private boolean checkCurrentMethodRLList() {
		boolean isError = false;
		int idx = -1;
		for (RLMessage msg : currentMethodRLList) {
			idx++;
			// �ˬd@RL�M�椺��exception���O���h�O�_���T
			int idx2 = 0;
			for (RLMessage msg2 : currentMethodRLList) {
				if (idx >= idx2++) {
					continue;
				}
				if (isParentFrontSon(msg2.getTypeBinding(), msg.getTypeBinding().getQualifiedName()))
					isError = true;
			}
		}
		return isError;
	}
	
	/**
	 * ��RL List�ƦC�զX�ƧǡA�C�ƦC�@���P�_���ǬO�_���T�A�Y���T�⥿�T���G�s��currentMethodRLList��
	 * 
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
        	if (!isRLListCorrect(newRLList)) {
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
	private boolean isRLListCorrect(RLMessage[] newRLList)
	{
		int msg1 = 0;
		for (int i = 1; i < newRLList.length; i++) {
			msg1++;
			int k = 1;
			for (int j=1; j < newRLList.length; j++) {
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
    private boolean isParentFrontSon(ITypeBinding typeBinding,String typeName)
	{
		if (typeBinding == null || typeName == null)
			return false;

		String qname = typeBinding.getQualifiedName();
		if (qname.equals(typeName))
			return true;

		//�P�_�����O
		ITypeBinding superClass = typeBinding.getSuperclass();
		if (superClass != null) {
			if (superClass.getQualifiedName().equals(typeName))
				return true;
		}

		//�P�_����
		ITypeBinding[] interfaceType = typeBinding.getInterfaces();
		if (interfaceType != null && interfaceType.length > 0) {
			for (int i = 0, size = interfaceType.length; i < size; i++) {
				if (interfaceType[i].getQualifiedName().equals(typeName))
					return true;
			}
		}
		return false;
	}
    
	/**
	 * ��sRL Annotation
	 * 
	 * @param isAllUpdate
	 * 		�O�_��s������Annotation
	 * @param pos
	 * @param level
	 */
	private void updateRLAnnotation(int pos) {
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
			
			int msgIdx = 0;
			for (RLMessage rlmsg : currentMethodRLList) {
				rlary.expressions().add(
					getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
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
			
			//�N�n�ܧ󪺸�Ƽg�^��Document���A�äϥ�
			applyChange();
		}
		catch (Exception ex) {
			logger.error("[updateRLAnnotation] EXCEPTION ",ex);
		}
	}
	
	/**
	 * ����RL Annotation��RL���
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
		}
		catch(Exception ex){
			logger.error("[RLQuickFix] EXCEPTION ",ex);
		}
	}
}
