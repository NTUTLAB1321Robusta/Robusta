package ntut.csie.filemaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Use this to read runtime project both IType and CompilationUnit
 * @author Charles
 *
 */
public class RuntimeEnvironmentProjectReader {

	/**
	 * @see org.eclipse.jdt.core.IType
	 * @param projectName The project name that in your runtime eclipse you want to test.
	 * @param packageName The full qualified package name that in you runtime eclipse you want to test.
	 * @param className The class name that in your runtime eclipse you want to test.
	 * @return IType hint: you can use it to get CompilationUnit
	 * @throws JavaModelException
	 */
	public static IType getType(String projectName, String packageName,
			String className) throws JavaModelException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		IJavaProject javaProject = JavaCore.create(project);
		IType type = javaProject.findType(packageName + "." + className);
		return type;
	}
	
	/**
	 * @see org.eclipse.jdt.core.dom.CompilationUnit
	 * I use AST.JLS3 parser edition, because it's for JDK5 or later.
	 * @param type
	 * @return
	 */
	public static CompilationUnit getCompilationUnit(IType type){
		IJavaElement javaElement = JavaCore.create(type.getResource());
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource((ICompilationUnit)javaElement);
		parser.setResolveBindings(true);
		CompilationUnit unit = (CompilationUnit) parser.createAST(null); 
		return unit;
	}
	
	/**
	 * @see org.eclipse.core.resources.IResource
	 * @param projectName The project name that in your runtime eclipse you want to test.
	 * @param packageName The full qualified package name that in you runtime eclipse you want to test.
	 * @param className The class name that in your runtime eclipse you want to test.
	 * @return IResource
	 * @throws JavaModelException
	 */
	public static IResource getIResource(String projectName, String packageName, String className) throws JavaModelException {
		return RuntimeEnvironmentProjectReader.getType(projectName, packageName, className).getResource();
	}
}
