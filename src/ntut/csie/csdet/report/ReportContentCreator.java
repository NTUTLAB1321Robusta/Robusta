package ntut.csie.csdet.report;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportContentCreator {

	private static Logger logger = LoggerFactory.getLogger(ReportContentCreator.class);
	String dataPath;
	String resultPath;
	File destFolder;
	
	
	public String getResultPath() {
		return resultPath;
	}
	
	
	public ReportContentCreator(String dataPath) {
		super();
		this.dataPath = dataPath;
		File directory = (new File(dataPath)).getParentFile();
		destFolder = new File(directory.getAbsolutePath() + "/../report/");
		destFolder.mkdirs();
	}
	
	public void buildReportContent() {
		try {
			exportReportResources();
			createJavaScriptData(dataPath);
			resultPath = destFolder.getAbsolutePath() + "/index.html";
		} catch (IOException e) {
			logger.error("[IOException] ", e);
		}
	}
	
	void closeStream(Closeable io) {
		try {
			if (io != null)
				io.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}

	void createJavaScriptData(String xmlFilePath) throws IOException {
		InputStream inputStream = null;
		FileOutputStream outputSteam = null;
		try {
			inputStream = this.getClass().getResourceAsStream(
					"/report/datatransform.xsl");
			Source xslSource = new StreamSource(inputStream);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = null;
			transformer = tf.newTransformer(xslSource);
			
			Source xmlSource = new StreamSource(new File(dataPath));
			File outputFile = new File(destFolder.getAbsolutePath() + "/js/data.js");
			outputFile.getParentFile().mkdirs();
			outputSteam = new FileOutputStream(outputFile);
			Result transfromResult = new StreamResult(outputSteam);
			transformer.transform(xmlSource, transfromResult);
		} catch (TransformerConfigurationException e) {
			logger.error("[Transformer Configuration Exception] EXCEPTION ", e);
		} catch (TransformerException e) {
			logger.error("[Transformer Exception] EXCEPTION ", e);
		} finally {
			closeStream(inputStream);
			closeStream(outputSteam);
		}
	}
	
	private void exportReportResources() {
		InputStream fileListInput = getClass().getResourceAsStream("/report/filelist.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(fileListInput));
		String line;
		try {
			while((line = br.readLine())!= null && line.trim().length() > 0) {
				InputStream input = getClass().getResourceAsStream(line);
				File dest = new File(destFolder.getAbsoluteFile() + "/../" + line);
				copyFileUsingFileStreams(input, dest);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void copyFileUsingFileStreams(InputStream source, File dest) throws IOException {
		dest.getParentFile().mkdirs();
		OutputStream output = null;
		try {
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = source.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			source.close();
			output.close();
		}
	}
}
