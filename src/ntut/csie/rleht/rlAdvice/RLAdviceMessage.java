package ntut.csie.rleht.rlAdvice;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.robusta.agile.exception.RTag;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;


/**
 * collect information from expression statement which may throw exception
 * this class consult the practice of CSMessage
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
	 * RLinfo is used to record the Robustness level and corresponding exception class of method which has been defined
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
	 * @return all RL annotation of method 
	 */
	public RLInfo[] getRobustnessLevel(){
		RLInfo[] rlinfo = null;
		boolean isRobustnessAnnotationExist = false;
		for (IAnnotationBinding annotation : annotations) {
			if (annotation.getAnnotationType().getBinaryName().equals("ntut.csie.robusta.agile.exception.Robustness")) {
				IMemberValuePairBinding[] mvpb = annotation.getAllMemberValuePairs();
				//save robustness level and exception information inside the robustness annotation 
				Object[] values = (Object[]) mvpb[0].getValue();
				rlinfo = new RLInfo[values.length];
				for(int j = 0; j<values.length; j++){
					IAnnotationBinding binding = (IAnnotationBinding) values[j];
					// access robustness level
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
			rlinfo[0].setLevel(RTag.LEVEL_1_ERR_REPORTING);
			rlinfo[0].setException(getExceptionType());
		}
		return rlinfo;
	}
}
