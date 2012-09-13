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

	//記錄Note Message
	private Stack<NoteModel> noteStack = new Stack<NoteModel>();
	//記錄Note Message與跟它配對的Instance
	private Map<NoteModel, InstanceModel > noteMap = new HashMap<NoteModel, InstanceModel>();
	//現在的Y軸位置
	private int currentY = 0;
	//Instance的最小寬度
	private int MIN_WIDTH = 150;
	//文字的寬度
	private int FONT_WIDTH = 7;
	
	public int picPostion = MIN_WIDTH + 20;

	ArrayList<Integer> picsWidth = new ArrayList<Integer>();

	int picCounter = 0;

	// 註解的文字大小
	private final float FONT_SIZE = 6.5f;

	public RLSequenceModelBuilder() {
		root.setShowIcon(true);
	}

	/**
	 * 產生Instance
	 * @param instanceName	Instance名稱
	 * @param isPackage		是否顯示Package
	 * @param isShowAll		是否顯示全部的Package
	 * @param isTopDown		true:由上到下數	false:由下往上數
	 * @param packageCount	欲顯示Package個數
	 * @return
	 */
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
			if (instanceName.length()*FONT_WIDTH <MIN_WIDTH)
				width = MIN_WIDTH;
			else
				width = pos*FONT_WIDTH;
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
			if (instanceName.length()/2 <= pos)			//上半部較大
				width = pos*FONT_WIDTH;
			else
				width = (instanceName.length()-pos)*7;	//下半部較大
			//過小就把它調為預設
			if (width < MIN_WIDTH)
				width = MIN_WIDTH;
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

	/**
	 * 產生Actor
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
	 * 產生Message(呼叫其它Instance的Method)
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
			//每多一個class方框的高就更矮一些
			currentY += 20;
			//畫class方框下面的長方形方框
			model.setConstraint(					
					new Rectangle(
							//畫到下一個class的方塊的中間
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

		//將Robustness Level的資訊使用Note顯示
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
	 * 產生Message(Call自己)
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

		//將Robustness Level的資訊使用Note顯示
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
	 * 產生註解訊息(Robustness Level訊息)
	 * @param noteMessage
	 * @param current2 
	 */
	private void createNoteMessage(String methodName, String noteMessage) {
		//若有
		if(noteMessage != null && !"".equals(noteMessage.trim())) {
			NoteModel aNote = new NoteModel();

			//RobustnessLevel Message個數
			int rlCounter =1;
			// NoteMessage內單行最長字數
			int maxLength =0;
			// Robustness Level End Index
			int endIdx = noteMessage.indexOf("} {");

			// 若只有一個RobustnessLevel Message
			if (endIdx == -1) {
				maxLength = noteMessage.length();
				aNote.setContent(noteMessage);
			}
			// 若超過一個以上的RobustnessLevel Message
			else {
				String remainder = noteMessage;
				maxLength = endIdx +1;
				while (remainder.indexOf("} {") != -1) {
					rlCounter++;
					endIdx = remainder.indexOf("} {");

					remainder = remainder.substring(endIdx + 2);
				}
				//取得最長的單行字數
				if (maxLength < remainder.length()-1)
					maxLength = remainder.length()-1;

				//插入換行符號
				aNote.setContent(noteMessage.replace("} ", "}\n"));
			}
			//加入Method名稱
			aNote.setContent(methodName + "\n" + aNote.getContent());
			aNote.setConstraint(new Rectangle(current.getConstraint().x, 0,
											  (int)((float)maxLength * FONT_SIZE), 30 + 14 * rlCounter));

			aNote.setShowIcon(true);

			//記錄Note
			noteStack.push(aNote);
			//記錄Note與跟Note搭配的Instance
			noteMap.put(aNote, current.getOwnerLine().getOwner());

			root.addUMLModel(aNote);
		}
	}

	/**
	 * 將Message因不同Robustness Level使用不同顏色
	 * @param messageModel
	 * @param level
	 */
	private void setMessageModelColor(SyncMessageModel messageModel, int level) {
/*		Shiau: RL的訊息應依照Exception為單位而非Method,所以刪除。
		// 如果顯示RL資訊，才將名稱增加RL
		if (isShowRL)
			message = "Tag " + level + "\t" + message;
*/
		String message = messageModel.getName();
		messageModel.setName(" " + message);
		
		// 依不同Level給予不同顏色
		switch (level) {
			case 1:		// RL1:紅
				messageModel.setForegroundColor(new RGB(255, 0, 0));
				break;
			case 2:		// RL2:黃
				messageModel.setForegroundColor(new RGB(229, 109, 29));
				break;
			case 3:		// RL3:綠
				messageModel.setForegroundColor(new RGB(54, 157, 54));
				break;
			default:	// RL0:黑
				messageModel.setForegroundColor(new RGB(0, 0, 0));
		}
	}
	
	/**
	 * 產生XML檔
	 * @return
	 */
	public String toXML() {
		MessageOrderUtil.computeMessageOrders(root);
		root.adjustLifeLine();
		
		//將Note做排序
		arrangeNote();

		return XStreamSerializer.serialize(root, getClass().getClassLoader());
	}

	/**
	 * 將Note與Instance做排序
	 */
	private void arrangeNote() {
		//記錄Instance與此Instance的下一個Instance的位置
		Map<InstanceModel, Integer> widthMap = new HashMap<InstanceModel, Integer>();
		
		/*	Note Y軸排列	*/
		for (int i=0; i < noteStack.size(); i++) {
			NoteModel aNote = noteStack.get(i);
			//取得Note的Instance
			InstanceModel instance = noteMap.get(aNote);
			Rectangle rect = instance.getModel().getConstraint();

			//記錄Instance到下一個Instance最大距離(找最大的Note寬度)
			Integer width = widthMap.get(instance);
			if (width == null || width < aNote.getConstraint().width)
				widthMap.put(instance, aNote.getConstraint().right());

			//若單一Instance中有重複的Note，將它放置此Note下方
			int axisY = rect.y + rect.height;
			for (int j=i-1; j >= 0; j--) {
				NoteModel lastNote = noteStack.get(j);
				InstanceModel lastInstance = noteMap.get(lastNote);
				//前面是否有相同Instance的Note
				if (instance == lastInstance) {
					axisY = lastNote.getConstraint().bottom();
					break;
				}
			}
			aNote.getConstraint().y = axisY +10;
		}

		/*	將下一個Instance增加到可容下Note的寬度	*/
		List<InstanceModel> instanceList = root.getInstances();
		for (int i=0; i < instanceList.size(); i++) {
			//取得該Instance預計調整下一個Instance的位置
			Integer nextAxisX = widthMap.get(instanceList.get(i));
			//若沒有表示不需要調整
			if (nextAxisX == null)
				continue;

			//原Instance與調整過後的Instance的相差值
			int diff = 0;
			if (i+1 < instanceList.size()) {
				InstanceModel model = instanceList.get(i+1);
				diff = nextAxisX - model.getConstraint().x - model.getConstraint().width/2 +10;
			}
			//差值必須要大於0
			if (diff > 0) {
				//將之後的Instance都調整位置
				for (int j = i+1; j < instanceList.size(); j++) {
					InstanceModel nextInstance = instanceList.get(j);
					Rectangle rect = nextInstance.getConstraint();
					nextInstance.setConstraint(
							new Rectangle(rect.x + diff, rect.y, rect.width, rect.height));
					
					//將之後的計算的NoteWidth也調整位置
					Integer nextWidth = widthMap.get(nextInstance);
					if (nextWidth != null)
						widthMap.put(nextInstance, nextWidth +diff);
				}
			}
		}
		/*	Note X軸排列	*/
		Set<NoteModel> keySet = noteMap.keySet();
		Iterator<NoteModel> iterator = keySet.iterator();
		while(iterator.hasNext()) {
			NoteModel aNote = (NoteModel) iterator.next();
			InstanceModel instance = noteMap.get(aNote);
			//將Note的x軸位置移到Instance的X軸
			aNote.getConstraint().x = instance.getConstraint().x + instance.getConstraint().width/2;
		}
	}

	//沒用到(沒人call它)註解掉
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
