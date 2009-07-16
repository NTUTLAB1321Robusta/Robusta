package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;

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
import org.jdom.Attribute;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OverLogging��Code Smell
 * @author Shiau
 *
 */
public class OverLoggingDetector {
	private static Logger logger = LoggerFactory.getLogger(OverLoggingDetector.class);

	//�x�s�ҧ�쪺OverLoging 
	private List<CSMessage> overLoggingList = new ArrayList<CSMessage>();
	//AST Tree��root(�ɮצW��)
	private CompilationUnit root;
	//�̩��h��Method
	private MethodDeclaration startMethod;
	//�x�s�ϥΪ̳]�w���J���૬Exception�ٰ��������]�w
	private boolean detectTransExSet = false;
	//�x�s����"Library��Name"�M"�O�_Library"
	//store�ϥΪ̭n������library�W�١A�M"�O�_�n������library"
	private TreeMap<String, String> libMap = new TreeMap<String, String>();

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
		//�NMethodDeclaration(AST)�ഫ��IMethod
		IMethod method = (IMethod) startMethod.resolveBinding().getJavaElement();

		//�NUser���OverLogging���]�w�s�U��
		getOverLoggingSettings();
		
		//�ѪRAST�ݦ��S���o��Logging�Sthrow Exception (�ťզr����̩ܳ��hMethod)
		LoggingThrowAnalyzer visitor = new LoggingThrowAnalyzer(root, libMap);
		startMethod.accept(visitor);
		//�O�_�~�򰻴�
		boolean isTrace = visitor.getIsKeepTrace();
		//�x�s�̩��hException
		String baseException = visitor.getBaseException();

		if (isTrace) {
			//�P�_Catch Throw��Exception�O�_�PMethod Throw��Exception�ۦP
			if (isCTEqualMT(startMethod,baseException)) {
				//�Y�ϥΪ̳]�w�Y��Exception�૬�ᤴ�]�w�A�h��P�_Exception�]���ť�("")
				if (detectTransExSet)
					baseException = "";

				//�ϥλ��j�P�_�O�_�o��OverLogging�A�YOverLogging�h�O����Message
				if (detectOverLogging(method, baseException)) {
					overLoggingList = visitor.getOverLoggingList();
				}
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
		//���S�����Logging�ʧ@
		boolean isOverLogging;

		//TODO ���o��Internal Error
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
					MethodDeclaration md = transMethodNode(method);
					String classInfo = md.resolveBinding().getDeclaringClass().getQualifiedName();

					//�ѪRAST�ݨϥΦ�Method��Catch Block�����S���o��Logging
					LoggingAnalyzer visitor = new LoggingAnalyzer(baseException, classInfo,
													method.getElementName(), libMap);
					methodNode.accept(visitor);

					//�O�_��Logging
					boolean isLogging = visitor.getIsLogging();
					//�O�_�~��Trace
					boolean isTrace = visitor.getIsKeepTrace();

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
		//�Y�ϥΪ̳]�w���ഫException�����~�򰻴��A�h���P�_
		if (detectTransExSet)
			return true;
		
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
			//Parser Jar�ɮɡA�|������ICompilationUnit
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
	 * �NUser���OverLogging���]�w�s�U��
	 * @return
	 */
	private void getOverLoggingSettings(){		
		Element root = JDomUtil.createXMLContent();
		//�p�G�Onull����XML�ɬO��ئn��,�٨S��OverLogging��tag,�������X�h
		if(root.getChild(JDomUtil.OverLoggingTag) != null){
			//�o�̪��ܤ��e�ϥΪ̤w�g���]�w�Lpreference�F,�h���o���������]�w��
			Element overLogging = root.getChild(JDomUtil.OverLoggingTag);
			Element rule = overLogging.getChild("rule");
			String log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			String javaLogger = rule.getAttribute(JDomUtil.java_Logger).getValue();

			/// �⤺�ذ����[�J��W�椺 ///
			//��log4j�MjavaLog�[�J������
			if (log4jSet.equals("Y"))
				libMap.put("org.apache.log4j",null);
			if (javaLogger.equals("Y"))
				libMap.put("java.util.logging",null);

			Element libRule = overLogging.getChild("librule");
			// ��~��Library�MStatement�x�s�bList��
			List<Attribute> libRuleList = libRule.getAttributes();

			/// ��ϥΪ̩ҳ]�w��Exception�૬�������]�w ///
			Element exrule = overLogging.getChild("exrule");
			String exSet = exrule.getAttribute(JDomUtil.transException).getValue();
			detectTransExSet = exSet.equals("Y");
			
			//��~����Library�[�J�����W�椺
			for (int i=0;i<libRuleList.size();i++) {
				if (libRuleList.get(i).getValue().equals("Y")) {
					String temp = libRuleList.get(i).getQualifiedName();					

					//�Y��.*���u����Library�AValue�]��1
					if (temp.indexOf(".EH_STAR")!=-1){
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0,pos),"1");
					//�Y��*.���u����Method�AValue�]��2
					}else if (temp.indexOf("EH_STAR.") != -1){
						libMap.put(temp.substring(8),"2");
					//���S�����������AKey�]��Library�AValue�]��Method
					}else if (temp.lastIndexOf(".")!=-1){
						int pos = temp.lastIndexOf(".");
						//�Y�w�g�s�b�A�h���[�J
						if(!libMap.containsKey(temp.substring(0,pos)))
							libMap.put(temp.substring(0,pos),temp.substring(pos+1));
					//�Y���䥦�Ϊp�h�]��Method
					}else{
						libMap.put(temp,"2");
					}
				}
			}
		}
	}
	
	/**
	 * ���oOverLogging��T
	 * @return
	 */
	public List<CSMessage> getOverLoggingList() {
		return overLoggingList;
	}
}