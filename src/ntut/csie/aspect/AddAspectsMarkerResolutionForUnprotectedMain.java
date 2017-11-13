package ntut.csie.aspect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.csdet.data.MarkerInfo;
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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

public class AddAspectsMarkerResolutionForUnprotectedMain implements
		IMarkerResolution, IMarkerResolution2 {
	private String label;
	private String description = "Add a aspectJ file to expose influence of bad smell!";
	private QuickFixCore quickFixCore;
	private CompilationUnit compilationUnit;
	private IProject project;
	private IJavaProject javaproject;
	private List<String> importObjects = new ArrayList<String>();
	private IMarker marker;
	private String packageFilePath;
	private String projectPath;
	private MethodDeclaration methodDeclarationWhichHasBadSmell;

	public AddAspectsMarkerResolutionForUnprotectedMain(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return label;
	}

	public void setMarker(IMarker setMarker) {
		marker = setMarker;
	}

	@Override
	public void run(IMarker markerInRun) {
		marker = markerInRun;

		BadSmellTypeConfig config = new BadSmellTypeConfig(markerInRun);

		methodDeclarationWhichHasBadSmell = getMethodDeclarationWhichHasBadSmell(marker);
		generateAspectJFile(config);
		generateUtFileForAspectJ(config);

		refreshProject();
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

	private void generateUtFileForAspectJ(BadSmellTypeConfig config) {
		// create a package
		String packageChain = "ntut.csie.test." + config.getBadSmellType();
		createPackage(packageChain);

		// find method name
		// 命名需要修正

//		List<String> allMethodListInMain = config
//				.getAllMethodInvocationInMain();

		// create a test file
		String testFilePath = projectPath + "\\" + packageFilePath
				+ "\\ntut\\csie\\test" + "\\" + config.getBadSmellType() + "\\"
				+ "test" + makeFirstCharacterUpperCase(config.getClassName())
				+ "MethodUseAspetctJ.java";
		String testFileContent = buildTestFile(config, packageChain,
				testFilePath);

		WriteFile(testFileContent, testFilePath);
		refreshPackageExplorer(testFilePath);

	}

	public String buildTestFile(BadSmellTypeConfig config, String packageName,
			String testFilePath) {

		String testFileContent = "";
		String beforeContent = "package " + packageName + ";\n\n"
				+ "import org.junit.Test;\nimport ntut.csie.aspect."
				+ config.getBadSmellType() + ".AspectJSwitch;\n"
				+ "import org.junit.Assert;\n";
		String generateTestFile = "\r\n"
				+ "public class test"
				+ makeFirstCharacterUpperCase(config.getClassName())
				+ "MethodUseAspetctJ {"
				+ "\r\n\n\tprivate AspectJSwitch repo = AspectJSwitch.getInstance();\n";
		String appendNewTestCase = "";

		// Specific exception import
		String imports = "";
		for (String importObj : config.getImportObjects())
			imports = imports + "import " + importObj.trim() + ";\r\n";
		beforeContent += imports;
		String testMethodName = "";
		File filePathUnitTest = new File(testFilePath);
		String result = "";
		if (filePathUnitTest.exists()) {
			try {
				FileReader fr = new FileReader(filePathUnitTest);
				BufferedReader br = new BufferedReader(fr);
				String temp;
				while ((temp = br.readLine()) != null) {
					if (temp.indexOf("public class") > -1) {
						result = result + imports + "\r\n"; // 一次加入所有的import
					} else if (imports.indexOf(temp) > -1) // 重複的不加進去result
						continue;
					result = result + temp + "\r\n";
				}

				result = result.substring(0, result.lastIndexOf('}'));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (String testOneMethod : config.getAllMethodInvocationInMain()) {
			if (result.indexOf(testOneMethod) < 0) {
				if(!appendNewTestCase.contains(makeFirstCharacterUpperCase(testOneMethod)+"ThrowExceptionInMain()")){
					String generateOneTestCase = "\n\t@Test"
							+ "\n\t"
							+ "public void test"
							+ makeFirstCharacterUpperCase(testOneMethod)
							+ "ThrowExceptionInMain() {\n\t\trepo.initResponse();"
							+ "\n\t\trepo.addResponse(\""
							+ testOneMethod
							+ "/f("
							+ config.getExceptionType()
							+ ")\");"
							+ "\n\t\trepo.toBeforeFirstResponse();"
							+ "\n\t\ttry{\n\t\t\tString[] args={};\n\t\t\t"
							+ config.getClassName()
							+ ".main(args);\n\t\t" 
							+"}catch (Exception e) {\n\t\t\tAssert.fail(\"It is a bad smell for "
							+ config.getBadSmellType()
							+ ".\");"
							+ "\n\t\t}" + "\n\t}";
					appendNewTestCase = appendNewTestCase + generateOneTestCase;
					testMethodName = "test"
							+ makeFirstCharacterUpperCase(testOneMethod)
							+ "ThrowExceptionInMain";
				}
			}
		}
		if (!filePathUnitTest.exists())
			testFileContent = beforeContent + generateTestFile
					+ appendNewTestCase + "\n}";
		else
			testFileContent = result + appendNewTestCase + "\n}";

		return testFileContent;
	}
	
	private String makeFirstCharacterUpperCase(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	private void generateAspectJFile(BadSmellTypeConfig config) {

		String nameOfMethodWhichHasBadSmell = config
				.getMethodDeclarationWhichHasBadSmell().getName().toString();
		String packageChain = "ntut.csie.aspect." + config.getBadSmellType();
		createPackage(packageChain);

		projectPath = ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toOSString();

		String filePathAspectJFile = projectPath + "\\" + packageFilePath
				+ "\\ntut\\csie\\aspect" + "\\" + config.getBadSmellType()
				+ "\\" + config.getClassName() +"AspectException.aj";
		String filePathAspectJSwitch = projectPath + "\\" + packageFilePath
				+ "\\ntut\\csie\\aspect" + "\\" + config.getBadSmellType()
				+ "\\" + "AspectJSwitch.java";

		String aspectJFileContent = config.buildUpAspectsFile(packageChain,
				filePathAspectJFile);
		String aspectJSwitchFileContent = buildUpAspectJSwitch(packageChain);

		WriteFile(aspectJFileContent, filePathAspectJFile);
		WriteFile(aspectJSwitchFileContent, filePathAspectJSwitch);

		refreshPackageExplorer(filePathAspectJFile);
		refreshPackageExplorer(filePathAspectJSwitch);

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

	public String buildUpAspectJSwitch(String packageChain) {
		String importContent = "import java.util.ArrayList;" + "\r\n"
				+ "import java.util.Iterator;" + "\r\n"
				+ "import java.util.List;";

		String AspectJSwitchContentClassTitle = "\r\n"
				+ "public class AspectJSwitch {";
		String initialization = "private static AspectJSwitch instance;"
				+ "\r\n" + "\t"
				+ "private List<String> opres = new ArrayList<String>();"
				+ "\r\n" + "\t" + "public Iterator<String> iterator = null;";
		String constructor = "\r\n\r\n" + "\t" + "private AspectJSwitch() {"
				+ "\r\n" + "\t" + "}";
		String getInstanceContent = "\r\n\r\n" + "\t"
				+ "public static AspectJSwitch getInstance() {" + "\r\n"
				+ "\t\t" + "if (instance == null)" + "\r\n" + "\t\t\t"
				+ "instance = new AspectJSwitch();" + "\r\n" + "\t\t"
				+ "return instance;" + "\r\n" + "\t" + "}";
		String initResponseContent = "\r\n\r\n" + "\t"
				+ "public void initResponse() {" + "\r\n" + "\t\t"
				+ "opres.clear();" + "\r\n" + "\t" + "}";
		String addResponseContent = "\r\n\r\n" + "\t"
				+ "public void addResponse(String opRes) {" + "\r\n" + "\t\t"
				+ "opres.add(opRes);" + "\r\n" + "\t" + "}";
		String toBeforeFirstResponseContent = "\r\n\r\n" + "\t"
				+ "public void toBeforeFirstResponse() {" + "\r\n" + "\t\t"
				+ "iterator = opres.iterator();" + "\r\n" + "\t" + "}";
		String nextActionContent = "\r\n\r\n" + "\t"
				+ "public synchronized String nextAction(String op) {" + "\r\n"
				+ "\t\t" + "String ret = \"s\";" + "\r\n" + "\t\t"
				+ "for (String action : opres)" + "\r\n" + "\t\t\t"
				+ "if (action.startsWith(op + \"/\")) {" + "\r\n" + "\t\t\t\t"
				+ "String[] parts = action.split(\"/\");" + "\r\n" + "\t\t\t\t"
				+ "ret = parts[1];" + "\r\n" + "\t\t\t\t"
				+ "opres.remove(action);" + "\r\n" + "\t\t\t\t" + "break;"
				+ "\r\n" + "\t\t\t" + "}" + "\r\n" + "\t\t" + "return ret;"
				+ "\r\n" + "\t" + "}";
		String aspectJSwitchEnd = "\r\n\r\n" + "}";

		String AspectJSwitchContent = "package " + packageChain + ";"
				+ "\r\n\r\n" + importContent + "\r\n\r\n"
				+ AspectJSwitchContentClassTitle + "\r\n" + "\t"
				+ initialization + "\r\n" + "\t" + constructor + "\t"
				+ getInstanceContent + "\t" + initResponseContent + "\t"
				+ addResponseContent + "\t" + toBeforeFirstResponseContent
				+ "\t" + nextActionContent + "\t" + aspectJSwitchEnd;

		return AspectJSwitchContent;

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

	// packageName could be in format like a.b.c.d
	public void createPackage(String packageName) {
		try {
			project = marker.getResource().getProject();
			javaproject = JavaCore.create(project);
			IPackageFragmentRoot root = getSourceFolderOfCurrentProject();
			String ss = root.createPackageFragment(packageName, false, null)
					.getPath().toString();
			packageFilePath = root.getPath().makeAbsolute().toOSString();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
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

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_FIX);
	}
}
