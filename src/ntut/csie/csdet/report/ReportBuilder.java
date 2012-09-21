package ntut.csie.csdet.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.visitor.CarelessCleanupVisitor;
import ntut.csie.csdet.visitor.DummyHandlerVisitor;
import ntut.csie.csdet.visitor.IgnoreExceptionVisitor;
import ntut.csie.csdet.visitor.NestedTryStatementVisitor;
import ntut.csie.csdet.visitor.UnprotectedMainProgramVisitor;
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.jcis.builder.core.internal.support.LOCCounter;
import ntut.csie.jcis.builder.core.internal.support.LOCData;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
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
	// Report�����
	private ReportModel model;
	// �O�_����������Package
	private Boolean isAllPackage;
	// �x�sfilter����List
	private List<String> filterRuleList = new ArrayList<String>();
	private LOCCounter noFormatCounter = new LOCCounter();

	/**
	 * �إ�Report
	 * 
	 * @param project
	 * @param model
	 */
	public ReportBuilder(IProject project, ReportModel model) {
		this.project = project;
		this.model = model;
	}

	public void run() {
		// ���o�M�צW��
		model.setProjectName(project.getName());
		// ���o�سy�ɶ�
		model.setBuildTime();

		// �NUser���Filter���]�w�s�U��
		getFilterSettings();

		// �ѪR�M��
		analysisProject(project);

		// ����HTM
		SmellReport createHTM = new SmellReport(model);
		createHTM.build();

		// ���͹Ϫ�
		BarChart smellChart = new BarChart(model);
		smellChart.build();
	}

	/**
	 * �NUser���Filter���]�w�s�U��
	 * 
	 * @return
	 */
	private void getFilterSettings() {
		Element root = JDomUtil.createXMLContent();

		// �p�G�Onull���XML�ɬO��ئn��,�٨S��EHSmellFilterTaq��tag,�������X�h
		if (root.getChild(JDomUtil.EHSmellFilterTaq) != null) {

			// �o�̪�ܤ��e�ϥΪ̤w�g���]�w�Lpreference�F,�h���o���������]�w��
			Element filter = root.getChild(JDomUtil.EHSmellFilterTaq).getChild(
					"filter");
			isAllPackage = Boolean.valueOf(filter.getAttribute("IsAllPackage")
					.getValue());

			// �Y���O����������Project�A�h��Rule�����x�s
			if (!isAllPackage) {
				List<?> filterList = filter.getAttributes();

				for (int i = 0; i < filterList.size(); i++) {
					// ���L���ݩ�Rule���]�w
					if (((Attribute)filterList.get(i)).getQualifiedName() == "IsAllPackage")
						continue;
					// �YRule�]��true�~�x�s
					if (Boolean.valueOf(((Attribute)filterList.get(i)).getValue()))
						filterRuleList
								.add(((Attribute)filterList.get(i)).getQualifiedName());
				}
				model.setFilterList(filterRuleList);
			}
		} else
			isAllPackage = true;

		model.setDerectAllproject(isAllPackage);
	}

	/**
	 * �x�s��@Class���Ҧ�Smell��T
	 * 
	 * @param icu
	 * @param isRecord
	 * @param pkPath
	 * @param newPackageModel
	 */
	private void setSmellInfo(ICompilationUnit icu, boolean isRecord,
			PackageModel newPackageModel, String pkPath) {
		List<SSMessage> suppressSmellList = null;

		ExceptionAnalyzer visitor = null;
//		CodeSmellAnalyzer csVisitor = null;
		IgnoreExceptionVisitor ieVisitor = null;
		DummyHandlerVisitor dhVisitor = null;
		NestedTryStatementVisitor ntsVisitor = null;
		UnprotectedMainProgramVisitor mainVisitor = null;
		CarelessCleanupVisitor ccVisitor = null;
		OverLoggingDetector loggingDetector = null;

		// �غcAST
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit root = (CompilationUnit) parser.createAST(null);

		ASTMethodCollector methodCollector = new ASTMethodCollector();

		root.accept(methodCollector);
		// ���o�M�פ��Ҧ���method
		List<MethodDeclaration> methodList = methodCollector.getMethodList();

		ClassModel newClassModel = new ClassModel();
		newClassModel.setClassName(icu.getElementName());
		newClassModel.setClassPath(pkPath);

		// �ثe��Method AST Node
		MethodDeclaration currentMethodNode = null;
		int methodIdx = -1;
		for (MethodDeclaration method : methodList) {
			methodIdx++;

			visitor = new ExceptionAnalyzer(root, method.getStartPosition(), 0);
			method.accept(visitor);
			currentMethodNode = visitor.getCurrentMethodNode();
			suppressSmellList = visitor.getSuppressSemllAnnotationList();

			MethodDeclaration methodName = currentMethodNode;

			// SuppressSmell
			TreeMap<String, Boolean> detMethodSmell = new TreeMap<String, Boolean>();
			TreeMap<String, List<Integer>> detCatchSmell = new TreeMap<String, List<Integer>>();
			inputSuppressData(suppressSmellList, detMethodSmell, detCatchSmell);

			// ��M�M�פ��Ҧ���ignore Exception
//			csVisitor = new CodeSmellAnalyzer(root);
			// ���o�M�פ���ignore Exception
			if (detMethodSmell.get(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
				ieVisitor = new IgnoreExceptionVisitor(root);
				method.accept(ieVisitor);
				List<MarkerInfo> ignoreExList = checkCatchSmell(ieVisitor.getIgnoreList()
						, detCatchSmell
						.get(RLMarkerAttribute.CS_INGNORE_EXCEPTION));
				newClassModel.setIgnoreExList(ignoreExList, methodName
						.getName().toString());
				model.addIgnoreTotalSize(ignoreExList.size());
			}
			// ���o�M�פ�dummy handler
			if (detMethodSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
				dhVisitor = new DummyHandlerVisitor(root);
				method.accept(dhVisitor);
				List<MarkerInfo> dummyList = checkCatchSmell(dhVisitor.getDummyList()
						, detCatchSmell
						.get(RLMarkerAttribute.CS_DUMMY_HANDLER));
				newClassModel.setDummyList(dummyList, methodName.getName()
						.toString());
				model.addDummyTotalSize(dummyList.size());
			}
			// ���o�M�פ���Nested Try Block
			if (detMethodSmell.get(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)) {
				ntsVisitor = new NestedTryStatementVisitor(root);
				method.accept(ntsVisitor);
				List<MarkerInfo> nestedTryList = checkCatchSmell(ntsVisitor.getNestedTryStatementList()
						, detCatchSmell
						.get(RLMarkerAttribute.CS_NESTED_TRY_BLOCK));
				newClassModel.setDummyList(nestedTryList, methodName.getName()
						.toString());
				model.addNestedTotalTrySize(nestedTryList.size());
			}
			// �M���method����unprotected main program
			mainVisitor = new UnprotectedMainProgramVisitor(root);
			method.accept(mainVisitor);
			if (detMethodSmell.get(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				newClassModel
						.setUnprotectedMain(mainVisitor.getUnprotedMainList(),
								methodName.getName().toString());
				model.addUnMainTotalSize(mainVisitor.getUnprotedMainList()
						.size());
			}
			// ��M�M�פ��Ҧ���Careless Cleanup
			ccVisitor = new CarelessCleanupVisitor(root);
			method.accept(ccVisitor);
			if (detMethodSmell.get(RLMarkerAttribute.CS_CARELESS_CLEANUP)) {
				newClassModel.setCarelessCleanUp(ccVisitor.getCarelessCleanupList(), methodName.getName().toString());
				model.addCarelessCleanUpSize(ccVisitor.getCarelessCleanupList().size());
			}
			// �M���method����OverLogging
			loggingDetector = new OverLoggingDetector(root, method);
			loggingDetector.detect();
			if (detMethodSmell.get(RLMarkerAttribute.CS_OVER_LOGGING)) {
				List<MarkerInfo> olList = checkCatchSmell(loggingDetector
						.getOverLoggingList(), detCatchSmell
						.get(RLMarkerAttribute.CS_OVER_LOGGING));
				newClassModel.setOverLogging(olList, methodName.getName()
						.toString());
				model.addOverLoggingSize(olList.size());
			}
			// �O��Code Information //
			model.addTryCounter(dhVisitor.getTryCounter());
			model.addCatchCounter(dhVisitor.getCatchCounter());
			model.addFinallyCounter(dhVisitor.getFinallyCounter());
		}
		// �O����ReportModel��
		newPackageModel.addClassModel(newClassModel);
	}

	/**
	 * @param csVisitor
	 * @param detCatchSmell
	 * @param allSmellList
	 * @return
	 */
	private List<MarkerInfo> checkCatchSmell(List<MarkerInfo> allSmellList,
			List<Integer> posList) {
		List<MarkerInfo> smellList = new ArrayList<MarkerInfo>();
		if (posList != null && posList.size() == 0)
			smellList = allSmellList;
		else {
			for (MarkerInfo msg : allSmellList) {
				if (!suppressMarker(posList, msg.getPosition()))
					smellList.add(msg);
			}
		}
		return smellList;
	}

	/**
	 * �P�_�O�_�n���KMarker
	 * 
	 * @param smellPosList
	 * @param pos
	 * @return
	 */
	private boolean suppressMarker(List<Integer> smellPosList, int pos) {
		if(smellPosList != null) {
			for (Integer index : smellPosList)
				// �YCatch��m�ۦP�A��ܭn��Marker���P�@��Marker
				if (pos == index)
					return true;
		}
		return false;
	}

	/**
	 * �x�sSuppress Smell���]�w
	 * 
	 * @param suppressSmellList
	 * @param detMethodSmell
	 * @param detCatchSmell
	 */
	private void inputSuppressData(List<SSMessage> suppressSmellList,
			TreeMap<String, Boolean> detMethodSmell,
			TreeMap<String, List<Integer>> detCatchSmell) {
		// ��l�Ƴ]�w�A�w�]�C��Smell������
		for (String smellType : RLMarkerAttribute.CS_TOTAL_TYPE)
			detMethodSmell.put(smellType, true);

		for (String smellType : RLMarkerAttribute.CS_CATCH_TYPE)
			detCatchSmell.put(smellType, new ArrayList<Integer>());

		for (SSMessage msg : suppressSmellList) {
			// �Y��Method�W���]�w
			if (!msg.isInCatch()) {
				// �Y�ϥΪ̰�������Smell�������A�N���Smell�����]�w��false
				for (String smellType : msg.getSmellList())
					detMethodSmell.put(smellType, false);
				// �Y��Catch�����]�w
			} else {
				// �Y�ϥΪ̳]�wCatch��Smell�������A�O����Smell�Ҧb��Catch��m
				for (String smellType : msg.getSmellList()) {
					List<Integer> smellPosList = detCatchSmell.get(smellType);
					if (smellPosList != null)
						smellPosList.add(msg.getPosition());
				}
			}
		}
	}

	/**
	 * ���R�S�wProject����Smell��T
	 * @param project
	 */
	private void analysisProject(IProject project) {
		// ���o�M�ת����|
		IJavaProject javaPrj = JavaCore.create(project);
		String workPath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		model.setProjectPath(workPath + javaPrj.getPath());

		try {
			List<IPackageFragmentRoot> root = getSourcePaths(javaPrj);
			for (int i = 0; i < root.size(); i++) {
				// ���oFolder���W��
				String folderName = root.get(i).getElementName();

				// ���oRoot���U���Ҧ�Package
				IJavaElement[] packages = root.get(i).getChildren();

				for (IJavaElement iJavaElement : packages) {

					if (iJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment iPackageFgt = (IPackageFragment) iJavaElement;

						// �P�_�O�_�n�O��
						boolean isRecord = determineRecord(folderName,
								iPackageFgt);

						// ���oPackage���U��class
						ICompilationUnit[] compilationUnits = iPackageFgt
								.getCompilationUnits();
						PackageModel newPackageModel = null;

						// �Y�n�����h�s�WPackage
						if (isRecord) {
							if (compilationUnits.length != 0) {
								// �إ�PackageModel
								newPackageModel = model
										.addSmellList(iPackageFgt
												.getElementName());
								// �O��Package��Folder�W��
								newPackageModel.setFolderName(root.get(i)
										.getElementName());
							}

							// ���oPackage���U���Ҧ�class��smell��T
							for (int k = 0; k < compilationUnits.length; k++) {
								setSmellInfo(compilationUnits[k], isRecord,
										newPackageModel, iPackageFgt.getPath()
												.toString());

								// �O��LOC
								int codeLines = countFileLOC(compilationUnits[k]
										.getPath().toString());
								// ������Package��
								newPackageModel.addTotalLine(codeLines);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] EXCEPTION ", e);
		}
	}

	/**
	 * �P�_�O�_�n�O���o��Package��Smell��T
	 * 
	 * @param folderName
	 * @param pk
	 * @return
	 */
	private boolean determineRecord(String folderName, IPackageFragment pk) {
		// �Y��������Package �h�����O��
		if (isAllPackage) {
			return true;
		} else {
			for (String filterRule : filterRuleList) {
				//�P�_���w������A���S���]�t��Ƨ�
				if (isConformFolderFormat(filterRule)) {
					//[Folder]�Ҧ��C�p�G�̫e�P�᳣̫�Osquare bracket�A�N��ϥΪ̭n�ݾ�Ӹ�Ƨ�
					if(filterRule.indexOf(JDomUtil.EH_Left) == 0 && (filterRule.indexOf(JDomUtil.EH_Right)+JDomUtil.EH_Right.length()) == filterRule.length()){
						if(getFolderName(filterRule).equals(folderName)){
							return true;
						}
					}
					//[Folder]+Package.*���Ҧ�
					else if(filterRule.contains("."+JDomUtil.EH_Star)){
						if(getFolderName(filterRule).equals(folderName) &&
							isConformMultiPackageFormat(pk, filterRule)){
							return true;
						}
					}
					//[Folder]+Package
					else{
						if(getFolderName(filterRule).equals(folderName) && pk.getElementName().equals(getTrimFolderName(filterRule))){
							return true;
						}
					}
				//���B���S�����w��Ƨ����P�_
				} else {
					if (filterRule.contains("." + JDomUtil.EH_Star)) {
						if (isConformMultiPackageFormat(pk, filterRule)) {
							return true;
						}
					} else if (pk.getElementName().equals(filterRule)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * .*�n�bfilteRule���̫᭱
	 * @param iPkgFgt
	 * @param filterRule
	 * @return <i>True</i> if match "Package.*" <br />
	 * 		Others, return false.
	 */
	private boolean isConformMultiPackageFormat(IPackageFragment iPkgFgt, String filterRule) {
		if ((filterRule.length() - (JDomUtil.EH_Star.length() + 1)) == filterRule.indexOf("." + JDomUtil.EH_Star)) {
			String pkgHead = filterRule.substring(0, filterRule.length() - (1+JDomUtil.EH_Star.length()));
			
			//�p�G�]�tFolder�A���N�n��Folder�屼
			if(isConformFolderFormat(pkgHead)){
				pkgHead = pkgHead.substring(pkgHead.indexOf(JDomUtil.EH_Right)+JDomUtil.EH_Right.length());
			}
			
			//�Y�ӧ�Ӫ�����package���סA��ϥγo�]�w��package.*��
			if (iPkgFgt.getElementName().length() >= pkgHead.length()) {
				// �S�APackage�e�b�q���ת��W�ٻPfilter rule�@�ˡA�h�[�J����smell���M�椤
				if (iPkgFgt.getElementName().substring(0, pkgHead.length()).equals(pkgHead))
					return true;
			}
		}
		return false;
	}

	/**
	 * ���^EH_LEFT<i>folder</i>EH_RIGHT������folder�W��
	 * 
	 * @param filterRule
	 * @param folderName
	 * @return String , the name of folder
	 */
	private String getFolderName(String filterRule) {
		int left = filterRule.indexOf(JDomUtil.EH_Left);
		int right = filterRule.indexOf(JDomUtil.EH_Right);
		// �ϥΪ̷|��J[FolderName]�A���B�t�d�������k[]�A���oFolder�W�r
		String pkFolder = filterRule.substring(left
				+ JDomUtil.EH_Left.length(), right);
		return pkFolder;
	}
	
	/**
	 * �ˬd�O�_�ŦXFolder����h<br />
	 * 1. �n��&quot;[&quot; & &quot;]&quot; <br />
	 * 2. &quot;[&quot;�n�b&quot;]&quot;�e��
	 * @param filterRule
	 * @return
	 */
	private boolean isConformFolderFormat(String filterRule){
		int left = filterRule.indexOf(JDomUtil.EH_Left);
		int right = filterRule.indexOf(JDomUtil.EH_Right);
		if (left != -1 && right != -1 && left < right) {
			return true;
		}
		return false;
	}

	/**
	 * �NEH_LEFT<i>folder</i>EH_RIGHT���r��L�o��
	 * 
	 * @param filterRule
	 * @return String, the string without folder and &quot;square barker&quot;
	 */
	private String getTrimFolderName(String filterRule) {

		return filterRule.substring(filterRule
				.indexOf(JDomUtil.EH_Right)
				+ JDomUtil.EH_Right.length());

	}

	/**
	 * ���oPackageFragmentRoot List (�L�ojar)
	 */
	public List<IPackageFragmentRoot> getSourcePaths(IJavaProject project)
			throws JavaModelException {
		List<IPackageFragmentRoot> sourcePaths = new ArrayList<IPackageFragmentRoot>();

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
	 * 
	 * @param filePath
	 *            class��File Path
	 * @return class��LOC��
	 */
	private int countFileLOC(String filePath) {
		// ���oclass���|
		String workspace = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toOSString();
		String path = workspace + filePath;

		File file = new File(path);

		// �YClass�s�b�A�p��Class
		if (file.exists()) {
			LOCData noFormatData = null;
			try {
				// �p��LOC
				noFormatData = noFormatCounter.countFileLOC(file);
			} catch (FileNotFoundException e) {
				logger.error(
						"[File Not Found Exception] FileNotFoundException ", e);
			}
			return noFormatData.getTotalLine();
		}
		return 0;
	}
}