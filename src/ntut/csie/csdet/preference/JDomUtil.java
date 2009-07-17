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
 * 主要是用來建立xml檔的(存放在workspace的下面,所有專案共享一個config檔)
 * @author chewei
 */

public class JDomUtil {
	
	//XML tag
	final static public String root = "CodeSmellDetect";
	final static public String project = "ProjectName";
	final static public String DummyHandlerTag = "DummyHandler";
	final static public String OverLoggingTag = "OverLogging";
	final static public String transException = "detectionTransException";
	final static public String eprintstacktrace = "eprintstacktrace";
	final static public String systemoutprint = "systemoutprint";
	final static public String println = "systemoutprintln";
	final static public String apache_log4j = "apache_log4j";
	final static public String java_Logger = "java_Logger";
	final static public String EHSmellFilterTaq = "EHSmellFilter";
	final static public String detUserMethod="detUserMethod";
	
	public static Element createXMLContent(){
		Document docJDOM = readXMLFile();	
		Element elementRoot ;
		//先取得docJDOM,不存在就從先建立一個
		if(docJDOM == null){
			// 建立一個root
			elementRoot = new Element(root);
			docJDOM = new Document(elementRoot);				
		}else{
			elementRoot = docJDOM.getRootElement();
		}
		return elementRoot;
	}
	
	/**
	 * 輸出xml file
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
			System.out.println("XML輸出失敗！！！！");
			
		}
	}
	
	/**
	 * 取得document,順便判斷xml檔本來有沒有存在
	 * @return
	 */
	public static Document readXMLFile(){
		String path = getWorkspace()+File.separator+"CSPreference.xml";
		File xmlPath = new File(path);
		if(xmlPath.exists()){
			Document docJDOM = null;
			//建立SAXBuiler來parse內容
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
	 * 取得workspace的位置
	 * @return
	 */
	public static String getWorkspace(){
		String workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		return workspace;
	}
}
