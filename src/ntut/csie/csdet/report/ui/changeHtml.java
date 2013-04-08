package ntut.csie.csdet.report.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class changeHtml {
	//URL url= new URL("http://www.yahoo.com");
//	URL url;
	String path;
	public changeHtml(String path) throws MalformedURLException{
//		url = new URL(path);	
		this.path = path;
	}
	public void change() throws IOException{
		System.out.println(Jsoup.class.toString());
		Document doc = Jsoup.parse(path);
		
        // Get first table
        Element table = doc.select("div").first();
        // Get td Iterator
        Iterator<Element> ite = table.select("ul").iterator();
        // Print content
        int cnt = 0;
        while(ite.hasNext())
        {
            cnt++;
            System.out.println("Value " + cnt + ": " + ite.next().text());
        }        
	}
}
