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
		//class名稱的方塊寬度
		int width;
		InstanceModel model = new InstanceModel();
		//在package+class字串裡找到class名稱的起始位置
		int pos = instanceName.lastIndexOf(".");
		
		//若不顯示package或是顯示0層的package或package無"."
		if (!isPackage || (!isShowAll && packageCount == 0) || pos == -1)
		{
			//若instanceName裡沒有"."把全部顯示出來，若有則把package名稱給清掉
			instanceName = (pos != -1 ? instanceName.substring(pos + 1) : instanceName);

			model.setName(instanceName);
			
			//過小就把它調為預設
			if (instanceName.length()*6 <MAX_WIDTH)
				width = 150;
			else
				width = pos*6;
		}
		//顯示package時
		else
		{
			//若顯示全部的package
			if (isShowAll)
				//若instanceName裡沒有"."把全部顯示出來，若有則把class名稱移到下一行
				instanceName = (pos != -1 ? instanceName.substring(0, pos + 1) + "\n" + instanceName.substring(pos + 1) : instanceName);
			//若顯式方式是由上到下
			else if (isTopDown)
			{
				//package後的"."的位置
				int docPos = 0;
				//找到指定的package數後的"."的位置(若超過則用最後一個package的"."之位置)
				for (int i=0;instanceName.indexOf(".",docPos+1)!=-1 && i<packageCount;i++)
					docPos = instanceName.indexOf(".",docPos+1);
				//把超過選擇package數目的範圍的package名稱給清掉
				instanceName = instanceName.substring(0,docPos+1) + "\n" + instanceName.substring(pos+1);
				//重新找一次class名稱的位置
				pos = instanceName.lastIndexOf(".");
			}
			//若顯示方式是由下往上
			else
			{
				//class前的"."的位置
				int docPos = pos;
				//找到指定的package數後的"."的位置
				int i=0;
				for (;instanceName.lastIndexOf(".",docPos-1)!=-1 && i<packageCount;i++)
					docPos = instanceName.lastIndexOf(".",docPos-1);
				//若超過則全顯示
				if (i<packageCount)
					instanceName = instanceName.substring(0, pos + 1) + "\n" + instanceName.substring(pos + 1);
				else
					//把超過選擇package數目的範圍的package名稱給清掉
					instanceName = instanceName.substring(docPos+1,pos+1) + "\n" + instanceName.substring(pos+1);
				//重新找一次class名稱的位置
				pos = instanceName.lastIndexOf(".");
			}
			model.setName(instanceName);
			
			//判斷是上半部名比較大還是下半部
			if (instanceName.length()/2<=pos)			//上半部較大
				width = pos*6;
			else
				width = (instanceName.length()-pos)*6;	//下半部較大
			//過小就把它調為預設
			if (width<MAX_WIDTH)
				width = 150;
		}
		
		// root.getInstances().get(root).getConstraint().getRight().getPosition(root.getConstraint().getRight());

		//Debugger到第一個method的位置
		model.setConstraint(new Rectangle(picPostion + 20, InstanceModel.DEFAULT_LOCATION, width , -1));

		//儲存起來給底下畫方塊圖的參照
		picsWidth.add(width/2-5);
		//記錄位置給下個圖使用
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

//	沒用到(沒人call它)註解掉
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
			//每多一個class方框的高就更矮一些
			currentY += 20;
			//畫class方框下面的長方形方框
			model.setConstraint(					
					new Rectangle(
							//畫到下一個class的方塊的中間
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
	
//	沒用到(沒人call它)註解掉
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
