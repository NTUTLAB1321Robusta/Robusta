package ntut.csie.aspect;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

public class AddAspectsMarkerResoluation implements IMarkerResolution,
		IMarkerResolution2 {
	private String label;
	private String description = "Add a aspectJ file to expose influence of bad smell!";
	private QuickFixCore quickFixCore;
	private CompilationUnit compilationUnit;
	private IProject project;
	private IJavaProject javaproject;
	public static final String ASPECTJ_FILE_EXTENSION = ".java";
	public List<String> importObjects = new ArrayList<String>();

	public AddAspectsMarkerResoluation(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return label;
	}

	@Override
	public void run(IMarker marker) {
		MethodDeclaration methodDeclarationWhichHasBadSmell = getMethodDeclarationWhichHasBadSmell(marker);
		// 取得壞味道行數
		int badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);
		List<TryStatement> tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
		// 比對壞味道行數及trystatement所在位置，鎖定該被inject的try statement
		TryStatement tryStatementWillBeInject = getTargetTryStetment(tryStatements, badSmellLineNumber);
		// 取得會被try block接注的例外型別
		List<CatchClause> catchClauses = tryStatementWillBeInject.catchClauses();
		String exceptionType = getExceptionTypeOfCatchClauseWhichHasBadSmell(
				badSmellLineNumber, catchClauses);
		MethodInvocation methodWhichWillThrowSpecificException = getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput(exceptionType, tryStatementWillBeInject);
		// 搜尋try block拿出第一個會丟出例外的node
		
		
		String injectedMethodReturnType = getMethodReturnType(methodWhichWillThrowSpecificException);
		String objectTypeOfInjectedMethod = getTheObjectTypeOfMethodInvocation(methodWhichWillThrowSpecificException);
		// 以這個node為主角，開始inject例外
		
		String badSmellType = "";
		try {
			badSmellType = (String)marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		badSmellType = badSmellType.replace("_", "");
		System.out.println(badSmellType);
		String space = " ";
		String and = "&&";
		
		String className = getClassNameOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
		String nameOfMethodWhichHasBadSmell = methodDeclarationWhichHasBadSmell.getName().toString();
		String returnTypeOfMethodWhichHasBadSmell = getMethodDeclarationReturnType(methodDeclarationWhichHasBadSmell);
		String injectedMethodName = methodWhichWillThrowSpecificException.getName().toString();
		//aspects file content parts
		String aspectJClassTitle = "public aspect " + className +"Aspect {";
		String pointCut = "pointcut find"+injectedMethodName+"("+objectTypeOfInjectedMethod+" object"+") : ";
		String call = "call"+"("+injectedMethodReturnType+space+objectTypeOfInjectedMethod+"."+injectedMethodName+"(..))";
		String target = "target" +"(object)";
		String withInCode = "withincode"+"("+returnTypeOfMethodWhichHasBadSmell+space+
				className+"."+nameOfMethodWhichHasBadSmell+"(..)"+");";
		
		String around = "void around("+objectTypeOfInjectedMethod+" object"+") throws "+exceptionType+" : find"+injectedMethodName+"(object) {";
		String aroundContent = "boolean swich = false;"+
							   "if(swich){"+
							   "throw new "+exceptionType+"();"+
							   "} else {"+
							   "object."+injectedMethodName+"();"+
							   "}"
							 +"}"
							+"}";
		String aspectJFileConetent = aspectJClassTitle+pointCut+call+and+target+and+withInCode+around+aroundContent;
		
		
		System.out.println("aspectJFileConetent" + aspectJFileConetent);
		
		for(String a :importObjects){
			System.out.println(a);
		}
		
		
		project = marker.getResource().getProject();
		javaproject = JavaCore.create(project);
//		try {
//			createDotJavaFile("aspectTest","aspectTest","aspectTest", marker);
//		} catch (CoreException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		
	}

	private void createDotJavaFile(String packageName, String className,
			String content, IMarker marker) throws CoreException {
		String folderName = "aspects";
		IPackageFragmentRoot root = javaproject
				.getPackageFragmentRoot(folderName);
		IPackageFragment ipf;
		if (!root.exists()) {
			ipf = createPackage(packageName);
		} else {
			root.open(null);
			ipf = root.getPackageFragment(packageName);
		}

		if (!className.endsWith(ASPECTJ_FILE_EXTENSION)) {
			className += ASPECTJ_FILE_EXTENSION;
		}

		ipf.createCompilationUnit(className, content, false, null);
	}

	// packageName could be in format like a.b.c.d
	private IPackageFragment createPackage(String packageName)
			throws CoreException {
		IFolder sourceFolder = project.getFolder("123");
		if (!sourceFolder.exists()) {
			createSourceFolder("123");
		}
		IPackageFragmentRoot root = javaproject
				.getPackageFragmentRoot(sourceFolder);
		return root.createPackageFragment(packageName, false, null);
	}

	private void createSourceFolder(String folderName) throws CoreException {
		IFolder sourceFolder = project.getFolder(folderName);
		sourceFolder.create(false, true, null);
		IPackageFragmentRoot root = javaproject
				.getPackageFragmentRoot(sourceFolder);
		// original content in .class file
		IClasspathEntry[] existedEntries = javaproject.getRawClasspath();
		// prepare one more extra space
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		// copy data from existedEntries to extendedEntries
		System.arraycopy(existedEntries, 0, extendedEntries, 0,
				existedEntries.length);
		// add extra folder in one more extra space
		extendedEntries[existedEntries.length] = JavaCore.newSourceEntry(root
				.getPath());
		javaproject.setRawClasspath(extendedEntries, null);
	}

	private String getTheObjectTypeOfMethodInvocation(
			MethodInvocation methodWhichWillThrowSpecificException) {
		FindTheFirstExpressionObjectTypeOfMethodInvocationVisitor theFirstExpressionVisitor = new FindTheFirstExpressionObjectTypeOfMethodInvocationVisitor();
		methodWhichWillThrowSpecificException.accept(theFirstExpressionVisitor);
		String objectPackageName = theFirstExpressionVisitor.getTheFirstExpression().resolveTypeBinding().getPackage().toString().replace("package", "");
		String objectName = theFirstExpressionVisitor.getTheFirstExpression().resolveTypeBinding().getName();
		importObjects.add(objectPackageName+"."+objectName);
		return objectName;
	}

	private String getMethodDeclarationName(MethodDeclaration method) {
		return method.getName().toString();
	}

	private String getMethodDeclarationReturnType(MethodDeclaration method) {
		String objectPackageName = method.getName().resolveTypeBinding().getPackage().toString().replace("package", "");
		String objectName = method.getName().resolveTypeBinding().getName();
		importObjects.add(objectPackageName+"."+objectName);
		return objectName;
	}

	private String getClassNameOfMethodDeclaration(MethodDeclaration method) {
		TypeDeclaration classOfMethod = (TypeDeclaration) method.getParent();
		String className = classOfMethod.resolveBinding().getName().toString();
		importObjects.add(classOfMethod.getName().resolveTypeBinding().getPackage().toString().replace("package", "")+"."+className);
		return className;
	}

	private MethodDeclaration getMethodDeclarationWhichHasBadSmell(
			IMarker marker) {
		String methodIdx = "";
		try {
			methodIdx = (String) marker
					.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		quickFixCore.setJavaFileModifiable(marker.getResource());
		compilationUnit = quickFixCore.getCompilationUnit();
		return QuickFixUtils.getMethodDeclaration(compilationUnit,
				Integer.parseInt(methodIdx));
	}

	private List<TryStatement> getAllTryStatementOfMethodDeclaration(
			MethodDeclaration methodDeclaration) {
		// 取出在method中所有的try block
		FindAllTryStatementVisitor visitor = new FindAllTryStatementVisitor();
		methodDeclaration.accept(visitor);
		return visitor.getTryStatementsList();
	}

	private int getBadSmellLineNumberFromMarker(IMarker marker) {
		int badSmellLineNumber = 0;
		try {
			badSmellLineNumber = (Integer) marker
					.getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return badSmellLineNumber;
	}

	private String getMethodReturnType(
			MethodInvocation methodWhichWillThrowSpecificException) {
		ITypeBinding returnType = methodWhichWillThrowSpecificException
				.resolveTypeBinding();
		return returnType.getName().toString();
	}

	private MethodInvocation getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput(
			String exceptionType, TryStatement tryStatementWillBeInject) {
		Block body = tryStatementWillBeInject.getBody();
		FindStatementWhichWillThrowSpecificExceptionVisitor visitor = new FindStatementWhichWillThrowSpecificExceptionVisitor(
				exceptionType);
		body.accept(visitor);
		return visitor.getMethodInvocationWhichWiThrowException();
	}

	private String getExceptionTypeOfCatchClauseWhichHasBadSmell(
			int badSmellLineNumber, List<CatchClause> catchClauses) {
		for (CatchClause catchBlock : catchClauses) {
			int catchClauseLineNumber = compilationUnit
					.getLineNumber(catchBlock.getStartPosition());
			if (badSmellLineNumber == catchClauseLineNumber) {
				return catchBlock.getException().getType().toString();
			}
		}
		return null;
	}

	public void setCompilationUnitForTesting(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}

	private TryStatement getTargetTryStetment(List<TryStatement> tryStatements,
			int badSmellLineNumber) {
		TryStatement candidate = null;
		for (TryStatement tryStatement : tryStatements) {
			int lineNumberOfTryStatement = compilationUnit
					.getLineNumber(tryStatement.getStartPosition());
			if (lineNumberOfTryStatement < badSmellLineNumber) {
				candidate = tryStatement;
			} else {
				break;
			}
		}
		return candidate;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_FIX);
	}

}
