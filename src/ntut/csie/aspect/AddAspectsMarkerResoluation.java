package ntut.csie.aspect;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;
import ntut.csie.util.PopupDialog;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
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
	private List<String> importObjects = new ArrayList<String>();

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
		int badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);
		List<TryStatement> tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
		TryStatement tryStatementWillBeInject = getTargetTryStetment(
				tryStatements, badSmellLineNumber);
		List<CatchClause> catchClauses = tryStatementWillBeInject.catchClauses();
		String exceptionType = getExceptionTypeOfCatchClauseWhichHasBadSmell(
				badSmellLineNumber, catchClauses);
		MethodInvocation methodWhichWillThrowSpecificException = getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput(
				exceptionType, tryStatementWillBeInject);
		String injectedMethodReturnType = getMethodInvocationReturnType(methodWhichWillThrowSpecificException);
		String objectTypeOfInjectedMethod = getTheObjectTypeOfMethodInvocation(methodWhichWillThrowSpecificException);
		String badSmellType = "";
		try {
			badSmellType = (String) marker
					.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		badSmellType = badSmellType.replace("_", "");

		String className = getClassNameOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
		String nameOfMethodWhichHasBadSmell = methodDeclarationWhichHasBadSmell
				.getName().toString();
		String returnTypeOfMethodWhichHasBadSmell = getMethodDeclarationReturnType(methodDeclarationWhichHasBadSmell);

		String injectedMethodName = methodWhichWillThrowSpecificException
				.getName().toString();
		String packageChain = "ntut.csie.aspect." + badSmellType;
		project = marker.getResource().getProject();
		javaproject = JavaCore.create(project);
		IPackageFragmentRoot root = getSourceFolderOfCurrentProject();
		createPackage(packageChain, root);
		int lineNumberOfMethodWhichWillThrowSpecificException = getStatementLineNumber(methodWhichWillThrowSpecificException);
		String fileContent = buildUpAspectsFile(exceptionType,
				injectedMethodReturnType, objectTypeOfInjectedMethod,
				className, nameOfMethodWhichHasBadSmell,
				returnTypeOfMethodWhichHasBadSmell, injectedMethodName,
				badSmellType, packageChain, lineNumberOfMethodWhichWillThrowSpecificException);
		String projectPath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toOSString();
		String FilePath = root.getPath().makeAbsolute().toOSString();
		String filePath = projectPath + "\\" + FilePath
				+ "\\ntut\\csie\\aspect" + "\\" + badSmellType + "\\"
				+ className + "Aspect"+exceptionType+"In"+makeFirstCharacterUpperCase(nameOfMethodWhichHasBadSmell)+"FunctionAtLine"+lineNumberOfMethodWhichWillThrowSpecificException+".aj";
		WriteFile(fileContent, filePath);
		refreshPackageExplorer(filePath);
		refreshProject();
	}
	
	private String makeFirstCharacterUpperCase(String name){
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	private void refreshPackageExplorer(String fileCreateFile) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath location = Path.fromOSString(fileCreateFile);
		IFile file = workspace.getRoot().getFileForLocation(location);
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void refreshProject() {
		if (project != null) {
			// save project to a final variable so that it can be used in Job,
			// it should be safe for that project should not change over time
			final IProject project2 = project;
			Job job = new Job("Refreshing Project") {
				protected IStatus run(IProgressMonitor monitor) {
					refreshProject(project2);
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.SHORT);
			job.schedule();
		}
	}

	private void showOneButtonPopUpMenu(final String title, final String msg) {
		PopupDialog.showDialog(title, msg);
	}

	private void refreshProject(IProject project) {
		// build project to refresh
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD,
					new NullProgressMonitor());
		} catch (CoreException e) {
			showOneButtonPopUpMenu("Refresh failed",
					"Fail to refresh your project, please do it manually");
		}
	}

	private String buildUpAspectsFile(String exceptionType,
			String injectedMethodReturnType, String objectTypeOfInjectedMethod,
			String className, String nameOfMethodWhichHasBadSmell,
			String returnTypeOfMethodWhichHasBadSmell,
			String injectedMethodName, String badSmellType, String packageChain, int lineNumberOfMethodWhichWillThrowSpecificException) {
		String space = " ";
		String and = "&&";
		String aspectJClassTitle = "\r\n" + "public aspect " + className
				+ "Aspect"+exceptionType+"In"+makeFirstCharacterUpperCase(nameOfMethodWhichHasBadSmell)+"FunctionAtLine"+lineNumberOfMethodWhichWillThrowSpecificException+" {";
		String pointCut = "pointcut find" + makeFirstCharacterUpperCase(injectedMethodName) + "("
				+ objectTypeOfInjectedMethod + " object" + ") : ";
		String call = "call" + "(" + injectedMethodReturnType + space
				+ objectTypeOfInjectedMethod + "." + injectedMethodName
				+ "(..))";
		String target = "target" + "(object)";
		String withInCode = "withincode" + "("
				+ returnTypeOfMethodWhichHasBadSmell + space + className + "."
				+ nameOfMethodWhichHasBadSmell + "(..)" + ");";

		String around = injectedMethodReturnType+" around(" + objectTypeOfInjectedMethod + " object"
				+ ") throws " + exceptionType + " : find" + makeFirstCharacterUpperCase(injectedMethodName)
				+ "(object) {";
		String aroundContent = "\t" + "  boolean swich = false;" + "\r\n"
				+ "\t" + "  if(swich){" + "\r\n" + "\t\t" + "throw new "
				+ exceptionType + "();" + "\r\n" + "\t" + "  } else {" + "\r\n";
		String elseContent = "";
		if(injectedMethodReturnType.equals("void")){
			elseContent =  "\t\t" + "object." + injectedMethodName + "();" + "\r\n";
		}else{
			elseContent =  "\t\t" + " return object." + injectedMethodName + "();" + "\r\n";
		}
		String aroundEnd = "\t" + "  }" + "\r\n" + "\t" + "}" + "\r\n" + "}";

		String imports = "";
		for (String importObj : importObjects) {
			imports = imports + "import" + importObj + ";\r\n";
		}

		String aspectJFileConetent = "package " + packageChain + ";"
				+ "\r\n\r\n" + imports + aspectJClassTitle + "\r\n" + "\t"
				+ pointCut + call + "\r\n" + "\t" + and + target + "\r\n"
				+ "\t" + and + withInCode + "\r\n\r\n" + "\t" + around + "\r\n"
				+ aroundContent + elseContent + aroundEnd;

		return aspectJFileConetent;
	}

	public void WriteFile(String str, String path) {
		BufferedWriter writer = null;
		try {
			File file = new File(path);
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, false), "utf8"));
			writer.write(str);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} finally {
			closeWriter(writer);
		}
	}

	private void closeWriter(BufferedWriter writer) {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			ignore();
		}
	}

	private void ignore() {
	}

	private IPackageFragmentRoot getSourceFolderOfCurrentProject() {
		IPackageFragmentRoot[] roots;
		try {
			roots = javaproject.getAllPackageFragmentRoots();
			for (IPackageFragmentRoot root : roots) {
				if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
					return root;
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return null;
	}

	// packageName could be in format like a.b.c.d
	private void createPackage(String packageName, IPackageFragmentRoot root) {
		try {
			root.createPackageFragment(packageName, false, null);
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	private String getTheObjectTypeOfMethodInvocation(
			MethodInvocation methodWhichWillThrowSpecificException) {
		FindTheFirstExpressionObjectTypeOfMethodInvocationVisitor theFirstExpressionVisitor = new FindTheFirstExpressionObjectTypeOfMethodInvocationVisitor();
		methodWhichWillThrowSpecificException.accept(theFirstExpressionVisitor);
		String objectPackageName = theFirstExpressionVisitor
				.getTheFirstExpression().resolveTypeBinding().getPackage()
				.toString().replace("package", "");
		String objectName = theFirstExpressionVisitor.getTheFirstExpression()
				.resolveTypeBinding().getName();
		importObjects.add(objectPackageName + "." + objectName);
		return objectName;
	}

	private String getMethodDeclarationReturnType(MethodDeclaration method) {
		ITypeBinding type = method.resolveBinding().getReturnType();
		if(!type.isPrimitive()){
			importObjects.add(type.getBinaryName());
		}
		return type.getName().toString();
	}

	private String getMethodInvocationReturnType(MethodInvocation methodWhichWillThrowSpecificException) {
		IMethodBinding returnType = methodWhichWillThrowSpecificException.resolveMethodBinding();
		ITypeBinding type = returnType.getReturnType();
		if(!type.isPrimitive() ){
			importObjects.add(type.getBinaryName());
		} 
		return methodWhichWillThrowSpecificException.resolveTypeBinding().getName().toString();
	}
	
	private String getClassNameOfMethodDeclaration(MethodDeclaration method) {
		TypeDeclaration classOfMethod = (TypeDeclaration) method.getParent();
		String className = classOfMethod.resolveBinding().getName().toString();
		importObjects.add(classOfMethod.getName().resolveTypeBinding()
				.getPackage().toString().replace("package", "")
				+ "." + className);
		return className;
	}
	
	public List<String> getImportObjectsForTesting(){
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
		return QuickFixUtils.getMethodDeclaration(compilationUnit, Integer.parseInt(methodIdx));
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
			throw new RuntimeException();
		}
		return badSmellLineNumber;
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
				String objectPackageName = catchBlock.getException().getType()
						.resolveBinding().getPackage().toString()
						.replace("package", "");
				String objectName = catchBlock.getException().getType()
						.resolveBinding().getName();
				importObjects.add(objectPackageName + "." + objectName);
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
		int lineNumberOfTryStatement = compilationUnit.getLineNumber(node.getStartPosition());
		return lineNumberOfTryStatement;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_FIX);
	}

}
