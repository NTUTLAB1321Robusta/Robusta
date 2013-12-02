package ntut.csie.csdet.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.visitor.BadSmellCollector;
import ntut.csie.csdet.visitor.TryStatementCounterVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.jcis.builder.core.internal.support.LOCCounter;
import ntut.csie.jcis.builder.core.internal.support.LOCData;

import org.eclipse.core.resources.IProject;
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
	 * 分析特定Project內的Smell資訊
	 * @param project
	 */
	private void analysisProject(IProject project) {
		// 取得專案的路徑
		IJavaProject javaPrj = JavaCore.create(project);
		model.setProjectPath(project.getLocation().toString());

		try {
			List<IPackageFragmentRoot> root = getSourcePaths(javaPrj);
			for (int i = 0; i < root.size(); i++) {
				//Check if the user dont want to detect some root source folders.
				if(!shouldDetectPackageFragmentRoot(root.get(i)))
					continue;
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
								newPackageModel = new PackageModel();
								//設置Package名稱
								newPackageModel.setPackageName(iPackageFgt.getElementName());
								// 記錄Package的Folder名稱
								newPackageModel.setFolderName(root.get(i).getElementName());

							// 取得Package底下的所有class的smell資訊
							for (int k = 0; k < compilationUnits.length; k++) {
								setSmellInfo(compilationUnits[k], isRecord, newPackageModel, iPackageFgt.getPath().toString());

								// 記錄LOC
								int codeLines = countFileLOC(compilationUnits[k].getPath().toString());
								// 紀錄到Package中
								newPackageModel.addTotalLine(codeLines);
							}
								model.addPackageModel(newPackageModel);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] EXCEPTION ", e);
		}
	}
	private boolean shouldDetectPackageFragmentRoot(IPackageFragmentRoot root) {
		RobustaSettings robustaSettings = new RobustaSettings(new File(UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(project)), project);
		return robustaSettings.getProjectDetectAttribute(root.getPath().segment(1).toString());
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
		File file = new File(project.getLocation().toString() + "/.." + filePath);

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