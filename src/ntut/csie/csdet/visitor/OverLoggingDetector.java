package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OverLogging的Code Smell
 * @author Shiau
 */
public class OverLoggingDetector {
	private static Logger logger = LoggerFactory.getLogger(OverLoggingDetector.class);
	// 儲存所找到的OverLoging 
	private List<MarkerInfo> overLoggingList = new ArrayList<MarkerInfo>();
	// AST Tree的root(檔案名稱)
	private CompilationUnit root;
	// 最底層的Method
	private MethodDeclaration startMethod;
	
	private OverLoggingVisitor visitor;

	/**
	 * Constructor
	 * @param root
	 * @param method
	 */
	public OverLoggingDetector(CompilationUnit root, MethodDeclaration method) {
		this.root = root;
		startMethod = method;
	}
	
	/**
	 * 尋查Method
	 */
	public void detect() {
		// 將MethodDeclaration(AST)轉換成IMethod
		IMethod method = (IMethod) startMethod.resolveBinding().getJavaElement();
		// 解析AST看有沒有發生Logging又throw Exception (空白字串表示最底層Method)
		visitor = new OverLoggingVisitor(root, method.getElementName());
		startMethod.accept(visitor);
		// 是否繼續偵測
		if (visitor.getIsKeepTrace()) {
			// 使用遞迴判斷是否發生OverLogging，若OverLogging則記錄其Message
			if (detectOverLogging(method))
				overLoggingList = visitor.getOverLoggingList();
		}
	}
	
	/**
	 * 判斷是否OverLogging
	 * @param method		被Call的Method
	 * @param baseException	最底層的Exception
	 * @return				是否OverLogging
	 */
	private boolean detectOverLogging(IMethod method) {
		//TODO 曾發生Internal Error
		//往下一層邁進
		IMember[] methodArray = new IMember[] {method};
		MethodWrapper[] currentMW = CallHierarchy.getDefault().getCallerRoots(methodArray);
		if (currentMW.length != 1)
			return false;
		MethodWrapper[] calls = currentMW[0].getCalls(new NullProgressMonitor());
		/* Eclipse3.3:
		 * MethodWrapper currentMW = new CallHierarchy().getCallerRoot(method);
		 * MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());
		 */

		// 若有Caller
		if (calls.length != 0) {
			for (MethodWrapper mw : calls) {
				if (mw.getMember() instanceof IMethod) {
					IMethod callerMethod = (IMethod) mw.getMember();

					// 避免Recursive，若Caller Method仍然是自己就不偵測
					if (callerMethod.equals(method))
						continue;

					// IMethod轉成MethodDeclaration ASTNode
					MethodDeclaration methodNode = transMethodNode(callerMethod);

					// methodNode出錯時，就不偵測
					if (methodNode == null)
						continue;

					// 解析AST看使用此Method的Catch Block中有沒有發生Logging
					methodNode.accept(visitor);

					// 若有Logging動作則回傳有Logging不再往下偵測
					if (visitor.getIsLogging())
						return true;

					// 是否往上一層追蹤
					if (visitor.getIsKeepTrace()) {
						// 若上一層結果為OverLogging就回傳true，否則繼續
						if (detectOverLogging(callerMethod))
							return true;
					}
				}
			}
		}
		//沒有Caller，或所有Caller跑完仍沒有
		return false;
	}

	/**
	 * 轉換成ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		// Parser Jar檔時，會取不到ICompilationUnit
		if (method.getCompilationUnit() == null) {
			return null;
		}
		// 產生AST
		ASTParser parserAST = ASTParser.newParser(AST.JLS3);
		parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
		parserAST.setSource(method.getCompilationUnit());
		parserAST.setResolveBindings(true);
		CompilationUnit root = (CompilationUnit) parserAST.createAST(null);

		// 取得MethodDeclaration
		if (method.getParent() instanceof IType) {
			TypeDeclaration td = findTypeDeclaration(root, (IType) method.getParent());
			try {
				md = findMethodDeclaration(td, method);
			} catch (JavaModelException e) {
				logger.error("[Exception] transMethodNode", e);
			}
		}
		return md;
	}

	/**
	 * 尋找Type Declaration
	 * @param root	要尋找的Type Declaration所在的ComplilationUnit
	 * @param type	要尋找的Type Declaration的ITypeDeclaration
	 * @return
	 */
	private TypeDeclaration findTypeDeclaration(CompilationUnit root, IType type) {
		for (Iterator<?> I = root.types().iterator(); I.hasNext();) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) I.next();
			if (typeDeclaration.getName().toString().equals(type.getElementName()))
				return typeDeclaration;
		}
		return null;
	}

	/**
	 * 尋找Method Declaration
	 * @param type		要尋找的Method Declaration所在的Type Declaration
	 * @param method	要尋找的Method Declaration的IMethod
	 * @return			null:沒有找到
	 */
	private MethodDeclaration findMethodDeclaration(TypeDeclaration type, IMethod method) throws JavaModelException {
		ISourceRange sourceRange = method.getSourceRange();
		for (Iterator<?> I = type.bodyDeclarations().iterator(); I.hasNext();) {
			BodyDeclaration declaration = (BodyDeclaration) I.next();
			if (declaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
				MethodDeclaration methodDeclaration = (MethodDeclaration) declaration;
				if ((sourceRange.getOffset() <= methodDeclaration.getStartPosition()) &&
					(sourceRange.getLength() >= methodDeclaration.getLength()))
					return methodDeclaration;
			}
		}
		return null;
	}
	
	/**
	 * 取得OverLogging資訊
	 * @return
	 */
	public List<MarkerInfo> getOverLoggingList() {
		return overLoggingList;
	}
}
