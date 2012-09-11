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
 * OverLogging��Code Smell
 * @author Shiau
 */
public class OverLoggingDetector {
	static Logger logger = LoggerFactory.getLogger(OverLoggingDetector.class);
	// �x�s�ҧ�쪺OverLoging 
	List<MarkerInfo> overLoggingList = new ArrayList<MarkerInfo>();
	// AST Tree��root(�ɮצW��)
	CompilationUnit root;
	// �̩��h��Method
	MethodDeclaration startMethod;
	
	OverLoggingVisitor visitor;

	/**
	 * Constructor
	 * @param root
	 * @param method
	 */
	public OverLoggingDetector(CompilationUnit root, ASTNode method) {
		this.root = root;
		startMethod = (MethodDeclaration) method;
	}
	
	/**
	 * �M�dMethod
	 */
	public void detect() {
		// �NMethodDeclaration(AST)�ഫ��IMethod
		IMethod method = (IMethod) startMethod.resolveBinding().getJavaElement();
		// �ѪRAST�ݦ��S���o��Logging�Sthrow Exception (�ťզr���̩ܳ��hMethod)
		visitor = new OverLoggingVisitor(root, method.getElementName());
		startMethod.accept(visitor);
		// �O�_�~�򰻴�
		if (visitor.getIsKeepTrace()) {
			// �ϥλ��j�P�_�O�_�o��OverLogging�A�YOverLogging�h�O����Message
			if (detectOverLogging(method))
				overLoggingList = visitor.getOverLoggingList();
		}
	}
	
	/**
	 * �P�_�O�_OverLogging
	 * @param method		�QCall��Method
	 * @param baseException	�̩��h��Exception
	 * @return				�O�_OverLogging
	 */
	private boolean detectOverLogging(IMethod method) {
		//TODO ���o��Internal Error
		//���U�@�h�ڶi
		IMember[] methodArray = new IMember[] {method};
		MethodWrapper[] currentMW = CallHierarchy.getDefault().getCallerRoots(methodArray);
		if (currentMW.length != 1)
			return false;
		MethodWrapper[] calls = currentMW[0].getCalls(new NullProgressMonitor());
		/* Eclipse3.3:
		 * MethodWrapper currentMW = new CallHierarchy().getCallerRoot(method);
		 * MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());
		 */

		// �Y��Caller
		if (calls.length != 0) {
			for (MethodWrapper mw : calls) {
				if (mw.getMember() instanceof IMethod) {
					IMethod callerMethod = (IMethod) mw.getMember();

					// �קKRecursive�A�YCaller Method���M�O�ۤv�N������
					if (callerMethod.equals(method))
						continue;

					// IMethod�নMethodDeclaration ASTNode
					MethodDeclaration methodNode = transMethodNode(callerMethod);

					// methodNode�X���ɡA�N������
					if (methodNode == null)
						continue;

					// �ѪRAST�ݨϥΦ�Method��Catch Block�����S���o��Logging
					methodNode.accept(visitor);

					// �Y��Logging�ʧ@�h�^�Ǧ�Logging���A���U����
					if (visitor.getIsLogging())
						return true;

					// �O�_���W�@�h�l��
					if (visitor.getIsKeepTrace()) {
						// �Y�W�@�h���G��OverLogging�N�^��true�A�_�h�~��
						if (detectOverLogging(callerMethod))
							return true;
					}
				}
			}
		}
		//�S��Caller�A�ΩҦ�Caller�]�����S��
		return false;
	}

	/**
	 * �ഫ��ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		// Parser Jar�ɮɡA�|������ICompilationUnit
		if (method.getCompilationUnit() == null) {
			return null;
		}
		// ����AST
		ASTParser parserAST = ASTParser.newParser(AST.JLS3);
		parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
		parserAST.setSource(method.getCompilationUnit());
		parserAST.setResolveBindings(true);
		CompilationUnit root = (CompilationUnit) parserAST.createAST(null);

		// ���oMethodDeclaration
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
	 * �M��Type Declaration
	 * @param root	�n�M�䪺Type Declaration�Ҧb��ComplilationUnit
	 * @param type	�n�M�䪺Type Declaration��ITypeDeclaration
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
	 * �M��Method Declaration
	 * @param type		�n�M�䪺Method Declaration�Ҧb��Type Declaration
	 * @param method	�n�M�䪺Method Declaration��IMethod
	 * @return			null:�S�����
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
	 * ���oOverLogging��T
	 * @return
	 */
	public List<MarkerInfo> getOverLoggingList() {
		return overLoggingList;
	}
}
