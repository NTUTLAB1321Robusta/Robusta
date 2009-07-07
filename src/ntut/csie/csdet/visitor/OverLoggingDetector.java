package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OverLogging��Code Smell
 * @author Shiau
 *
 */
public class OverLoggingDetector {
	private static Logger logger = LoggerFactory.getLogger(OverLoggingDetector.class);

	//�x�s�ҧ�쪺ignore Exception 
	private List<CSMessage> overLoggingList = new ArrayList<CSMessage>();
	//AST Tree��root(�ɮצW��)
	private CompilationUnit root;
	//�̩��h��Method
	MethodDeclaration startMethod;

	public OverLoggingDetector(CompilationUnit root, ASTNode method) {
		this.root = root;
		startMethod = (MethodDeclaration) method;
	}
	
	/**
	 * �M�dMethod
	 */
	public void detect() {
		//�NMethodDeclaration(AST)�ഫ��IMethod
		IMethod method = (IMethod) startMethod.resolveBinding().getJavaElement();

		//�ѪRAST�ݦ��S���o��Logging�Sthrow Exception (�ťզr���̩ܳ��hMethod)
		LoggingAnalyzer visitor = new LoggingAnalyzer(root, "");
		startMethod.accept(visitor);
		//�O�_�~�򰻴�
		boolean isTrace = visitor.getIsKeepTrace();
		//�x�s�̩��hException
		String baseException = visitor.getBaseException();

		if (isTrace) {
			//�P�_Catch Throw��Exception�O�_�PMethod Throw��Exception�ۦP
			if (isCTEqualMT(startMethod,baseException)) {
				//�ϥλ��j�P�_�O�_�o��OverLogging�A�YOverLogging�h�O����Message
				if (detectOverLogging(method,baseException))
					overLoggingList = visitor.getOverLoggingList();
			}
		}
	}
	
	/**
	 * �P�_�O�_OverLogging
	 * @param method		�QCall��Method
	 * @param baseException	�̩��h��Exception
	 * @return				�O�_OverLogging
	 */
	private boolean detectOverLogging(IMethod method,String baseException)
	{
		boolean isOverLogging;
		
		//���U�@�h�ڶi
		MethodWrapper currentMW = new CallHierarchy().getCallerRoot(method);				
		MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());

		//�Y��Caller
		if (calls.length != 0) {
			for (MethodWrapper mw : calls) {
				if (mw.getMember() instanceof IMethod) {
					IMethod callerMethod = (IMethod) mw.getMember();

					//�קKRecursive�A�YCaller Method���M�O�ۤv�N������
					if (callerMethod.equals(method))
						continue;

					//IMethod�নMethodDeclaration ASTNode
					MethodDeclaration methodNode = transMethodNode(callerMethod);

					//methodNode�X���ɡA�N������
					if (methodNode == null)
						continue;

					//�ѪRAST�ݦ��S��OverLogging
					LoggingAnalyzer visitor = new LoggingAnalyzer(null, baseException);
					methodNode.accept(visitor);

					//�O�_��Logging
					boolean isLogging = visitor.getIsLogging();
					//�O�_�~��Trace
					boolean isTrace = visitor.getIsKeepTrace();

					System.out.println(methodNode.getName() + " :");
					System.out.println("OverLogging :" + isLogging + "  | " + "Trace :" + isTrace);

					//�Y��Logging�ʧ@�h�^�Ǧ�Logging���A���U����
					if (isLogging)
						return true;

					//�O�_���W�@�h�l��
					if (isTrace) {
						//�P�_Method Throws��Exception�P�̩��h��Throw Exception�O�_�ۦP
						if (!isCTEqualMT(methodNode,baseException))
							continue;

						isOverLogging = detectOverLogging(callerMethod, baseException);
						
						//�Y�W�@�h���G��OverLoggin�N�^��true�A�_�h�~��
						if (isOverLogging)
							return true;
						else
							continue;
					}
				}
			}
		}
		//�S��Caller�A�ΩҦ�Caller�]�����S��
		return false;
	}
	
	/**
	 * �P�_Catch Throw��Exception�O�_�PMethod Throw��Exception�ۦP
	 * @param method
	 * @param catchThrowEx
	 * @return
	 */
	private boolean isCTEqualMT(MethodDeclaration method, String catchThrowEx) {
		//���oMethod��Throw Exception
		ITypeBinding[] throwExList = method.resolveBinding().getExceptionTypes();
		//TODO �����Ҽ{�Ƽƭ�
		if (throwExList != null && throwExList.length == 1) {
			//�YThrow Exception �P Method Throw Exception�@�ˤ~����
			if (throwExList[0].getName().equals(catchThrowEx))
				return true;
		}
		return false;
	}

	/**
	 * �ഫ��ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		
		try {
			//Parser Jar�ɮɡA������ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			//����AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode ast = parserAST.createAST(null);

			//���oAST��Method����
			ASTNode methodNode = NodeFinder.perform(ast, method.getSourceRange().getOffset(), method.getSourceRange().getLength());

			//�Y��ASTNode�ݩ�MethodDeclaration�A�h�૬
			if(methodNode instanceof MethodDeclaration) {
				md = (MethodDeclaration) methodNode;
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}

		return md;
	}

	/**
	 * ���oOverLogging��T
	 * @return
	 */
	public List<CSMessage> getOverLoggingList() {
		return overLoggingList;
	}
}
