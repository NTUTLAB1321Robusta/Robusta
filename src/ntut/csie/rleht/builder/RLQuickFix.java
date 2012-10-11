
package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.csdet.quickfix.BaseQuickFix;
import ntut.csie.rleht.common.ErrorLog;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLChecker;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.robusta.agile.exception.RTag;

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
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RLQuickFix extends BaseQuickFix implements IMarkerResolution, IMarkerResolution2 {
	private static Logger logger = LoggerFactory.getLogger(RLQuickFix.class);
	// 目前method的Exception資訊
	private List<RLMessage> currentMethodExList = null;
	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;
	private String label;
	private String errMsg;
	private int levelForUpdate;

	public RLQuickFix(String label, String errMsg) {
		this.label = label;
		this.errMsg = errMsg;
	}

	public RLQuickFix(String label, int level, String errMsg) {
		this.label = label;
		this.levelForUpdate = level;
		this.errMsg = errMsg;
	}

	public String getLabel() {
		return label;
	}
	
	@Override
	public String getDescription() {
		// return Messages.format(CorrectionMessages.MarkerResolutionProposal_additionaldesc, "@Tag");
		return Messages.format(CorrectionMessages.MarkerResolutionProposal_additionaldesc, errMsg);
	}

	@Override
	public Image getImage() {
		// Resource Icons的Annotation圖示
		// return ImageManager.getInstance().get("annotation");
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
	}

	public void run(IMarker marker) {
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			//Tag Level有問題(超出1~3範圍內)
			if (problem != null && problem.equals(RLMarkerAttribute.ERR_RL_LEVEL)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if (isok) {
					this.updateRLAnnotation(Integer.parseInt(msgIdx), levelForUpdate);
					// 定位到Annotation該行
					// selectLine(marker, methodIdx);
				}
			}
			//無RL資訊
			else if (problem != null && problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if (isok) {
					this.addOrRemoveRLAnnotation(true, Integer.parseInt(msgIdx));
					// 定位到Annotation該行
					// selectLine(marker, methodIdx);
				}
			}
			//RL順序對調
			else if (problem != null && problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				// 調整RL Annotation順序
				new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);

				// 定位到Annotation該行
				// selectLine(marker, methodIdx);
			}
		} catch (CoreException e) {
			ErrorLog.getInstance().logError("[RLQuickFix]", e);
		}
	}

	/**
	 * 取得Method的資訊
	 * @param resource
	 * @param methodIdx		method的Index
	 * @return				是否成功
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
				this.actRoot.accept(methodCollector);
				List<MethodDeclaration> methodList = methodCollector.getMethodList();

				MethodDeclaration method = methodList.get(methodIdx);
				if (method != null) {
					ExceptionAnalyzer visitor = new ExceptionAnalyzer(this.actRoot, method.getStartPosition(), 0);
					method.accept(visitor);
					this.currentMethodNode = visitor.getCurrentMethodNode();
					currentMethodRLList = visitor.getMethodRLAnnotationList();

					if (this.currentMethodNode != null) {
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

	/**
	 * 若未import Robustness Library，則把它import
	 */
	@SuppressWarnings("unchecked")
	private void addImportDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		List<ImportDeclaration> importList = this.actRoot.imports();

		//是否已存在Robustness及RL的宣告
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;

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
			imp.setName(rootAst.newName(RTag.class.getName()));
			this.actRoot.imports().add(imp);
		}
	}

	/**
	 * 將RL Annotation訊息增加到指定Method上
	 * 
	 * @param add
	 * @param pos
	 */
	@SuppressWarnings("unchecked")
	private void addOrRemoveRLAnnotation(boolean add, int pos) {

		RLMessage msg = this.currentMethodExList.get(pos);

		try {
			this.actRoot.recordModifications();

			AST ast = this.currentMethodNode.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);
			
			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			if (add) {
				addImportDeclaration();

				// 增加現在所選Exception的@Tag Annotation
				rlary.expressions().add(
						getRLAnnotation(ast, msg.getRLData().getLevel() <= 0 ? RTag.LEVEL_1_ERR_REPORTING : msg
								.getRLData().getLevel(), msg.getRLData().getExceptionType()));
			}

			// 加入舊有的@Tag Annotation
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

			MethodDeclaration method = (MethodDeclaration) this.currentMethodNode;

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

			this.applyChange();
		}
		catch (Exception ex) {
			logger.error("[addOrRemoveRLAnnotation] EXCEPTION ",ex);
		}
	}

	/**
	 * 更新RL Annotation
	 * 
	 * @param isAllUpdate	是否更新全部的Annotation
	 * @param pos
	 * @param level
	 */
	private void updateRLAnnotation(int pos, int level) {
		try {

			this.actRoot.recordModifications();

			AST ast = currentMethodNode.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);

			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			//若全部更新，表示為currentMethodRLList排序，所以加入Annotation Library宣告
			
			int msgIdx = 0;
			for (RLMessage rlmsg : currentMethodRLList) {
				//若isAllUpdate表示為RL Annotation排序，排序不用更改level，所以使它不進入
				if (msgIdx++ == pos) {
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

			this.applyChange();
		}
		catch (Exception ex) {
			logger.error("[updateRLAnnotation] EXCEPTION ",ex);
		}
	}

	/**
	 * 產生RL Annotation之RL資料
	 * 
	 * @param ast		AST Object
	 * @param levelVal	強健度等級
	 * @param exClass	例外類別
	 * @return NormalAnnotation AST Node
	 */
	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal, String exClass) {
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("Tag"));

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

//	/**
//	 * 將要變更的資料寫回至Document中
//	 */
//	private void applyChange()
//	{
//		//寫回Edit中
//		try{
//			ICompilationUnit cu = (ICompilationUnit) actOpenable;
//			Document document = new Document(cu.getBuffer().getContents());
//			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
//			edits.apply(document);
//			cu.getBuffer().setContents(document.get());
//		}
//		catch(Exception ex){
//			logger.error("[RLQuickFix] EXCEPTION ",ex);
//		}
//	}
//	/**
//	 * 游標定位(定位到RL Annotation那行)
//	 * @param marker
//	 * @param methodIdx
//	 * @throws JavaModelException
//	 */
//	private void selectLine(IMarker marker, String methodIdx) throws JavaModelException {
//		//重新取得新的Method資訊(因為資料已改變，method那些資訊是舊的)
//		boolean isok = findMethod(marker.getResource(), Integer.parseInt(methodIdx));
//
//		if (isok) {
//			ICompilationUnit cu = (ICompilationUnit) actOpenable;
//			Document document = new Document(cu.getBuffer().getContents());
//
//			//取得目前的EditPart
//			IEditorPart editorPart = EditorUtils.getActiveEditor();
//			ITextEditor editor = (ITextEditor) editorPart;
//	
//			//取得Method的起點位置
//			int srcPos = currentMethodNode.getStartPosition();
//			//用Method起點位置取得Method位於第幾行數(起始行數從0開始，不是1，所以減1)
//			int numLine = this.actRoot.getLineNumber(srcPos)-1;
//	
//			//取得行數的資料
//			IRegion lineInfo = null;
//			try {
//				lineInfo = document.getLineInformation(numLine);
//			} catch (BadLocationException e) {
//				logger.error("[BadLocation] EXCEPTION ",e);
//			}
//	
//			//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
//			editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
//		}
//	}
}
