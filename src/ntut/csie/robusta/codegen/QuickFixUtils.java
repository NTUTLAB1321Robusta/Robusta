package ntut.csie.robusta.codegen;

import java.util.List;

import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class QuickFixUtils {
	/**
	 * When developer give ast, robustness level, and exception type,
	 * this method makes a robustness level annotation.  
	 * This method generate NormalAnnotation of this kind: 
	 * 	&quot;@RTag(level = 2, exception = java.io.FileNotFoundException.class)&quot;
	 * @param ast
	 * @param levelVal
	 * @param exceptionType Qualified name of exception type.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static NormalAnnotation makeRLAnnotation(AST ast, int levelVal, String exceptionType) {
		/*
		 * 這樣一整段，是一種NormalAnnotation
		 * @Robustness(value = {
            @RTag(level = 2, exception = java.io.FileNotFoundException.class),
            @RTag(level = 2, exception = java.io.IOException.class) })
         *   
         * 這樣一整段，都是MemberValuePair
		 * value = {
            @RTag(level = 2, exception = java.io.FileNotFoundException.class),
            @RTag(level = 2, exception = java.io.IOException.class) }
         *
         * 在MemberValuePair中的Value裡面，下面這段是ArrayInitializer
         * {
            @RTag(level = 2, exception = java.io.FileNotFoundException.class),
            @RTag(level = 2, exception = java.io.IOException.class) }
         *
         * 在ArrayInitializer裡的其中一個元素，又是另一段Normal Annotation
           @RTag(level = 2, exception = java.io.FileNotFoundException.class)
         * 
         * 在這段NormalAnnotation裡面，有兩個MemberValuePair
           level = 2
           exception = java.io.FileNotFoundException.class
         *
         * 在 level = 2這個MemberValuePair中，2 是NumberLiteral
           
           exception = java.io.FileNotFoundException.class，
           java.io.FileNotFoundException.class 是TypeLiteral
         * 
		 */
		
		//要建立@RTag(level=1, exception=java.lang.RuntimeException.class)這樣的Annotation
		NormalAnnotation rlNormalAnnotation = ast.newNormalAnnotation();
		rlNormalAnnotation.setTypeName(ast.newSimpleName(RTag.class.getSimpleName()));

		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		//throw statement 預設level = 1
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		//TODO HOW TO ADD without warning
		rlNormalAnnotation.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair rlException = ast.newMemberValuePair();
		rlException.setName(ast.newSimpleName(RTag.EXCEPTION));
		TypeLiteral exclass = ast.newTypeLiteral();
		exclass.setType(ast.newSimpleType(ast.newName(exceptionType)));
		rlException.setValue(exclass);
		//TODO HOW TO ADD without warning
		rlNormalAnnotation.values().add(rlException);

		return rlNormalAnnotation;
	}
	
	/**
	 * Get the existing Robustness Annotation.
	 * @param methodDeclaration
	 * @return Full Robustness Annotation if existing, null if not.
	 */
	public static NormalAnnotation getExistingRLAnnotation(MethodDeclaration methodDeclaration) {
		for(Object iExtendedModifier: methodDeclaration.modifiers()) {
			IExtendedModifier iem = (IExtendedModifier) iExtendedModifier;
			if(iem.isAnnotation() && iem.toString().indexOf("@" + Robustness.class.getSimpleName()) != -1) {
				return (NormalAnnotation)iem;
			}
		}
		return null;
	}

	/**
	 * Detect if the indicated class is imported in source code 
	 * @param clazz The indicated class.
	 * @param lstImportDeclaration Source code, the list of importing classes.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static boolean isClassImported(Class<?> clazz, CompilationUnit cu){
		// TODO unchecked type
		List<ImportDeclaration> lstImportDeclaration = cu.imports();
		for (ImportDeclaration id : lstImportDeclaration) {
			if (clazz.getName().equals(
					id.getName().getFullyQualifiedName())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 在指定的method declaration上面宣告拋出例外。
	 * 如果有已經有宣告拋出例外，就不再宣告。
	 * @param ast
	 * @param methodDeclaration
	 * @param exceptionName
	 */
	@SuppressWarnings("unchecked")
	public static void addThrowExceptionOnMethodDeclaration(AST ast, MethodDeclaration methodDeclaration, String exceptionName) {
		// TODO unchecked type
		List<SimpleName> thrownExceptions = methodDeclaration.thrownExceptions();
		// 沒有任何拋出例外的宣告，那就直接加上去
		if(thrownExceptions.size() == 0) {
			thrownExceptions.add(ast.newSimpleName(exceptionName));
			return;
		}
		
		// 有拋出例外的宣告，再檢查後才拋出
		for(SimpleName thrownEx : thrownExceptions) {
			if(thrownEx.getIdentifier().equals(exceptionName)) {
				thrownExceptions.add(ast.newSimpleName(exceptionName));
				return;
			}
		}
	}

	/**
	 * 在特定Method上增加強健度等級註記
	 * @param ast
	 * @param compilationUnit
	 * @param methodDeclaration
	 * @param exception
	 */
	public static void addRobustnessLevelAnnotation(AST ast, CompilationUnit compilationUnit, MethodDeclaration methodDeclaration, String exception) {
		NormalAnnotation normalAnnotation = ast.newNormalAnnotation();
		normalAnnotation.setTypeName(ast.newSimpleName(Robustness.class
				.getSimpleName()));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName(Robustness.VALUE));
		normalAnnotation.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		List<RLMessage> existedRobustnessLevelAnnotation = getExceptionCodeSmells(compilationUnit, methodDeclaration);
		if(existedRobustnessLevelAnnotation.size() == 0) {
			// 本來沒有強健度等級註記
			rlary.expressions().add(getRobustnessAnnotation(ast, 1,	RuntimeException.class.getSimpleName()));
		}else {
			// 本來就有強健度等級註記
			for(RLMessage robustnessAnnotation : existedRobustnessLevelAnnotation) {
				int pos = robustnessAnnotation.getRLData().getExceptionType().lastIndexOf(".");
				String cut = robustnessAnnotation.getRLData().getExceptionType().toString().substring(pos+1);

				if((!cut.equals(exception)) && (robustnessAnnotation.getRLData().getLevel() == 1)) {					
					rlary.expressions().add(
							getRobustnessAnnotation(ast, robustnessAnnotation.getRLData().getLevel(), 
									robustnessAnnotation.getRLData().getExceptionType()));	
				}
			}
			rlary.expressions().add(getRobustnessAnnotation(ast,1,exception));
			
			List<IExtendedModifier> modifiers = methodDeclaration.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//找到舊有的annotation後將它移除
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf(Robustness.class.getSimpleName()) != -1) {
					methodDeclaration.modifiers().remove(i);
					break;
				}
			}
		}
		
		if (rlary.expressions().size() > 0) {
			methodDeclaration.modifiers().add(0, normalAnnotation);
		}
		
		addRobustnessAndRTagImporting(compilationUnit);
	}
	
	/**
	 * 加上強健度等級註記的import資訊
	 * @param compilationUnit
	 */
	private static void addRobustnessAndRTagImporting(CompilationUnit compilationUnit) {
		// 判斷是否已經Import Robustness及RL的宣告
		List<ImportDeclaration> importList = compilationUnit.imports();
		//是否已存在Robustness及RL的宣告
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;

		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName())) {
				isImportRobustnessClass = true;
			}
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName())) {
				isImportRLClass = true;
			}
		}

		AST rootAst = compilationUnit.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			compilationUnit.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			compilationUnit.imports().add(imp);
		}
	}
	
	private static NormalAnnotation getRobustnessAnnotation(AST ast, int levelVal, String excption) {
		// 要建立@Tag(level=1,
		// exception=java.lang.RuntimeException.class)這樣的Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName(RTag.class.getSimpleName().toString()));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		// throw statement 預設level = 1
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName(RTag.EXCEPTION));
		TypeLiteral exclass = ast.newTypeLiteral();
		// 預設為RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}

	/**
	 * 從特定Method上，取得指定的例外處理壞味道
	 * 
	 * @param problem 你想要蒐集的例外處理壞味道
	 * @param methodDeclaration 你指定從哪個Method上蒐集
	 * @return
	 */
	public static List<RLMessage> getExceptionCodeSmells(
			CompilationUnit compilationUnit, MethodDeclaration methodDeclaration) {
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(
				compilationUnit, methodDeclaration.getStartPosition(), 0);
		methodDeclaration.accept(exVisitor);
		
		return exVisitor.getMethodRLAnnotationList(); 
	}
	
	/**
	 * 從IResource取得CompilationUnit
	 * @param resource
	 * @return
	 */
	public static CompilationUnit getCompilationUnit(IResource resource) {
		CompilationUnit compilationUnit = null;
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {

				IJavaElement javaElement = JavaCore.create(resource);
				
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
	
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				compilationUnit = (CompilationUnit) parser.createAST(null);
				
				//AST 2.0紀錄方式
				compilationUnit.recordModifications();			
		}
		return compilationUnit;
	}
	
	/**
	 * 根據MethodDeclaration的索引編號，從CompilationUnit取出MethodDeclaration node
	 * @param compilationUnit
	 * @param methodIndex
	 * @return
	 */
	public static MethodDeclaration getMethodDeclaration(CompilationUnit compilationUnit, int methodIndex) {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		//取得目前要被修改的method node
		return methodList.get(methodIndex);
	}
	
	/**
	 * 增加一個拋出例外的宣告
	 * @param ast
	 * @param exceptionType
	 * @return
	 */
	public static ASTNode generateThrowExceptionForDeclaration(AST ast, Class<?> exceptionType) {
		SimpleName sn = ast.newSimpleName(exceptionType.getSimpleName());
		return sn;
	}
	
	/**
	 * @see QuickFixUtils#generateThrowNewExceptionNode(String, AST, String)
	 * @param exceptionVariableName
	 * @param ast
	 * @param exceptionType
	 * @return
	 */
	public static ASTNode generateThrowNewExceptionNode(String exceptionVariableName, AST ast, Class<?> exceptionType) {
		return generateThrowNewExceptionNode(exceptionVariableName, ast, exceptionType.getSimpleName());
	}

	/**
	 * 產生throw new xxxException()的節點
	 * @param ast
	 * @param exceptionVariableName catch(IOException e)裡面的e
	 * @param exceptionType 要重新拋出的例外類型
	 * @return
	 */
	@SuppressWarnings("unchecked")	
	public static ASTNode generateThrowNewExceptionNode(String exceptionVariableName, AST ast, String exceptionType) {
		// 自行建立一個throw statement加入
		ThrowStatement ts = ast.newThrowStatement();
		
		// 將throw的variable傳入
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		// throw new RuntimeException()
		cic.setType(ast.newSimpleType(ast.newSimpleName(exceptionType)));
		// TODO: How to add without warning
		// 將throw new RuntimeException(ex)括號中加入參數
		cic.arguments().add(ast.newSimpleName(exceptionVariableName));
		ts.setExpression(cic);
		return ts;
	}
	
	/**
	 * 將指定的catch clause中原來拋出的例外轉型成其他例外拋出
	 * @param cc 指定的catch clause
	 * @param ast CompilationUnit的AST
	 * @param exceptionType 想要轉型成此種例外拋出
	 */
	@SuppressWarnings("unchecked")
	public static void addThrowRefinedException(CatchClause cc, AST ast, String exceptionType) {
		ASTNode throwStatement = generateThrowNewExceptionNode(cc.getException().resolveBinding().getName(), ast, exceptionType);
		cc.getBody().statements().add(throwStatement);	
	}
	
	/**
	 * @see QuickFixUtils#addThrowRefinedException(CatchClause, AST, String)
	 * @param cc
	 * @param ast
	 * @param exceptionType
	 */
	public static void addThrowRefinedException(CatchClause cc, AST ast, Class<?> exceptionType) {
		addThrowRefinedException(cc, ast, exceptionType.getSimpleName());
	}
	
	public static void removeStatementsInCatchClause(CatchClause catchClause, String... statements) {
		for(String removingStatement : statements) {
			ExpressionStatementStringFinderVisitor expressionStatementFinder = new ExpressionStatementStringFinderVisitor(removingStatement);
			catchClause.accept(expressionStatementFinder);
			ASTNode removeNode = expressionStatementFinder.getFoundExpressionStatement();
			if(removeNode != null) {
				removeNode.delete();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void addImportDeclaration(CompilationUnit compilationUnit, IType exceptionType) {
		// 判斷是否有import library
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = compilationUnit.imports();
		for(ImportDeclaration id : importList) {
			if(exceptionType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())) {
				isImportLibrary = true;
				break;
			}
		}
		
		// 假如沒有import就加入到AST中
		AST compilationUnitAST = compilationUnit.getAST(); 
		if(!isImportLibrary) {
			ImportDeclaration imp = compilationUnitAST.newImportDeclaration();
			imp.setName(compilationUnitAST.newName(exceptionType.getFullyQualifiedName()));
			compilationUnit.imports().add(imp);
		}
	}
	
	public static IOpenable getIOpenable(IResource iResource) {
		IJavaElement javaElement = JavaCore.create(iResource);
		if(javaElement instanceof IOpenable) {
			return (IOpenable) javaElement;
		}
		return null;
	}
}
