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
	private int sourceStart;

	private int sourceEnd;

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
	}

	protected ExceptionAnalyzer(CompilationUnit root, boolean currentMethodFound, String parentId) {
		super(true);
		this.currentMethodFound = currentMethodFound;
		this.parentId = parentId;
		this.root = root;
		idxTry = 0;
		idxCatch = 0;
		exceptionList = new ArrayList<RLMessage>();
	}

	/**
	 * get annotation information of method
	 * @param node
	 */
	private void getMethodAnnotation(ASTNode node) {
		MethodDeclaration method = (MethodDeclaration) node;
		
		
		IAnnotationBinding[] annoBinding = method.resolveBinding().getAnnotations();
		
		for (int i = 0, size = annoBinding.length; i < size; i++) {
			if (annoBinding[i].getAnnotationType().getQualifiedName().equals(Robustness.class.getName())) {
				IMemberValuePairBinding[] mvpb = annoBinding[i].getAllMemberValuePairs();

				for (int x = 0, xsize = mvpb.length; x < xsize; x++) {
					if (mvpb[x].getValue() instanceof Object[]) {

						Object[] values = (Object[]) mvpb[x].getValue();
						for (int y = 0, ysize = values.length; y < ysize; y++) {
							IAnnotationBinding binding = (IAnnotationBinding) values[y];
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
	 * get exception type name which will be throw from method 
	 * @param node	MethodDeclaration
	 */
	private void getMethodThrowsList(ASTNode node) {
		MethodDeclaration method = (MethodDeclaration) node;
		List<Name> throwsList = method.thrownExceptions();

		for (Name name : throwsList) {
			
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

		try {
			switch (node.getNodeType()) {
				case ASTNode.METHOD_DECLARATION:
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
					if (currentMethodFound) {
						return false;
					}
					return true;
				case ASTNode.THROW_STATEMENT:
					if (currentMethodFound) {
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
							this.findExceptionTypes(node, cic.resolveConstructorBinding().getExceptionTypes());
						}
					}
					return true;
				case ASTNode.CONSTRUCTOR_INVOCATION:
					if (currentMethodFound) {
						ConstructorInvocation ci = (ConstructorInvocation) node;
						if (!this.findAnnotation(node, ci.resolveConstructorBinding().getAnnotations())) {
							this.findExceptionTypes(node, ci.resolveConstructorBinding().getExceptionTypes());
						}
					}
					return true;
				case ASTNode.METHOD_INVOCATION:
					if (currentMethodFound) {
						MethodInvocation mi = (MethodInvocation) node;
						/* 
						 * if code is not obey the rule of IDE, it will cause binding fail of MethdoInvocation.
						 * when binding is fail, it will return null. 
						 * ex. announce a closable instance in the try block
						 */
						
						if (mi.resolveMethodBinding() == null)
							return false;	
						if (!this.findAnnotation(node, mi.resolveMethodBinding().getAnnotations())) {
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
	}

	/**
	 * 1. detect exception handling smell of Nested Try Statement，and then use visitor to analyze Try, Catch and Finally block.
	 * 2. record exception type of catch clause.
	 * 3. get suppress smell information of catch block 
	 * 
	 * @param node	TryStatement
	 */
	private void processTryStatement(ASTNode node) {
		
		idxTry = (++tryBlock);
		idxCatch = 0;

		TryStatement trystat = (TryStatement) node;		
		
		//if trystat is the first one try statement, we won't take it as code smell
		if(!parentId.equals("")){
			MarkerInfo csmsg = new MarkerInfo(	RLMarkerAttribute.CS_NESTED_TRY_STATEMENT, null,											
												trystat.toString(), trystat.getStartPosition(),
												getLineNumber(trystat.getStartPosition()), trystat.toString());
		}
		
		// access Try Block
		ExceptionAnalyzer visitor = new ExceptionAnalyzer(root, true, createParentId());
		trystat.getBody().accept(visitor);
		
		mergeRL(visitor.getExceptionList());
		visitor.clear();

		List<?> catchList = trystat.catchClauses();
		CatchClause cc = null;
		
		for (int i = 0, size = catchList.size(); i < size; i++) {
			idxCatch++;
			
			cc = (CatchClause) catchList.get(i);
			
			// access catch block
			SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			
			if (svd == null || cc == null) {
				continue;
			}
			
			RLMessage rlmsg = new RLMessage(-1, svd.resolveBinding().getType(), cc.toString(),
											cc.getStartPosition(), this.getLineNumber(cc.getStartPosition()));
			String key = parentId + idxTry + "." + idxCatch + "-0.0";			
			addRL(rlmsg, key);

			visitor = new ExceptionAnalyzer(root, true, createParentId());
			
			cc.getBody().accept(visitor);
			mergeRL(visitor.getExceptionList());
			visitor.clear();

		}

		// access finally block
		Block finallyBlock = trystat.getFinally();
		
		if (finallyBlock != null) {
			idxCatch++;
			visitor = new ExceptionAnalyzer(root, true, createParentId());
			finallyBlock.accept(visitor);
			mergeRL(visitor.getExceptionList());
			visitor.clear();
		}

		idxCatch = 0;
		idxTry = 0;
	}

	private void findExceptionTypes(ASTNode node, ITypeBinding[] tbary) {
		for (int i = 0, size = tbary.length; i < size; i++) {
			RLMessage rlmsg = new RLMessage(0, tbary[i], node.toString(), node.getStartPosition(),
											getLineNumber(node.getStartPosition()));
			addRL(rlmsg, idxCatch);
		}
	}

	private boolean findAnnotation(ASTNode node, IAnnotationBinding[] annotationBinding) {
		boolean hasRLAnnoation = false;

		// get annotation message
		for (int i = 0, size = annotationBinding.length; i < size; i++) {

			ITypeBinding typeBinding = annotationBinding[i].getAnnotationType();

			if (typeBinding.getQualifiedName().equals(Robustness.class.getCanonicalName())) {

				IMemberValuePairBinding[] mvpb = annotationBinding[i].getAllMemberValuePairs();

				for (int x = 0, xsize = mvpb.length; x < xsize; x++) {
					if (mvpb[x].getValue() instanceof Object[]) {

						Object[] values = (Object[]) mvpb[x].getValue();
						for (int y = 0, ysize = values.length; y < ysize; y++) {
							IAnnotationBinding binding = (IAnnotationBinding) values[y];

							// access robustness level
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
	}

	private void addRL(RLMessage rlmsg, String key) {
		rlmsg.setKey(key);
		rlmsg.setKeyList((List<String>) new StrTokenizer(key, "-").getTokenList());
		exceptionList.add(rlmsg);
	}
	
	private void mergeRL(List<RLMessage> childInfo) {
		if (childInfo == null || childInfo.size() == 0) {
			return;
		}
		for (RLMessage msg : childInfo) {
			exceptionList.add(msg);
		}
	}
	
	
	public void clear() {
		if(exceptionList != null) {
			exceptionList.clear();
		}
		
		if(methodRLList != null) {
			methodRLList.clear();
		}
		
		currentRLAnnotationNode = null;
		currentMethodNode = null;
	}
	
	public MethodDeclaration getCurrentMethodNode() {
		return currentMethodNode;
	}

	public ASTNode getCurrentRLAnnotationNode() {
		return currentRLAnnotationNode;
	}

	/**
	 * get all exception which is possible happening and catch clause information from current method  
	 * 
	 * @return
	 */
	public List<RLMessage> getExceptionList() {
		return exceptionList;
	}

	/**
	 * get robustness level information from current method
	 * 
	 * @return
	 */
	public List<RLMessage> getMethodRLAnnotationList() {
		return this.methodRLList;
	}
}
