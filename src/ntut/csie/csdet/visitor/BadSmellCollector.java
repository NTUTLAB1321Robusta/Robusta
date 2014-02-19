package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.aidvisitor.ASTInitializerCollector;
import ntut.csie.csdet.visitor.aidvisitor.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

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
	TreeMap<String, Boolean> isDetectingBadSmell;
	
	public BadSmellCollector(IProject project, CompilationUnit root) {
		this.project = project;
		this.root = root;
		this.badSmells = new TreeMap<String, List<MarkerInfo>>();
		this.smellSetting = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		this.robustaSettings = new RobustaSettings(UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(project), project);
		isDetectingBadSmell = new TreeMap<String, Boolean>();
		initializeIsDetectingBadSmell();
	}
	
	public void collectBadSmell() {
		detectInitializers();
		detectMethods();
	}

	/**
	 * Detect the smells in initializers only
	 */
	private void detectInitializers() {
		DummyHandlerVisitor dmhVisitor = new DummyHandlerVisitor(root);
		EmptyCatchBlockVisitor emptyCatchBlockVisitor = new EmptyCatchBlockVisitor(root);
		NestedTryStatementVisitor nestedTryStatementVisitor = new NestedTryStatementVisitor(root);
		ASTInitializerCollector initializerCollector = new ASTInitializerCollector();
		ThrownExceptionInFinallyBlockVisitor thrownInFinallyVisitor = new ThrownExceptionInFinallyBlockVisitor(root);
		
		root.accept(initializerCollector);
		for(Initializer init : initializerCollector.getInitializerList()) {
			init.accept(dmhVisitor);
			init.accept(emptyCatchBlockVisitor);
			init.accept(nestedTryStatementVisitor);

			// Thrown exception in finally block
			if (isDetectingBadSmell
					.get(SmellSettings.SMELL_THROWNEXCEPTIONINFINALLYBLOCK)) {
				init.accept(thrownInFinallyVisitor);
				List<MarkerInfo> throwInFinallyList = thrownInFinallyVisitor.getThrownInFinallyList();
				setMethodNameAndIndexForInitializer(throwInFinallyList);
				addBadSmell(
						RLMarkerAttribute.CS_THROWN_EXCEPTION_IN_FINALLY_BLOCK,throwInFinallyList);
			}
		}
		
		setMethodNameAndIndexForInitializer(dmhVisitor.getDummyList());
		setMethodNameAndIndexForInitializer(emptyCatchBlockVisitor.getEmptyCatchList());
		setMethodNameAndIndexForInitializer(nestedTryStatementVisitor.getNestedTryStatementList());
		addBadSmell(RLMarkerAttribute.CS_DUMMY_HANDLER, dmhVisitor.getDummyList());
		addBadSmell(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK, emptyCatchBlockVisitor.getEmptyCatchList());
		addBadSmell(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT, nestedTryStatementVisitor.getNestedTryStatementList());
	}

	/**
	 * Detect the smells in methods only
	 */
	private void detectMethods() {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		root.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();

		/*
		 * We have to initialize each visitor for each method declaration,
		 * because we want to add specific info on the mark
		 */

		int methodIdx = -1;
		for (MethodDeclaration method : methodList) {
			methodIdx++;
			SuppressWarningVisitor swVisitor = new SuppressWarningVisitor(root);
			method.accept(swVisitor);
			List<SSMessage> suppressSmellList = swVisitor.getSuppressWarningList();
			
			TreeMap<String,Boolean> detMethodSmell = new TreeMap<String,Boolean>();
			TreeMap<String, List<Integer>> detCatchSmell = new TreeMap<String, List<Integer>>();
			for (String smellType : RLMarkerAttribute.CS_TOTAL_TYPE)
				detMethodSmell.put(smellType, true);
			
			inputSuppressData(suppressSmellList, detMethodSmell, detCatchSmell);
			
			// Nested again(The second time is collect in Method)
			if(detMethodSmell.get(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT)) {
				NestedTryStatementVisitor nestedTryVisitor = new NestedTryStatementVisitor(root);
				method.accept(nestedTryVisitor);
				List<MarkerInfo> nestedList = nestedTryVisitor.getNestedTryStatementList();
				setMethodNameAndIndex(nestedList, method.getName().toString(), methodIdx);
				addBadSmell(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT, nestedList);
			}

			// Careless cleanup
			if(isDetectingBadSmell.get(SmellSettings.SMELL_CARELESSCLEANUP)) {
				if(detMethodSmell.get(RLMarkerAttribute.CS_CARELESS_CLEANUP)) {
					CarelessCleanupVisitor ccVisitor = new CarelessCleanupVisitor(root);
					method.accept(ccVisitor);
					List<MarkerInfo> carelessCleanupList = ccVisitor.getCarelessCleanupList();
					setMethodNameAndIndex(carelessCleanupList, method.getName().toString(), methodIdx);
					addBadSmell(RLMarkerAttribute.CS_CARELESS_CLEANUP, carelessCleanupList);
				}
			}
			
			// Empty catch block
			if(detMethodSmell.get(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK)) {
				EmptyCatchBlockVisitor ieVisitor = new EmptyCatchBlockVisitor(root);
				method.accept(ieVisitor);
				List<MarkerInfo> ignoreList = ieVisitor.getEmptyCatchList();
				List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK);
				removeFromSuppress(ignoreList, posList);
				setMethodNameAndIndex(ignoreList, method.getName().toString(), methodIdx);
				addBadSmell(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK, ignoreList);
			}
			
			// Dummy handler
			if(detMethodSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
				DummyHandlerVisitor dhVisitor = new DummyHandlerVisitor(root);
				method.accept(dhVisitor);
				List<MarkerInfo> dummyList = dhVisitor.getDummyList();
				List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER);
				removeFromSuppress(dummyList, posList);
				setMethodNameAndIndex(dummyList, method.getName().toString(), methodIdx);
				addBadSmell(RLMarkerAttribute.CS_DUMMY_HANDLER, dummyList);
			}
			
			// Over logging
			if(detMethodSmell.get(RLMarkerAttribute.CS_OVER_LOGGING)){
				OverLoggingDetector loggingDetector = new OverLoggingDetector(root, method);
				loggingDetector.detect();
				List<MarkerInfo> overLoggingList = loggingDetector.getOverLoggingList();
				List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_OVER_LOGGING);
				removeFromSuppress(overLoggingList, posList);
				setMethodNameAndIndex(overLoggingList, method.getName().toString(), methodIdx);
				addBadSmell(RLMarkerAttribute.CS_OVER_LOGGING, overLoggingList);
			}
			
			// Unprotected main
			if(detMethodSmell.get(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				UnprotectedMainProgramVisitor mainVisitor = new UnprotectedMainProgramVisitor(root);
				method.accept(mainVisitor);
				List<MarkerInfo> unprotectedMain = mainVisitor.getUnprotedMainList();
				setMethodNameAndIndex(unprotectedMain, method.getName().toString(), methodIdx);
				addBadSmell(RLMarkerAttribute.CS_UNPROTECTED_MAIN, unprotectedMain);
			}

			// Thrown exception in finally block
			if(isDetectingBadSmell.get(SmellSettings.SMELL_THROWNEXCEPTIONINFINALLYBLOCK)) {
				if(detMethodSmell.get(RLMarkerAttribute.CS_THROWN_EXCEPTION_IN_FINALLY_BLOCK)) {
					ThrownExceptionInFinallyBlockVisitor thrownInFinallyVisitor = new ThrownExceptionInFinallyBlockVisitor(root);
					method.accept(thrownInFinallyVisitor);
					List<MarkerInfo> throwInFinallyList = thrownInFinallyVisitor.getThrownInFinallyList();
					setMethodNameAndIndex(throwInFinallyList, method.getName().toString(), methodIdx);
					addBadSmell(RLMarkerAttribute.CS_THROWN_EXCEPTION_IN_FINALLY_BLOCK, throwInFinallyList);
				}
			}
		}
	}

	private void removeFromSuppress(List<MarkerInfo> markerInfos, List<Integer> posList) {
		List<MarkerInfo> nonSuppressSmell = new ArrayList<MarkerInfo>();
		for (MarkerInfo markerInfo : markerInfos) {
			if(!suppressMarker(posList, markerInfo.getPosition())) {
				nonSuppressSmell.add(markerInfo);
			}
		}
		markerInfos.clear();
		markerInfos.addAll(nonSuppressSmell);
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
	
	private void inputSuppressData(List<SSMessage> suppressSmellList,
		TreeMap<String, Boolean> detMethodSmell, TreeMap<String, List<Integer>> detCatchSmell) {
		for (String smellType : RLMarkerAttribute.CS_CATCH_TYPE)
			detCatchSmell.put(smellType, new ArrayList<Integer>());

		for (SSMessage msg : suppressSmellList) {
			if (!msg.isInCatch()) {
				for (String smellType : msg.getSmellList())
					detMethodSmell.put(smellType, false);
			} else {
				for (String smellType : msg.getSmellList()) {
					List<Integer> smellPosList = detCatchSmell.get(smellType);
					if (smellPosList != null)
						smellPosList.add(msg.getPosition());
				}
			}
		}
	}
	
	private boolean suppressMarker(List<Integer> smellPosList, int pos) {
		if(smellPosList != null) {
			for (Integer index : smellPosList)
				if (pos == index)
					return true;
		}
		return false;
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

	/**
	 * Load the SmellSettings file, and save if want to detect TEIFB and CC
	 * (Should save other bad smells in the future)
	 * @author pig
	 */
	private void initializeIsDetectingBadSmell() {
		SmellSettings smellSettings = new SmellSettings(
				UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		final String badSmellTEIFB = SmellSettings.SMELL_THROWNEXCEPTIONINFINALLYBLOCK;
		isDetectingBadSmell.put(badSmellTEIFB,
				new Boolean(smellSettings.isDetectingSmell(badSmellTEIFB)));

		final String badSmellCC = SmellSettings.SMELL_CARELESSCLEANUP;
		isDetectingBadSmell.put(badSmellCC,
				new Boolean(smellSettings.isDetectingSmell(badSmellCC)));
	}
}
