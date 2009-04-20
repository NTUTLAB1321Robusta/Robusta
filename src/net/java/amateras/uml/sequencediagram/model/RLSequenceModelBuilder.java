package net.java.amateras.uml.sequencediagram.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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

	private int currentY = 0;

	private int MAX_WIDTH = 150;
	
	public int picPostion = MAX_WIDTH + 20;
	ArrayList<Integer> picsWidth = new ArrayList<Integer>();
	int count = 0;

	public RLSequenceModelBuilder() {
		root.setShowIcon(true);
	}

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
			if (instanceName.length()*6 <MAX_WIDTH)
				width = 150;
			else
				width = pos*6;
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
			if (instanceName.length()/2<=pos)			//�W�b�����j
				width = pos*6;
			else
				width = (instanceName.length()-pos)*6;	//�U�b�����j
			//�L�p�N�⥦�լ��w�]
			if (width<MAX_WIDTH)
				width = 150;
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

	public void endMessage() {
		if (!messageStack.isEmpty()) {
			MessageModel model = (MessageModel) messageStack.pop();
			back(model);
		}
	}

//	�S�Ψ�(�S�Hcall��)���ѱ�
//	public MessageModel createMessage(String message, String instanceName) {
//		InstanceModel model = createInstance(instanceName);
//		return createMessage(message, model);
//	}

	public MessageModel createMessage(String message, InstanceModel target) {
		ActivationModel model = new ActivationModel();
		current.copyPresentation(model);
		ActivationModel targetModel = getTargetModel(currentY, target);
		if (targetModel == null) {
			model.setConstraint(new Rectangle(target.getConstraint().x + 45, currentY, ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT));
			target.getModel().addActivation(model);
		} else {
			model.setConstraint(new Rectangle(target.getConstraint().x + 45 + current.getNestLevel() * 5, current.getConstraint().y + 30, ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT));
			targetModel.addActivation(model);
		}

		message = this.setNewLine(message);
		int newlineCount = this.getNewLineCount(message);
		SyncMessageModel messageModel = new SyncMessageModel();
		messageModel.setName(message);
		messageModel.setSource(current);
		messageModel.setTarget(model);
		messageModel.attachSource();
		messageModel.attachTarget();
		current.copyPresentation(messageModel);
		messageStack.push(messageModel);

		ReturnMessageModel returnMessageModel = new ReturnMessageModel();
		returnMessageModel.setSource(model);
		returnMessageModel.setTarget(current);
		returnMessageModel.attachSource();
		returnMessageModel.attachTarget();
		current.copyPresentation(returnMessageModel);

		messageMap.put(current.getOwnerLine().getOwner().getName() + "-" + message + "-" + target.getName(), messageModel);
		model.computeCaller();
		current = model;
		currentY += 20 * newlineCount;
		return messageModel;
	}

	public MessageModel createMessage(String message, String backMessage, InstanceModel target) {
		ActivationModel model = new ActivationModel();
		current.copyPresentation(model);
		
		currentY+=5;
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
							target.getConstraint().x + picsWidth.get(count++),
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
		messageModel.setSource(current);
		messageModel.setTarget(model);
		messageModel.attachSource();
		messageModel.attachTarget();
		current.copyPresentation(messageModel);
		messageStack.push(messageModel);

		// ----------------------------------------------------------------------
		ReturnMessageModel returnMessageModel = new ReturnMessageModel();

		returnMessageModel.setName(backMessage);
		returnMessageModel.setSource(model);
		
//		Rectangle srcR= model.getConstraint();
//		//srcR.y=srcR.y+20;
//		srcR.height=srcR.height+50;
//		model.setConstraint(srcR);
		
		returnMessageModel.setTarget(current);
		returnMessageModel.attachSource();
		returnMessageModel.attachTarget();
		
		current.copyPresentation(returnMessageModel);
		if(backMessage!=null && !"".equals(backMessage.trim())){
			returnMessageModel.setForegroundColor(new RGB(255,0,0));
		}
		else{
			returnMessageModel.setForegroundColor(new RGB(0,0,0));
		}
		
		// ----------------------------------------------------------------------

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

	public MessageModel createSelfCallMessage(String message) {
		ActivationModel model = new ActivationModel();
		current.copyPresentation(model);
		currentY += 20;
		model.setConstraint(new Rectangle(current.getConstraint().x + 5, currentY, ActivationModel.DEFAULT_WIDTH, ActivationModel.DEFAULT_HEIGHT));
		current.addActivation(model);
		SyncMessageModel messageModel = new SyncMessageModel();
		messageModel.setName(message);
		messageModel.setSource(current);
		messageModel.setTarget(model);
		messageModel.attachSource();
		messageModel.attachTarget();
		current.copyPresentation(messageModel);
		messageStack.push(messageModel);
		model.computeCaller();
		current = model;
		currentY += 20;
		return messageModel;
	}
	
	

	public MessageModel createSelfCallMessage(String message,String backMessage) {
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
		current.copyPresentation(messageModel);
		
		// ----------------------------------------------------------------------
		ReturnMessageModel returnMessageModel = new ReturnMessageModel();

		returnMessageModel.setName(backMessage);
		returnMessageModel.setSource(model);	
		
		returnMessageModel.setTarget(current);
		returnMessageModel.attachSource();
		returnMessageModel.attachTarget();
		
		current.copyPresentation(returnMessageModel);
		if(backMessage!=null && !"".equals(backMessage.trim())){
			returnMessageModel.setForegroundColor(new RGB(255,0,0));
			returnMessageModel.setShowIcon(true);
		}
		else{
			returnMessageModel.setForegroundColor(new RGB(0,0,0));
		}
		
		// ----------------------------------------------------------------------
		
		messageStack.push(messageModel);
		model.computeCaller();
		current = model;
		currentY += 20;
		return messageModel;
	}
	
//	�S�Ψ�(�S�Hcall��)���ѱ�
//	public MessageModel createCreationMessage(String message, String instanceName) {
//		InstanceModel model = createInstance(instanceName);
//		return createCreationMessage(message, model);
//	}

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

	public String toXML() {
		MessageOrderUtil.computeMessageOrders(root);
		root.adjustLifeLine();
		return XStreamSerializer.serialize(root, getClass().getClassLoader());
	}
	// public ActivationModel createInstanciateMessage(String message,
	// ActivationModel source, InstanceModel target) {
	//		
	// }
	//
	// public ActivationWrapper createRecursiveMessage(String message,
	// ActivationWrapper source, InstanceWrapper target) {
	//		
	// }
}
