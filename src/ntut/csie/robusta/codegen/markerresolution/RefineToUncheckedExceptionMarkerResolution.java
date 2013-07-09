package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.refactoring.RefineExceptionRefactoring;
import ntut.csie.robusta.codegen.refactoringui.CodeSmellRefactoringWizard;
import ntut.csie.robusta.codegen.refactoringui.RethrowExInputPage;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * �bMarker�W����Quick Fix���[�JRefactoring(Rethrow Unchecked Exception)���\��
 * @author chewei
 */

public class RefineToUncheckedExceptionMarkerResolution implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(RefineToUncheckedExceptionMarkerResolution.class);
	private String label;
	
	public RefineToUncheckedExceptionMarkerResolution(String label){
		this.label = label;
	}	
	
	@Override
	public String getLabel() {	
		return label;
	}

	@Override
	public void run(IMarker marker) {
		//�ϥΪ��I��ignore ex �Ϊ�dummy handler��marker��,�|�h��M������Refactor��k
		String problem;
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			/*
			 * ���bQuickFixer�̭��A�C��warning���w�g�M�w�|���W����resolution�A
			 * �o�̨�ꤣ�ίS�a�A���@���a���D�������P�_�~��A���D�C���a���D���|�]�i�ӡC
			 * ���ڭ��~��դU�h�C
			 */
			if (((problem == null) || (!problem.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK))) &&
				((problem == null) || (!problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)))){
				return;
			}

			// �إ߾ާ@Refactor������,�ñNmarker�Ƕi�h�H�Q������ocode smell������T
			RefineExceptionRefactoring refactoring = new RefineExceptionRefactoring(marker);

			/*
			 * 1. �Ұ�Refactor dialog (�ϥΪ��٬O�i�H�n�D�ߥX Checked Exception�A�o���OBug)
			 * 2. �ϥΪ̩ߥX�Y��Unchecked Exception�|���ѡA�i���]�O���Ǩҥ~�������غc�l�S��throwable
			 */
			CodeSmellRefactoringWizard csRefactoringWizard = new CodeSmellRefactoringWizard(refactoring);
			csRefactoringWizard.setUserInputPage(new RethrowExInputPage("Rethrow Unchecked Exception"));
			csRefactoringWizard.setDefaultPageTitle("Refine to Unchecked Exception");
			RefactoringWizardOpenOperation operation = 
				new RefactoringWizardOpenOperation(csRefactoringWizard);
			operation.run(new Shell(), "Rethrow Unchecked Exception");

//			//�YAnnotation���Ǥ���A�h�洫���ǡC�̫�A�w��
//			refactoring.changeAnnotation();
			
		} catch (CoreException e) {
			logger.error("");
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			logger.error("");
			throw new RuntimeException(e);
		}
	}
}
