package ntut.csie.csdet.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.csdet.visitor.MainAnalyzer;
import ntut.csie.jcis.builder.core.internal.support.LOCCounter;
import ntut.csie.jcis.builder.core.internal.support.LOCData;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jdom.Attribute;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportBuilder {
	private static Logger logger = LoggerFactory.getLogger(ReportBuilder.class);

	private IProject project;
	//Report�����
	private ReportModel model;
	//�O�_����������Package
	private Boolean isAllPackage;
	//�x�sfilter����List
	private List<String> filterRuleList = new ArrayList<String>();
	private LOCCounter noFormatCounter = new LOCCounter();

	/**
	 * �إ�Report
	 * @param project
	 * @param model
	 */	
	public ReportBuilder(IProject project,ReportModel model) {
		this.project = project;	
		this.model = model;
	}
	
	public void run() {		
		//���o�M�צW��
		model.setProjectName(project.getName());
		//���o�سy�ɶ�
		model.setBuildTime();
		
		//�NUser���Filter���]�w�s�U��
		getFilterSettings();
		
		//�ѪR�M��
		analysisProject(project);

		//����HTM
		SmellReport createHTM = new SmellReport(model);
		createHTM.build();

		//���͹Ϫ�
		BarChart smellChart = new BarChart(model);
		smellChart.build();
	}

	/**
	 * �NUser���Filter���]�w�s�U��
	 * @return
	 */
	private void getFilterSettings() {
		Element root = JDomUtil.createXMLContent();

		//�p�G�Onull���XML�ɬO��ئn��,�٨S��EHSmellFilterTaq��tag,�������X�h
		if(root.getChild(JDomUtil.EHSmellFilterTaq) != null) {

			// �o�̪�ܤ��e�ϥΪ̤w�g���]�w�Lpreference�F,�h���o���������]�w��
			Element filter = root.getChild(JDomUtil.EHSmellFilterTaq).getChild("filter");
			isAllPackage = Boolean.valueOf(filter.getAttribute("IsAllPackage").getValue());

			//�Y���O����������Project�A�h��Rule�����x�s
			if (!isAllPackage) {
				List<Attribute> filterList = filter.getAttributes();
				
				for (int i=0; i<filterList.size(); i++) {
					//���L���ݩ�Rule���]�w
					if (filterList.get(i).getQualifiedName() == "IsAllPackage")
						continue;
					//�YRule�]��true�~�x�s
					if (Boolean.valueOf(filterList.get(i).getValue())) 
						filterRuleList.add(filterList.get(i).getQualifiedName());
				}
				model.setFilterList(filterRuleList);
			}
		} else
			isAllPackage = true;

		model.setDerectAllproject(isAllPackage);
	}
	
	/**
	 * �x�s��@Class���Ҧ�Smell��T
	 * @param icu
	 * @param isRecord
	 * @param pkPath
	 * @param newPackageModel
	 */
	private void setSmellInfo (ICompilationUnit icu, boolean isRecord, PackageModel newPackageModel, String pkPath) {
		
		ExceptionAnalyzer visitor = null;
		CodeSmellAnalyzer csVisitor = null;
		MainAnalyzer mainVisitor = null;

		// �ثemethod��Exception��T
//		List<RLMessage> currentMethodExList = null;
		// �ثemethod��RL Annotation��T
//		List<RLMessage> currentMethodRLList = null;
//		List<CSMessage> spareHandler = null;

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit root = (CompilationUnit) parser.createAST(null);

		ASTMethodCollector methodCollector = new ASTMethodCollector();

		root.accept(methodCollector);
		//���o�M�פ��Ҧ���method
		List<ASTNode> methodList = methodCollector.getMethodList();

		ClassModel newClassModel = new ClassModel();
		newClassModel.setClassName(icu.getElementName());
		newClassModel.setClassPath(pkPath);
		
		//�ثe��Method AST Node
		ASTNode currentMethodNode = null;
		int methodIdx = -1;
		for (ASTNode method : methodList) {
			methodIdx++;

			visitor = new ExceptionAnalyzer(root, method.getStartPosition(), 0);
			method.accept(visitor);
			currentMethodNode = visitor.getCurrentMethodNode();
//			currentMethodRLList = visitor.getMethodRLAnnotationList();

			MethodDeclaration methodName = (MethodDeclaration) currentMethodNode;

			//��M�M�פ��Ҧ���ignore Exception
			csVisitor = new CodeSmellAnalyzer(root);
			method.accept(csVisitor);
			//���o�M�פ���ignore Exception
			newClassModel.setIgnoreExList(csVisitor.getIgnoreExList(), methodName.getName().toString());
			model.addIgnoreTotalSize(csVisitor.getIgnoreExList().size());
				
			//���o�M�פ�dummy handler
			newClassModel.setDummyList(csVisitor.getDummyList(), methodName.getName().toString());
			model.addDummyTotalSize(csVisitor.getDummyList().size());

			//���o�M�פ���Nested Try Block
			newClassModel.setNestedTryList(visitor.getNestedTryList(), methodName.getName().toString());
			model.addNestedTotalTrySize(visitor.getNestedTryList().size());

			//�M���method����unprotected main program
			mainVisitor = new MainAnalyzer(root);
			method.accept(mainVisitor);
			newClassModel.setUnprotectedMain(mainVisitor.getUnprotedMainList(), methodName.getName().toString());
			model.addUnMainTotalSize(mainVisitor.getUnprotedMainList().size());
	
			///�O��Code Information///
			model.addTryCounter(csVisitor.getTryCounter());
			model.addCatchCounter(csVisitor.getCatchCounter());
			model.addFinallyCounter(csVisitor.getFinallyCounter());
		}
		//�O����ReportModel��
		newPackageModel.addClassModel(newClassModel);
	}

	/**
	 * ���R�S�wProject����Semll��T
	 * @param project
	 */
	private void analysisProject(IProject project) {
		//���o�M�ת����|
		IJavaProject javaP = JavaCore.create(project);
		String workPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		model.setProjectPath(workPath + javaP.getPath());

		try {
			List<IPackageFragmentRoot> root = getSourcePaths(javaP);
			for (int i = 0 ; i < root.size() ; i++){
				//���oRoot���U���Ҧ�Package
				IJavaElement[] packages = root.get(i).getChildren();

				for (IJavaElement ije : packages) {
					
					if (ije.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment pk = (IPackageFragment)ije;

						//�P�_�O�_�n�O��
						boolean isRecord = determineRecod(pk);

						//���oPackage���U��class
						ICompilationUnit[] compilationUnits = pk.getCompilationUnits();
						PackageModel newPackageModel = null;

						//�Y�n�����h�s�WPackage
						if (isRecord) {
							if (compilationUnits.length != 0)
								newPackageModel = model.addSmellList(pk.getElementName());

							//���oPackage���U���Ҧ�class��smell��T
							for (int k = 0; k < compilationUnits.length; k++) {
								setSmellInfo(compilationUnits[k], isRecord, newPackageModel, pk.getPath().toString());

								//�O��LOC
								int codeLines = countFileLOC(compilationUnits[k].getPath().toString());
								//������Package��
								newPackageModel.addTotalLine(codeLines);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] EXCEPTION ",e);
		}
	}

	/**
	 * �P�_�O�_�n�����o��Package��Smell��T
	 * @param pk
	 * @return
	 */
	private boolean determineRecod(IPackageFragment pk) {
		//�Y��������Package �h��������
		if (isAllPackage) {
			return true;
		} else {
			for (String filterRule : filterRuleList) {
				//�P�_�O�_��"Package.*"��Rule
				if (filterRule.indexOf(".EH_STAR") != -1) {
					if ((filterRule.length() -8) == filterRule.indexOf(".EH_STAR")) {
						String temp = filterRule.substring(0, filterRule.length()-8);
						if (pk.getElementName().length() >= temp.length()) {
							//�YPackage�e�b�q���ת��W�ٻPfilter rule�@�˫h����
							if (pk.getElementName().substring(0,temp.length()).equals(temp))
								return true;
						}
					}
				//�YPackage�PFilterRule�@�˫h����
				} else if (pk.getElementName().equals(filterRule)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * ���oPackageFragmentRoot List (�L�ojar)
	 */
	public List<IPackageFragmentRoot> getSourcePaths(IJavaProject project)throws JavaModelException {
        
	    List<IPackageFragmentRoot> sourcePaths =
	            new ArrayList<IPackageFragmentRoot>();

	    IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();

	    for (IPackageFragmentRoot r : roots) {
	    	if (!r.getPath().toString().endsWith(".jar")) {
	    		sourcePaths.add(r);
	    	}
	    }
	    return sourcePaths;
	}

	/**
	 * �p��Class File��LOC
	 * @param filePath		class��File Path
	 * @return				class��LOC��
	 */
	private int countFileLOC(String filePath) {
		//���oclass���|
		String workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		String path = workspace + filePath;

		File file = new File(path);

		//�YClass�s�b�A�p��Class
		if (file.exists()) {
			LOCData noFormatData = null;
			try {
				//�p��LOC
				noFormatData = noFormatCounter.countFileLOC(file);
			} catch (FileNotFoundException e) {
				logger.error("[File Not Found Exception] FileNotFoundException ",e);
			}
			return noFormatData.getTotalLine();
		}
		return 0;
	}
}
