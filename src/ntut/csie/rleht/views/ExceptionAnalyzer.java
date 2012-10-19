package ntut.csie.rleht.views;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;
import ntut.csie.robusta.agile.exception.Robustness;

import org.apache.commons.lang.text.StrTokenizer;
import org.eclipse.jdt.core.dom.ASTNode;
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


public class ExceptionAnalyzer extends RLBaseVisitor {
	private static Logger logger = LoggerFactory.getLogger(ExceptionAnalyzer.class);
	// 目前源碼選擇之開始位置
	private int sourceStart;

	// 目前源碼選擇之結束位置
	private int sourceEnd;

	// 目前所在之Method節點
	private MethodDeclaration currentMethodNode;

	private int currentMethodStart;

	private int currentMethodEnd;

	private boolean currentMethodFound;

	private int idxTry = 0;

	private int idxCatch = 0;

	private int tryBlock = 0;

	private String parentId = "";

	private List<RLMessage> exceptionList;

	private List<RLMessage> methodRLList;

//	private List<SSMessage> suppressList;
	
	// 紀錄Nested Try Block的位置
//	private List<MarkerInfo> nestedTryList;

	private ASTNode currentRLAnnotationNode;

	private CompilationUnit root;

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
//		suppressList = new ArrayList<SSMessage>();
//		nestedTryList = new ArrayList<MarkerInfo>();
	}

	protected ExceptionAnalyzer(CompilationUnit root, boolean currentMethodFound, String parentId) {
		super(true);
		this.currentMethodFound = currentMethodFound;
		this.parentId = parentId;
		this.root = root;
		idxTry = 0;
		idxCatch = 0;
		exceptionList = new ArrayList<RLMessage>();
//		suppressList = new ArrayList<SSMessage>();
//		nestedTryList = new ArrayList<MarkerInfo>();
	}

	/**
	 * 取得Method上的Annotation資訊
	 * @param node
	 */
	private void getMethodAnnotation(ASTNode node) {
		MethodDeclaration method = (MethodDeclaration) node;
		
		//logger.debug("#####===>method=" + method.getName());
		
		IAnnotationBinding[] annoBinding = method.resolveBinding().getAnnotations();
		
		for (int i = 0, size = annoBinding.length; i < size; i++) {
//			//取得Method上的SuppressSmell資訊
//			if (annoBinding[i].getAnnotationType().getQualifiedName().equals(SuppressSmell.class.getName())) {
//				IMemberValuePairBinding[] mvpb = annoBinding[i].getAllMemberValuePairs();
//
//				SSMessage ssmsg = new SSMessage(node.getStartPosition(),
//												getLineNumber(node.getStartPosition()));
//				//若SuppressSmell內為String
//				if (mvpb[0].getValue() instanceof String) {
//					ssmsg.addSmellList((String) mvpb[0].getValue());
//				//若SuppressSmell內為Array
//				} else if (mvpb[0].getValue() instanceof Object[]) {
//					Object[] values = (Object[]) mvpb[0].getValue();
//					for (Object obj : values) {
//						if (obj instanceof String)
//							ssmsg.addSmellList((String) obj);
//					}
//				}
//				suppressList.add(ssmsg);
//			}		
			//取得Method上的Robustness資訊
			if (annoBinding[i].getAnnotationType().getQualifiedName().equals(Robustness.class.getName())) {
				IMemberValuePairBinding[] mvpb = annoBinding[i].getAllMemberValuePairs();

				for (int x = 0, xsize = mvpb.length; x < xsize; x++) {
					if (mvpb[x].getValue() instanceof Object[]) {

						Object[] values = (Object[]) mvpb[x].getValue();
						for (int y = 0, ysize = values.length; y < ysize; y++) {
							IAnnotationBinding binding = (IAnnotationBinding) values[y];
							// 處理RL
							IMemberValuePairBinding[] rlMvpb = binding.getAllMemberValuePairs();
							if (rlMvpb.length == 2) {

								int level = ((Integer) rlMvpb[0].getValue()).intValue();
								
								RLMessage rlmsg = new RLMessage(level, ((ITypeBinding) rlMvpb[1].getValue()),
																node.toString(), node.getStartPosition(), 
																getLineNumber(node.getStartPosition()));
								rlmsg.setKey("");
								rlmsg.setKeyList(null);
								methodRLList.add(rlmsg);
							}
						}
					}
				}
			}
		}
	}

	private String createParentId() {
		return parentId + idxTry + "." + idxCatch + "-";
	}

	/**
	 * 取得Method所宣告的throws Exception Type Name
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
				if(pos2 == -1)
					pos2 = code.indexOf(";", pos1 + 1);
				code = code.substring(pos1, pos2);
				RLMessage rlmsg = new RLMessage(0, name.resolveTypeBinding(), code, name.getStartPosition(), getLineNumber(name.getStartPosition()));
				rlmsg.setKey("");
				rlmsg.setKeyList(null);
				exceptionList.add(rlmsg);
			} catch (Exception ex) {
				logger.error("[getMethodThrowsList] pos1 = " + pos1 + " pos2 = "+pos2 + " ==> " + code,ex);
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
					// 尋找所選擇文字在那一個Method內
					currentMethodStart = node.getStartPosition();
					currentMethodEnd = currentMethodStart + node.getLength();

					if (currentMethodEnd < sourceStart || sourceEnd < currentMethodStart) {
						currentMethodFound = false;
					}
					
					if (currentMethodStart <= sourceStart && sourceEnd <= currentMethodEnd) {
						currentMethodNode = (MethodDeclaration)node;
						currentMethodFound = true;
						getMethodAnnotation(node);
						getMethodThrowsList(node);
					} else {
						currentMethodFound = false;
					}
					return true;					
				case ASTNode.TRY_STATEMENT:
					// currentMethodFound + ":" + node);
					if (currentMethodFound) {
//						processTryStatement(node);
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
							rlmsg = new RLMessage(	0, ((ClassInstanceCreation) obj).resolveTypeBinding(), ts.toString()
													, ts.getStartPosition(), getLineNumber(ts.getStartPosition()));

						} else if (obj instanceof SimpleName) {
							rlmsg = new RLMessage(	0, ((SimpleName) obj).resolveTypeBinding(), ts.toString(),
													ts.getStartPosition(), getLineNumber(ts.getStartPosition()));
						}

						if (rlmsg != null) {
							addRL(rlmsg, idxCatch);
						}
					}
					return true;
				case ASTNode.CLASS_INSTANCE_CREATION:
					if (currentMethodFound) {
						ClassInstanceCreation cic = (ClassInstanceCreation) node;
						if (!this.findAnnotation(node, cic.resolveConstructorBinding().getAnnotations())) {
							// 取得Method的Throw Exception Type
							this.findExceptionTypes(node, cic.resolveConstructorBinding().getExceptionTypes());
						}
					}
					return true;
				case ASTNode.CONSTRUCTOR_INVOCATION:
					if (currentMethodFound) {
						ConstructorInvocation ci = (ConstructorInvocation) node;
						if (!this.findAnnotation(node, ci.resolveConstructorBinding().getAnnotations())) {
							// 取得Method的Throw Exception Type
							this.findExceptionTypes(node, ci.resolveConstructorBinding().getExceptionTypes());
						}
					}
					return true;
				case ASTNode.METHOD_INVOCATION:
					if (currentMethodFound) {
						MethodInvocation mi = (MethodInvocation) node;
						/* 如果User的code不符合IDE的規定，會造成MethdoInvocation的Binding錯誤，
						 * 導致mi.resolveMethodBinding()為null。
						 * ex: 在try block宣告一個需要close的instance
						 */
						if (mi.resolveMethodBinding() == null)
							return false;	//避免下面的程式碼傳回null pointer exception
						if (!this.findAnnotation(node, mi.resolveMethodBinding().getAnnotations())) {
							// 取得Method的Throw Exception Type
							findExceptionTypes(node, mi.resolveMethodBinding().getExceptionTypes());
						}
					}
					return true;
				default:
					return true;
			}
		} catch (Exception ex) {
			logger.error("[visitNode] EXCEPTION ", ex);
			return false;
		}
		finally{
			//long m2=Runtime.getRuntime().freeMemory();
			//logger.debug("!!!!!===>["+parentId+"][END]  freeMemory=["+ m2 +"] ["+(m2-m1)+"] " );
		}
	}

	/**
	 * 1. 偵測Nested Try Block的EH Smell，對其Try、Catch與Finally再使用Visitor偵測。
	 * 2. 記錄Catch Clause所宣告的Exception Type
	 * 3. 取得Catch內的Suppress Smell的資訊
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
		
		// 假如是第一個Try,那就不是Code Smell,不用加進去
		if(!parentId.equals("")){
			MarkerInfo csmsg = new MarkerInfo(	RLMarkerAttribute.CS_NESTED_TRY_BLOCK, null,											
												trystat.toString(), trystat.getStartPosition(),
												getLineNumber(trystat.getStartPosition()), trystat.toString());
//			nestedTryList.add(csmsg);	
		}
		
		// 處理Try Block
		ExceptionAnalyzer visitor = new ExceptionAnalyzer(root, true, createParentId());
		trystat.getBody().accept(visitor);
		
//		mergeCS(visitor.getNestedTryList());
		mergeRL(visitor.getExceptionList());
		visitor.clear();

		// this.visitNode(trystat.getBody());

//		 ConsoleLog.debug("TRY===>" + idxTry + ":" + idxCatch + "\t[END]"+
//		 trystat.getBody().getStartPosition());
		
		List<?> catchList = trystat.catchClauses();
		CatchClause cc = null;
		
		for (int i = 0, size = catchList.size(); i < size; i++) {
			idxCatch++;
			
			cc = (CatchClause) catchList.get(i);
			
			// 處理各個Catch
			SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			//logger.debug("\t#####===>CatchClause= "+cc.toString() );
			
			if (svd == null || cc == null) {
				continue;
			}
			
			//取得Catch內的SuppressSmell Annotation
//			List<?> modifyList = svd.modifiers();
//			for (int j = 0; j < modifyList.size(); j++) {
//				if (modifyList.get(j) instanceof Annotation) {
//					Annotation annotation = (Annotation) modifyList.get(j);
//					IAnnotationBinding iab  = annotation.resolveAnnotationBinding();
//					//判斷Annotation Type是否為SuppressSmell
//					if (iab.getAnnotationType().getQualifiedName().equals(SuppressSmell.class.getName())) {
//						IMemberValuePairBinding[] mvpb = iab.getAllMemberValuePairs();
//
//						SSMessage ssmsg = new SSMessage(cc.getStartPosition(), getLineNumber(cc.getStartPosition()), i);
//						//若Annotation內容為String
//						if (mvpb[0].getValue() instanceof String) {
//							ssmsg.addSmellList((String) mvpb[0].getValue());
//						//若Annotation內容為Array
//						} else if (mvpb[0].getValue() instanceof Object[]) {
//							Object[] values = (Object[]) mvpb[0].getValue();
//							for (Object obj : values) {
//								if (obj instanceof String)
//									ssmsg.addSmellList((String) obj);
//							}
//						}
//						suppressList.add(ssmsg);
//					}
//				}
//			}

			RLMessage rlmsg = new RLMessage(-1, svd.resolveBinding().getType(), cc.toString(),
											cc.getStartPosition(), this.getLineNumber(cc.getStartPosition()));
			// addRL(rlmsg, idxCatch);
			String key = parentId + idxTry + "." + idxCatch + "-0.0";			
			addRL(rlmsg, key);

			// ConsoleLog.debug(i + ") CATCH===>" + idxTry + ":" + idxCatch +
			// "\t\t[BEGIN]" + cc.getBody().getStartPosition());
			// ConsoleLog.debug("CATCH===>" + cc.getBody());

			// this.visitNode(trystat.getBody());
			visitor = new ExceptionAnalyzer(root, true, createParentId());
			
			cc.getBody().accept(visitor);
			mergeRL(visitor.getExceptionList());
//			mergeCS(visitor.getNestedTryList());
			visitor.clear();

			// ConsoleLog.debug(i + ") CATCH===>" + idxTry + ":" + idxCatch +
			// "\t\t[END]" + cc.getBody().getStartPosition());
		}

		// 處理Finally Block
		Block finallyBlock = trystat.getFinally();
		
		if (finallyBlock != null) {
			//logger.debug("\t#####===> FinallyBlock  " );			
			idxCatch++;
			visitor = new ExceptionAnalyzer(root, true, createParentId());
			finallyBlock.accept(visitor);
			mergeRL(visitor.getExceptionList());
//			mergeCS(visitor.getNestedTryList());
			visitor.clear();
		}

		idxCatch = 0;
		idxTry = 0;

		// ConsoleLog.debug("[TRY_STATEMENT][END]<<<<<<<<<<<<<<<<<<<<<<<<<");

	}

	private void findExceptionTypes(ASTNode node, ITypeBinding[] tbary) {
		for (int i = 0, size = tbary.length; i < size; i++) {
			RLMessage rlmsg = new RLMessage(0, tbary[i], node.toString(), node.getStartPosition(),
											getLineNumber(node.getStartPosition()));
			addRL(rlmsg, idxCatch);
			//logger.debug("\t#####===> findExceptionTypes="+node.toString() );
		}
	}

	private boolean findAnnotation(ASTNode node, IAnnotationBinding[] annotationBinding) {
		boolean hasRLAnnoation = false;

		// logger.debug("=========>>>" + node.toString());
		// logger.debug("=========>>>IAnnotationBinding[] size=" +
		// annotationBinding.length);

		// 取得Anotatoin 的訊息
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

							// 處理RL
							IMemberValuePairBinding[] rlMvpb = binding.getAllMemberValuePairs();
							if (rlMvpb.length == 2) {
								int level = ((Integer) rlMvpb[0].getValue()).intValue();
								RLMessage rlmsg = new RLMessage(level, ((ITypeBinding) rlMvpb[1].getValue()),
																node.toString(), node.getStartPosition(), 
																getLineNumber(node.getStartPosition()));
								addRL(rlmsg, idxCatch);
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
	private void addRL(RLMessage rlmsg, int currentCatch) {
		String key = parentId + idxTry + "." + currentCatch;
		rlmsg.setKey(key);
		rlmsg.setKeyList((List<String>) new StrTokenizer(key, "-").getTokenList());
		this.exceptionList.add(rlmsg);
		// ConsoleLog.debug("[addRL]===>" + key + "---->>>" + rlmsg);
	}

	private void addRL(RLMessage rlmsg, String key) {
		rlmsg.setKey(key);
		rlmsg.setKeyList((List<String>) new StrTokenizer(key, "-").getTokenList());
		exceptionList.add(rlmsg);
		// ConsoleLog.debug("[addRL]===>" + key + "---->>>" + rlmsg);
	}
	
	private void mergeRL(List<RLMessage> childInfo) {
		if (childInfo == null || childInfo.size() == 0) {
			return;
		}
		for (RLMessage msg : childInfo) {
			exceptionList.add(msg);
		}
	}
	
//	private void mergeCS(List<MarkerInfo> childInfo) {
//		if (childInfo == null || childInfo.size() == 0) {
//			return;
//		}
//		for(MarkerInfo msg : childInfo){
//			nestedTryList.add(msg);
//		}
//	}
	
	public void clear() {
		if(exceptionList != null) {
			exceptionList.clear();
//			nestedTryList.clear();
		}
		
		if(methodRLList != null) {
			methodRLList.clear();
		}
		
		currentRLAnnotationNode = null;
		currentMethodNode = null;
	}
	
	/**
	 * 紀錄Nested Try Block的位置
	 */
//	public List<MarkerInfo> getNestedTryList() {
//		return nestedTryList;
//	}
	
	public MethodDeclaration getCurrentMethodNode() {
		return currentMethodNode;
	}

	public ASTNode getCurrentRLAnnotationNode() {
		return currentRLAnnotationNode;
	}

	/**
	 * 取得目前method內所有可能發生的exception及catch資訊
	 * 
	 * @return
	 */
	public List<RLMessage> getExceptionList() {
		return exceptionList;
	}

	/**
	 * 取得目前所在method的RL訊息
	 * 
	 * @return
	 */
	public List<RLMessage> getMethodRLAnnotationList() {
		return this.methodRLList;
	}
	
	/**
	 * 取得SuppressSmell資訊(包含Method上與Catch內)
	 * @return
	 */
//	public List<SSMessage> getSuppressSemllAnnotationList() {
//		return suppressList;
//	}
}
