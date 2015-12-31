package ntut.csie.analyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.empty.EmptyCatchBlockVisitor;
import ntut.csie.analyzer.nested.NestedTryStatementVisitor;
import ntut.csie.analyzer.thrown.ExceptionThrownFromFinallyBlockVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.util.AbstractBadSmellVisitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class BadSmellCollector {

	TreeMap<String, List<MarkerInfo>> badSmells;
	SmellSettings smellSetting;
	RobustaSettings robustaSettings;
	IProject project;
	CompilationUnit root;
	TreeMap<String, Boolean> smellDetectionCheckList;
	
	public BadSmellCollector(IProject project, CompilationUnit root) {
		this.project = project;
		this.root = root;
		this.badSmells = new TreeMap<String, List<MarkerInfo>>();
		this.smellSetting = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		this.robustaSettings = new RobustaSettings(UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(project), project);
		this.smellDetectionCheckList = getSmellsSelectedByUser();
	}
	
	public void collectBadSmell() {
		detectInitializers();
		detectBadSmellsInMethods();
	}

	/**
	 * Detect the smells in initializers only
	 */
	// TODO:-------------- for some bad smells, we are not checking whether to detect it or not------------------------------
	private void detectInitializers() { 
		DummyHandlerVisitor dmhVisitor = new DummyHandlerVisitor(root);
		EmptyCatchBlockVisitor emptyCatchBlockVisitor = new EmptyCatchBlockVisitor(root);
		NestedTryStatementVisitor nestedTryStatementVisitor = new NestedTryStatementVisitor(root);
		ASTInitializerCollector initializerCollector = new ASTInitializerCollector();
		ExceptionThrownFromFinallyBlockVisitor thrownInFinallyVisitor = new ExceptionThrownFromFinallyBlockVisitor(root);
		
		root.accept(initializerCollector);
		for(Initializer init : initializerCollector.getInitializerList()) {
			init.accept(dmhVisitor);
			init.accept(emptyCatchBlockVisitor);
			init.accept(nestedTryStatementVisitor);

			// Exception thrown from finally block
			if (smellDetectionCheckList
					.get(SmellSettings.SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK)) {
				init.accept(thrownInFinallyVisitor);
				List<MarkerInfo> throwInFinallyList = thrownInFinallyVisitor.getThrownInFinallyList();
				setMethodNameAndIndexForInitializer(throwInFinallyList);
				addBadSmell(
						RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK,throwInFinallyList);
			}
		}
		
		setMethodNameAndIndexForInitializer(dmhVisitor.getDummyHandlerList());
		setMethodNameAndIndexForInitializer(emptyCatchBlockVisitor.getEmptyCatchList());
		setMethodNameAndIndexForInitializer(nestedTryStatementVisitor.getNestedTryStatementList());
		addBadSmell(RLMarkerAttribute.CS_DUMMY_HANDLER, dmhVisitor.getDummyHandlerList());
		addBadSmell(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK, emptyCatchBlockVisitor.getEmptyCatchList());
		addBadSmell(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT, nestedTryStatementVisitor.getNestedTryStatementList());
	}

	private void detectBadSmellsInMethods() { 
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		root.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();

		boolean isDetectCCOutsideTryStatement = smellSetting.isExtraRuleExist(
				SmellSettings.SMELL_CARELESSCLEANUP,
				SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);

		int methodIdx = -1;
		for (MethodDeclaration method : methodList) {
			methodIdx++;
			
			SuppressWarningVisitor swVisitor = new SuppressWarningVisitor(root);
			method.accept(swVisitor);
			List<SSMessage> suppressWarningList = swVisitor.getSuppressWarningList();
			// the list of bad smells suppressed by user defined annotation
			Map<String, Boolean> suppressedSmellCheckList = getSuppressedSmells(suppressWarningList);
			
			for (String smellType : SmellSettings.ALL_BAD_SMELLS) {
				if(smellDetectionCheckList.get(smellType) && suppressedSmellCheckList.get(SmellSettings.toRLMarkerAttributeConstant(smellType)) == null) {
					AbstractBadSmellVisitor badSmellVisitor = BadSmellVisitorFactory.createVisitor(smellType, root, isDetectCCOutsideTryStatement);
					method.accept(badSmellVisitor);
					List<MarkerInfo> badSmellCollected = badSmellVisitor.getBadSmellCollected();
					setMethodNameAndIndex(badSmellCollected, method.getName().toString(), methodIdx);
					addBadSmell(SmellSettings.toRLMarkerAttributeConstant(smellType), badSmellCollected);
				}
			}
		}
	}
	
	private Map<String, Boolean> getSuppressedSmells(List<SSMessage> suppressWarningList) {
		Map<String, Boolean> suppressedSmells = new HashMap<String, Boolean>();
		for(SSMessage message : suppressWarningList) {
			for(String smellType : message.getSmellList()) {
				suppressedSmells.put(smellType, true);
			}
		}
		return suppressedSmells;
	}

	private void addBadSmell(String type, List<MarkerInfo> markerInfos) {
		List<MarkerInfo> currentMarkerInfos = badSmells.get(type);
		if(currentMarkerInfos == null)
			badSmells.put(type, markerInfos);
		else
			currentMarkerInfos.addAll(markerInfos);
	}

	private void setMethodNameAndIndexForInitializer(List<MarkerInfo> markerInfos) {
		setMethodNameAndIndex(markerInfos, "Initializer", -1);
	}
	
	private void setMethodNameAndIndex(List<MarkerInfo> markerInfos, String methodName, int methodIndex) {
		for (int i=0; i<markerInfos.size(); i++) {
			MarkerInfo markerInfo = markerInfos.get(i);
			markerInfo.setMethodName(methodName);
			markerInfo.setMethodIndex(methodIndex);
			markerInfo.setBadSmellIndex(i);
		}
	}
	
	public List<MarkerInfo> getBadSmells(String badSmellType) {
		List<MarkerInfo> badSmellList = badSmells.get(badSmellType);
		if(badSmellList == null) 
			return new ArrayList<MarkerInfo>();
		return badSmellList;
	}
	
	public List<MarkerInfo> getAllBadSmells() {
		List<MarkerInfo> badSmellList = new ArrayList<MarkerInfo>();
		for (Entry<String, List<MarkerInfo>> map : badSmells.entrySet()) {
			badSmellList.addAll(map.getValue());
		}
		return badSmellList;
	}

	private TreeMap<String, Boolean> getSmellsSelectedByUser() {
		TreeMap<String, Boolean> smellsUserChecked = new TreeMap<String, Boolean>();
		for (String smellType : SmellSettings.ALL_BAD_SMELLS) {
			smellsUserChecked.put(smellType, smellSetting.isDetectingSmell(smellType));
		}
		return smellsUserChecked;
	}
}
