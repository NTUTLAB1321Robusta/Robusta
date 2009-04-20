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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallersSeqDiagram {
	private static Logger logger = LoggerFactory.getLogger(CallersSeqDiagram.class);
	private IEditorPart editor = null;

	
	/**
	 * 用來copy舊的sdd資料
	 * @param sdd
	 * @return
	 */
	private SeqDiagramData copySeqData(SeqDiagramData sdd){
		SeqDiagramData copy = new SeqDiagramData(); 
		copy.setClassName(sdd.getClassName());
		copy.setExceptions(sdd.getExceptions());
		copy.setMethodName(sdd.getMethodName());
		copy.setRLAnnotations(sdd.getRLAnnotations());
		return copy;
	}
	
	public void draw(IWorkbenchPartSite site, TreeItem[] items,boolean isShowCallerType,
					 boolean isPackage, boolean isShowAll, boolean isTopDown, int packageCount) {
		// instanciate builder.
		RLSequenceModelBuilder builder = new RLSequenceModelBuilder();
	    List<SeqDiagramData> copyList = new ArrayList<SeqDiagramData>();
	    
		this.findSelectedItemPath(items);

        /*------------------------------------------------------------------------*
        -  透過isShowCallerType來判斷是由下往上call hierarchy
		        如果遇到這種情形,則將順序反過來,並且把Level對調
        *-------------------------------------------------------------------------*/
		if(isShowCallerType){
			int count = 0;
			for(int i=seqdataList.size()-1;i>=0;i--){
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
				InstanceModel obj = builder.createInstance(sdd.getClassName(),isPackage,isShowAll,isTopDown,packageCount);
				
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
					mm = builder.createSelfCallMessage(sdd.getMethodName(), sdd.getRLAnnotations());
				} else {
					mm = builder.createMessage(sdd.getMethodName(), sdd.getRLAnnotations(), im);					
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

		IEditorPart editor = site.getPage().getActiveEditor();
		//TODO editor可能為null point
		IEditorInput input = editor.getEditorInput();
		try {
			if (input instanceof IFileEditorInput) {
				String fn = "seq.sqd";
				IFile file = ((IFileEditorInput) input).getFile();

				IProject project = file.getProject();

				IFolder folder = project.getFolder("SEQ_DIAGRAM");
				file = folder.getFile(fn);
				// at this point, no resources have been created
				if (!project.exists())
					project.create(null);
				if (!project.isOpen())
					project.open(null);
				if (!folder.exists())
					folder.create(IResource.NONE, true, null);

				if (file.exists()) {
					if (editor.getTitle().equals(fn)) {
						site.getPage().closeEditor(editor, true);
					}

					try {
						//TODO 刪除SD時會有ResourceExcpetion,原因是有東西鎖住檔案
						//若採用沒有被版本控管的專案,則不會有此問題發生
//						file.refreshLocal(IResource.DEPTH_INFINITE, null);
						file.delete(true,null);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					
				}

				if (!file.exists()) {
					// logger.debug("<seq diagram xml>="+builder.toXML());
					byte[] bytes = builder.toXML().getBytes();
					InputStream source = new ByteArrayInputStream(bytes);
					file.create(source, IResource.NONE, null);
				}

				// Open Editor
				IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
				editor = site.getPage().openEditor(new FileEditorInput(file), desc.getId());

			}
		} catch (CoreException ex) {
			logger.error("[handleGenSeqDiagram]", ex);
			ex.printStackTrace();
		}

	}

	// private List<String> instanceMap=new ArrayList<String>();

	private List<SeqDiagramData> seqdataList = new ArrayList<SeqDiagramData>();

	@SuppressWarnings("restriction")
	private void findSelectedItemPath(TreeItem[] items) {
		//logger.debug("\t---->findSelectedItemPath BEGIN");
		for (int i = 0, size = items.length; i < size; i++) {
			TreeItem item = items[i];
			if (item.getChecked()) {
				MethodWrapper wrapper = (MethodWrapper) item.getData();

				if (wrapper.getMember() instanceof IMethod) {
					IMethod method = (IMethod) wrapper.getMember();
					IType type = method.getDeclaringType();

					SeqDiagramData sdd = new SeqDiagramData();
					sdd.setClassName(type.getFullyQualifiedName());
					sdd.setMethodName(method.getElementName());
					sdd.setLevel(wrapper.getLevel());
					sdd.setRLAnnotations(item.getText(1));
					sdd.setExceptions(item.getText(2));
					seqdataList.add(sdd);
				}
			}
			
			if (item.getItemCount() >= 1) {
				findSelectedItemPath(item.getItems());
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
		private int level;
		private String methodName;
		private String className;
		private String exceptions;
		private String RLAnnotations;

		public int getLevel() {
			return level;
		}

		public void setLevel(int level) {
			this.level = level;
		}

		public String getMethodName() {
			return methodName;
		}

		public void setMethodName(String methodName) {
			this.methodName = methodName;
		}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public String getExceptions() {
			return exceptions;
		}

		public void setExceptions(String exceptions) {
			this.exceptions = exceptions;
		}

		public String getRLAnnotations() {
			return RLAnnotations;
		}

		public void setRLAnnotations(String annotations) {
			RLAnnotations = annotations;
		}

	}

}
