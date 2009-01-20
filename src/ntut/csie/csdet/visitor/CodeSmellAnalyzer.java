package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

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
		int dummySettings = getDummySettings();
		for(int i=0;i<statementTemp.size();i++){
			//取得Expression statement,因為e.printstackTrace這類都是算這種型態			
			if(statementTemp.get(i) instanceof ExpressionStatement){
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

				//先取得xml檔的設定,false表示為預設值(只取e.printStackTrace())
				switch (dummySettings){
					case 0: //偵測System.out.print ,log4j,java Logger
						if(statement.getExpression().toString().contains("System.out.print")||
								statement.getExpression().toString().contains("printStackTrace")){					
							//建立Dummy handler的type
							CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,
									svd.resolveBinding().getType(),cc.toString(),cc.getStartPosition(),
									this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
								this.dummyList.add(csmsg);
								// 新增一筆dummy handler
								flag++;
						}else{
							if(findBindingLib(dummySettings,statement,flag)){
								//找到就跳出去可以不用偵測了
								break;
							}							
						}
					
						break;
					case 1: //偵測System.out.print ,log4j					
						if(statement.getExpression().toString().contains("System.out.print")||
								statement.getExpression().toString().contains("printStackTrace")){					
							//建立Dummy handler的type
							CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,
									svd.resolveBinding().getType(),cc.toString(),cc.getStartPosition(),
									this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
								this.dummyList.add(csmsg);
								// 新增一筆dummy handler
								flag++;
						}else{
							if(findBindingLib(dummySettings,statement,flag)){
								break;
							}							
						}
						
						break;
					case 2: //偵測System.out.print ,java Logger
						if(statement.getExpression().toString().contains("System.out.print")||
								statement.getExpression().toString().contains("printStackTrace")){					
							//建立Dummy handler的type
							CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,
									svd.resolveBinding().getType(),cc.toString(),cc.getStartPosition(),
									this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
								this.dummyList.add(csmsg);
								// 新增一筆dummy handler
								flag++;
						}else{
							if(findBindingLib(dummySettings,statement,flag)){
								break;
							}							
						}
					case 3: //偵測System.out.print
						if(statement.getExpression().toString().contains("System.out.print")||
								statement.getExpression().toString().contains("printStackTrace")){					
							//建立Dummy handler的type
							CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,
									svd.resolveBinding().getType(),cc.toString(),cc.getStartPosition(),
									this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
								this.dummyList.add(csmsg);
								// 新增一筆dummy handler
								flag++;
						}

						break;
					case 4: //偵測log4j,java Logger
//						if(findBindingLib(dummySettings,statement,flag)){
//							break;
//						}	
//						break;
					case 5: //偵測log4j
//						if(findBindingLib(dummySettings,statement,flag)){
//							break;
//						}	
						break;						
					case 6: //偵測java Logger
						if(findBindingLib(dummySettings,statement,flag)){
							break;
						}	
						break;	
						
					
					default: //只偵測printStackTrace
						if(statement.getExpression().toString().contains("printStackTrace")){					
							//建立Dummy handler的type
							CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,
									svd.resolveBinding().getType(),cc.toString(),cc.getStartPosition(),
									this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
							this.dummyList.add(csmsg);
							// 新增一筆dummy handler
							flag++;
						}
						break;
						
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
	 * 用來找尋這個catch Clause中是否有log4j or java.logger的東西
	 * 如果有表示RL = 1,所以不能標示marker
	 */
	private Boolean findBindingLib(int dummySettings,ExpressionStatement statement,int flag){
		ASTBinding visitor = new ASTBinding(dummySettings);
		statement.getExpression().accept(visitor);
		if(visitor.getResult()){
			//假如找到log4j or java.logger,就把之前找到的smell去掉
			int size = this.dummyList.size()-1;
			for(int x=0;x<flag;x++){
				this.dummyList.remove(size-x);
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 將user對於dummy handler的設定存下來
	 * @return
	 */
	private int getDummySettings(){
		Element root = JDomUtil.createXMLContent();
		// 如果是null表示xml檔是剛建好的,還沒有dummy handler的tag,直接跳出去
		if(root.getChild(JDomUtil.DummyHandlerTag) == null){
			return 10000;
		}else{
			// 這裡表示之前使用者已經有設定過preference了,去取得相關偵測設定值
			Element dummyHandler = root.getChild(JDomUtil.DummyHandlerTag);
			Element rule = dummyHandler.getChild("rule");			
			String settings = rule.getAttribute(JDomUtil.systemoutprint).getValue();
			String log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			String javaLogger = rule.getAttribute(JDomUtil.java_Logger).getValue();
			if(settings.equals("Y") && log4jSet.equals("Y") && javaLogger.equals("Y")){
				//偵測System.out.print ,log4j,java Logger
				return 0;
			}else if(settings.equals("Y") && log4jSet.equals("N") && javaLogger.equals("Y")){
				//偵測System.out.print ,log4j
				return 1;
			}else if(settings.equals("Y") && log4jSet.equals("Y") && javaLogger.equals("N")){
				//偵測System.out.print ,java Logger
				return 2;
			}else if(settings.equals("Y") && log4jSet.equals("N") && javaLogger.equals("N")){
				//偵測System.out.print
				return 3;
			}else if(settings.equals("N") && log4jSet.equals("Y") && javaLogger.equals("Y")){
				//偵測log4j,java Logger
				return 4;
			}else if(settings.equals("N") && log4jSet.equals("Y") && javaLogger.equals("N")){
				//偵測log4j
				return 5;
			}else if(settings.equals("N") && log4jSet.equals("N") && javaLogger.equals("Y")){
				//偵測java Logger
				return 6;
			}else{
				//default,只偵測e.printStackTrace
				return 1000;
			}

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
