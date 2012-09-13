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
	 * �Ψ�copy�ª�sdd���
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
	 * �eSequence Diagram
	 * @param selectProject		��󪺱M��
	 * @param site				WorkPart
	 * @param items				Tree Item
	 * @param isShowCallerType	Caller / Callee
	 * @param isShowPackage		�O�_���Package
	 * @param isShowAllPackage	�O�_��ܩҦ���Package
	 * @param isTopDown			�O�_�q�W���U��
	 * @param packageCount		��ܪ�Package�Ӽ�
	 * @param isShowRL			�O�_���RL��T
	 * @param isShowPath		�O�_���Exception���W��
	 * @param isTraced			�O�_�wTrace RL��T
	 */
	public void draw(IProject selectProject, IWorkbenchPartSite site, TreeItem[] items,boolean isShowCallerType,
					 boolean isShowPackage, boolean isShowAllPackage, boolean isTopDown,
					 int packageCount, boolean isShowRL, boolean isShowPath, boolean isTraced) {
		// instanciate builder.
		RLSequenceModelBuilder builder = new RLSequenceModelBuilder();
	    List<SeqDiagramData> copyList = new ArrayList<SeqDiagramData>();
	    
		this.findSelectedItemPath(items, isTraced, isShowRL, isShowPath);

        /*------------------------------------------------------------------------*
        -  �z�LisShowCallerType�ӧP�_�O�ѤU���Wcall hierarchy
		        �p�G�J��o�ر���,�h�N���ǤϹL��,�åB��Level���
        *-------------------------------------------------------------------------*/
		if (isShowCallerType) {
			int count = 0;
			for (int i=seqdataList.size()-1; i >= 0; i--) {
				//���qArray�̫᭱�⪫��copy�i�h
				SeqDiagramData sdd = copySeqData(seqdataList.get(i)); 
				//�]�w�n���઺Level
				sdd.setLevel(seqdataList.get(count).getLevel());
				copyList.add(sdd);
				count++;
			}
			//��copy�᪺���Gassign
			seqdataList = copyList;
		}
		
		Map<String, InstanceModel> instanceModelMap = new HashMap<String, InstanceModel>();
		//InstanceModel�����OClass or Actor������
		InstanceModel start = builder.createActor("Debugger");

		for (SeqDiagramData sdd : seqdataList) {
			if (instanceModelMap.get(sdd.getClassName()) == null) {
				//�����O�n�Qcreate��class(sequence diagram�W��class���)
				
				//��SeqDiagram�]�w�����o�쪺�ѼƶǨ�e�Ϫ����@�h
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

		//�쥻Editor�i�ରnull point
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

			//�ɮפ�����
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
	 * �M��Hierarchy Tree���Ҧ���ܪ�Node
	 * @param items	   Hierarchy Tree Item
	 * @param isTraced �O�_�w�d�LRL��T
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
					
					//�O�_���RL��T
					if (isShowRL) {
						//�O�_�w�gTrace�LRL��T
						if (isTraced) {
							sdd.setRLAnnotations(item.getText(1));
							sdd.setExceptions(item.getText(2));
						//�Y�٥��N�h���oRL��T�C
						} else {
							getRLMessage(wrapper);
							sdd.setRLAnnotations(colRLInfo);
							sdd.setExceptions(colExInfo);
						}
					}
					seqdataList.add(sdd);
				}
			}

			//�Y��Child�A�h�~��Trace
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

				// �Noffset�����method���̫᭱�A�O�]���Y�����ѡA�hRL�|�����X�ӡA�h�ݭn����method��
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
		 * �O�_��ܸ��|
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
			//�Y���Path�εLRL Annotation
			if (isShowPath || annotations == "") {
				RLAnnotations = annotations;
			//�Y�����path
			} else {
				//�ѤU���r��
				String remainder = annotations;

				int index = remainder.indexOf("}");
				for (; index != -1; index = remainder.indexOf("}")) {
					//�q"{"��"}"�P"\n"
					RLAnnotations += removePath(remainder.substring(0, index+2));

					//���R��l�r��
					remainder = remainder.substring(index +2);
				}
			}
		}

		/**
		 * �R��Exception Name��Path
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
		 * ���oRobustnessLevel(�Y���Ƽƨ��̤p��Level)
		 * @return
		 */
		public int getRobustnessLevel() {
			if (RLAnnotations==null || RLAnnotations=="")
				return 0;

			//�̤p��Robustness Level
			int minLevel = 99;
			//Robustness Level�}�l����m
			int index = 0;
			//�ѤU���r��
			String remainder = RLAnnotations;
			for (index = remainder.indexOf("{ "); index != -1; index = remainder.indexOf("{ ")) {
				//�վ��RL�Ʀr���a��
				index += 2;
				String number = String.valueOf(remainder.substring(index).charAt(0));
				int level = Integer.valueOf(number);
				
				//����̤p��Robustness Level
				if (level < minLevel)
					minLevel = level;
				
				//���R��l�r��
				remainder = remainder.substring(index +1);
			}

			return minLevel;
		}
	}
}
