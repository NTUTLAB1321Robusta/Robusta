package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.csdet.visitor.MainAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLMessage;

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
import org.jdom.Attribute;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportBuilder {
	private static Logger logger = LoggerFactory.getLogger(ReportBuilder.class);

	//Report�����
	private ReportModel model;
	//�O�_����������Package
	private Boolean isAllPackage;
	//�x�sfilter����List
	private List<String> filterRuleList = new ArrayList<String>();

	/**
	 * �إ�Report
	 * @param project
	 * @param model
	 */
	public ReportBuilder(IProject project,ReportModel model)
	{
		this.model = model;
		model.setProjectName(project.getName());
		
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
			}
		}
	}
	
	/**
	 * �x�sSmell��T
	 * @param icu
	 * @param isRecord
	 * @param newPackageModel
	 */
	private void setSmellInfo (ICompilationUnit icu, boolean isRecord, PackageModel newPackageModel) {
		ExceptionAnalyzer visitor = null;
		CodeSmellAnalyzer csVisitor = null;
		MainAnalyzer mainVisitor = null;

		//�ثemethod����ignore Exception��T
		List<CSMessage> ignoreExList = new ArrayList<CSMessage>();
		//�ثemethod����dummy handler��T
		List<CSMessage> dummyList = new ArrayList<CSMessage>();
		//�ثemethod����Nested Try Block��T
		List<CSMessage> nestedTryList = new ArrayList<CSMessage>(); 
		//�ثemethod����Unprotected Main��T
		List<CSMessage> unprotectedMain = new ArrayList<CSMessage>();

		// �ثemethod��Exception��T
		List<RLMessage> currentMethodExList = null;
		// �ثemethod��RL Annotation��T
		List<RLMessage> currentMethodRLList = null;
		List<CSMessage> spareHandler = null;

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit root = (CompilationUnit) parser.createAST(null);

		ASTMethodCollector methodCollector = new ASTMethodCollector();

		root.accept(methodCollector);
		//���o�M�פ��Ҧ���method
		List<ASTNode> methodList = methodCollector.getMethodList();

		//�ثe��Method AST Node
		ASTNode currentMethodNode = null;
		int methodIdx = -1;
		for (ASTNode method : methodList) {
			methodIdx++;

			visitor = new ExceptionAnalyzer(root, method.getStartPosition(), 0);
			method.accept(visitor);
			currentMethodNode = visitor.getCurrentMethodNode();
			currentMethodRLList = visitor.getMethodRLAnnotationList();

			//��M�M�פ��Ҧ���ignore Exception
			csVisitor = new CodeSmellAnalyzer(root);
			method.accept(csVisitor);
			//���o�M�פ���ignore Exception
			if(csVisitor.getIgnoreExList() != null)
				ignoreExList.addAll(csVisitor.getIgnoreExList());

			//���o�M�פ�dummy handler
			if(csVisitor.getDummyList() != null)
				dummyList.addAll(csVisitor.getDummyList());

			//���o�M�פ���Nested Try Block
			if(visitor.getNestedTryList() != null)
				nestedTryList.addAll(visitor.getNestedTryList());

			//�M���method����unprotected main program
			mainVisitor = new MainAnalyzer(root);
			method.accept(mainVisitor);
			if(mainVisitor.getUnprotedMainList() != null)
				unprotectedMain.addAll(mainVisitor.getUnprotedMainList());
		}

		//�O����ReportModel��
//		if (isRecord)
		ClassModel newClassModel = new ClassModel();
		newClassModel.setClassName(icu.getElementName());
		newClassModel.setIgnoreExList(ignoreExList);
		newClassModel.setDummyList(dummyList);
		newClassModel.setUnprotectedMain(unprotectedMain);
		newClassModel.setNestedTryList(nestedTryList);
		newPackageModel.addClassModel(newClassModel);

		model.addIgnoreTotalSize(ignoreExList.size());
		model.addDummyTotalSize(dummyList.size());
		model.addUnMainTotalSize(unprotectedMain.size());
		model.addNestedTotalTrySize(nestedTryList.size());
	}

	/**
	 * ���R�M��
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
								setSmellInfo(compilationUnits[k], isRecord, newPackageModel);
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
}
