package ntut.csie.csdet.preference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class JDomUtil {
	
	//XML tag
	final static public String root = "CodeSmellDetect";
	final static public String project = "ProjectName";
	final static public String DummyHandlerTag = "DummyHandler";
	final static public String systemoutprint = "systemoutprint";
	final static public String printStackTrace = "printStackTrace";
	
	public static Element createXMLContent(){
		Document docJDOM = readXMLFile();	
		Element elementRoot ;
		//�����odocJDOM,���s�b�N�q���إߤ@��
		if(docJDOM == null){
			elementRoot = new Element(root);
			docJDOM = new Document(elementRoot);	
			elementRoot.setAttribute(project,getSelectProject().getResource().getName());
		}else{
			elementRoot = docJDOM.getRootElement();
		}
		return elementRoot;
	}
	
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
	
	public static Document readXMLFile(){
		String path = getProjectPath()+File.separator+"CSPreference.xml";
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
	
	public static IJavaProject getSelectProject(){
		IWorkbench workbench = PlatformUI.getWorkbench();
		ISelection sel =  workbench.getActiveWorkbenchWindow().getActivePage().getSelection();
		if(!(sel instanceof IStructuredSelection)){
			return null;
		}
		//���ouser���I�諸�a��,�A�N���ഫ��Project���|
		IStructuredSelection selection = (IStructuredSelection)sel;	
		IJavaProject project = (IJavaProject) selection.getFirstElement();
		return project;
	}
	
	public static String getProjectPath(){
		String path = getSelectProject().getResource().getLocation().toOSString();
		return path;
	}
}
