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
	// �x�s�ҧ�쪺Unprotected main Program 
	private List<MarkerInfo> unprotectedMainList;	
	// AST tree��root(�ɮצW��)
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
	 * �M��main function
	 */
	public boolean visit(MethodDeclaration node) {
		if(!isDetectingUnprotectedMainProgramSmell)
			return false;
		// parse AST tree�ݬݬO�_��void main(java.lang.String[])
		if (node.resolveBinding().toString().contains("void main(java.lang.String[])")) {
			List<?> statement = node.getBody().statements();
			if(processMainFunction(statement)) {
				//�p�G�����code smell�N�N��[�J
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
	 * �ˬdmain function���O�_��code smell
	 * @param statement
	 * @return
	 */
	private boolean processMainFunction(List<?> statement) {
		if (statement.size() == 0) {
			// main function�̭����򳣨S���N����Ocode smell
			return false;
		} else if (statement.size() == 1) {
			if (((ASTNode)statement.get(0)).getNodeType() == ASTNode.TRY_STATEMENT) {
				List<?> catchList = ((TryStatement)statement.get(0)).catchClauses();
				for (int i = 0; i < catchList.size(); i++) {
					SingleVariableDeclaration svd = ((CatchClause)catchList.get(i)).getException();
					// �p�G��try�٭n�P�_catch�O�_��catch(Exception ..)
					if (svd.getType().toString().equals("Exception")) {
						//�p�G��catch(Exception ..)�N����code smell
						return false;
					}
				}
			}
			return true;
		} else {
			/* �p�GMain Block����إH�W��statement,�N��ܦ��F��S�Q
			 * Try block�]��,�Ϊ̮ڥ��S��try block
			 */
			return true;
		}
	}

	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(MethodDeclaration method) {
		int position = method.getStartPosition();
		List<?> modifiers = method.modifiers();
		for (int i = 0, size = modifiers.size(); i < size; i++) {
			// �p�G�쥻main function�W��annotation����,marker�|�ܦ��Цbannotation����
			// �ҥH�z�L�M��public���檺��m,�Ө��omarker�n�Хܪ����
			if ((!((IExtendedModifier)modifiers.get(i)).isAnnotation()) && (modifiers.get(i).toString().contains("public"))) {
				position = ((ASTNode)modifiers.get(i)).getStartPosition();
				break;
			}
		}
		//�p�G�S��annotation,�N�i�H�������omain function����
		return root.getLineNumber(position);
	}

	/**
	 * ���ounprotected Main���M��
	 */
	public List<MarkerInfo> getUnprotedMainList(){
		return unprotectedMainList;
	}
}
