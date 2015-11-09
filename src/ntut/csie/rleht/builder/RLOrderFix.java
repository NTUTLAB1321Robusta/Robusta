package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

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
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLOrderFix {
	private static Logger logger = LoggerFactory.getLogger(RLOrderFix.class);

	private CompilationUnit actRoot;
	
	private IOpenable actOpenable;
	private List<RLMessage> currentMethodRLList = null;
	private ASTNode currentMethodNode = null;

	/**
	 * adjust RL Annotation order
	 * @param resource		
	 * @param methodIdx		
	 * @param msgIdx		
	 * @param selectLine	high light selected line number
	 */
	public void run(IResource resource, String methodIdx, String msgIdx)
	{		
		//get currentMethodRLList
		findMethod(resource, Integer.parseInt(methodIdx));

		//check whether currentMethodRLList is error or not 
		boolean isError = checkCurrentMethodRLList();

		//reset currentMethodRLList when it is error
		if (isError)
		{			
			//create RLArray with one extra space than currentMethodRLList size(this extra space is used to reorder element)
			RLMessage[] newRLList = new RLMessage[currentMethodRLList.size()+1];
			//put currentMethodRLList element in RLArray
			for (int i=0;i < currentMethodRLList.size();i++)
				newRLList[i] = currentMethodRLList.get(i);
			//add redundant data to newRLList's last index in case null pointer
			newRLList[currentMethodRLList.size()] = newRLList[0];

			//reorder newRLList element, if order is correct then save newRLList element in currentMethodRLList
			permutation(newRLList,1);
		}

		updateRLAnnotation(Integer.parseInt(msgIdx));
	}
	
	/**
	 * get currentMethodRLList
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
				List<MethodDeclaration> methodList = methodCollector.getMethodList();

				MethodDeclaration method = methodList.get(methodIdx);
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
	 * check currentMethodRLList whether it is needed to be reset
	 * 
	 * @return	true is error
	 * 			false  is correct
	 */
	private boolean checkCurrentMethodRLList() {
		boolean isError = false;
		int idx = -1;
		for (RLMessage msg : currentMethodRLList) {
			idx++;
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
	 * Reorder RLList element order. 
	 * During reordering, check RLList element order is correct or not. 
	 * If RLList element is in order then put the element into currentMethodRLList.
	 * 
	 * @param newRLList
	 * 				newRLList which need to be reordered	
	 * @param i
	 * 				order index
	 */
	private void permutation(RLMessage[] newRLList, int i) {
		//reorder element
		if(i < newRLList.length - 1) {
    		for(int j = i; j <= newRLList.length - 1; j++) {
    			RLMessage tmp = newRLList[j];
                //shift the rightmost element to leftmost
                for(int k = j; k > i; k--)
                	newRLList[k] = newRLList[k-1];
                newRLList[i] = tmp;
                permutation(newRLList, i+1);
                //recover the position's change
                for(int k = i; k < j; k++)
                	newRLList[k] = newRLList[k+1];
                newRLList[j] = tmp;
        	}
   		//reorder element completely
    	} else {
        	//if newRLList element order is correct，put element in currentMethodRLList
        	if (!isRLListCorrect(newRLList)) {
        		currentMethodRLList.clear();
        		for(int j = 1; j <= newRLList.length - 1; j++)
        			currentMethodRLList.add(newRLList[j]);
        	}
        }
    }
	
	/**
	 * check RLList element order is correct
	 * 
	 * @param newRLList
	 * 		new RLlist which is unknown about order
	 * @return
	 * 		is newRLList element is in order
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

				//check whether position in newRLList of super class is in front of the position of the subclass  
				boolean isErr = isParentFrontSon(newRLList[j].getTypeBinding(),newRLList[i].getTypeBinding().getQualifiedName());
				if (isErr)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * check whether position of super class is in front of the position of the subclass
	 * 
	 * @param typeBinding
	 * 			super class
	 * @param typeName
	 * 			subclass name
	 * @return
	 * 			true, super class is in front of subclass
	 * 			false, typeBinding or typeName is null or class' order is wrong 
	 */
    private boolean isParentFrontSon(ITypeBinding typeBinding,String typeName)
	{
		if (typeBinding == null || typeName == null)
			return false;

		String qname = typeBinding.getQualifiedName();
		if (qname.equals(typeName))
			return true;

		//identify supper class
		ITypeBinding superClass = typeBinding.getSuperclass();
		if (superClass != null) {
			if (superClass.getQualifiedName().equals(typeName))
				return true;
		}

		//identify sub class
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
	 * update RLannotation // dead code
	 * @param pos
	 */
	private void updateRLAnnotation(int pos) {
		try {

			actRoot.recordModifications();

			AST ast = currentMethodNode.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName(Robustness.class.getSimpleName()));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName(Robustness.VALUE));

			root.values().add(value);

			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			
			int msgIdx = 0;
			for (RLMessage rlmsg : currentMethodRLList) {
				rlary.expressions().add(
					getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
			}

			MethodDeclaration method = (MethodDeclaration) currentMethodNode;

			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//remove annotation which has existed
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}

			if (rlary.expressions().size() > 0) {
				method.modifiers().add(0, root);
			}
			
			//store modifying data back document and high light
			applyChange();
		}
		catch (Exception ex) {
			logger.error("[updateRLAnnotation] EXCEPTION ",ex);
		}
	}
	
	/**
	 * generate RLinformation for RLannotation
	 * @param ast
	 *            AST Object
	 * @param levelVal
	 *            robustness level
	 * @param exClass
	 *            exception class
	 * @return NormalAnnotation AST Node
	 */
	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal, String exClass) {
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName(RTag.class.getSimpleName()));

		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));

		rl.values().add(level);

		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName(RTag.EXCEPTION));
		TypeLiteral exclass = ast.newTypeLiteral();
		exclass.setType(ast.newSimpleType(ast.newName(exClass)));
		exception.setValue(exclass);

		rl.values().add(exception);
		return rl;
	}
	
	/**
	 * update change information back to document 
	 */
	private void applyChange()
	{
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
