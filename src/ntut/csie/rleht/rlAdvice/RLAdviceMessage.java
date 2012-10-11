package ntut.csie.rleht.rlAdvice;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.robusta.agile.exception.RTag;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;


/**
 * 從會throw出exception的expressionStatement中，蒐集跟他相關的資訊
 * 本class參考了CSMessage的作法
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
	 * 拿來紀錄Method定義的RL等級及其對應的Exception class
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
	 * @return 此method所有的RL annotation。
	 */
	public RLInfo[] getRobustnessLevel(){
		RLInfo[] rlinfo = null;
		boolean isRobustnessAnnotationExist = false;
		//method可能有很多annotation
		for (IAnnotationBinding annotation : annotations) {
			//尋找Robustness的annotation
			if (annotation.getAnnotationType().getBinaryName().equals("ntut.csie.robusta.agile.exception.Robustness")) {
				IMemberValuePairBinding[] mvpb = annotation.getAllMemberValuePairs();
				//這裡有點醜，反正就是把robustness的annotation中，RL跟exception的資訊都存起來
				Object[] values = (Object[]) mvpb[0].getValue();
				rlinfo = new RLInfo[values.length];
				for(int j = 0; j<values.length; j++){
					IAnnotationBinding binding = (IAnnotationBinding) values[j];
					// 處理RL
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
//			System.out.println("RLAdviceMessage沒有Annotation，例外類型為: "+ getExceptionType());
		}
		return rlinfo;
	}
}
