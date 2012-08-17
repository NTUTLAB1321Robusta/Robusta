package ntut.csie.rleht.rlAdvice;

import ntut.csie.csdet.data.MarkerInfo;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import agile.exception.RL;

/**
 * �q�|throw�Xexception��expressionStatement���A�`����L��������T
 * ��class�ѦҤFCSMessage���@�k
 * @author Charles
 * @version 0.0.1
 */
public class RLAdviceMessage {
	private MarkerInfo csmsg;
	
	private IAnnotationBinding[] annotations;
	
	private int catchClauseStartPosition;
	
	private int catchClauselineNumber;
	
	public RLAdviceMessage(String type, ITypeBinding typeBinding, String statement, 
			int methodInvocationStartPosition, int catchClauseStartPosition,
			int methodInvocationlineNumber,	int catchClauselineNumber, 
			String exceptionType, IAnnotationBinding[] annotations) {
		csmsg = new MarkerInfo(type, typeBinding, statement, methodInvocationStartPosition, 
				methodInvocationlineNumber,	exceptionType);
		this.catchClauselineNumber = catchClauselineNumber;
		this.catchClauseStartPosition = catchClauseStartPosition;
		
		this.annotations = annotations;
	}
	
	public String getCodeSmellType(){
		return csmsg.getCodeSmellType();
	}
	
	public int getMethodInvocationLineNumber(){
		return csmsg.getLineNumber();
	}
	
	public String getExceptionType() {
		return csmsg.getExceptionType();
	}

	public String getStatement() {
		return csmsg.getStatement();
	}
	
	public ITypeBinding getTypeBinding() {
		return csmsg.getTypeBinding();
	}
	
	public int getMethodInvocationStartPosition() {
		return csmsg.getPosition(); 
	}
	
	public int getCatchClauseStartPosition(){
		return catchClauseStartPosition;
	}
	
	public int getCatchClauseLineNumber(){
		return catchClauselineNumber;
	}
	
	/**
	 * ���Ӭ���Method�w�q��RL���ŤΨ������Exception class
	 * @author Charles
	 *
	 */
	public class RLInfo{
		private int level;
		private String exception;
		public void setLevel(int level){
			this.level = level;
		}
		public int getLevel(){
			return level;
		}
		public void setException(String ex){
			exception = ex;
		}
		public String getExString(){
			return exception;
		}
	}
	
	/**
	 * @return ��method�Ҧ���RL annotation�C
	 */
	public RLInfo[] getRobustnessLevel(){
		RLInfo[] rlinfo = null;
		boolean isRobustnessAnnotationExist = false;
		//method�i�঳�ܦhannotation
		for (IAnnotationBinding annotation : annotations) {
			//�M��Robustness��annotation
			if (annotation.getAnnotationType().getBinaryName().equals("agile.exception.Robustness")) {
				IMemberValuePairBinding[] mvpb = annotation.getAllMemberValuePairs();
				//�o�̦��I��A�ϥ��N�O��robustness��annotation���ARL��exception����T���s�_��
				Object[] values = (Object[]) mvpb[0].getValue();
				rlinfo = new RLInfo[values.length];
				for(int j = 0; j<values.length; j++){
					IAnnotationBinding binding = (IAnnotationBinding) values[j];
					// �B�zRL
					IMemberValuePairBinding[] rlMvpb = binding.getAllMemberValuePairs();
					if (rlMvpb.length == 2) {
						rlinfo[j] = new RLInfo();
						rlinfo[j].setLevel(Integer.parseInt(rlMvpb[0].getValue().toString()));
						ITypeBinding itb = (ITypeBinding)rlMvpb[1].getValue();
						rlinfo[j].setException(itb.getBinaryName());
					}
				}
				isRobustnessAnnotationExist = true;
				break;
			}
		}
		
		if(!isRobustnessAnnotationExist){
			rlinfo = new RLInfo[1];
			rlinfo[0] = new RLInfo();
			rlinfo[0].setLevel(RL.LEVEL_1_ERR_REPORTING);
			rlinfo[0].setException(getExceptionType());
//			System.out.println("RLAdviceMessage�S��Annotation�A�ҥ~������: "+ getExceptionType());
		}
		return rlinfo;
	}
}
