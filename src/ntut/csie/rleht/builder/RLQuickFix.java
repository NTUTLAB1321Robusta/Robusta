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
	
	// 目前method的Exception資訊
	private List<RLMessage> currentMethodExList = null;

	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;

	// 目前的Method AST Node
	private ASTNode currentMethodNode = null;

	private CompilationUnit actRoot;

	private IOpenable actOpenable;

	private String label;

	private int levelForUpdate;
	
	//是否已存在Robustness及RL的宣告
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
			//RL順序對調
			else if (problem != null && problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE))
			{
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				findMethod(marker.getResource(), Integer.parseInt(methodIdx));

				//把currentMethodRLList存成Array + 1(加1為排序時可調整的空間)
				RLMessage[] newRLList = new RLMessage[currentMethodRLList.size()+1];
				//currentMethodRLList放入Array中
				for (int i=0;i < currentMethodRLList.size();i++)
					newRLList[i] = currentMethodRLList.get(i);
				//在新增調整空間放入資料，使它不為Null，才不會出錯
				newRLList[currentMethodRLList.size()] = newRLList[0];

				//把RL List重新排序，若位置正確把結果存到currentMethodRLList中
				permutation(newRLList,1);

				//更新全部的 RL Annotation List
				updateRLAnnotation(true, Integer.parseInt(msgIdx), levelForUpdate);
				
			}
		}
		catch (CoreException e) {
			ErrorLog.getInstance().logError("[RLQuickFix]", e);
		}

	}

	/**
	 * 把RL List重新排序，若位置正確把結果存到currentMethodRLList中
	 * @param newRLList
	 * 				新排序的RL Annotation List	
	 * @param i
	 * 				排列群組位置
	 */
	private void permutation(RLMessage[] newRLList, int i) {
		//排列中
		if(i < newRLList.length - 1) {
    		for(int j = i; j <= newRLList.length - 1; j++) {
    			RLMessage tmp = newRLList[j];
                //旋轉該區段最右邊數字至最左邊
                for(int k = j; k > i; k--)
                	newRLList[k] = newRLList[k-1];
                newRLList[i] = tmp;
                permutation(newRLList, i+1);
                //還原
                for(int k = i; k < j; k++)
                	newRLList[k] = newRLList[k+1];
                newRLList[j] = tmp;
        	}
   		//排列完成
    	} else {
        	//若RL Annotation List順序全部正確，把它記錄到currentMethodRLList裡
        	if (!isAllRLListCorrect(newRLList)) {
        		currentMethodRLList.clear();
        		for(int j = 1; j <= newRLList.length - 1; j++)
        			currentMethodRLList.add(newRLList[j]);
        	}
        }
    }
	
	/**
	 * 判斷所有的RL List位置是否正確
	 * 
	 * @param newRLList
	 * 		新位置的RL Annotation List
	 * @return
	 * 		RL List順序是否全部正確
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

				//判斷父類別是否在子類別前
				boolean isErr = isParentFrontSon(newRLList[j].getTypeBinding(),newRLList[i].getTypeBinding().getQualifiedName());
				if (isErr)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * 判斷父類別是否在子類別前
	 * 
	 * @param typeBinding
	 * 			父類別
	 * @param typeName
	 * 			子類別名稱
	 * @return
	 * 			父類別是否在子類別前
	 */
    public boolean isParentFrontSon(ITypeBinding typeBinding,String typeName)
	{
		if (typeBinding == null || typeName == null)
			return false;

		String qname = typeBinding.getQualifiedName();
		if (qname.equals(typeName))
			return true;

		// 判斷父類別
		ITypeBinding superClass = typeBinding.getSuperclass();
		if (superClass != null) {
			if (superClass.getQualifiedName().equals(typeName))
				return true;
		}

		// 判斷介面
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
		// 判斷是否已經Import Robustness及RL的宣告
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
	 * 將RL Annotation訊息增加到指定Method上
	 * 
	 * @param msg
	 *            目前事件所在的RLMessage物件訊息
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

				// 增加現在所選Exception的@RL Annotation
				rlary.expressions().add(
						getRLAnnotation(ast, msg.getRLData().getLevel() <= 0 ? RL.LEVEL_1_ERR_REPORTING : msg
								.getRLData().getLevel(), msg.getRLData().getExceptionType()));
			}

			// 加入舊有的@RL Annotation
			int idx = 0;
			for (RLMessage rlmsg : currentMethodRLList) {
				if (add) {
					// 新增
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
				}
				else {
					// 移除
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
	 * 更新RL Annotation
	 * 
	 * @param isAllUpdate
	 * 		是否更新全部的Annotation
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

			//若全部更新，表示為currentMethodRLList排序，所以加入Annotation Library宣告
			if (isAllUpdate)
				addImportDeclaration();
			
			int msgIdx = 0;
			for (RLMessage rlmsg : currentMethodRLList) {
				//若isAllUpdate表示為RL Annotation排序，排序不用更改level，所以使它不進入
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
				//找到舊有的annotation後將它移除
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}

			if (rlary.expressions().size() > 0) {
				//將新建立的annotation root加進去
				method.modifiers().add(0, root);
			}

			applyChange();
		}
		catch (Exception ex) {
			logger.error("[updateRLAnnotation] EXCEPTION ",ex);
		}
	}

	/**
	 * 產生RL Annotation之RL資料
	 * 
	 * @param ast
	 *            AST Object
	 * @param levelVal
	 *            強健度等級
	 * @param exClass
	 *            例外類別
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
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange()
	{
		//寫回Edit中
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
	 * 游標定位(定位到RL Annotation那行)
	 * @param document
	 */
	private void setCursorLocation(Document document) {
		//取得目前的EditPart
		IEditorPart editorPart = EditorUtils.getActiveEditor();
		ITextEditor editor = (ITextEditor) editorPart;

		//取得Method的起點位置
		int srcPos = currentMethodNode.getStartPosition();
		//用Method起點位置取得Method位於第幾行數(起始行數從0開始，不是1，所以減1)
		int numLine = this.actRoot.getLineNumber(srcPos)-1;

		//如果有import Robustness或RL的宣告行數就加1
		if(!isImportRobustnessClass)
			numLine++;
		if(!isImportRLClass)
			numLine++;

		//取得行數的資料
		IRegion lineInfo = null;
		try {
			lineInfo = document.getLineInformation(numLine);
		} catch (BadLocationException e) {
			logger.error("[BadLocation] EXCEPTION ",e);
		}
		
		//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
		editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
	}
}
