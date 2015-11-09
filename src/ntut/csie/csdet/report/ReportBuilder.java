package ntut.csie.csdet.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.BadSmellCollector;
import ntut.csie.analyzer.TryStatementCounterVisitor;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.jcis.builder.core.internal.support.LOCCounter;
import ntut.csie.jcis.builder.core.internal.support.LOCData;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportBuilder {
	private static Logger logger = LoggerFactory.getLogger(ReportBuilder.class);

	private IProject project;
	private ReportModel model;

	private LOCCounter noFormatCounter = new LOCCounter();
	private IProgressMonitor progressMonitor;

	public ReportBuilder(IProject project, IProgressMonitor progressMonitor) {
		this.project = project;
		this.progressMonitor = progressMonitor;
		this.model = new ReportModel();
		model.setProjectName(project.getName());
	}

	public IStatus run() {
		IStatus status = analysisProject(project);
		return status;
	}

	public ReportModel getReportModel() {
		return model;
	}
	
	/**
	 * save all smell information in class
	 * 
	 * @param icu
	 * @param pkPath
	 * @param newPackageModel
	 */
	private void setSmellInfo(ICompilationUnit icu, PackageModel newPackageModel, String pkPath) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit root = (CompilationUnit) parser.createAST(null);

		ClassModel newClassModel = new ClassModel();
		newClassModel.setClassName(icu.getElementName());
		newClassModel.setClassPath(pkPath);

		BadSmellCollector badSmellCollector = new BadSmellCollector(this.project, root);
		badSmellCollector.collectBadSmell();
		
		newClassModel.addSmellList(badSmellCollector.getAllBadSmells());
				
		TryStatementCounterVisitor counterVisitor = new TryStatementCounterVisitor();
		root.accept(counterVisitor);
			model.addTryCounter(counterVisitor.getTryCount());
			model.addCatchCounter(counterVisitor.getCatchCount());
			model.addFinallyCounter(counterVisitor.getFinallyCount());
		
		newPackageModel.addClassModel(newClassModel);
	}

	/**
	 * Analysis the project to add bad smell info to ReportModel instance
	 * 
	 * @param project
	 */
	private IStatus analysisProject(IProject project) {
		// Create the java project from the project for getting the structures
		IJavaProject javaPrj = JavaCore.create(project);
		try {
			List<IPackageFragmentRoot> root = getSourcePaths(javaPrj);
			for (int i = 0; i < root.size(); i++) {
				//Check if the user dont want to detect some root source folders.
				if(!shouldDetectPackageFragmentRoot(root.get(i)))
					continue;
				// get folder's name
				String folderName = root.get(i).getElementName();
				// get all package under IPackageFragmentRoot
				IPackageFragmentRoot fragmentRoot = root.get(i);				
				
				IJavaElement[] packages = fragmentRoot.getChildren();

				for (IJavaElement iJavaElement : packages) {

					if (iJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment iPackageFgt = (IPackageFragment) iJavaElement;

						//get all class under package
						ICompilationUnit[] compilationUnits = iPackageFgt.getCompilationUnits();
						PackageModel newPackageModel = null;
						if (compilationUnits.length != 0) {
							newPackageModel = new PackageModel();
							newPackageModel.setPackageName(iPackageFgt.getElementName());
							newPackageModel.setFolderName(folderName);

							/*
							 * Loop through all compilation unit in the package
							 * to collect bad smell info and add to package
							 * model
							 */
							for (int k = 0; k < compilationUnits.length; k++) {
								// if user canceled the task, we should return immediatly with cancel status
								if(progressMonitor.isCanceled()) {
									return Status.CANCEL_STATUS;
								}
								setSmellInfo(compilationUnits[k], newPackageModel, iPackageFgt.getPath().toString());

								int codeLines = countFileLOC(compilationUnits[k].getPath().toString());
								newPackageModel.addTotalLine(codeLines);
							}

							model.addPackageModel(newPackageModel);
						}
					}
				}
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] EXCEPTION ", e);
		}
		// Task finish successful, return OK status
		return Status.OK_STATUS;
	}

	/**
	 * Check the configuration file to see if we should collect bad smell info
	 * from the package fragment root or not
	 * 
	 * @param root
	 * @return
	 */
	private boolean shouldDetectPackageFragmentRoot(IPackageFragmentRoot root) {
		RobustaSettings robustaSettings = new RobustaSettings(new File(UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(project)), project);
		return robustaSettings.getProjectDetectAttribute(root.getPath().segment(1).toString());
	}

	/**
	 * Get all source path in project only. i.e. exclude all jar, zip...
	 * 
	 * @param project
	 * @return List of package fragment root of source only
	 * @throws JavaModelException
	 */
	public List<IPackageFragmentRoot> getSourcePaths(IJavaProject project) throws JavaModelException {
		List<IPackageFragmentRoot> sourcePaths = new ArrayList<IPackageFragmentRoot>();

		IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();

		for (IPackageFragmentRoot r : roots) {
			//Check the source folders only
			if(r.getKind() == IPackageFragmentRoot.K_SOURCE)
				sourcePaths.add(r);
		}
		return sourcePaths;
	}

	/**
	 * calculate LOC of class file 
	 * 
	 * @param filePath	class的File Path
	 * @return LOC of class
	 */
	private int countFileLOC(String filePath) {
		File file = new File(project.getLocation().toString() + "/.." + filePath);

		if (file.exists()) {
			LOCData noFormatData = null;
			try {
				noFormatData = noFormatCounter.countFileLOC(file);
			} catch (FileNotFoundException e) {
				logger.error("[File Not Found Exception] FileNotFoundException ", e);
			}
			return noFormatData.getTotalLine();
		}
		return 0;
	}
}