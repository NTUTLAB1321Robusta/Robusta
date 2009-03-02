package ntut.csie.rleht.common;

import ntut.csie.rleht.caller.CallersLabelProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ASTHandler {
	private static Logger logger = LoggerFactory.getLogger(CallersLabelProvider.class);
	private boolean statementsRecovery = false;

	@SuppressWarnings("deprecation")
	public CompilationUnit createAST(IOpenable input, int offset) throws JavaModelException, CoreException {
		long startTime = 0;
		long endTime = 0;
		CompilationUnit root = null;

		if ((input instanceof ICompilationUnit || input instanceof IClassFile)) {

			final IProblemRequestor requestor = this.createDefaultProblemRequestor();

			WorkingCopyOwner owner = new WorkingCopyOwner() {
				 public IProblemRequestor getProblemRequestor(ICompilationUnit
				 unit) {
				 return requestor;
				 }
			};

			ICompilationUnit wc;
			if (input instanceof ICompilationUnit) {

				wc = ((ICompilationUnit) input).getWorkingCopy(owner, null);

				ConsoleLog.debug("[ASTHandler][createAST]==>input is java files  ==>"+wc.exists() );
			} else {
				IClassFile classfile=((IClassFile) input);
				if(classfile.getSource()==null){
					ConsoleLog.debug("[ASTHandler][createAST]==> Java source file is not exist." );
					return null;
				}
								
				wc = classfile.becomeWorkingCopy(requestor, owner, null);
				
				ConsoleLog.debug("[ASTHandler][createAST]==>input is class files " );
			}

			try {

				startTime = System.currentTimeMillis();
				root = wc.reconcile(AST.JLS3, true, statementsRecovery, null, null);

				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				
				if (input instanceof ICompilationUnit) {
					parser.setSource((ICompilationUnit) input);
				} else {
					parser.setSource((IClassFile) input);
				}
				
				parser.setResolveBindings(true);
				
				root = (CompilationUnit) parser.createAST(null);

				endTime = System.currentTimeMillis();
			} catch (Exception ex) {
				logger.error("[createAST] EXCEPTION ", ex);
			} finally {
				wc.discardWorkingCopy();
			}

		} else {
			ConsoleLog.debug("[ASTHandler][createAST]==>ASTParser.newParser");

			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setResolveBindings(true);
			if (input instanceof ICompilationUnit) {
				parser.setSource((ICompilationUnit) input);
			} else {
				parser.setSource((IClassFile) input);
			}
			parser.setStatementsRecovery(statementsRecovery);

			startTime = System.currentTimeMillis();
			root = (CompilationUnit) parser.createAST(null);
			endTime = System.currentTimeMillis();
		}
		if (root != null) {

			ConsoleLog.debug("[ASTHandler][createAST]" + ((IJavaElement) input).getElementName() + "--->花費：" + (endTime - startTime) + " ms");
		}
		return root;
	}

	private IProblemRequestor createDefaultProblemRequestor() {

		IProblemRequestor problemRequestor = new IProblemRequestor() {
			public void acceptProblem(IProblem problem) {
			}

			public void beginReporting() {
			}

			public void endReporting() {
			}

			public boolean isActive() {
				return true;
			}
		};
		return problemRequestor;
	}
	
	

	/**
	 * 判斷是否為Checked Exception
	 * 
	 * @param typeBinding
	 * @return Y:是Checked Exception N:否
	 */
	public static boolean isCheckedException(ITypeBinding typeBinding) {
		try {
			return !(isInstance(typeBinding, RuntimeException.class.getName()) || isInstance(typeBinding, Error.class.getName()));
		} catch (Exception ex) {
			logger.error("[isCheckedException] EXCEPTION ", ex);
			return false;
		}
	}

	/**
	 * 是否為指定類型的實例
	 * 
	 * @param typeBinding
	 * @param typeName
	 *            指定之類型名稱(完整名稱)
	 * @return Y:是 N:否
	 */
	public static boolean isInstance(ITypeBinding typeBinding, String typeName) {
		
		if (typeBinding == null || typeName == null) {
			return false;
		}

		String qname = typeBinding.getQualifiedName();
		if (qname.equals(typeName)) {
			return true;
		}

		// 判斷父類別
		ITypeBinding superClass = typeBinding.getSuperclass();
		if (superClass != null) {
			if (superClass.getQualifiedName().equals(typeName)) {
				return true;
			}
		}

		// 判斷介面
		ITypeBinding[] interfaceType = typeBinding.getInterfaces();
		if (interfaceType != null && interfaceType.length > 0) {
			for (int i = 0, size = interfaceType.length; i < size; i++) {
				if (interfaceType[i].getQualifiedName().equals(typeName)) {
					return true;
				}
			}
		}

		return false;
	}
}
