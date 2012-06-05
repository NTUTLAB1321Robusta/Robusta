package ntut.csie.rleht.views;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.apache.commons.lang.text.StrTokenizer;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.Robustness;
import agile.exception.SuppressSmell;

public class ExceptionAnalyzer extends RLBaseVisitor {
	private static Logger logger =LoggerFactory.getLogger(ExceptionAnalyzer.class);
	// �ثe���X��ܤ��}�l��m
	private int sourceStart;

	// �ثe���X��ܤ�������m
	private int sourceEnd;

	// �ثe�Ҧb��Method�`�I
	private ASTNode currentMethodNode;

	private int currentMethodStart;

	private int currentMethodEnd;

	private boolean currentMethodFound;

	private int idxTry = 0;

	private int idxCatch = 0;

	private int tryBlock = 0;

	private String parentId = "";

	private List<RLMessage> exceptionList;

	private List<RLMessage> methodRLList;

	private List<SSMessage> suppressList;
	
	// ����Nested Try Block����m
	private List<CSMessage> nestedTryList;

	private ASTNode currentRLAnnotationNode;

	private CompilationUnit root;
	
	public void clear(){
		if(exceptionList!=null){
			exceptionList.clear();
			nestedTryList.clear();
		}
		
		if(methodRLList!=null){
			methodRLList.clear();
		}
		
		currentRLAnnotationNode=null;
		currentMethodNode=null;
	}

	/**
	 * ���o�ثemethod���Ҧ��i��o�ͪ�exception��catch��T
	 * 
	 * @return
	 */
	public List<RLMessage> getExceptionList() {
		return exceptionList;
	}

	/**
	 * ���o�ثe�Ҧbmethod��RL�T��
	 * 
	 * @return
	 */
	public List<RLMessage> getMethodRLAnnotationList() {
		return this.methodRLList;
	}
	/**
	 * ���oSuppressSmell��T(�]�tMethod�W�PCatch��)
	 * @return
	 */
	public List<SSMessage> getSuppressSemllAnnotationList() {
		return suppressList;
	}

	/**
	 * @param offset
	 * @param len
	 */
	public ExceptionAnalyzer(CompilationUnit root, int offset, int len) {
		super(true);
		sourceStart = offset;
		sourceEnd = offset + len;

		currentMethodFound = false;
		currentMethodStart = -1;
		currentMethodEnd = -1;
		parentId = "";
		this.root = root;
		exceptionList = new ArrayList<RLMessage>();
		methodRLList = new ArrayList<RLMessage>();
		suppressList = new ArrayList<SSMessage>();
		nestedTryList = new ArrayList<CSMessage>();
	}

	protected ExceptionAnalyzer(CompilationUnit root, boolean currentMethodFound, String parentId) {
		super(true);
		this.currentMethodFound = currentMethodFound;
		this.idxTry = 0;
		this.idxCatch = 0;
		this.parentId = parentId;
		this.root = root;
		exceptionList = new ArrayList<RLMessage>();
		suppressList = new ArrayList<SSMessage>();
		nestedTryList = new ArrayList<CSMessage>();
	}

	/**
	 * ���oMethod�W��Annotation��T
	 * @param node
	 */
	private void getMethodAnnotation(ASTNode node) {
		MethodDeclaration method = (MethodDeclaration) node;
		
		//logger.debug("#####===>method=" + method.getName());
		
		IAnnotationBinding[] annoBinding = method.resolveBinding().getAnnotations();
		
		for (int i = 0, size = annoBinding.length; i < size; i++) {
			//���oMethod�W��SuppressSmell��T
			if (annoBinding[i].getAnnotationType().getQualifiedName().equals(SuppressSmell.class.getName())) {
				IMemberValuePairBinding[] mvpb = annoBinding[i].getAllMemberValuePairs();

				SSMessage ssmsg = new SSMessage(node.getStartPosition(),
												this.getLineNumber(node.getStartPosition()));
				//�YSuppressSmell����String
				if (mvpb[0].getValue() instanceof String) {
					ssmsg.addSmellList((String) mvpb[0].getValue());
				//�YSuppressSmell����Array
				} else if (mvpb[0].getValue() instanceof Object[]) {
					Object[] values = (Object[]) mvpb[0].getValue();
					for (Object obj : values)
						if (obj instanceof String)
							ssmsg.addSmellList((String) obj);
				}
				suppressList.add(ssmsg);
			}			
			//���oMethod�W��Robustness��T
			if (annoBinding[i].getAnnotationType().getQualifiedName().equals(Robustness.class.getName())) {
				IMemberValuePairBinding[] mvpb = annoBinding[i].getAllMemberValuePairs();

				for (int x = 0, xsize = mvpb.length; x < xsize; x++) {
					if (mvpb[x].getValue() instanceof Object[]) {

						Object[] values = (Object[]) mvpb[x].getValue();
						for (int y = 0, ysize = values.length; y < ysize; y++) {
							IAnnotationBinding binding = (IAnnotationBinding) values[y];
							// �B�zRL
							IMemberValuePairBinding[] rlMvpb = binding.getAllMemberValuePairs();
							if (rlMvpb.length == 2) {

								int level = ((Integer) rlMvpb[0].getValue()).intValue();
								
								RLMessage rlmsg = new RLMessage(level, ((ITypeBinding) rlMvpb[1].getValue()),
										node.toString(), node.getStartPosition(), this.getLineNumber(node
												.getStartPosition()));

								rlmsg.setKey("");
								rlmsg.setKeyList(null);
								this.methodRLList.add(rlmsg);

							}
						}
					}
				}
			}
		}
		
	}

	private String createParentId() {
		return this.parentId + this.idxTry + "." + this.idxCatch + "-";
	}

	/**
	 * ���oMethod�ҫŧi��throws Exception Type Name
	 * @param node	MethodDeclaration
	 */
	private void getMethodThrowsList(ASTNode node) {
		MethodDeclaration method = (MethodDeclaration) node;
		List<Name> throwsList = method.thrownExceptions();

		for (Name name : throwsList) {
			
			//logger.debug("#####===>throw list=" + name.getFullyQualifiedName());
			
			String code = node.toString();
			int pos1 = code.indexOf("throws");
			int pos2 = code.indexOf("{", pos1 + 1);
			try {
				if(pos2==-1){
					pos2=code.indexOf(";", pos1 + 1);
				}
				code = code.substring(pos1, pos2);
				RLMessage rlmsg = new RLMessage(0, name.resolveTypeBinding(), code, name.getStartPosition(),
						this.getLineNumber(name.getStartPosition()));

				rlmsg.setKey("");
				rlmsg.setKeyList(null);
				this.exceptionList.add(rlmsg);
			} catch (Exception ex) {
				logger.error("[getMethodThrowsList] pos1="+ pos1 + " pos2="+pos2 + " ==>"+code,ex);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {

		//long m1=Runtime.getRuntime().freeMemory();
		//logger.debug("!!!!!===>["+parentId+"][BEGIN] freeMemory=["+m1+ "] max mem="+Runtime.getRuntime().maxMemory() + " totalMem="+Runtime.getRuntime().totalMemory() );		

		try {

			switch (node.getNodeType()) {

				case ASTNode.METHOD_DECLARATION:
					// �M��ҿ�ܤ�r�b���@��Method��
					currentMethodStart = node.getStartPosition();
					currentMethodEnd = currentMethodStart + node.getLength();

					if (currentMethodEnd < sourceStart || sourceEnd < currentMethodStart) {
						currentMethodFound = false;
					}
					
					if (currentMethodStart <= sourceStart && sourceEnd <= currentMethodEnd) {
						currentMethodNode = node;
						currentMethodFound = true;

						this.getMethodAnnotation(node);
						this.getMethodThrowsList(node);
					} else {
						currentMethodFound = false;
					}
					
					return true;					
				case ASTNode.TRY_STATEMENT:
					// currentMethodFound + ":" + node);
					if (currentMethodFound) {
						this.processTryStatement(node);
						return false;
					}
					return true;
				case ASTNode.THROW_STATEMENT:
					// ConsoleLog.debug("THROW_STATEMENT=>currentMethodFound="
					// + currentMethodFound + ":" + node);
					if (currentMethodFound) {
						// ConsoleLog.debug("THROW_STATEMENT");
						ThrowStatement ts = (ThrowStatement) node;
						Object obj = ts.getStructuralProperty(ThrowStatement.EXPRESSION_PROPERTY);
						
						RLMessage rlmsg = null;
						if (obj instanceof ClassInstanceCreation) {
							rlmsg = new RLMessage(0, ((ClassInstanceCreation) obj).resolveTypeBinding(), ts
									.toString(), ts.getStartPosition(), this.getLineNumber(ts
									.getStartPosition()));

						} else if (obj instanceof SimpleName) {
							rlmsg = new RLMessage(0, ((SimpleName) obj).resolveTypeBinding(), ts.toString(),
									ts.getStartPosition(), this.getLineNumber(ts.getStartPosition()));
						}

						if (rlmsg != null) {
							addRL(rlmsg, this.idxCatch);
						}

					}
					return true;

				case ASTNode.CLASS_INSTANCE_CREATION:
					if (currentMethodFound) {
						ClassInstanceCreation cic = (ClassInstanceCreation) node;

						if (!this.findAnnotation(node, cic.resolveConstructorBinding().getAnnotations())) {
							// ���oMethod��Throw Exception Type
							this.findExceptionTypes(node, cic.resolveConstructorBinding().getExceptionTypes());
						}

					}
					return true;
				case ASTNode.CONSTRUCTOR_INVOCATION:
					if (currentMethodFound) {
						ConstructorInvocation ci = (ConstructorInvocation) node;

						if (!this.findAnnotation(node, ci.resolveConstructorBinding().getAnnotations())) {
							// ���oMethod��Throw Exception Type
							this.findExceptionTypes(node, ci.resolveConstructorBinding().getExceptionTypes());
						}
					}
					return true;
				case ASTNode.METHOD_INVOCATION:
					if (currentMethodFound) {
						MethodInvocation mi = (MethodInvocation) node;

						/* �p�GUser��code���ŦXIDE���W�w�A�|�y��MethdoInvocation��Binding���~�A
						 * �ɭPmi.resolveMethodBinding()��null�C
						 * ex: �btry block�ŧi�@�ӻݭnclose��instance
						 */
						if (mi.resolveMethodBinding() == null)
							return false;	//�קK�U�����{���X�Ǧ^null pointer exception
						
						if (!this.findAnnotation(node, mi.resolveMethodBinding().getAnnotations())) {
							// ���oMethod��Throw Exception Type
							this.findExceptionTypes(node, mi.resolveMethodBinding().getExceptionTypes());

						}
					}
					return true;

				default:
					return true;

			}
		} catch (Exception ex) {
			logger.error("[visitNode] EXCEPTION ",ex);
			return false;
		}
		finally{
			//long m2=Runtime.getRuntime().freeMemory();
			//logger.debug("!!!!!===>["+parentId+"][END]  freeMemory=["+ m2 +"] ["+(m2-m1)+"] " );
		}
	}

	/**
	 * 1. ����Nested Try Block��EH Smell�A���Try�BCatch�PFinally�A�ϥ�Visitor�����C
	 * 2. �O��Catch Clause�ҫŧi��Exception Type
	 * 3. ���oCatch����Suppress Smell����T
	 * 
	 * @param node	TryStatement
	 */
	private void processTryStatement(ASTNode node) {
		
		//logger.debug("#####===>TRY_STATEMENT " );
		
		// ConsoleLog.debug("TRY_STATEMENT");
		// ------------------------------------------------------

		idxTry = (++tryBlock);
		idxCatch = 0;

		TryStatement trystat = (TryStatement) node;		

		// ConsoleLog.debug("[TRY_STATEMENT][BEGIN]>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		// ConsoleLog.debug("TRY===>" + idxTry + ":" + idxCatch + "\t[BEGIN]" +
		// trystat.getBody().getStartPosition());
		
		// ���p�O�Ĥ@��Try,���N���OCode Smell,���Υ[�i�h
		if(!this.parentId.equals("")){
			CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_NESTED_TRY_BLOCK,null,											
					trystat.toString(),trystat.getStartPosition(),
					this.getLineNumber(trystat.getStartPosition()),trystat.toString());
			this.nestedTryList.add(csmsg);	
			
		}
		
		// �B�zTry Block
		ExceptionAnalyzer visitor = new ExceptionAnalyzer(this.root, true, this.createParentId());
		trystat.getBody().accept(visitor);
		
		this.mergeCS(visitor.getNestedTryList());
		this.mergeRL(visitor.getExceptionList());
		visitor.clear();

		// this.visitNode(trystat.getBody());

//		 ConsoleLog.debug("TRY===>" + idxTry + ":" + idxCatch + "\t[END]"+
//		 trystat.getBody().getStartPosition());
		
		List<?> catchList = trystat.catchClauses();
		CatchClause cc = null;
		
		for (int i = 0, size = catchList.size(); i < size; i++) {
			idxCatch++;
			
			cc = (CatchClause) catchList.get(i);
			
			// �B�z�U��Catch
			SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
					.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			//logger.debug("\t#####===>CatchClause= "+cc.toString() );
			
			if (svd == null || cc == null) {
				continue;
			}
			
			//���oCatch����SuppressSmell Annotation
			List<?> modifyList = svd.modifiers();
			for (int j = 0; j < modifyList.size(); j++) {
				if (modifyList.get(j) instanceof Annotation) {
					Annotation annotation = (Annotation) modifyList.get(j);
					IAnnotationBinding iab  = annotation.resolveAnnotationBinding();
					//�P�_Annotation Type�O�_��SuppressSmell
					if (iab.getAnnotationType().getQualifiedName().equals(SuppressSmell.class.getName())) {
						IMemberValuePairBinding[] mvpb = iab.getAllMemberValuePairs();

						SSMessage ssmsg = new SSMessage(cc.getStartPosition(),
								this.getLineNumber(cc.getStartPosition()), i);
						//�YAnnotation���e��String
						if (mvpb[0].getValue() instanceof String) {
							ssmsg.addSmellList((String) mvpb[0].getValue());
						//�YAnnotation���e��Array
						} else if (mvpb[0].getValue() instanceof Object[]) {
							Object[] values = (Object[]) mvpb[0].getValue();
							for (Object obj : values)
								if (obj instanceof String)
									ssmsg.addSmellList((String) obj);
						}
						suppressList.add(ssmsg);
					}
				}
			}

			RLMessage rlmsg = new RLMessage(-1, svd.resolveBinding().getType(), cc.toString(), cc
					.getStartPosition(), this.getLineNumber(cc.getStartPosition()));
			// addRL(rlmsg, idxCatch);
			String key = this.parentId + idxTry + "." + idxCatch + "-0.0";			
			this.addRL(rlmsg, key);

			// ConsoleLog.debug(i + ") CATCH===>" + idxTry + ":" + idxCatch +
			// "\t\t[BEGIN]" + cc.getBody().getStartPosition());
			// ConsoleLog.debug("CATCH===>" + cc.getBody());

			// this.visitNode(trystat.getBody());
			visitor = new ExceptionAnalyzer(this.root, true, this.createParentId());
			
			cc.getBody().accept(visitor);
			this.mergeRL(visitor.getExceptionList());
			this.mergeCS(visitor.getNestedTryList());
			visitor.clear();

			// ConsoleLog.debug(i + ") CATCH===>" + idxTry + ":" + idxCatch +
			// "\t\t[END]" + cc.getBody().getStartPosition());
		}

		// �B�zFinally Block
		Block finallyBlock = trystat.getFinally();
		
		if (finallyBlock != null) {
			//logger.debug("\t#####===> FinallyBlock  " );			
			idxCatch++;
			visitor = new ExceptionAnalyzer(this.root, true, this.createParentId());
			finallyBlock.accept(visitor);
			this.mergeRL(visitor.getExceptionList());
			this.mergeCS(visitor.getNestedTryList());
			visitor.clear();
		}

		idxCatch = 0;
		idxTry = 0;

		// ConsoleLog.debug("[TRY_STATEMENT][END]<<<<<<<<<<<<<<<<<<<<<<<<<");

	}

	private void findExceptionTypes(ASTNode node, ITypeBinding[] tbary) {
		for (int i = 0, size = tbary.length; i < size; i++) {
			RLMessage rlmsg = new RLMessage(0, tbary[i], node.toString(), node.getStartPosition(), this
					.getLineNumber(node.getStartPosition()));
			addRL(rlmsg, this.idxCatch);
			//logger.debug("\t#####===> findExceptionTypes="+node.toString() );
		}
	}

	private boolean findAnnotation(ASTNode node, IAnnotationBinding[] annotationBinding) {
		boolean hasRLAnnoation = false;

		// logger.debug("=========>>>" + node.toString());
		// logger.debug("=========>>>IAnnotationBinding[] size=" +
		// annotationBinding.length);

		// ���oAnotatoin ���T��
		for (int i = 0, size = annotationBinding.length; i < size; i++) {

			// logger.debug(i + " >>>" +
			// annotationBinding[i].getAnnotationType().getQualifiedName() + ":"
			// + Robustness.class.getCanonicalName());

			ITypeBinding typeBinding = annotationBinding[i].getAnnotationType();

			if (typeBinding.getQualifiedName().equals(Robustness.class.getCanonicalName())) {

				IMemberValuePairBinding[] mvpb = annotationBinding[i].getAllMemberValuePairs();
				// IMemberValuePairBinding[]
				// mvpb=annotationBinding[i].getDeclaredMemberValuePairs();

				for (int x = 0, xsize = mvpb.length; x < xsize; x++) {
					if (mvpb[x].getValue() instanceof Object[]) {

						Object[] values = (Object[]) mvpb[x].getValue();
						for (int y = 0, ysize = values.length; y < ysize; y++) {
							IAnnotationBinding binding = (IAnnotationBinding) values[y];

							// �B�zRL
							IMemberValuePairBinding[] rlMvpb = binding.getAllMemberValuePairs();
							if (rlMvpb.length == 2) {

								int level = ((Integer) rlMvpb[0].getValue()).intValue();

								RLMessage rlmsg = new RLMessage(level, ((ITypeBinding) rlMvpb[1].getValue()),
										node.toString(), node.getStartPosition(), this.getLineNumber(node
												.getStartPosition()));
								addRL(rlmsg, this.idxCatch);

								hasRLAnnoation = true;

							}
						}
					}
				}
			}
		}
		return hasRLAnnoation;
	}

	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}

	/**
	 * @param level
	 * @param excepName
	 * @param currentCatch
	 */
	@SuppressWarnings("unchecked")
	private void addRL(RLMessage rlmsg, int currentCatch) {

		String key = this.parentId + idxTry + "." + currentCatch;

		rlmsg.setKey(key);
		rlmsg.setKeyList((List<String>) new StrTokenizer(key, "-").getTokenList());

		this.exceptionList.add(rlmsg);

		// ConsoleLog.debug("[addRL]===>" + key + "---->>>" + rlmsg);

	}

	@SuppressWarnings("unchecked")
	private void addRL(RLMessage rlmsg, String key) {

		rlmsg.setKey(key);
		rlmsg.setKeyList((List<String>) new StrTokenizer(key, "-").getTokenList());

		this.exceptionList.add(rlmsg);

		// ConsoleLog.debug("[addRL]===>" + key + "---->>>" + rlmsg);

	}
	
	/**
	 * @param childrenRL
	 */
	private void mergeRL(List<RLMessage> childInfo) {
		if (childInfo == null || childInfo.size() == 0) {
			return;
		}

		for (RLMessage msg : childInfo) {
			this.exceptionList.add(msg);
		}
	}
	
	private void mergeCS(List<CSMessage> childInfo){
		if (childInfo == null || childInfo.size() == 0) {
			return;
		}
		
		for(CSMessage msg : childInfo){
			this.nestedTryList.add(msg);
		}
	}
	
	/**
	 * ����Nested Try Block����m
	 */
	public List<CSMessage> getNestedTryList(){
		return nestedTryList;
	}
	
	/**
	 * @return
	 */
	public ASTNode getCurrentMethodNode() {
		return currentMethodNode;
	}

	public ASTNode getCurrentRLAnnotationNode() {
		return currentRLAnnotationNode;
	}
	

}
