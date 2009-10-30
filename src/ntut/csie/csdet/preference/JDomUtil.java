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
 * �D�n�O�Ψӫإ�XML�ɪ�(�s��bworkspace���U��,�Ҧ��M�צ@�ɤ@��config��)
 * @author chewei
 */

public class JDomUtil {
	
	//XML Tag
	final static public String root = "CodeSmellDetect";
	//Main
	final static public String DetectSmellTag = "DetectSmell";
	final static public String detect_all = "detectall";
	final static public String det_smell_type = "detsmelltype";
	//Dummy Handler
	final static public String DummyHandlerTag = "DummyHandler";
	final static public String e_printstacktrace = "eprintstacktrace";
	final static public String systemout_print = "systemoutprint";
	final static public String apache_log4j = "apache_log4j";
	final static public String java_Logger = "java_Logger";
	//OverLogging
	final static public String OverLoggingTag = "OverLogging";
	final static public String trans_Exception = "detectionTransException";
	//Careless CleanUp
	final static public String CarelessCleanUpTag="CarelessCleanUp";
	final static public String det_user_method="detusermethod";
	//Filter
	final static public String EHSmellFilterTaq = "EHSmellFilter";
	final static public String project_name = "ProjectName";

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
	
	/**
	 * ���oworkspace����m
	 * @return
	 */
	public static String getWorkspace(){
		String workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		return workspace;
	}
}
