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
 * 主要是用來建立XML檔的(存放在workspace的下面,所有專案共享一個config檔)
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

	/** Packet_Punctuation_SquareBracket_Left<br />
	 *  &quot;EH_LEFT&quot;
	 */
	final static public String EH_Left = "EH_LEFT";
	/** Packet_Punctuation_SquareBracket_Right<br /> 
	 *  &quot;EH_RIGHT&quot;
	 */
	final static public String EH_Right = "EH_RIGHT";
	/** Punctuation_Star<br />
	 *  &quot;EH_STAR&quot;
	 */
	final static public String EH_Star = "EH_STAR";

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
			throw new RuntimeException(e);
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
				throw new RuntimeException(e);
			}catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
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
