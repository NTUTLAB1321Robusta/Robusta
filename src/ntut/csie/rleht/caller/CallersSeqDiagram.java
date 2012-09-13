package ntut.csie.rleht.caller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.java.amateras.uml.sequencediagram.model.InstanceModel;
import net.java.amateras.uml.sequencediagram.model.MessageModel;
import net.java.amateras.uml.sequencediagram.model.RLSequenceModelBuilder;
import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.rleht.views.RLMethodModel;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallersSeqDiagram {
	private static Logger logger = LoggerFactory.getLogger(CallersSeqDiagram.class);
	
	private List<SeqDiagramData> seqdataList = new ArrayList<SeqDiagramData>();
	
	/**
	 * 用來copy舊的sdd資料
	 * @param sdd
	 * @return
	 */
	private SeqDiagramData copySeqData(SeqDiagramData sdd){
		SeqDiagramData copy = new SeqDiagramData(sdd.isShowPath); 
		copy.setClassName(sdd.getClassName());
		copy.setExceptions(sdd.getExceptions());
		copy.setMethodName(sdd.getMethodName());
		copy.setRLAnnotations(sdd.getRLAnnotations());
		return copy;
	}
	
	/**
	 * 畫Sequence Diagram
	 * @param selectProject		位於的專案
	 * @param site				WorkPart
	 * @param items				Tree Item
	 * @param isShowCallerType	Caller / Callee
	 * @param isShowPackage		是否顯示Package
	 * @param isShowAllPackage	是否顯示所有的Package
	 * @param isTopDown			是否從上往下數
	 * @param packageCount		顯示的Package個數
	 * @param isShowRL			是否顯示RL資訊
	 * @param isShowPath		是否顯示Exception的名稱
	 * @param isTraced			是否已Trace RL資訊
	 */
	public void draw(IProject selectProject, IWorkbenchPartSite site, TreeItem[] items,boolean isShowCallerType,
					 boolean isShowPackage, boolean isShowAllPackage, boolean isTopDown,
					 int packageCount, boolean isShowRL, boolean isShowPath, boolean isTraced) {
		// instanciate builder.
		RLSequenceModelBuilder builder = new RLSequenceModelBuilder();
	    List<SeqDiagramData> copyList = new ArrayList<SeqDiagramData>();
	    
		this.findSelectedItemPath(items, isTraced, isShowRL, isShowPath);

        /*------------------------------------------------------------------------*
        -  透過isShowCallerType來判斷是由下往上call hierarchy
		        如果遇到這種情形,則將順序反過來,並且把Level對調
        *-------------------------------------------------------------------------*/
		if (isShowCallerType) {
			int count = 0;
			for (int i=seqdataList.size()-1; i >= 0; i--) {
				//先從Array最後面把物件copy進去
				SeqDiagramData sdd = copySeqData(seqdataList.get(i)); 
				//設定要反轉的Level
				sdd.setLevel(seqdataList.get(count).getLevel());
				copyList.add(sdd);
				count++;
			}
			//把copy後的結果assign
			seqdataList = copyList;
		}
		
		Map<String, InstanceModel> instanceModelMap = new HashMap<String, InstanceModel>();
		//InstanceModel指的是Class or Actor之類的
		InstanceModel start = builder.createActor("Debugger");

		for (SeqDiagramData sdd : seqdataList) {
			if (instanceModelMap.get(sdd.getClassName()) == null) {
				//指的是要被create的class(sequence diagram上的class方塊)
				
				//把SeqDiagram設定視窗得到的參數傳到畫圖的那一層
				InstanceModel obj = builder.createInstance(sdd.getClassName(),isShowPackage,isShowAllPackage,isTopDown,packageCount);
				
				if (start == null) {
					start = obj;
				}
				instanceModelMap.put(sdd.getClassName(), obj);
			}
		}
		
		if (start == null) {
			return;
		}

		builder.init(start);
		// MessageModel msg = builder.createMessage("Message to Next", next);
		Stack<MessageModel> stackMsgModel = new Stack<MessageModel>();
		Stack<InstanceModel> stackInstance = new Stack<InstanceModel>();
		Stack<Integer> stackLevel = new Stack<Integer>();

		try {
			InstanceModel imLast = null;
			for (int i = 0, size = seqdataList.size(); i < size; i++) {
				SeqDiagramData sdd = (SeqDiagramData) seqdataList.get(i);

				InstanceModel im = instanceModelMap.get(sdd.getClassName());
				if (i > 0 && sdd.getLevel() <= stackLevel.peek().intValue()) {

					while (stackLevel.size() > 0) {
						if (sdd.getLevel() >= stackLevel.peek().intValue()) {
							if (stackMsgModel.size() > 1) {
								builder.back(stackMsgModel.peek());
								InstanceModel tim=stackInstance.pop();
								imLast=stackInstance.peek();	
								stackInstance.push(tim);
								logger.debug("\t " + sdd.getMethodName() + " peek(+)  >> size=" + stackInstance.size() + ": LastIM="+imLast.getName().replace('\n',' '));
							}
							break;
						}

						stackLevel.pop();
						InstanceModel im2 = stackInstance.pop();						
						builder.back(stackMsgModel.pop());
						
						imLast=im2;
						logger.debug("\t " + sdd.getMethodName() + " pop(+) " + im2.getName() + " >> size=" + stackInstance.size() + ": LastIM="+imLast.getName().replace('\n',' '));

					}

				}
				
				stackInstance.push(im);
				stackLevel.push(new Integer(sdd.getLevel()));

				MessageModel mm = null;
				if (imLast != null && imLast.getName().equals(im.getName())) {
					mm = builder.createSelfCallMessage(sdd.getMethodName(), sdd.getRLAnnotations(), sdd.getRobustnessLevel());
				} else {
					mm = builder.createMessage(sdd.getMethodName(), sdd.getRLAnnotations(), im, sdd.getRobustnessLevel());
				}
				
				stackMsgModel.push(mm);
				
				imLast = im;				
				logger.debug("\t " + sdd.getMethodName() + " push(+) " + im.getName() + " >> size=" + stackInstance.size() + ": LastIM="+imLast.getName().replace('\n',' '));

			}
		} catch (Exception e) {
			logger.error("", e);
		}
		// // create instances
		// InstanceModel start = builder.createInstance("Start");
		// InstanceModel next = builder.createInstance("Next");
		// InstanceModel last = builder.createInstance("Last");
		// InstanceModel instanciated = builder.createInstance("NewObject");
		//
		// // ready to create message
		// builder.init(start);
		//
		// // create message from Start to Next
		// MessageModel msg = builder.createMessage("Message to Next", next);
		//
		// // create message from Next to Last
		// builder.createCreationMessage("Message to Last", last);
		//
		// // create message from Last to Start
		// builder.createMessage("Return to Start", start);
		//
		// // create self-call message on Start
		// builder.createSelfCallMessage("Self-Call");
		//
		// // back to pointer
		// builder.back(msg);
		//
		// // create message from Start to Last on next to first message.
		// builder.createMessage("Message to Last", last);
		//
		// // add creation message.
		// builder.createCreationMessage("create", instanciated);

		// convert to xml
		// logger.debug("[handleGenSeqDiagram]" + builder.toXML());

		//原本Editor可能為null point
		//IEditorPart editor = site.getPage().getActiveEditor();
		//IEditorInput input = editor.getEditorInput();
		try {
			String headName = "seq";
			String tailName = ".sqd";

			IFolder folder = selectProject.getFolder("SEQ_DIAGRAM");
			// at this point, no resources have been created
			if (!selectProject.exists())
				selectProject.create(null);
			if (!selectProject.isOpen())
				selectProject.open(null);
			if (!folder.exists())
				folder.create(IResource.NONE, true, null);

			//檔案不重複
			IFile file;
			for (int i=1; true; i++) {
				file = folder.getFile(headName + i + tailName);
				if (!file.exists())
					break;
			}

			if (!file.exists()) {
				byte[] bytes = builder.toXML().getBytes();
				InputStream source = new ByteArrayInputStream(bytes);
				file.create(source, IResource.NONE, null);
			}

			// Open Editor
			IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
			site.getPage().openEditor(new FileEditorInput(file), desc.getId());
		} catch (CoreException ex) {
			logger.error("[handleGenSeqDiagram]", ex);
			ex.printStackTrace();
		}

	}

	/**
	 * 尋找Hierarchy Tree中所有選擇的Node
	 * @param items	   Hierarchy Tree Item
	 * @param isTraced 是否已查過RL資訊
	 * @param isShowRL 
	 * @param isShowPath 
	 */
	@SuppressWarnings("restriction")
	private void findSelectedItemPath(TreeItem[] items, boolean isTraced, boolean isShowRL, boolean isShowPath) {
		for (int i = 0, size = items.length; i < size; i++) {
			TreeItem item = items[i];
			if (item.getChecked()) {
				MethodWrapper wrapper = (MethodWrapper) item.getData();

				if (wrapper.getMember() instanceof IMethod) {
					
					IMethod method = (IMethod) wrapper.getMember();
					IType type = method.getDeclaringType();

					SeqDiagramData sdd = new SeqDiagramData(isShowPath);
					sdd.setClassName(type.getFullyQualifiedName());
					sdd.setMethodName(method.getElementName());
					sdd.setLevel(wrapper.getLevel());
					
					//是否顯示RL資訊
					if (isShowRL) {
						//是否已經Trace過RL資訊
						if (isTraced) {
							sdd.setRLAnnotations(item.getText(1));
							sdd.setExceptions(item.getText(2));
						//若還未就去取得RL資訊。
						} else {
							getRLMessage(wrapper);
							sdd.setRLAnnotations(colRLInfo);
							sdd.setExceptions(colExInfo);
						}
					}
					seqdataList.add(sdd);
				}
			}

			//若有Child，則繼續Trace
			if (item.getItemCount() >= 1) {
				findSelectedItemPath(item.getItems(), isTraced, isShowRL, isShowPath);
			}
		}
	}
	
	private String colRLInfo = "";
	private String colExInfo = "";
	private void getRLMessage(MethodWrapper wrapper) {
		this.colExInfo = "";
		this.colRLInfo = "";
		if (wrapper != null) {
			RLMethodModel model = new RLMethodModel();
			try {
				IOpenable input = wrapper.getMember().getOpenable();
				int offset = wrapper.getMember().getSourceRange().getOffset();
				int length = wrapper.getMember().getSourceRange().getLength();

				// 將offset取到該method的最後面，是因為若有註解，則RL會取不出來，則需要指到method內
				offset = offset + length - 10;
				length = 0;

				if (!model.createAST(input, offset)) {
					RLEHTPlugin.logError("AST could not be created." + input,
							null);
				} else {

					model.parseDocument(offset, length);

					List<RLMessage> rlmsgs = model.getRLAnnotationList();

					if (rlmsgs != null) {
						for (RLMessage rlmsg : rlmsgs) {
							this.colRLInfo += ("{ "
									+ rlmsg.getRLData().getLevel() + " , "
									+ rlmsg.getRLData().getExceptionType() + " } ");
						}
						rlmsgs.clear();

					} else {
						this.colRLInfo = "NULL";
					}

					rlmsgs = model.getExceptionList();
					if (rlmsgs != null) {
						for (RLMessage rlmsg : rlmsgs) {
							if (rlmsg.getRLData().getLevel() < 0) {
								continue;
							}
							if (rlmsg.isHandleByCatch()) {
								continue;
							}
							if (this.colExInfo.indexOf(rlmsg.getRLData()
									.getExceptionType()) == -1) {
								this.colExInfo += (rlmsg.getRLData()
										.getExceptionType() + ", ");
							}
						}
						rlmsgs.clear();
					} else {
						this.colExInfo = "NULL";
					}

				}
			} catch (Exception ex) {
				logger.error("[getRLMessage] Error!", ex);
				RLEHTPlugin.logError(
						"[CallersLabelProvider][getRLMessage] Error!", null);
				this.colRLInfo = "ERROR";
			} finally {
				if (model != null) {
					model.clear();
				}

			}
		}
	}

	private class SeqMessageModelData {
		private MessageModel messageModel = null;
		private SeqDiagramData seqDiagramData = null;
		private InstanceModel parentInstanceModel = null;

		public MessageModel getMessageModel() {
			return messageModel;
		}

		public void setMessageModel(MessageModel messageModel) {
			this.messageModel = messageModel;
		}

		public SeqDiagramData getSeqDiagramData() {
			return seqDiagramData;
		}

		public void setSeqDiagramData(SeqDiagramData seqDiagramData) {
			this.seqDiagramData = seqDiagramData;
		}

		public InstanceModel getParentInstanceModel() {
			return parentInstanceModel;
		}

		public void setParentInstanceModel(InstanceModel parentInstanceModel) {
			this.parentInstanceModel = parentInstanceModel;
		}

	}

	private class SeqDiagramData {
		private boolean isShowPath;
		
		private int level;
		private String methodName;
		private String className;
		private String exceptions;
		private String RLAnnotations = "";

		public SeqDiagramData(boolean isShowPath) {
			this.isShowPath = isShowPath;
		}
		
		/**
		 * 是否顯示路徑
		 */
		public boolean isShowPath() {
			return isShowPath;
		}
		/**
		 * Robustness Level
		 */
		public int getLevel() {
			return level;
		}
		public void setLevel(int level) {
			this.level = level;
		}
		/**
		 * Method Name
		 */
		public String getMethodName() {
			return methodName;
		}
		public void setMethodName(String methodName) {
			this.methodName = methodName;
		}
		/**
		 * Class Name
		 */
		public String getClassName() {
			return className;
		}
		public void setClassName(String className) {
			this.className = className;
		}
		/**
		 * Exception Names
		 */
		public String getExceptions() {
			return exceptions;
		}
		public void setExceptions(String exceptions) {
			this.exceptions = exceptions;
		}
		/**
		 * Tag Annotation
		 */
		public String getRLAnnotations() {
			return RLAnnotations;
		}
		public void setRLAnnotations(String annotations) {
			//若顯示Path或無RL Annotation
			if (isShowPath || annotations == "") {
				RLAnnotations = annotations;
			//若不顯示path
			} else {
				//剩下的字串
				String remainder = annotations;

				int index = remainder.indexOf("}");
				for (; index != -1; index = remainder.indexOf("}")) {
					//從"{"到"}"與"\n"
					RLAnnotations += removePath(remainder.substring(0, index+2));

					//分析其餘字串
					remainder = remainder.substring(index +2);
				}
			}
		}

		/**
		 * 刪掉Exception Name的Path
		 * @param rlInfo
		 */
		private String removePath(String rlInfo) {			
			int startIndex = -1;
			int endIndex = rlInfo.lastIndexOf(".");

			for (int i = endIndex; i >= 0; i--) {
				if (rlInfo.charAt(i) == ' ') {
					startIndex = i;
					return rlInfo.substring(0, startIndex) + " " + rlInfo.substring(endIndex +1);
				}
			}

			return rlInfo;
		}
		
		/**
		 * 取得RobustnessLevel(若為複數取最小的Level)
		 * @return
		 */
		public int getRobustnessLevel() {
			if (RLAnnotations==null || RLAnnotations=="")
				return 0;

			//最小的Robustness Level
			int minLevel = 99;
			//Robustness Level開始的位置
			int index = 0;
			//剩下的字串
			String remainder = RLAnnotations;
			for (index = remainder.indexOf("{ "); index != -1; index = remainder.indexOf("{ ")) {
				//調整到RL數字的地方
				index += 2;
				String number = String.valueOf(remainder.substring(index).charAt(0));
				int level = Integer.valueOf(number);
				
				//比較最小的Robustness Level
				if (level < minLevel)
					minLevel = level;
				
				//分析其餘字串
				remainder = remainder.substring(index +1);
			}

			return minLevel;
		}
	}
}
