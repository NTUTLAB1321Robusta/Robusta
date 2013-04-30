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
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.csdet.visitor.OverwrittenLeadExceptionVisitor;
import ntut.csie.csdet.visitor.SuppressWarningVisitor;
import ntut.csie.csdet.visitor.TryStatementCounterVisitor;
import ntut.csie.csdet.visitor.UnprotectedMainProgramVisitor;
import ntut.csie.jcis.builder.core.internal.support.LOCCounter;
import ntut.csie.jcis.builder.core.internal.support.LOCData;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

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
	// Report的資料
	private ReportModel model;
	// 是否偵測全部的Package
	private Boolean isAllPackage;
	// 儲存filter條件的List
	private List<String> filterRuleList = new ArrayList<String>();
	private LOCCounter noFormatCounter = new LOCCounter();

	/**
	 * 建立Report
	 * 
	 * @param project
	 * @param model
	 */
	public ReportBuilder(IProject project, ReportModel model) {
		this.project = project;
		this.model = model;
	}

	public void run() {
		// 取得專案名稱
		model.setProjectName(project.getName());
		// 取得建造時間
		model.setBuildTime();

		// 將User對於Filter的設定存下來
		getFilterSettings();

		// 解析專案
		analysisProject(project);

		// 產生HTM
		SmellReport createHTM = new SmellReport(model);
		createHTM.build();

		// 產生圖表
		BarChart smellChart = new BarChart(model);
		smellChart.build();
	}

	/**
	 * 將User對於Filter的設定存下來
	 * 
	 * @return
	 */
	private void getFilterSettings() {
		Element root = JDomUtil.createXMLContent();

		// 如果是null表示XML檔是剛建好的,還沒有EHSmellFilterTaq的tag,直接跳出去
		if (root.getChild(JDomUtil.EHSmellFilterTaq) != null) {

			// 這裡表示之前使用者已經有設定過preference了,去取得相關偵測設定值
			Element filter = root.getChild(JDomUtil.EHSmellFilterTaq).getChild("filter");
			isAllPackage = Boolean.valueOf(filter.getAttribute("IsAllPackage").getValue());

			// 若不是偵測全部的Project，則把Rule條件儲存
			if (!isAllPackage) {
				List<?> filterList = filter.getAttributes();

				for (int i = 0; i < filterList.size(); i++) {
					// 略過不屬於Rule的設定
					if (((Attribute)filterList.get(i)).getQualifiedName() == "IsAllPackage")
						continue;
					// 若Rule設成true才儲存
					if (Boolean.valueOf(((Attribute)filterList.get(i)).getValue()))
						filterRuleList.add(((Attribute)filterList.get(i)).getQualifiedName());
				}
				model.setFilterList(filterRuleList);
			}
		} else
			isAllPackage = true;

		model.setDerectAllproject(isAllPackage);
	}

	/**
	 * 儲存單一Class內所有Smell資訊
	 * 
	 * @param icu
	 * @param isRecord
	 * @param pkPath
	 * @param newPackageModel
	 */
	private void setSmellInfo(ICompilationUnit icu, boolean isRecord,
			PackageModel newPackageModel, String pkPath) {
		List<SSMessage> suppressSmellList = null;

		IgnoreExceptionVisitor ieVisitor = null;
		DummyHandlerVisitor dhVisitor = null;
		NestedTryStatementVisitor ntsVisitor = null;
		UnprotectedMainProgramVisitor mainVisitor = null;
		CarelessCleanupVisitor ccVisitor = null;
		OverLoggingDetector loggingDetector = null;
		TryStatementCounterVisitor counterVisitor = null;
		OverwrittenLeadExceptionVisitor overwrittenVisitor = null;

		// 建構AST
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit root = (CompilationUnit) parser.createAST(null);

		ASTMethodCollector methodCollector = new ASTMethodCollector();

		root.accept(methodCollector);
		// 取得專案中所有的method
		List<MethodDeclaration> methodList = methodCollector.getMethodList();

		ClassModel newClassModel = new ClassModel();
		newClassModel.setClassName(icu.getElementName());
		newClassModel.setClassPath(pkPath);

		// 目前的Method AST Node
		int methodIdx = -1;
		for (MethodDeclaration method : methodList) {
			methodIdx++;
			SuppressWarningVisitor swVisitor = new SuppressWarningVisitor(root);
			method.accept(swVisitor);
			suppressSmellList = swVisitor.getSuppressWarningList();
			
			// SuppressSmell
			TreeMap<String, Boolean> detMethodSmell = new TreeMap<String, Boolean>();
			TreeMap<String, List<Integer>> detCatchSmell = new TreeMap<String, List<Integer>>();
			inputSuppressData(suppressSmellList, detMethodSmell, detCatchSmell);

			// 取得專案中的ignore Exception
			if (detMethodSmell.get(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
				ieVisitor = new IgnoreExceptionVisitor(root);
				method.accept(ieVisitor);
				List<MarkerInfo> ignoreExList = checkCatchSmell(ieVisitor.getIgnoreList(), detCatchSmell.get(RLMarkerAttribute.CS_INGNORE_EXCEPTION));
				newClassModel.setIgnoreExList(ignoreExList, method.getName().toString());
				model.addIgnoreTotalSize(ignoreExList.size());
			}
			// 取得專案中dummy handler
			if (detMethodSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
				dhVisitor = new DummyHandlerVisitor(root);
				method.accept(dhVisitor);
				List<MarkerInfo> dummyList = checkCatchSmell(dhVisitor.getDummyList(), detCatchSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER));
				newClassModel.setDummyList(dummyList, method.getName().toString());
				model.addDummyTotalSize(dummyList.size());
			}
			// 取得專案中的Nested Try Block
			if (detMethodSmell.get(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)) {
				ntsVisitor = new NestedTryStatementVisitor(root);
				method.accept(ntsVisitor);
				List<MarkerInfo> nestedTryList = checkCatchSmell(ntsVisitor.getNestedTryStatementList(), detCatchSmell.get(RLMarkerAttribute.CS_NESTED_TRY_BLOCK));
				newClassModel.setNestedTryList(nestedTryList, method.getName().toString());
				model.addNestedTotalTrySize(nestedTryList.size());
			}
			// 尋找該method內的unprotected main program
			mainVisitor = new UnprotectedMainProgramVisitor(root);
			method.accept(mainVisitor);
			if (detMethodSmell.get(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				newClassModel.setUnprotectedMain(mainVisitor.getUnprotedMainList(), method.getName().toString());
				model.addUnMainTotalSize(mainVisitor.getUnprotedMainList().size());
			}
			// 找尋專案中所有的Careless Cleanup
			ccVisitor = new CarelessCleanupVisitor(root);
			method.accept(ccVisitor);
			if (detMethodSmell.get(RLMarkerAttribute.CS_CARELESS_CLEANUP)) {
				newClassModel.setCarelessCleanUp(ccVisitor.getCarelessCleanupList(), method.getName().toString());
				model.addCarelessCleanUpSize(ccVisitor.getCarelessCleanupList().size());
			}
			// 尋找該method內的OverLogging
			loggingDetector = new OverLoggingDetector(root, method);
			loggingDetector.detect();
			if (detMethodSmell.get(RLMarkerAttribute.CS_OVER_LOGGING)) {
				List<MarkerInfo> olList = checkCatchSmell(loggingDetector.getOverLoggingList(), detCatchSmell.get(RLMarkerAttribute.CS_OVER_LOGGING));
				newClassModel.setOverLogging(olList, method.getName().toString());
				model.addOverLoggingSize(olList.size());
			}
			// 找尋專案中所有的Overwritten Lead Exception
			overwrittenVisitor = new OverwrittenLeadExceptionVisitor(root);
			method.accept(overwrittenVisitor);
			if(detMethodSmell.get(RLMarkerAttribute.CS_OVERWRITTEN_LEAD_EXCEPTION)) {
				List<MarkerInfo> owList = checkCatchSmell(overwrittenVisitor.getOverwrittenList(), detCatchSmell.get(RLMarkerAttribute.CS_OVERWRITTEN_LEAD_EXCEPTION));
				newClassModel.setOverwrittenLead(owList, method.getName().toString());
				model.addOverwrittenSize(owList.size());
			}
			// 記錄Code Information
			counterVisitor = new TryStatementCounterVisitor();
			method.accept(counterVisitor);
			model.addTryCounter(counterVisitor.getTryCount());
			model.addCatchCounter(counterVisitor.getCatchCount());
			model.addFinallyCounter(counterVisitor.getFinallyCount());
		}
		// 記錄到ReportModel中
		newPackageModel.addClassModel(newClassModel);
	}

	/**
	 * @param csVisitor
	 * @param detCatchSmell
	 * @param allSmellList
	 * @return
	 */
	private List<MarkerInfo> checkCatchSmell(List<MarkerInfo> allSmellList, List<Integer> posList) {
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
	 * 判斷是否要不貼Marker
	 * 
	 * @param smellPosList
	 * @param pos
	 * @return
	 */
	private boolean suppressMarker(List<Integer> smellPosList, int pos) {
		if(smellPosList != null) {
			for (Integer index : smellPosList)
				// 若Catch位置相同，表示要抑制的Marker為同一個Marker
				if (pos == index)
					return true;
		}
		return false;
	}

	/**
	 * 儲存Suppress Smell的設定
	 * 
	 * @param suppressSmellList
	 * @param detMethodSmell
	 * @param detCatchSmell
	 */
	private void inputSuppressData(	List<SSMessage> suppressSmellList,
									TreeMap<String, Boolean> detMethodSmell,
									TreeMap<String, List<Integer>> detCatchSmell) {
		// 初始化設定，預設每個Smell都偵測
		for (String smellType : RLMarkerAttribute.CS_TOTAL_TYPE)
			detMethodSmell.put(smellType, true);

		for (String smellType : RLMarkerAttribute.CS_CATCH_TYPE)
			detCatchSmell.put(smellType, new ArrayList<Integer>());

		for (SSMessage msg : suppressSmellList) {
			// 若為Method上的設定
			if (!msg.isInCatch()) {
				// 若使用者偵測哪個Smell不偵測，就把該Smell偵測設定為false
				for (String smellType : msg.getSmellList())
					detMethodSmell.put(smellType, false);
				// 若為Catch內的設定
			} else {
				// 若使用者設定Catch內Smell不偵測，記錄該Smell所在的Catch位置
				for (String smellType : msg.getSmellList()) {
					List<Integer> smellPosList = detCatchSmell.get(smellType);
					if (smellPosList != null)
						smellPosList.add(msg.getPosition());
				}
			}
		}
	}

	/**
	 * 分析特定Project內的Smell資訊
	 * @param project
	 */
	private void analysisProject(IProject project) {
		// 取得專案的路徑
		IJavaProject javaPrj = JavaCore.create(project);
		String workPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		model.setProjectPath(workPath + javaPrj.getPath());

		try {
			List<IPackageFragmentRoot> root = getSourcePaths(javaPrj);
			for (int i = 0; i < root.size(); i++) {
				// 取得Folder的名稱
				String folderName = root.get(i).getElementName();
				// 取得Root底下的所有Package
				IJavaElement[] packages = root.get(i).getChildren();

				for (IJavaElement iJavaElement : packages) {

					if (iJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment iPackageFgt = (IPackageFragment) iJavaElement;

						// 判斷是否要記錄
						boolean isRecord = determineRecord(folderName, iPackageFgt);

						// 取得Package底下的class
						ICompilationUnit[] compilationUnits = iPackageFgt.getCompilationUnits();
						PackageModel newPackageModel = null;

						// 若要紀錄則新增Package
						if (isRecord) {
							if (compilationUnits.length != 0) {
								// 建立PackageModel
								newPackageModel = model.addSmellList(iPackageFgt.getElementName());
								// 記錄Package的Folder名稱
								newPackageModel.setFolderName(root.get(i).getElementName());
							}

							// 取得Package底下的所有class的smell資訊
							for (int k = 0; k < compilationUnits.length; k++) {
								setSmellInfo(compilationUnits[k], isRecord, newPackageModel, iPackageFgt.getPath().toString());

								// 記錄LOC
								int codeLines = countFileLOC(compilationUnits[k].getPath().toString());
								// 紀錄到Package中
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
	 * 判斷是否要記錄這個Package的Smell資訊
	 * 
	 * @param folderName
	 * @param pk
	 * @return
	 */
	private boolean determineRecord(String folderName, IPackageFragment pk) {
		// 若偵測全部Package 則全部記錄
		if (isAllPackage) {
			return true;
		} else {
			for (String filterRule : filterRuleList) {
				//判斷給定的條件，有沒有包含資料夾
				if (isConformFolderFormat(filterRule)) {
					//[Folder]模式。如果最前與最後都是square bracket，代表使用者要看整個資料夾
					if(filterRule.indexOf(JDomUtil.EH_Left) == 0 && (filterRule.indexOf(JDomUtil.EH_Right)+JDomUtil.EH_Right.length()) == filterRule.length()){
						if(getFolderName(filterRule).equals(folderName)){
							return true;
						}
					}
					//[Folder]+Package.*的模式
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
				//此處為沒有給定資料夾的判斷
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
	 * .*要在filteRule的最後面
	 * @param iPkgFgt
	 * @param filterRule
	 * @return <i>True</i> if match "Package.*" <br />
	 * 		Others, return false.
	 */
	private boolean isConformMultiPackageFormat(IPackageFragment iPkgFgt, String filterRule) {
		if ((filterRule.length() - (JDomUtil.EH_Star.length() + 1)) == filterRule.indexOf("." + JDomUtil.EH_Star)) {
			String pkgHead = filterRule.substring(0, filterRule.length() - (1+JDomUtil.EH_Star.length()));
			
			//如果包含Folder，那就要把Folder砍掉
			if(isConformFolderFormat(pkgHead)){
				pkgHead = pkgHead.substring(pkgHead.indexOf(JDomUtil.EH_Right)+JDomUtil.EH_Right.length());
			}
			
			//某個抓來的完整package長度，比使用這設定的package.*長
			if (iPkgFgt.getElementName().length() >= pkgHead.length()) {
				// 又，Package前半段長度的名稱與filter rule一樣，則加入偵測smell的清單中
				if (iPkgFgt.getElementName().substring(0, pkgHead.length()).equals(pkgHead))
					return true;
			}
		}
		return false;
	}

	/**
	 * 取回EH_LEFT<i>folder</i>EH_RIGHT之中的folder名稱
	 * 
	 * @param filterRule
	 * @param folderName
	 * @return String , the name of folder
	 */
	private String getFolderName(String filterRule) {
		int left = filterRule.indexOf(JDomUtil.EH_Left);
		int right = filterRule.indexOf(JDomUtil.EH_Right);
		// 使用者會輸入[FolderName]，此處負責扣掉左右[]，取得Folder名字
		String pkFolder = filterRule.substring(left + JDomUtil.EH_Left.length(), right);
		return pkFolder;
	}
	
	/**
	 * 檢查是否符合Folder的原則<br />
	 * 1. 要有&quot;[&quot; & &quot;]&quot; <br />
	 * 2. &quot;[&quot;要在&quot;]&quot;前面
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
	 * 將EH_LEFT<i>folder</i>EH_RIGHT的字串過濾掉
	 * 
	 * @param filterRule
	 * @return String, the string without folder and &quot;square barker&quot;
	 */
	private String getTrimFolderName(String filterRule) {
		return filterRule.substring(filterRule.indexOf(JDomUtil.EH_Right) + JDomUtil.EH_Right.length());
	}

	/**
	 * 取得PackageFragmentRoot List (過濾jar)
	 */
	public List<IPackageFragmentRoot> getSourcePaths(IJavaProject project) throws JavaModelException {
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
	 * 計算Class File的LOC
	 * 
	 * @param filePath	class的File Path
	 * @return class的LOC數
	 */
	private int countFileLOC(String filePath) {
		// 取得class路徑
		String workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		String path = workspace + filePath;

		File file = new File(path);

		// 若Class存在，計算Class
		if (file.exists()) {
			LOCData noFormatData = null;
			try {
				// 計算LOC
				noFormatData = noFormatCounter.countFileLOC(file);
			} catch (FileNotFoundException e) {
				logger.error("[File Not Found Exception] FileNotFoundException ", e);
			}
			return noFormatData.getTotalLine();
		}
		return 0;
	}
}