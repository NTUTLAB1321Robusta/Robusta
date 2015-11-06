package ntut.csie.robusta.codegen;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
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
		 * this paragraph is NormalAnnotation
		 * @Robustness(value = {
            @RTag(level = 2, exception = java.io.FileNotFoundException.class),
            @RTag(level = 2, exception = java.io.IOException.class) })
         *   
         * this paragraph is MemberValuePair
		 * value = {
            @RTag(level = 2, exception = java.io.FileNotFoundException.class),
            @RTag(level = 2, exception = java.io.IOException.class) }
         *
         * in MemberValuePair, this is ArrayInitializer
         * {
            @RTag(level = 2, exception = java.io.FileNotFoundException.class),
            @RTag(level = 2, exception = java.io.IOException.class) }
         *
         * one element in ArrayInitializer is a Normal Annotation
           @RTag(level = 2, exception = java.io.FileNotFoundException.class)
         * 
         *  there are two MemberValuePair in this NormalAnnotation
           level = 2
           exception = java.io.FileNotFoundException.class
         *
         * in "level = 2", 2 is NumberLiteral
           
           exception = java.io.FileNotFoundException.class，
           java.io.FileNotFoundException.class 是TypeLiteral
         * 
		 */
		
		//to generate @RTag(level=1, exception=java.lang.RuntimeException.class) this kind of Annotation
		NormalAnnotation rlNormalAnnotation = ast.newNormalAnnotation();
		rlNormalAnnotation.setTypeName(ast.newSimpleName(RTag.class.getSimpleName()));

		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		//default level of throw statement is 1
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
	 * add exception declaration on specified method declaration 
	 * it would not declare exception if there has been a exception declaration on method declaration.
	 * @param ast
	 * @param methodDeclaration
	 * @param exceptionName
	 */
	@SuppressWarnings("unchecked")
	public static void addThrowExceptionOnMethodDeclaration(AST ast, MethodDeclaration methodDeclaration, String exceptionName) {
		// TODO unchecked type
		List<SimpleName> thrownExceptions = methodDeclaration.thrownExceptions();
		//add an exception declaration if there are not any exception declaration.
		if(thrownExceptions.size() == 0) {
			thrownExceptions.add(ast.newSimpleName(exceptionName));
			return;
		}
		
		//check whether there are duplicate exception declaration.
		for(SimpleName thrownEx : thrownExceptions) {
			if(thrownEx.getIdentifier().equals(exceptionName)) {
				thrownExceptions.add(ast.newSimpleName(exceptionName));
				return;
			}
		}
	}

	/**
	 * add robustness level annotation on specified method
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
		//check whether has added robust level annotation
		if(existedRobustnessLevelAnnotation.size() == 0) {
			rlary.expressions().add(getRobustnessAnnotation(ast, 1,	RuntimeException.class.getSimpleName()));
		}else {
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
				// remove original annotation
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
	 * import Robustness class and RTag class
	 * @param compilationUnit
	 */
	private static void addRobustnessAndRTagImporting(CompilationUnit compilationUnit) {
		// check whether has imported Robustness class and RTag class
		List<ImportDeclaration> importList = compilationUnit.imports();

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
		// establish @Tag(level=1,
		// Annotation will be like exception=java.lang.RuntimeException.class)
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName(RTag.class.getSimpleName().toString()));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		//default level of throw statement is 1
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName(RTag.EXCEPTION));
		TypeLiteral exclass = ast.newTypeLiteral();
		// default exception is RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}

	/**
	 * get the bad smells of exception handling from specified method 
	 * 
	 * @param CompilationUnit the class file of specified method
	 * @param methodDeclaration specified method
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
	 * get CompilationUnit from IResource 
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
				
				compilationUnit.recordModifications();			
		}
		return compilationUnit;
	}
	
	/**
	 * take MethodDeclaration as index to get MethodDeclaration node from CompilationUnit
	 * @param compilationUnit
	 * @param methodIndex
	 * @return
	 */
	public static MethodDeclaration getMethodDeclaration(CompilationUnit compilationUnit, int methodIndex) {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		return methodList.get(methodIndex);
	}
	
	/**
	 * generate an exception declaration will been thrown
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
	 * generate an ASTNode of throw new xxxException()
	 * @param ast
	 * @param exceptionVariableName 
	 *                   the name of e in "catch(IOException e)"
	 * @param exceptionType 
	 * @return
	 */
	@SuppressWarnings("unchecked")	
	public static ASTNode generateThrowNewExceptionNode(String exceptionVariableName, AST ast, String exceptionType) {
		ThrowStatement ts = ast.newThrowStatement();
		
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		cic.setType(ast.newSimpleType(ast.newSimpleName(exceptionType)));
		cic.arguments().add(ast.newSimpleName(exceptionVariableName));
		ts.setExpression(cic);
		return ts;
	}
	
	/**
	 * cast exception,thrown from catch clause, to another exception type
	 * @param cc specified catch clause
	 * @param ast CompilationUnit
	 * @param exceptionType another exception type want to be casted to
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
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = compilationUnit.imports();
		for(ImportDeclaration id : importList) {
			if(exceptionType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())) {
				isImportLibrary = true;
				break;
			}
		}
		
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
