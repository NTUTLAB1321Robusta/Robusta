package ntut.csie.csdet.preference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * �D�n�O�Ψӫإ�xml�ɪ�(�s��bworkspace���U��,�Ҧ��M�צ@�ɤ@��config��)
 * @author chewei
 */

public class JDomUtil {
	
	//XML tag
	final static public String root = "CodeSmellDetect";
	final static public String project = "ProjectName";
	final static public String DummyHandlerTag = "DummyHandler";
	final static public String systemoutprint = "systemoutprint";
	final static public String println = "systemoutprintln";
	
	public static Element createXMLContent(){
		Document docJDOM = readXMLFile();	
		Element elementRoot ;
		//�����odocJDOM,���s�b�N�q���إߤ@��
		if(docJDOM == null){
			// �إߤ@��root
			elementRoot = new Element(root);
			docJDOM = new Document(elementRoot);				
		}else{
			elementRoot = docJDOM.getRootElement();
		}
		return elementRoot;
	}
	
	/**
	 * ��Xxml file
	 * @param docXML
	 * @param path
	 */
	public static void OutputXMLFile(Document docXML,String path){
		try{
			XMLOutputter xmlOut = new XMLOutputter();
			FileOutputStream fout = new FileOutputStream(path);
			xmlOut.setFormat(Format.getPrettyFormat());
			xmlOut.output(docXML, fout);
			xmlOut.output(docXML,System.out);
			fout.close();
		}catch(IOException e){
			System.out.println("XML��X���ѡI�I�I�I");
		}
	}
	
	/**
	 * ���odocument,���K�P�_xml�ɥ��Ӧ��S���s�b
	 * @return
	 */
	public static Document readXMLFile(){
		String path = getWorkspace()+File.separator+"CSPreference.xml";
		File xmlPath = new File(path);
		if(xmlPath.exists()){
			Document docJDOM = null;
			//�إ�SAXBuiler��parse���e
			SAXBuilder builder = new SAXBuilder();
			try{
				docJDOM = builder.build(xmlPath);
			}catch(JDOMException e){
				e.printStackTrace();
			}catch (Exception ex) {
				ex.printStackTrace();
			}		
				return docJDOM;
		}else{
				return null;
		}
	}
	
//	/**
//	 * ��user�h�]�w�@��preference��,�N�i�H�Q�Φ�method
//	 * �Ө��o�{�buser���I�諸project
//	 * @return
//	 */
//	public static IJavaProject getSelectProject(){
//		IProject project = RLNature.project;
//		if(project !=null){
//			IJavaProject javaProject = JavaCore.create(project);
//			return javaProject;
//		}		
//		else{
//			// ��project����null��ܨϥΪ��٥��[nature
//			IWorkbench workbench = PlatformUI.getWorkbench();
//			ISelection sel = workbench.getActiveWorkbenchWindow().getActivePage().getSelection();
//			//���ouser���I�諸�a��,�A�N���ഫ��Project���|
//			IStructuredSelection selection = (IStructuredSelection)sel;	
//			IJavaProject javaproject = (IJavaProject) selection.getFirstElement();
//			return javaproject;
//		}
//
//	}
	
	public static String getWorkspace(){
		String workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		return workspace;
	}
}
