package robusta.specificNodeCounter;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * 偵測指定專案，逐一走訪每個 java 檔，計算指定 ASTNode 的個數
 * 需要「CompilationUnitMaker」的協助，目前無用
 * @author pig
 */
public class SpecificNodeCounter {

	int specificNodeIndex = ASTNode.TRY_STATEMENT;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
