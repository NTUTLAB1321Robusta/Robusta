package ntut.csie.aspect;

import ntut.csie.analyzer.careless.CloseInvocationExecutionChecker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ntut.csie.util.MethodInvocationCollectorVisitor;
import ntut.csie.util.PopupDialog;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class BadSmellTypeConfig {
	private QuickFixCore quickFixCore = new QuickFixCore();
	private CompilationUnit compilationUnit;
	private List<String> importObjects = new ArrayList<String>();

	private MethodDeclaration methodDeclarationWhichHasBadSmell;
	private int badSmellLineNumber;
	private List<TryStatement> tryStatements;
	private TryStatement tryStatementWillBeInject;
	private List<CatchClause> catchClauses;
	private String exceptionType;
	private String objectTypeOfInjectedMethod;// import用
	private String badSmellType = "";
	private String className;
	private List<String> allMethodInvocationInMain;
	private List<String> methodThrowInSpecificExceptionList;
	private String methodInFinal;
	private List<String> collectBadSmellMethods;
	private List<String> collectBadSmellExceptionTypes;

	public enum BadSmellType_enum {
		DummyHandler, EmptyCatchBlock, UnprotectedMainProgram, ExceptionThrownFromFinallyBlock, CarelessCleanup
	}

	BadSmellType_enum badSmellType_enum = null;

	public BadSmellTypeConfig(IMarker marker) {

		badSmellType = getBadSmellType(marker);
		System.out.println("badSmellType=" + badSmellType);
		for (BadSmellType_enum tempBadSmellType : BadSmellType_enum.values()) {
			if (tempBadSmellType.name().equals(badSmellType))
				badSmellType_enum = tempBadSmellType;
		}
		methodDeclarationWhichHasBadSmell = getMethodDeclarationWhichHasBadSmell(marker);
		className = getClassNameOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
		badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);
		switch (badSmellType_enum) {

		case DummyHandler:
		case EmptyCatchBlock:
			methodThrowInSpecificExceptionList = new ArrayList<String>();
			tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
			tryStatementWillBeInject = getTargetTryStetment(tryStatements,
					badSmellLineNumber);

			catchClauses = tryStatementWillBeInject.catchClauses();
			exceptionType = getExceptionTypeOfCatchClauseWhichHasBadSmell(
					badSmellLineNumber, catchClauses);

			if (exceptionType == null)
				return;
			List<MethodInvocation> allMethodWhichWillThrowException = getMethodInvocationWhichWillThrowTheSameExceptionAsInput(
					exceptionType, tryStatementWillBeInject);
			for (MethodInvocation objectMethod : allMethodWhichWillThrowException) {
				String specificException = objectMethod.resolveMethodBinding()
						.getExceptionTypes()[0].getName();
				if (specificException.equals(getExceptionType())) {
					if (objectMethod.toString().indexOf(".") > -1) {
						methodThrowInSpecificExceptionList
								.add(objectMethod.toString()
										.substring(
												objectMethod.toString()
														.indexOf(".") + 1,
												objectMethod.toString()
														.indexOf("(")));
					} else {
						methodThrowInSpecificExceptionList.add(objectMethod
								.toString().substring(0,
										objectMethod.toString().indexOf("(")));
					}
				}

			}
			objectTypeOfInjectedMethod = getTheObjectTypeOfMethodInvocation(allMethodWhichWillThrowException
					.get(0));// 需要做這件事才能拿到正確import，但寫檔不需要傳這個參數
			break;

		case UnprotectedMainProgram:

			exceptionType = "RuntimeException";

			List<MethodInvocation> allMethod = getMethodInvocationWhichWillThrowExceptionAsInput();
			allMethodInvocationInMain = new ArrayList<String>();

			tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
			// 找main裡面的所有catch、final裡的method
			List<MethodInvocation> methodInCatchAndFinal = new ArrayList<MethodInvocation>();
			methodInCatchAndFinal = addMethodInCatchAndFinal(tryStatements);

			for (MethodInvocation objectMethod : allMethod) {
				boolean objectMethodInCatchOrFinal = false;
				// 比對getStartPosition
				if (methodInCatchAndFinal != null) {// 先確定這個List不是null
					for (MethodInvocation tmp : methodInCatchAndFinal) {
						if (tmp.getStartPosition() == objectMethod
								.getStartPosition()) {
							objectMethodInCatchOrFinal = true;
							break;
						}
					}
				}
				// allMethodInvocationInMain.add(要產生UT的method)
				if (!objectMethodInCatchOrFinal) {
					String tempMethod = "";
					int position;
					if (objectMethod.toString().contains(".")) {
						position = objectMethod.toString().lastIndexOf('.');
						tempMethod = objectMethod.toString().substring(
								position + 1,
								objectMethod.toString().indexOf('(', position));
					} else {
						tempMethod = objectMethod.toString().substring(0,
								objectMethod.toString().lastIndexOf("("));
					}

					allMethodInvocationInMain.add(tempMethod);
				}

			}
			break;
		case ExceptionThrownFromFinallyBlock:
			tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
			tryStatementWillBeInject = getTargetTryStetment(tryStatements,
					badSmellLineNumber);

			Block finallyBlock = tryStatementWillBeInject.getFinally();
			MethodInvocation methodInvocationInFinal = getMethodInvocationInFinally(
					badSmellLineNumber, finallyBlock);
			if (methodInvocationInFinal.toString().contains(".")) {
				methodInFinal = methodInvocationInFinal.toString()
						.substring(
								methodInvocationInFinal.toString().lastIndexOf(
										'.') + 1,
								methodInvocationInFinal.toString().lastIndexOf(
										'('));

			} else {
				methodInFinal = methodInvocationInFinal.toString().substring(0,
						methodInvocationInFinal.toString().lastIndexOf("("));
			}
			exceptionType = getExceptionTypeWhichWillThrow(methodInvocationInFinal);
			break;
		case CarelessCleanup:
			importObjects.add("java.io.IOException");
			MethodInvocationCollectorVisitor allMethodInvVisitor = new MethodInvocationCollectorVisitor();
			methodDeclarationWhichHasBadSmell.accept(allMethodInvVisitor);
			tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);

			List<MethodInvocation> methodInvocations = allMethodInvVisitor
					.getMethodInvocations();
			MethodInvocation closeMethod = getCloseMethodInvocation(
					methodInvocations, badSmellLineNumber);
			if (closeMethod == null) {
				return;
			}
			collectBadSmellMethods = new ArrayList<String>();
			collectBadSmellExceptionTypes = new ArrayList<String>();
			CloseInvocationExecutionChecker closeChecker = new CloseInvocationExecutionChecker();
			List<ASTNode> methodThrowExBeforeClose = new ArrayList<ASTNode>();
			methodThrowExBeforeClose = closeChecker
					.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(closeMethod);

			// 讓ASTNode轉成MethodInvocation
			MethodInvocationCollectorVisitor invocationVisitor = new MethodInvocationCollectorVisitor();
			for (ASTNode astNodeBeforeClose : methodThrowExBeforeClose) {
				astNodeBeforeClose.accept(invocationVisitor);
			}
			MethodInvocationCollectorVisitor invocationVisitorForTryStmts = new MethodInvocationCollectorVisitor();

			// 收集Try裡的第一個MethodInvocation
			for (TryStatement tmpTry : tryStatements) {
				tmpTry.accept(invocationVisitorForTryStmts);
				invocationVisitorForTryStmts.resetFirstInv();
			}
			for (MethodInvocation eachMethodBeforeClose : invocationVisitor
					.getMethodInvocations()) {
				boolean objectMethodInTryStmt = false;

				// 比對method是不是在tryStatement裡，如果是就把objectMethodInTryStmt=true
				if (tryStatements != null) {
					objectMethodInTryStmt = checkMethodHasSameScope(
							eachMethodBeforeClose,
							invocationVisitorForTryStmts.getMethodInvocations());
				}

				// 把不在tryStatement的method加進collectBadSmellMethods
				if (!objectMethodInTryStmt) {
					collectBadSmellMethods
							.add(getObjMethodName(eachMethodBeforeClose));
					collectBadSmellExceptionTypes
							.add(getExceptionTypeWhichWillThrow(eachMethodBeforeClose));
				}
			}

			// 把tryStatement裡的第一個method加進collectBadSmellMethods
			for (MethodInvocation firstMethodInTry : invocationVisitorForTryStmts
					.getFirstInvocations()) {

				collectBadSmellMethods.add(getObjMethodName(firstMethodInTry));
				collectBadSmellExceptionTypes
						.add(getExceptionTypeWhichWillThrow(firstMethodInTry));
			}

			break;
		}
	}

	public String buildUpAspectsFile(String packageChain,
			String filePathAspectJFile) {
		String tempAspectContent = "";
		String Newimports = "";
		String result = "";
		File file = new File(filePathAspectJFile);
		System.out.println(badSmellType_enum);
		switch (badSmellType_enum) {
		case DummyHandler:
		case EmptyCatchBlock:
		case ExceptionThrownFromFinallyBlock:
		case UnprotectedMainProgram:

			for (String importObj : getImportObjects()) {
				Newimports = Newimports + "import " + importObj.trim()
						+ ";\r\n";
			}

			result = addNewImports(Newimports, filePathAspectJFile, file);
			String exception = getExceptionType();
			if (!tempAspectContent.contains(exception)) {

				String AJInsertPositionContent = decideWhereToInsertAJ(
						badSmellType, exception);

				String beforeContent = "\t"
						+ "String name = thisJoinPoint.getSignature().getName();"
						+ "\r\n"
						+ "\t"
						+ "if (thisJoinPoint.getKind().equals(\"constructor-call\"))"
						+ "\r\n"
						+ "\t\t"
						+ "name = thisJoinPoint.getSignature().getDeclaringTypeName().substring(thisJoinPoint.getSignature().getDeclaringTypeName().lastIndexOf(\".\") + 1);"
						+ "\r\n"
						+ "\t"
						+ "String resp = AspectJSwitch.getInstance().nextAction(name);"
						+ "\r\n" + "\t" + "if (resp.equals(\"f(" + exception
						+ ")\"))" + "\r\n" + "\t\t" + "throw new " + exception
						+ "();" + "\r\n\r\n" + "\t" + "}";
				String newAspectContent = AJInsertPositionContent + "\r\n\r\n"
						+ beforeContent;
				if (file.exists()
						&& result.replaceAll("\\s", "").indexOf(
								newAspectContent.replaceAll("\\s", "")) < 0) {
					tempAspectContent += newAspectContent + "\r\n" + "\t";
				} else if (!file.exists()) {
					tempAspectContent += newAspectContent + "\r\n" + "\t";
				}
			}

			break;
		case CarelessCleanup:
			String space = " ";
			String and = "&&";
			String aspectJClassTitle = "\r\n" + "public aspect "
					+ getClassName() + "AspectException {";
			tempAspectContent = "";
			Newimports = "";
			result = "";
			for (String importObj : getImportObjects()) {
				Newimports = Newimports + "import " + importObj.trim()
						+ ";\r\n";
			}

			file = new File(filePathAspectJFile);
			result = addNewImports(Newimports, filePathAspectJFile, file);

			String before = "before()" + ": (";
			String call = "call" + "(* *.close(..) throws IOException" + "))";
			String withIn = "within" + "(" + getClassName() + "){";

			String beforeContent = "\t"
					+ "String name = thisJoinPoint.getSignature().getName();"
					+ "\r\n"
					+ "\t"
					+ "if (thisJoinPoint.getKind().equals(\"constructor-call\"))"
					+ "\r\n"
					+ "\t\t"
					+ "name = thisJoinPoint.getSignature().getDeclaringTypeName().substring(thisJoinPoint.getSignature().getDeclaringTypeName().lastIndexOf(\".\") + 1);"
					+ "\r\n"
					+ "\t"
					+ "String resp = AspectJSwitch.getInstance().nextAction(name);"
					+ "\r\n" + "\t"
					+ "if (resp.equals(\"f(RuntimeException)\"))" + "\r\n"
					+ "\t\t"
					+ "throw new RuntimeException(\"erase bad smell\");"
					+ "\r\n\r\n" + "\t" + "}";
			String aspectCloseContent = before + call + space + and + space
					+ withIn + "\r\n\r\n" + beforeContent + "\r\n\t";

			for (String CarelessException : getCollectBadSmellExceptionTypes()) {
				if (!tempAspectContent.contains("f(" + CarelessException + ")")) {

					String AJInsertPositionContent = decideWhereToInsertAJ(
							badSmellType, CarelessException);
					beforeContent = "\t"
							+ "String name = thisJoinPoint.getSignature().getName();"
							+ "\r\n"
							+ "\t"
							+ "if (thisJoinPoint.getKind().equals(\"constructor-call\"))"
							+ "\r\n"
							+ "\t\t"
							+ "name = thisJoinPoint.getSignature().getDeclaringTypeName().substring(thisJoinPoint.getSignature().getDeclaringTypeName().lastIndexOf(\".\") + 1);"
							+ "\r\n"
							+ "\t"
							+ "String resp = AspectJSwitch.getInstance().nextAction(name);"
							+ "\r\n" + "\t" + "if (resp.equals(\"f("
							+ CarelessException + ")\"))" + "\r\n" + "\t\t"
							+ "throw new " + CarelessException + "();"
							+ "\r\n\r\n" + "\t" + "}";
					String newAspectContent = AJInsertPositionContent
							+ "\r\n\r\n" + beforeContent;
					if (file.exists()
							&& result.replaceAll("\\s", "").indexOf(
									newAspectContent.replaceAll("\\s", "")) < 0) {
						tempAspectContent += newAspectContent + "\r\n" + "\t";
					} else if (!file.exists()) {
						if (tempAspectContent
								.contains("RuntimeException(\"erase bad smell\")"))
							tempAspectContent += newAspectContent + "\r\n"
									+ "\t";
						else
							tempAspectContent += aspectCloseContent
									+ newAspectContent + "\r\n" + "\t";
					}
				}
			}
			break;

		}
		return getCompleteAJContent(Newimports, tempAspectContent, result,
				file, packageChain);

	}

	private String getCompleteAJContent(String Newimports,
			String tempAspectContent, String result, File file,
			String packageChain) {
		String beforeEnd = "\r\n" + "}";
		String aspectJClassTitle = "\r\n" + "public aspect " + getClassName()
				+ "AspectException {";
		String aspectJFileConetent = "package " + packageChain + ";"
				+ "\r\n\r\n" + Newimports + aspectJClassTitle + "\r\n" + "\t"
				+ tempAspectContent + beforeEnd;

		if (!file.exists()) {
			return aspectJFileConetent;
		} else {
			String aspectJFileConetentAppendNewException = "";
			aspectJFileConetentAppendNewException = result + "\r\n" + "\t"
					+ tempAspectContent + beforeEnd;
			return aspectJFileConetentAppendNewException;
		}

	}

	private String addNewImports(String Newimports, String filePathAspectJFile,
			File file) {
		String result = "";
		if (file.exists()) {
			try {
				FileReader fr = new FileReader(filePathAspectJFile);
				BufferedReader br = new BufferedReader(fr);
				String temp;
				while ((temp = br.readLine()) != null) {
					if (temp.indexOf("public") > -1)
						result = result + Newimports + "\r\n";
					else if (Newimports.indexOf(temp) > -1)
						continue;
					result = result + temp + "\r\n";
				}
				result = result.substring(0, result.lastIndexOf('}'));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;

	}

	private String decideWhereToInsertAJ(String badSmellType, String exception) {
		String before;
		String call;
		String withIn;
		String space = " ";
		String and = "&&";

		if (badSmellType.equals("UnprotectedMainProgram")) {
			before = "before()  : (";
			call = "call" + "(* *(..) ) || call(*.new(..) ))";
			withIn = "within" + "(" + getClassName()
					+ ") && withincode(* main(..) ){";
		} else {
			before = "before() throws " + exception + ": (";
			call = "call" + "(* *(..) throws " + exception
					+ ") || call(*.new(..) throws " + exception + "))";
			withIn = "within" + "(" + getClassName() + "){";
		}
		return before + call + space + and + space + withIn;

	}

	private boolean checkMethodHasSameScope(MethodInvocation storeMethod,
			List<MethodInvocation> checkMethodCollect) {
		for (MethodInvocation eachCheckMethod : checkMethodCollect) {
			if (storeMethod.getStartPosition() == eachCheckMethod
					.getStartPosition())
				return true;
		}

		return false;
	}

	private String getObjMethodName(MethodInvocation method) {
		String objMethod = "";

		boolean objMethodHasDot = method.toString().contains(".") ? true
				: false;

		if (objMethodHasDot) {
			objMethod = method.toString().substring(
					method.toString().lastIndexOf('.') + 1,
					method.toString().lastIndexOf('('));

		} else
			objMethod = method.toString().substring(0,
					method.toString().lastIndexOf("("));
		return objMethod;
	}

	private MethodInvocation getCloseMethodInvocation(
			List<MethodInvocation> methodInvocations, int badSmellLineNumber) {
		MethodInvocation candidate = null;
		for (MethodInvocation methodInv : methodInvocations) {
			int lineNumberOfCloseInvocation = getStatementLineNumber(methodInv);
			if (lineNumberOfCloseInvocation == badSmellLineNumber) {
				return methodInv;
			}
		}
		return candidate;
	}

	// 回傳一個list包含catch、finally裡的所有method
	private List<MethodInvocation> addMethodInCatchAndFinal(
			List<TryStatement> tryStatements) {
		FindAllMethodInvocationVisitor getAllMethodInvocation = new FindAllMethodInvocationVisitor();
		for (TryStatement ts : tryStatements) {
			// 抓catch
			for (int i = 0; i < ts.catchClauses().size(); i++) {
				((CatchClause) ts.catchClauses().get(i))
						.accept(getAllMethodInvocation);
			}
			// 抓finally
			if (ts.getFinally() != null) {
				ts.getFinally().accept(getAllMethodInvocation);
			}
		}
		return getAllMethodInvocation.getMethodInvocations();
	}

	public String getMethodInFinal() {
		return methodInFinal;
	}

	private String getExceptionTypeWhichWillThrow(MethodInvocation method) {
		String importException = method.resolveMethodBinding()
				.getExceptionTypes()[0].getBinaryName();
		checkIsDuplicate(importException);

		String exceptionTypes = method.resolveMethodBinding()
				.getExceptionTypes()[0].getName();
		return exceptionTypes;
	}

	private MethodInvocation getMethodInvocationInFinally(
			int badSmellLineNumber, Block finallyBlock) {
		FindAllMethodInvocationVisitor getAllMethodInvocation = new FindAllMethodInvocationVisitor();
		finallyBlock.accept(getAllMethodInvocation);
		List<MethodInvocation> methodThrowExceptionList = getAllMethodInvocation
				.getMethodInvocations();

		for (MethodInvocation m : methodThrowExceptionList)
			if (getStatementLineNumber(m) == badSmellLineNumber)
				return m;
		return null;
	}

	public List<String> getAllMethodInvocationInMain() {
		return allMethodInvocationInMain;
	}

	private List<MethodInvocation> getMethodInvocationWhichWillThrowExceptionAsInput() {
		FindAllMethodInvocationVisitor getAllMethodInvocation = new FindAllMethodInvocationVisitor();
		methodDeclarationWhichHasBadSmell.accept(getAllMethodInvocation);
		List<MethodInvocation> methodThrowExceptionList = getAllMethodInvocation
				.getMethodInvocations();
		return methodThrowExceptionList;
	}

	public List<String> getCollectBadSmellMethods() {
		return collectBadSmellMethods;
	}

	public List<String> getCollectBadSmellExceptionTypes() {
		return collectBadSmellExceptionTypes;
	}

	public List<String> getAllMethodThrowInSpecificExceptionList() {
		return methodThrowInSpecificExceptionList;
	}

	public MethodDeclaration getMethodDeclarationWhichHasBadSmell() {
		return methodDeclarationWhichHasBadSmell;
	}

	public int getBadSmellLineNumber() {
		return badSmellLineNumber;
	}

	public List<TryStatement> getTryStatements() {
		return tryStatements;
	}

	public TryStatement getTryStatementWillBeInject() {
		return tryStatementWillBeInject;
	}

	public List<CatchClause> getCatchClauses() {
		return catchClauses;
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public String getBadSmellType() {
		return badSmellType;
	}

	public String getClassName() {
		return className;
	}

	public List<String> getImportObjects() {
		return importObjects;
	}

	private MethodDeclaration getMethodDeclarationWhichHasBadSmell(
			IMarker marker) {
		String methodIdx = "";

		try {
			methodIdx = (String) marker
					.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		quickFixCore.setJavaFileModifiable(marker.getResource());
		compilationUnit = quickFixCore.getCompilationUnit();

		return QuickFixUtils.getMethodDeclaration(compilationUnit,
				Integer.parseInt(methodIdx));
	}

	private int getBadSmellLineNumberFromMarker(IMarker marker) {
		int badSmellLineNumber = 0;
		try {
			badSmellLineNumber = (Integer) marker
					.getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return badSmellLineNumber;
	}

	private List<TryStatement> getAllTryStatementOfMethodDeclaration(
			MethodDeclaration methodDeclaration) {
		// 取出在method中所有的try block
		FindAllTryStatementVisitor visitor = new FindAllTryStatementVisitor();
		methodDeclaration.accept(visitor);
		return visitor.getTryStatementsList();
	}

	private TryStatement getTargetTryStetment(List<TryStatement> tryStatements,
			int badSmellLineNumber) {
		TryStatement candidate = null;
		for (TryStatement tryStatement : tryStatements) {
			int lineNumberOfTryStatement = getStatementLineNumber(tryStatement);
			if (lineNumberOfTryStatement < badSmellLineNumber) {
				candidate = tryStatement;
			} else {
				break;
			}
		}
		return candidate;
	}

	private int getStatementLineNumber(ASTNode node) {
		int lineNumberOfTryStatement = compilationUnit.getLineNumber(node
				.getStartPosition());
		return lineNumberOfTryStatement;
	}

	private String getExceptionTypeOfCatchClauseWhichHasBadSmell(
			int badSmellLineNumber, List<CatchClause> catchClauses) {
		for (CatchClause catchBlock : catchClauses) {
			int catchClauseLineNumber = compilationUnit
					.getLineNumber(catchBlock.getStartPosition());
			if (badSmellLineNumber == catchClauseLineNumber) {
				ITypeBinding exceptionType = catchBlock.getException()
						.getType().resolveBinding();
				String exceptionPackage = exceptionType.getBinaryName();
				if (exceptionPackage.equalsIgnoreCase("java.lang.Exception")) {
					showOneButtonPopUpMenu("Remind you!",
							"It is not allowed to inject super Exception class in AspectJ!");
					return null;
				} else {
					checkIsDuplicate(exceptionPackage);
					return exceptionType.getName().toString();
				}
			}
		}
		return null;
	}

	private void showOneButtonPopUpMenu(final String title, final String msg) {
		PopupDialog.showDialog(title, msg);
	}

	private void checkIsDuplicate(String content) {
		for (String importContent : importObjects) {
			if (importContent.equalsIgnoreCase(content.trim())) {
				return;
			}
		}
		importObjects.add(content.trim());
	}

	private List<MethodInvocation> getMethodInvocationWhichWillThrowTheSameExceptionAsInput(
			String exceptionType, TryStatement tryStatementWillBeInject) {
		Block body = tryStatementWillBeInject.getBody();
		FindAllMethodInvocationVisitor getAllMethodInvocation = new FindAllMethodInvocationVisitor();
		body.accept(getAllMethodInvocation);
		List<MethodInvocation> methodInv = getAllMethodInvocation
				.getMethodInvocations();
		List<MethodInvocation> MethodInvocationWithSpecificException = new ArrayList<MethodInvocation>();
		FindThrowSpecificExceptionStatementVisitor getTheFirstMethodInvocationWithSpecificException = null;
		for (MethodInvocation method : methodInv) {
			getTheFirstMethodInvocationWithSpecificException = new FindThrowSpecificExceptionStatementVisitor(
					exceptionType);
			method.accept(getTheFirstMethodInvocationWithSpecificException);
			if (getTheFirstMethodInvocationWithSpecificException
					.isGetAMethodInvocationWhichWiThrowException()) {
				MethodInvocationWithSpecificException
						.add(getTheFirstMethodInvocationWithSpecificException
								.getMethodInvocationWhichWillThrowException());
			}
		}

		return MethodInvocationWithSpecificException;
	}

	private String getTheObjectTypeOfMethodInvocation(
			MethodInvocation methodWhichWillThrowSpecificException) {
		FindExpressionObjectOfMethodInvocationVisitor theFirstExpressionVisitor = new FindExpressionObjectOfMethodInvocationVisitor();
		methodWhichWillThrowSpecificException.accept(theFirstExpressionVisitor);
		checkIsDuplicate(theFirstExpressionVisitor.getObjectPackageName());
		return theFirstExpressionVisitor.getObjectName();
	}

	private String getClassNameOfMethodDeclaration(MethodDeclaration method) {
		TypeDeclaration classOfMethod = (TypeDeclaration) method.getParent();
		String className = classOfMethod.resolveBinding().getName().toString();
		checkIsDuplicate(classOfMethod.getName().resolveTypeBinding()
				.getPackage().toString().replace("package", "")
				+ "." + className);
		return className;
	}

	private String getBadSmellType(IMarker marker) {
		String badSmellType = "";
		try {
			badSmellType = (String) marker
					.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		badSmellType = badSmellType.replace("_", "");
		return badSmellType;
	}

}
