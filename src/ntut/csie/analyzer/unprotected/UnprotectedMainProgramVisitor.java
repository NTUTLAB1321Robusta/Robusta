package ntut.csie.analyzer.unprotected;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.marker.AnnotationInfo;
import ntut.csie.util.AbstractBadSmellVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class UnprotectedMainProgramVisitor extends AbstractBadSmellVisitor {
	// 儲存所找到的Unprotected main Program 
	private List<MarkerInfo> unprotectedMainList;	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
	private boolean isDetectingUnprotectedMainProgramSmell;
	ArrayList<AnnotationInfo> annotationList = new ArrayList<AnnotationInfo>(32);
	
	public UnprotectedMainProgramVisitor(CompilationUnit root){
		this.root = root;
		unprotectedMainList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingUnprotectedMainProgramSmell = smellSettings
				.isDetectingSmell(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM);
	}
	
	/**
	 * 先根據設定檔的資訊，決定要不要繼續拜訪，
	 * 再尋找main function
	 */
	public boolean visit(MethodDeclaration node) {
		if(!isDetectingUnprotectedMainProgramSmell)
			return false;
		// parse AST tree看看是否有void main(java.lang.String[])
		if(node == null)
			return false;
		if(node.resolveBinding() == null)
			return false;
		if (node.resolveBinding().toString().contains("void main(java.lang.String[])")) {
			List<?> statements = node.getBody().statements();
			if(containUnprotectedStatement(statements)) {
				//如果有找到code smell就將其加入
				MarkerInfo markerInfo = new MarkerInfo(
						RLMarkerAttribute.CS_UNPROTECTED_MAIN, 
						null,
						((CompilationUnit)node.getRoot()).getJavaElement().getElementName(), // class name
						node.toString(),
						node.getStartPosition(),
						getLineNumber(node), 
						null,
						annotationList);
				unprotectedMainList.add(markerInfo);				
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 檢查main function中是否有code smell
	 * @param statement
	 * @return
	 */
	private boolean containUnprotectedStatement(List<?> statement) {
		/* 如果Main Block有statement沒被擁有catch(Exception e) 或 catch(Throwable t)
		 * 的try statement包住, 就是Unprotected Main
		 */
		int unprotectedStatementCount = 0;
		for(Object s: statement) {
			ASTNode node = (ASTNode) s;
			
			if(node.getNodeType() == ASTNode.TRY_STATEMENT) {
				if(doesCatchesAllException((TryStatement) node))
					continue;
			}
			
			AnnotationInfo ai = new AnnotationInfo(root.getLineNumber(node.getStartPosition()), 
					node.getStartPosition(), 
					node.getLength(), 
					"Not All statements In Main Enclosed In A Try Statement Catching All Possible Exceptions");
			annotationList.add(ai);
			unprotectedStatementCount++;
		}
		
		return unprotectedStatementCount != 0? true : false;
	}

	private boolean doesCatchesAllException(TryStatement tryStatement) {
		List<CatchClause> catchClauseList = tryStatement.catchClauses();
		for (CatchClause catchClause : catchClauseList) {
			if (catchClause.getException().getType().toString().equals("Exception")
					|| catchClause.getException().getType().toString().equals("Throwable")) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * 根據startPosition來取得行數
	 */
	private int getLineNumber(MethodDeclaration method) {
		int position = method.getStartPosition();
		List<?> modifiers = method.modifiers();
		for (int i = 0, size = modifiers.size(); i < size; i++) {
			// 如果原本main function上有annotation的話,marker會變成標在annotation那行
			// 所以透過尋找public那行的位置,來取得marker要標示的行數
			if ((!((IExtendedModifier)modifiers.get(i)).isAnnotation()) && (modifiers.get(i).toString().contains("public"))) {
				position = ((ASTNode)modifiers.get(i)).getStartPosition();
				break;
			}
		}
		//如果沒有annotation,就可以直接取得main function那行
		return root.getLineNumber(position);
	}

	/**
	 * 取得unprotected Main的清單
	 */
	public List<MarkerInfo> getUnprotectedMainList(){
		return unprotectedMainList;
	}

	@Override
	public List<MarkerInfo> getBadSmellCollected() {
		return getUnprotectedMainList();
	}
}
