package ntut.csie.csdet.visitor;

import static org.junit.Assert.*;

import java.io.File;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DummyHandlerVisitorTest {
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;
	DummyHandlerVisitor dummyhandlerBSV;
	
	public DummyHandlerVisitorTest() {
	}
	
	@Before
	public void setUp() throws Exception {
		// Ū�������ɮ׼˥����e
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, "test");
		
		jpm = new JavaProjectMaker("DummyHandlerTest");
		jpm.setJREDefaultContainer();
		// �s�W�����J��library
		jpm.addJarToBuildPath("lib\\log4j-1.2.15.jar");
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		jpm.createJavaFile("ntut.csie.exceptionBadSmells", "DummyHandlerExample.java", "package ntut.csie.exceptionBadSmells;\n" + jfs.getFileContent());
		// �إ�XML
		CreateDummyHandlerXML();
		
		Path path = new Path("DummyHandlerTest\\src\\ntut\\csie\\exceptionBadSmells\\DummyHandlerExample.java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
		unit = (CompilationUnit) parser.createAST(null); 
		unit.recordModifications();
		dummyhandlerBSV = new DummyHandlerVisitor(unit);
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// �p�Gxml�ɮצs�b�A�h�R����
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// �R���M��
		jpm.deleteProject();
	}

	@Test
	public void testGetSpecifiedParentNode() {
		ASTNode result = dummyhandlerBSV.getSpecifiedParentNode(unit, ASTNode.COMPILATION_UNIT);
		assertNull(result);
	}
	
	@Test
	public void testVisitNode() {
		int dummy = 0;
		unit.accept(dummyhandlerBSV);
		if(dummyhandlerBSV.getDummyList() != null)
			dummy = dummyhandlerBSV.getDummyList().size();
		
		// �����`�@���X��bad smell
		assertEquals(14, dummy);
	}
	
	/**
	 * �إ�CSPreference.xml�ɮ�
	 */
	private void CreateDummyHandlerXML() {
		//����XML��root
		Element root = JDomUtil.createXMLContent();

		//�إ�Dummy Handler��Tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//���pe.printStackTrace���Q�Ŀ�_��
		rule.setAttribute(JDomUtil.e_printstacktrace,"Y");

		//���psystem.out.println���Q�Ŀ�_��
		rule.setAttribute(JDomUtil.systemout_print,"Y");
		
		rule.setAttribute(JDomUtil.apache_log4j,"Y");
		rule.setAttribute(JDomUtil.java_Logger,"Y");

		//��ϥΪ̦ۭq��Rule�s�JXML
		Element libRule = new Element("librule");
		
		//�N�s�ت�tag�[�i�h
		dummyHandler.addContent(rule);
		dummyHandler.addContent(libRule);

		if (root.getChild(JDomUtil.DummyHandlerTag) != null)
			root.removeChild(JDomUtil.DummyHandlerTag);

		root.addContent(dummyHandler);

		//�N�ɮ׼g�^
		String path = JDomUtil.getWorkspace() + File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}
}
