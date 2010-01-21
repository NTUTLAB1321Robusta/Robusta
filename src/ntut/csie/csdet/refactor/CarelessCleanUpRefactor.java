package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.CarelessCleanUpAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Careless CleanUp Refactoring的具體操作都在這個class中
 * @author Min
 */
public class CarelessCleanUpRefactor extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(CarelessCleanUpRefactor.class);
	
	private IJavaProject project;
		
	//使用者所點選的Marker
	private IMarker marker;
	
	private IOpenable actOpenable;
	
	private TextFileChange textFileChange;
	
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	private List<CSMessage> CarelessCleanUpList=null;
	
	String msgIdx;
	String methodIdx;
	
	//methodName的變數名稱,預設是close
	private String methodName;
	
	//modifier的Type，預設是private
	private String modifierType;
	
	//log的type,預設是e.printStackTrace
	private String logType;
	
	//欲刪除的程式碼資訊
	private String delLine;
	
	private ExpressionStatement delLineES;
	
	private Block finallyBlock;

	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//去修改AST Tree
		collectChange(marker.getResource());
		//不需check final condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}
	
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//不需check initial condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}
	
	
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		//把要變更的結果包成composite傳出去
		Change[] changes = new Change[] {textFileChange};
		CompositeChange change = new CompositeChange("My Extract Method", changes);
		return change;
	}
	
	@Override
	public String getName() {		
		return "My Extract Method";
	}
	
	/**
	 * 把marker傳進來供此class存取一些code smell資訊
	 * @param marker
	 */
	public void setMarker(IMarker marker){
		this.marker = marker;
		this.project = JavaCore.create(marker.getResource().getProject());
	}
	
	/**
	 * parse AST Tree並取得要修改的method node
	 * @param resource
	 */
	private void collectChange(IResource resource){
		//取得要修改的CompilationUnit
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				
				if (javaElement instanceof IOpenable) {
					this.actOpenable = (IOpenable) javaElement;
				}
				
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.actRoot = (CompilationUnit) parser.createAST(null);

				//取得該class所有的method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();
				methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				
				//取得目前要被修改的method node
				this.currentMethodNode = methodList.get(Integer.parseInt(methodIdx));
				
				if(currentMethodNode != null){
					CarelessCleanUpAnalyzer visitor = new CarelessCleanUpAnalyzer(this.actRoot);
					currentMethodNode.accept(visitor);
					//取得code smell的List
					CarelessCleanUpList = visitor.getCarelessCleanUpList();	
					
					myExtractMethod();
					
				}
			
			}catch (Exception ex) {
				logger.error("[Find CS Method] EXCEPTION ",ex);
			}
		}
	}
	/**
	 * set methodName變數名稱
	 */
	public RefactoringStatus setMethodName(String methodName){
		if(methodName.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Method Name is empty");
		}else{
			this.methodName = methodName;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set modifier Type
	 */
	public RefactoringStatus setModifierType(String modifierType){
		if(modifierType.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.modifierType = modifierType;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set Log type
	 */
	public RefactoringStatus setLogType(String logType){
		if(logType.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.logType = logType;
			return new RefactoringStatus();
		}
	}
	
	private void myExtractMethod(){
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();
					
		//若try Statement裡沒有Finally Block,建立Finally Block
		judgeFinallyBlock(ast);
		
		//取得欲刪除的程式碼
		findDelLine();
		
		//刪除fos.close();
		delLine();
		
		//在finally中加入closeStream(fos)
		extractMethod(ast);
		
		//寫回Edit中
		applyChange();
	}
	
	
	private void findDelLine(){
		CSMessage csMsg=CarelessCleanUpList.get(Integer.parseInt(msgIdx));
		delLine=csMsg.getStatement();
	}
	
	/**
	 * 判斷Try Statment是否有Finally Block
	 * @return boolean
	 */
	private void judgeFinallyBlock(AST ast){
		//取得方法中所有的statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement ts=(TryStatement) statement.get(i);
				Block finallyBlock=ts.getFinally();
				if(finallyBlock==null){
					Block block=ast.newBlock();
					ts.setFinally(block);
					break;
				}
			}
		}
	}
	
	private void delLine(){
		//取得方法中所有的statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement ts=(TryStatement) statement.get(i);
				Block tsBlock=ts.getBody();
				List<?> tsStat=tsBlock.statements();
				//比對Try Statement裡是否有欲移動的程式碼,若有則移除
				for(int j=0; j<tsStat.size(); j++){
					String temp = tsStat.get(j).toString();
					if(temp.contains(delLine)){
						delLineES=(ExpressionStatement) tsStat.get(j);
						tsStat.remove(j);						
						break;
					}
				}
			}
		}
	}
	
	private void extractMethod(AST ast){
		//取得資訊
		MethodInvocation delLineMI=(MethodInvocation) delLineES.getExpression();
		Expression exp=delLineMI.getExpression();
		SimpleName sn=(SimpleName)exp;
		
		
		//取得方法中所有的statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		//Block finallyBlock;
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement ts=(TryStatement) statement.get(i);
				finallyBlock=ts.getFinally();
				break;
			}
		}
		
		//新增的Method Invocation
		MethodInvocation newMI=ast.newMethodInvocation();
		//設定MI的name
		newMI.setName(ast.newSimpleName(methodName));
		//設定MI的參數
		newMI.arguments().add(ast.newSimpleName(sn.getIdentifier()));
		
		ExpressionStatement es=ast.newExpressionStatement((Expression)newMI);
		finallyBlock.statements().add(es);
		
		//新增Method Declaration
		MethodDeclaration newMD=ast.newMethodDeclaration();
		
		//設定存取型別(public)
		if(modifierType=="public"){
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));			
		}else if(modifierType=="protected"){
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD));						
		}else{
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));									
		}
		//設定return type
		newMD.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		//設定MD的名稱
		newMD.setName(ast.newSimpleName(methodName));
		//設定參數
		SingleVariableDeclaration svd=ast.newSingleVariableDeclaration();
		svd.setType(ast.newSimpleType(ast.newSimpleName(exp.resolveTypeBinding().getName().toString())));
		svd.setName(ast.newSimpleName(sn.getIdentifier()));
		newMD.parameters().add(svd);
		
		//設定body
		Block block=ast.newBlock();
		newMD.setBody(block);
		
		TryStatement ts = ast.newTryStatement();
		Block tsBody=ts.getBody();
		tsBody.statements().add(delLineES);
		
		//替try 加入一個Catch clause
		List<CatchClause> catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		
		//存放程式碼所拋出的例外類型
		ITypeBinding[] iType;
		iType=delLineMI.resolveMethodBinding().getExceptionTypes();
	
		//建立catch的type為 catch(... ex)
		SingleVariableDeclaration svdCatch = ast.newSingleVariableDeclaration();
		svdCatch.setType(ast.newSimpleType(ast.newSimpleName(iType[0].getName())));
		svdCatch.setName(ast.newSimpleName("e"));
		cc.setException(svdCatch);

		//加入catch的body
		if(logType=="e.printStackTrace();"){
			//新增的Method Invocation
			MethodInvocation catchMI=ast.newMethodInvocation();
			//設定MI的name
			catchMI.setName(ast.newSimpleName("printStackTrace"));
			//設定MI的Expression
			catchMI.setExpression(ast.newSimpleName("e"));			
			ExpressionStatement catchES=ast.newExpressionStatement((Expression)catchMI);
			cc.getBody().statements().add(catchES);
		}else{
			//判斷是否有import java.util.logging.Logger
			boolean isImportLibrary = false;
			List<ImportDeclaration> importList = actRoot.imports();
			for(ImportDeclaration id : importList){
				if(id.getName().getFullyQualifiedName().contains("java.util.logging.Logger")){
					isImportLibrary = true;
				}
			}
			
			//假如沒有import,就加入到AST中
			AST rootAst = actRoot.getAST(); 
			if(!isImportLibrary){
				ImportDeclaration imp = rootAst.newImportDeclaration();
				imp.setName(rootAst.newName("java.util.logging.Logger"));
				actRoot.imports().add(imp);
			}
			
			//加入private Logger logger = Logger.getLogger(LoggerTest.class.getName());
			VariableDeclarationFragment vdf=ast.newVariableDeclarationFragment();
			//設定logger
			vdf.setName(ast.newSimpleName("logger"));
			
			//vdf的initializer的Method Invocation
			MethodInvocation initMI=ast.newMethodInvocation();
			//設定initMI的Name
			initMI.setName(ast.newSimpleName("getLogger"));
			//設定initMI的Expression
			initMI.setExpression(ast.newSimpleName("Logger"));
			
			//設定initMI的arguments的Method Invocation
			MethodInvocation arguMI=ast.newMethodInvocation();
			//設定arguMI的Name
			arguMI.setName(ast.newSimpleName("getName"));
			
			//設定arguMI的Expression
			
			//設定Expression的Type Literal
			TypeLiteral tl=ast.newTypeLiteral();
			//設定tl的Type
			//取得class Name
			ICompilationUnit icu = (ICompilationUnit) actOpenable;
			String javaName=icu.getElementName();
			String className=javaName.substring(0, javaName.length()-5);
			tl.setType(ast.newSimpleType(ast.newName(className)));
			
			arguMI.setExpression(tl);
			
			initMI.arguments().add(arguMI);
			
			vdf.setInitializer(initMI);

			FieldDeclaration fd=ast.newFieldDeclaration(vdf);
			fd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
			fd.setType(ast.newSimpleType(ast.newName("Logger")));
			
			List<AbstractTypeDeclaration> typeList=actRoot.types();
			TypeDeclaration td=(TypeDeclaration) typeList.get(0);
			td.bodyDeclarations().add(0, fd);
			
			//新增的Method Invocation
			MethodInvocation catchMI=ast.newMethodInvocation();
			//設定MI的name
			catchMI.setName(ast.newSimpleName("info"));
			//設定MI的Expression
			catchMI.setExpression(ast.newSimpleName("logger"));
			
			//設定catch的body的Method Invocation
			MethodInvocation cbMI=ast.newMethodInvocation();
			//設定cbMI的Name
			cbMI.setName(ast.newSimpleName("info"));
			//設定cbMI的Expression
			cbMI.setExpression(ast.newSimpleName("logger"));
			
			//設定cbMI的arguments的Method Invocation
			MethodInvocation cbarguMI=ast.newMethodInvocation();
			cbarguMI.setName(ast.newSimpleName("getMessage"));
			cbarguMI.setExpression(ast.newSimpleName("e"));
			
			cbMI.arguments().add(cbarguMI);
			
			ExpressionStatement catchES=ast.newExpressionStatement((Expression)cbMI);
			cc.getBody().statements().add(catchES);
			
		}
		catchStatement.add(cc);

		//將新增的try statement加進來
		block.statements().add(ts);
		//將new MD加入
		List<AbstractTypeDeclaration> typeList=actRoot.types();
		TypeDeclaration td=(TypeDeclaration) typeList.get(0);
		td.bodyDeclarations().add(newMD);
	}
	
	/**
	 * 取得JavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}
	/**
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange(){
		//寫回Edit中
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
		}catch (JavaModelException e) {
			logger.error("[Apply Change My Extract Method] EXCEPTION",e);
		}
	}
}
