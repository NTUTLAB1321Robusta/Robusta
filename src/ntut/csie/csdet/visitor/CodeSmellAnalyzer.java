package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * 找專案中的Ignore Exception and dummy handler
 * @author chewei
 */

public class CodeSmellAnalyzer extends RLBaseVisitor {
	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
    
	// 儲存所找到的ignore Exception 
	private List<CSMessage> codeSmellList;
	
	// 儲存所找到的dummy handler
	private List<CSMessage> dummyList;
	
	//儲存偵測"Library的Name"和"是否Library"
	private TreeMap<String, Boolean> libMap = new TreeMap<String, Boolean>();

	//是否偵測e.printStackTrace和system.out.print
	boolean isPrint = false;
	boolean isSyso = false;
	
	public CodeSmellAnalyzer(CompilationUnit root){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyList = new ArrayList<CSMessage>();
	}
	
	/**
	 * 先作兩個constructor,之後怕會有需要更多的資訊
	 * 如果沒有可以將其註解掉
	 */
	public CodeSmellAnalyzer(CompilationUnit root,boolean currentMethod,int pos){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyList = new ArrayList<CSMessage>();
	}
	
	/*
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.TRY_STATEMENT:

				return true;
			case ASTNode.CATCH_CLAUSE:
				processCatchStatement(node);
				return true;
			default:
				//return true則繼續訪問其node的子節點,false則不繼續
				return true;
		}	
	}
		
	
	/**
	 * 去尋找catch的節點,並且判斷節點內的statement是否為空
	 * @param node
	 */
	private void processCatchStatement(ASTNode node){
		//轉換成catch node
		CatchClause cc = (CatchClause) node;
		//取的catch(Exception e)其中的e
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		//判斷這個catch是否有ignore exception or dummy handler
		judgeIgnoreEx(cc,svd);
	}
	
	/**
	 * 判斷這個catch block是不是ignore EX
	 * @param cc : catch block的資訊
	 * @param svd : throw exception會用到
	 */
	private void judgeIgnoreEx(CatchClause cc,SingleVariableDeclaration svd ){
		List statementTemp = cc.getBody().statements();
		// 如果catch statement裡面是空的話,表示是ignore exception
		if(statementTemp.size() == 0){	
			//建立一個ignore exception type
			CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_INGNORE_EXCEPTION,svd.resolveBinding().getType(),											
									cc.toString(),cc.getStartPosition(),
									this.getLineNumber(cc.getStartPosition()),svd.getType().toString());
			this.codeSmellList.add(csmsg);
		}else{
	        /*------------------------------------------------------------------------*
            -  假如statement不是空的,表示有可能存在dummy handler,不另外寫一個class來偵測,
                 原因是不希望在RLBilder要parse每個method很多次,code部分也會增加,所以就寫在這邊
            *-------------------------------------------------------------------------*/
			judgeDummyHandler(statementTemp,cc,svd);
		}
	}
	
	/**
	 * 判斷這個catch內是否有dummy handler
	 * @param statementTemp
	 * @param cc
	 * @param svd
	 */
	private void judgeDummyHandler(List statementTemp,CatchClause cc,SingleVariableDeclaration svd){
        /*------------------------------------------------------------------------*
        -  假設這個catch裡面有throw東西 or 有使用Log的API,就判定不是dummy handler
             如果只要有一個e.printStackTrace或者符合user所設定的條件,就判定為dummy handler  
        *-------------------------------------------------------------------------*/	

		// 利用此flag來記錄到底加入了多少的dummy handler
		int flag = 0;
		getDummySettings();

		for(int i=0;i<statementTemp.size();i++){
			//取得Expression statement,因為e.printstackTrace這類都是算這種型態			
			if(statementTemp.get(i) instanceof ExpressionStatement){
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

				//偵測printStackTrace
				if (isPrint){
					if(statement.getExpression().toString().contains("printStackTrace")){					
						//建立Dummy handler的type
						CSMessage csmsg = getDummyMessage(cc, svd, statement);
						this.dummyList.add(csmsg);
						// 新增一筆dummy handler
						flag++;
						continue;
					}
				}
				//偵測System.out.print
				if (isSyso){
					if(statement.getExpression().toString().contains("System.out.print")){					
						CSMessage csmsg = getDummyMessage(cc, svd, statement);
						this.dummyList.add(csmsg);
						// 新增一筆dummy handler
						flag++;
						continue;
					}
				}
				//偵測外部Library
				if(findBindingLib(statement,flag)){
					CSMessage csmsg = getDummyMessage(cc, svd, statement);
					this.dummyList.add(csmsg);
					// 新增一筆dummy handler
					flag++;
					continue;
				}
			}else if(statementTemp.get(i) instanceof ThrowStatement){
				// 碰到有throw 東西出來,就判定不是dummy handler
				// 可能會碰到有e.printStackTrace(),但下一行又throw東西出來
				// 所以先取得之前加了幾個dummy handler,接著從list最尾端開始移除
				int size = this.dummyList.size()-1;
				for(int x=0;x<flag;x++){
					this.dummyList.remove(size-x);
				}
			}
		}
	}

	/**
	 * 得到Dummy Handler的訊息
	 */	
	private CSMessage getDummyMessage(CatchClause cc,
			SingleVariableDeclaration svd, ExpressionStatement statement) {
		CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,
				svd.resolveBinding().getType(),cc.toString(),cc.getStartPosition(),
				this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
		return csmsg;
	}

	/**
	 * 用來找尋這個catch Clause中是否有log4j or java.logger的東西
	 * 如果有表示RL = 1,所以不能標示marker
	 */
	private Boolean findBindingLib(ExpressionStatement statement,int flag){
		ASTBinding visitor = new ASTBinding(libMap);
		statement.getExpression().accept(visitor);
		if(visitor.getResult()){
			//假如找到log4j or java.logger,就把之前找到的smell去掉
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 將user對於dummy handler的設定存下來
	 * @return
	 */
	private void getDummySettings(){
		Element root = JDomUtil.createXMLContent();
		// 如果是null表示xml檔是剛建好的,還沒有dummy handler的tag,直接跳出去
		
		if(root.getChild(JDomUtil.DummyHandlerTag) != null){
			// 這裡表示之前使用者已經有設定過preference了,去取得相關偵測設定值
			Element dummyHandler = root.getChild(JDomUtil.DummyHandlerTag);
			Element rule = dummyHandler.getChild("rule");
			String eprintSet = rule.getAttribute(JDomUtil.eprintstacktrace).getValue();
			String sysoSet = rule.getAttribute(JDomUtil.systemoutprint).getValue();
			String log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			String javaLogger = rule.getAttribute(JDomUtil.java_Logger).getValue();
			Element libRule = dummyHandler.getChild("librule");
			// 把外部Library的名單儲存在List內
			List<Attribute> ruleList = libRule.getAttributes();

			//把外部的Library加入偵測名單內
			for (int i=0;i<ruleList.size();i++)
				libMap.put(ruleList.get(i).getQualifiedName(),ruleList.get(i).getValue().equals("Y"));

			if (eprintSet.equals("Y"))
				isPrint = true;
			if (sysoSet.equals("Y"))
				isSyso = true;
			
			//把log4j和javaLog加入偵測內
			libMap.put("org.apache.log4j.Logger",log4jSet.equals("Y"));
			libMap.put("java.util.logging.Logger",javaLogger.equals("Y"));
		}
	}
	
	/**
	 * 根據startPosition來取得行數
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * 取得dummy handler的List
	 */
	public List<CSMessage> getIgnoreExList(){
		return codeSmellList;
	}
	
	/**
	 * 取得dummy handler的List
	 */
	public List<CSMessage> getDummyList(){
		return dummyList;
	}
}
