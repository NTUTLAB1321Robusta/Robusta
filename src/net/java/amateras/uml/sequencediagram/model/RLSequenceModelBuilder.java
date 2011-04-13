package net.java.amateras.uml.sequencediagram.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.java.amateras.uml.model.NoteModel;
import net.java.amateras.xstream.XStreamSerializer;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.RGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLSequenceModelBuilder {
	private static Logger logger = LoggerFactory.getLogger(RLSequenceModelBuilder.class);

	private InteractionModel root = new InteractionModel();

	private ActivationModel current;

	private Stack<MessageModel> messageStack = new Stack<MessageModel>();

	private Map<String, MessageModel> messageMap = new HashMap<String, MessageModel>();

	//�O��Note Message
	private Stack<NoteModel> noteStack = new Stack<NoteModel>();
	//�O��Note Message�P�򥦰t�諸Instance
	private Map<NoteModel, InstanceModel > noteMap = new HashMap<NoteModel, InstanceModel>();
	//�{�b��Y�b��m
	private int currentY = 0;
	//Instance���̤p�e��
	private int MIN_WIDTH = 150;
	//��r���e��
	private int FONT_WIDTH = 7;
	
	public int picPostion = MIN_WIDTH + 20;

	ArrayList<Integer> picsWidth = new ArrayList<Integer>();

	int picCounter = 0;

	// ���Ѫ���r�j�p
	private final float FONT_SIZE = 6.5f;

	public RLSequenceModelBuilder() {
		root.setShowIcon(true);
	}

	/**
	 * ����Instance
	 * @param instanceName	Instance�W��
	 * @param isPackage		�O�_���Package
	 * @param isShowAll		�O�_��ܥ�����Package
	 * @param isTopDown		true:�ѤW��U��	false:�ѤU���W��
	 * @param packageCount	�����Package�Ӽ�
	 * @return
	 */
	public InstanceModel createInstance(String instanceName, boolean isPackage, boolean isShowAll, boolean isTopDown, int packageCount)
	{
		//class�W�٪�����e��
		int width;
		InstanceModel model = new InstanceModel();
		//�bpackage+class�r��̧��class�W�٪��_�l��m
		int pos = instanceName.lastIndexOf(".");

		//�Y�����package�άO���0�h��package��package�L"."
		if (!isPackage || (!isShowAll && packageCount == 0) || pos == -1)
		{
			//�YinstanceName�̨S��"."�������ܥX�ӡA�Y���h��package�W�ٵ��M��
			instanceName = (pos != -1 ? instanceName.substring(pos + 1) : instanceName);

			model.setName(instanceName);
			
			//�L�p�N�⥦�լ��w�]
			if (instanceName.length()*FONT_WIDTH <MIN_WIDTH)
				width = MIN_WIDTH;
			else
				width = pos*FONT_WIDTH;
		}
		//���package��
		else
		{
			//�Y��ܥ�����package
			if (isShowAll)
				//�YinstanceName�̨S��"."�������ܥX�ӡA�Y���h��class�W�ٲ���U�@��
				instanceName = (pos != -1 ? instanceName.substring(0, pos + 1) + "\n" + instanceName.substring(pos + 1) : instanceName);
			//�Y�㦡�覡�O�ѤW��U
			else if (isTopDown)
			{
				//package�᪺"."����m
				int docPos = 0;
				//�����w��package�ƫ᪺"."����m(�Y�W�L�h�γ̫�@��package��"."����m)
				for (int i=0;instanceName.indexOf(".",docPos+1)!=-1 && i<packageCount;i++)
					docPos = instanceName.indexOf(".",docPos+1);
				//��W�L���package�ƥت��d��package�W�ٵ��M��
				instanceName = instanceName.substring(0,docPos+1) + "\n" + instanceName.substring(pos+1);
				//���s��@��class�W�٪���m
				pos = instanceName.lastIndexOf(".");
			}
			//�Y��ܤ覡�O�ѤU���W
			else
			{
				//class�e��"."����m
				int docPos = pos;
				//�����w��package�ƫ᪺"."����m
				int i=0;
				for (;instanceName.lastIndexOf(".",docPos-1)!=-1 && i<packageCount;i++)
					docPos = instanceName.lastIndexOf(".",docPos-1);
				//�Y�W�L�h�����
				if (i<packageCount)
					instanceName = instanceName.substring(0, pos + 1) + "\n" + instanceName.substring(pos + 1);
				else
					//��W�L���package�ƥت��d��package�W�ٵ��M��
					instanceName = instanceName.substring(docPos+1,pos+1) + "\n" + instanceName.substring(pos+1);
				//���s��@��class�W�٪���m
				pos = instanceName.lastIndexOf(".");
			}
			model.setName(instanceName);
			
			//�P�_�O�W�b���W����j�٬O�U�b��
			if (instanceName.length()/2 <= pos)			//�W�b�����j
				width = pos*FONT_WIDTH;
			else
				width = (instanceName.length()-pos)*7;	//�U�b�����j
			//�L�p�N�⥦�լ��w�]
			if (width < MIN_WIDTH)
				width = MIN_WIDTH;
		}
		// root.getInstances().get(root).getConstraint().getRight().getPosition(root.getConstraint().getRight());

		//Debugger��Ĥ@��method����m
		model.setConstraint(new Rectangle(picPostion + 20, InstanceModel.DEFAULT_LOCATION, width , -1));

		//�x�s�_�ӵ����U�e����Ϫ��ѷ�
		picsWidth.add(width/2-5);
		//�O����m���U�ӹϨϥ�
		picPostion += width+20;

		//model.setConstraint(new Rectangle((MAX_WIDTH + 20) * root.getInstances().size() + 20, InstanceModel.DEFAULT_LOCATION, instanceName.length()*5 , -1));
		// model.setConstraint(new Rectangle(120 * root.getInstances().size() + 20, InstanceModel.DEFAULT_LOCATION, MAX_WIDTH, -1));
		Rectangle lineRect = model.getConstraint().getCopy();
		lineRect.translate(new Point(width/2, 0));
		lineRect.width = 5;
		lineRect.height = LifeLineModel.DEFAULT_HEIGHT;
		model.getModel().setConstraint(lineRect);
		root.addInstance(model);
		root.copyPresentation(model);
		return model;
	}

	/**
	 * ����Actor
	 * @param instanceName
	 * @return
	 */
	public ActorModel createActor(String instanceName) {
		ActorModel model = new ActorModel();
		model.setName(instanceName);
		model.setConstraint(new Rectangle(120 * root.getInstances().size() + 20, InstanceModel.DEFAULT_LOCATION, 100, -1));
		Rectangle lineRect = model.getConstraint().getCopy();
		lineRect.translate(new Point(50, 0));
		lineRect.width = 5;
		lineRect.height = LifeLineModel.DEFAULT_HEIGHT;
		model.getModel().setConstraint(lineRect);
		root.addInstance(model);
		root.copyPresentation(model);
		
		return model;
	}

	public void init(InstanceModel instance) {
		ActivationModel model = new ActivationModel();
		model.setConstraint(new Rectangle(instance.getConstraint().x + 45, 70, ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT));
		instance.getModel().addActivation(model);
		current = model;
		currentY = 70;
		instance.copyPresentation(current);
	}

	/**
	 * 
	 * @param key
	 *            [source instance name ]-[message name]-[target instance name]
	 */
	public void back(String key) {
		MessageModel model = (MessageModel) messageMap.get(key);
		back(model);
	}

	/**
	 * 
	 * @param model
	 */
	public void back(MessageModel model) {
		if (model != null) {
			MessageAcceptableModel target = (MessageAcceptableModel) model.getTarget();
			ActivationModel source = (ActivationModel) model.getSource();
			current = source;
			if (target instanceof ActivationModel) {
				currentY = target.getConstraint().y + target.getConstraint().height + 20;
			} else {
				currentY = target.getConstraint().y + target.getConstraint().height + 40;
			}
		}
	}

	/**
	 * ����Message(�I�s�䥦Instance��Method)
	 * @param message
	 * @param backMessage
	 * @param target
	 * @param level
	 * @return
	 */
	public MessageModel createMessage(String message, String backMessage, InstanceModel target, int level) {	
		ActivationModel model = new ActivationModel();
		current.copyPresentation(model);

		currentY += 5;
		ActivationModel targetModel = getTargetModel(currentY, target);

		//message = this.setNewLine(message);
		//int newlineCount = this.getNewLineCount(message);
		//backMessage = this.setNewLine(backMessage);
		//newlineCount += this.getNewLineCount(backMessage);

		if (targetModel == null) {
			//logger.info("<targetModel == null> message="+message+":" + currentY);
			//�C�h�@��class��ت����N��G�@��
			currentY += 20;
			//�eclass��ؤU��������Τ��
			model.setConstraint(					
					new Rectangle(
							//�e��U�@��class�����������
							target.getConstraint().x + picsWidth.get(picCounter++),
							currentY, 
							ActivationModel.DEFAULT_WIDTH, 
							ActivationModel.DEFAULT_HEIGHT));
			
			target.getModel().addActivation(model);
		} else {
			//logger.info("<targetModel != null>message="+message+":"+  currentY);
			model.setConstraint(
					new Rectangle(
							target.getConstraint().x + 45 + current.getNestLevel() * 5, 
							current.getConstraint().y + 30, 
							ActivationModel.DEFAULT_WIDTH, 
							ActivationModel.DEFAULT_HEIGHT));
			
			targetModel.addActivation(model);
		}
		// ----------------------------------------------------------------------
		SyncMessageModel messageModel = new SyncMessageModel();

		messageModel.setName(message);
		setMessageModelColor(messageModel, level);
		messageModel.setSource(current);
		messageModel.setTarget(model);
		messageModel.attachSource();
		messageModel.attachTarget();
		current.copyPresentation(messageModel);
		messageStack.push(messageModel);

		// ----------------------------------------------------------------------
		ReturnMessageModel returnMessageModel = new ReturnMessageModel();

		returnMessageModel.setSource(model);

		Rectangle srcR= model.getConstraint();
		//srcR.y=srcR.y+20;
		srcR.height=srcR.height+50;
		model.setConstraint(srcR);

		returnMessageModel.setTarget(current);
		returnMessageModel.attachSource();
		returnMessageModel.attachTarget();

//		returnMessageModel.setName(backMessage);
//		current.copyPresentation(returnMessageModel);
//		if(backMessage!=null && !"".equals(backMessage.trim())){
//			returnMessageModel.setForegroundColor(new RGB(255,0,0));
//		}
//		else{
//			returnMessageModel.setForegroundColor(new RGB(0,0,0));
//		}
		
		// ----------------------------------------------------------------------

		//�NRobustness Level����T�ϥ�Note���
		createNoteMessage(message, backMessage);
		
		messageMap.put(current.getOwnerLine().getOwner().getName() + "-" + message + "-" + target.getName(), messageModel);
		model.computeCaller();
		current = model;
		currentY += (20 * 1);
		
		return messageModel;
	}

	private String setNewLine(String message) {
		message = message.trim();
		while (message.endsWith(",")) {
			message = message.substring(0, message.length() - 1);
		}
		message = message.replace(',', '\n');
		return message;
	}

	private int getNewLineCount(String message) {
		int newlineCount = 0;
		int pos = message.indexOf("\n");
		while (pos != -1) {
			newlineCount++;
			pos = message.indexOf("\n", pos + 1);
		}
		return newlineCount;
	}

	/**
	 * ����Message(Call�ۤv)
	 * @param message
	 * @param backMessage
	 * @param level
	 * @return
	 */
	public MessageModel createSelfCallMessage(String message,String backMessage, int level) {
		ActivationModel model = new ActivationModel();
		current.copyPresentation(model);
		currentY += 25;
		model.setConstraint(new Rectangle(current.getConstraint().x + 5, currentY, ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT));
		current.addActivation(model);
		SyncMessageModel messageModel = new SyncMessageModel();
		messageModel.setName(message);
		messageModel.setSource(current);
		messageModel.setTarget(model);
		messageModel.attachSource();
		messageModel.attachTarget();
		setMessageModelColor(messageModel, level);
		current.copyPresentation(messageModel);

		// ----------------------------------------------------------------------
//		ReturnMessageModel returnMessageModel = new ReturnMessageModel();
//
//		returnMessageModel.setName(backMessage);
//		returnMessageModel.setSource(model);	
//		
//		returnMessageModel.setTarget(current);
//		returnMessageModel.attachSource();
//		returnMessageModel.attachTarget();
//		
//		current.copyPresentation(returnMessageModel);
//		if(backMessage!=null && !"".equals(backMessage.trim())){
//			returnMessageModel.setForegroundColor(new RGB(255,0,0));
//			returnMessageModel.setShowIcon(true);
//		}
//		else{
//			returnMessageModel.setForegroundColor(new RGB(0,0,0));
//		}
		// ----------------------------------------------------------------------

		//�NRobustness Level����T�ϥ�Note���
		createNoteMessage(message, backMessage);
	
		messageStack.push(messageModel);
		model.computeCaller();
		current = model;
		currentY += 20;

		return messageModel;
	}

	public MessageModel createCreationMessage(String message, InstanceModel target) {
		Rectangle rectangle = target.getConstraint().getCopy();
		Point p = rectangle.getTopLeft();
		p.y = currentY;
		rectangle.setLocation(p);
		target.setConstraint(rectangle);
		SyncMessageModel messageModel = new SyncMessageModel();
		messageModel.setName(message);
		messageModel.setSource(current);
		messageModel.setTarget(target);
		messageModel.attachSource();
		messageModel.attachTarget();
		messageStack.push(messageModel);
		ActivationModel newModel = new ActivationModel();
		newModel.setMovable(false);
		Point actP = rectangle.getBottom().getCopy().getTranslated(-ActivationModel.DEFAULT_WIDTH / 2, 20);
		target.copyPresentation(newModel);
		target.getModel().addActivation(newModel);
		target.setActive(newModel);
		newModel.setConstraint(new Rectangle(actP, new Dimension(ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT)));
		current = newModel;
		currentY += 40;
		return messageModel;
	}

	private ActivationModel getTargetModel(int y, InstanceModel target) {
		List children = target.getModel().getChildren();
		for (Iterator iter = children.iterator(); iter.hasNext();) {
			ActivationModel element = (ActivationModel) iter.next();
			if (element.getConstraint().y < y && element.getConstraint().y + element.getConstraint().height > y) {
				return element;
			}
		}
		return null;
	}

	/**
	 * ���͵��ѰT��(Robustness Level�T��)
	 * @param noteMessage
	 * @param current2 
	 */
	private void createNoteMessage(String methodName, String noteMessage) {
		//�Y��
		if(noteMessage != null && !"".equals(noteMessage.trim())) {
			NoteModel aNote = new NoteModel();

			//RobustnessLevel Message�Ӽ�
			int rlCounter =1;
			// NoteMessage�����̪��r��
			int maxLength =0;
			// Robustness Level End Index
			int endIdx = noteMessage.indexOf("} {");

			// �Y�u���@��RobustnessLevel Message
			if (endIdx == -1) {
				maxLength = noteMessage.length();
				aNote.setContent(noteMessage);
			}
			// �Y�W�L�@�ӥH�W��RobustnessLevel Message
			else {
				String remainder = noteMessage;
				maxLength = endIdx +1;
				while (remainder.indexOf("} {") != -1) {
					rlCounter++;
					endIdx = remainder.indexOf("} {");

					remainder = remainder.substring(endIdx + 2);
				}
				//���o�̪������r��
				if (maxLength < remainder.length()-1)
					maxLength = remainder.length()-1;

				//���J����Ÿ�
				aNote.setContent(noteMessage.replace("} ", "}\n"));
			}
			//�[�JMethod�W��
			aNote.setContent(methodName + "\n" + aNote.getContent());
			aNote.setConstraint(new Rectangle(current.getConstraint().x, 0,
											  (int)((float)maxLength * FONT_SIZE), 30 + 14 * rlCounter));

			aNote.setShowIcon(true);

			//�O��Note
			noteStack.push(aNote);
			//�O��Note�P��Note�f�t��Instance
			noteMap.put(aNote, current.getOwnerLine().getOwner());

			root.addUMLModel(aNote);
		}
	}

	/**
	 * �NMessage�]���PRobustness Level�ϥΤ��P�C��
	 * @param messageModel
	 * @param level
	 */
	private void setMessageModelColor(SyncMessageModel messageModel, int level) {
/*		Shiau: RL���T�����̷�Exception�����ӫDMethod,�ҥH�R���C
		// �p�G���RL��T�A�~�N�W�ټW�[RL
		if (isShowRL)
			message = "RL " + level + "\t" + message;
*/
		String message = messageModel.getName();
		messageModel.setName(" " + message);
		
		// �̤��PLevel�������P�C��
		switch (level) {
			case 1:		// RL1:��
				messageModel.setForegroundColor(new RGB(255, 0, 0));
				break;
			case 2:		// RL2:��
				messageModel.setForegroundColor(new RGB(229, 109, 29));
				break;
			case 3:		// RL3:��
				messageModel.setForegroundColor(new RGB(54, 157, 54));
				break;
			default:	// RL0:��
				messageModel.setForegroundColor(new RGB(0, 0, 0));
		}
	}
	
	/**
	 * ����XML��
	 * @return
	 */
	public String toXML() {
		MessageOrderUtil.computeMessageOrders(root);
		root.adjustLifeLine();
		
		//�NNote���Ƨ�
		arrangeNote();

		return XStreamSerializer.serialize(root, getClass().getClassLoader());
	}

	/**
	 * �NNote�PInstance���Ƨ�
	 */
	private void arrangeNote() {
		//�O��Instance�P��Instance���U�@��Instance����m
		Map<InstanceModel, Integer> widthMap = new HashMap<InstanceModel, Integer>();
		
		/*	Note Y�b�ƦC	*/
		for (int i=0; i < noteStack.size(); i++) {
			NoteModel aNote = noteStack.get(i);
			//���oNote��Instance
			InstanceModel instance = noteMap.get(aNote);
			Rectangle rect = instance.getModel().getConstraint();

			//�O��Instance��U�@��Instance�̤j�Z��(��̤j��Note�e��)
			Integer width = widthMap.get(instance);
			if (width == null || width < aNote.getConstraint().width)
				widthMap.put(instance, aNote.getConstraint().right());

			//�Y��@Instance�������ƪ�Note�A�N����m��Note�U��
			int axisY = rect.y + rect.height;
			for (int j=i-1; j >= 0; j--) {
				NoteModel lastNote = noteStack.get(j);
				InstanceModel lastInstance = noteMap.get(lastNote);
				//�e���O�_���ۦPInstance��Note
				if (instance == lastInstance) {
					axisY = lastNote.getConstraint().bottom();
					break;
				}
			}
			aNote.getConstraint().y = axisY +10;
		}

		/*	�N�U�@��Instance�W�[��i�e�UNote���e��	*/
		List<InstanceModel> instanceList = root.getInstances();
		for (int i=0; i < instanceList.size(); i++) {
			//���o��Instance�w�p�վ�U�@��Instance����m
			Integer nextAxisX = widthMap.get(instanceList.get(i));
			//�Y�S����ܤ��ݭn�վ�
			if (nextAxisX == null)
				continue;

			//��Instance�P�վ�L�᪺Instance���ۮt��
			int diff = 0;
			if (i+1 < instanceList.size()) {
				InstanceModel model = instanceList.get(i+1);
				diff = nextAxisX - model.getConstraint().x - model.getConstraint().width/2 +10;
			}
			//�t�ȥ����n�j��0
			if (diff > 0) {
				//�N���᪺Instance���վ��m
				for (int j = i+1; j < instanceList.size(); j++) {
					InstanceModel nextInstance = instanceList.get(j);
					Rectangle rect = nextInstance.getConstraint();
					nextInstance.setConstraint(
							new Rectangle(rect.x + diff, rect.y, rect.width, rect.height));
					
					//�N���᪺�p�⪺NoteWidth�]�վ��m
					Integer nextWidth = widthMap.get(nextInstance);
					if (nextWidth != null)
						widthMap.put(nextInstance, nextWidth +diff);
				}
			}
		}
		/*	Note X�b�ƦC	*/
		Set<NoteModel> keySet = noteMap.keySet();
		Iterator<NoteModel> iterator = keySet.iterator();
		while(iterator.hasNext()) {
			NoteModel aNote = (NoteModel) iterator.next();
			InstanceModel instance = noteMap.get(aNote);
			//�NNote��x�b��m����Instance��X�b
			aNote.getConstraint().x = instance.getConstraint().x + instance.getConstraint().width/2;
		}
	}

	//�S�Ψ�(�S�Hcall��)���ѱ�
	//public MessageModel createMessage(String message, InstanceModel target) {
	//	ActivationModel model = new ActivationModel();
	//	current.copyPresentation(model);
	//	ActivationModel targetModel = getTargetModel(currentY, target);
	//	if (targetModel == null) {
	//		model.setConstraint(new Rectangle(target.getConstraint().x + 45, currentY, ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT));
	//		target.getModel().addActivation(model);
	//	} else {
	//		model.setConstraint(new Rectangle(target.getConstraint().x + 45 + current.getNestLevel() * 5, current.getConstraint().y + 30, ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT));
	//		targetModel.addActivation(model);
	//	}
	//
	//	message = this.setNewLine(message);
	//	int newlineCount = this.getNewLineCount(message);
	//	SyncMessageModel messageModel = new SyncMessageModel();
	//	messageModel.setName(message);
	//	messageModel.setSource(current);
	//	messageModel.setTarget(model);
	//	messageModel.attachSource();
	//	messageModel.attachTarget();
	//	current.copyPresentation(messageModel);
	//	messageStack.push(messageModel);
	//
	//	ReturnMessageModel returnMessageModel = new ReturnMessageModel();
	//	returnMessageModel.setSource(model);
	//	returnMessageModel.setTarget(current);
	//	returnMessageModel.attachSource();
	//	returnMessageModel.attachTarget();
	//	current.copyPresentation(returnMessageModel);
	//
	//	messageMap.put(current.getOwnerLine().getOwner().getName() + "-" + message + "-" + target.getName(), messageModel);
	//	model.computeCaller();
	//	current = model;
	//	currentY += 20 * newlineCount;
	//	return messageModel;
	//}
	//public MessageModel createSelfCallMessage(String message) {
	//	ActivationModel model = new ActivationModel();
	//	current.copyPresentation(model);
	//	currentY += 20;
	//	model.setConstraint(new Rectangle(current.getConstraint().x + 5, currentY, ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT));
	//	current.addActivation(model);
	//	SyncMessageModel messageModel = new SyncMessageModel();
	//	messageModel.setName(message);
	//	messageModel.setSource(current);
	//	messageModel.setTarget(model);
	//	messageModel.attachSource();
	//	messageModel.attachTarget();
	//	current.copyPresentation(messageModel);
	//	messageStack.push(messageModel);
	//	model.computeCaller();
	//	current = model;
	//	currentY += 20;
	//	return messageModel;
	//}
	//public void endMessage() {
	//	if (!messageStack.isEmpty()) {
	//		MessageModel model = (MessageModel) messageStack.pop();
	//		back(model);
	//	}
	//}
	//public MessageModel createCreationMessage(String message, String instanceName) {
	//InstanceModel model = createInstance(instanceName);
	//	return createCreationMessage(message, model);
	//}
	//public ActivationModel createInstanciateMessage(String message,
	//ActivationModel source, InstanceModel target) {
	//		
	//}
	//public ActivationWrapper createRecursiveMessage(String message,
	//ActivationWrapper source, InstanceWrapper target) {
	//		
	//}
}
