package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class UnprotectedMainProgramVisitor extends ASTVisitor {
	// 儲存所找到的Unprotected main Program 
	private List<MarkerInfo> unprotectedMainList;	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
	private boolean isDetectingUnprotectedMainProgramSmell;
	
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
		if (node.resolveBinding().toString().contains("void main(java.lang.String[])")) {
			List<?> statement = node.getBody().statements();
			if(processMainFunction(statement)) {
				//如果有找到code smell就將其加入
				MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_UNPROTECTED_MAIN, null,											
										node.toString(),node.getStartPosition(),
										getLineNumber(node), null);
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
	private boolean processMainFunction(List<?> statement) {
		if (statement.size() == 0) {
			// main function裡面什麼都沒有就不算是code smell
			return false;
		} else if (statement.size() == 1) {
			if (((ASTNode)statement.get(0)).getNodeType() == ASTNode.TRY_STATEMENT) {
				List<?> catchList = ((TryStatement)statement.get(0)).catchClauses();
				for (int i = 0; i < catchList.size(); i++) {
					SingleVariableDeclaration svd = ((CatchClause)catchList.get(i)).getException();
					// 如果有try還要判斷catch是否為catch(Exception ..)
					if (svd.getType().resolveBinding().getQualifiedName().equals(Exception.class.getName()) ||
						svd.getType().resolveBinding().getQualifiedName().equals(RuntimeException.class.getName())) {
						//如果有catch(Exception ..)就不算code smell
						return false;
					}
				}
			}
			return true;
		} else {
			/* 如果Main Block有兩種以上的statement,就表示有東西沒被
			 * Try block包住,或者根本沒有try block
			 */
			return true;
		}
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
	public List<MarkerInfo> getUnprotedMainList(){
		return unprotectedMainList;
	}
}
