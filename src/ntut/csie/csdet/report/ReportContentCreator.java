package ntut.csie.csdet.report;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import ntut.csie.csdet.preference.RobustaSettings;

import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportContentCreator {
	private static Logger logger = LoggerFactory.getLogger(ReportContentCreator.class);
	private final String REPORT_FILE_LIST = "/report/filelist.txt";
	private String outputDataFilePath;
	private String inputXslTransformPath;
	private Document inputXmlDocument;
	private String projectName;
	private File destFolder;

	public ReportContentCreator(String outputDataFilePath, String inputXslTransformPath, Document inputXmlDocument, String projectName) {
		super();
		this.outputDataFilePath = outputDataFilePath;
		this.inputXslTransformPath = inputXslTransformPath;
		this.inputXmlDocument = inputXmlDocument;
		this.projectName = projectName;

		String reportFolder = RobustaSettings.getRobustaReportFolder(this.projectName);
		File directory = new File(reportFolder);
		destFolder = new File(directory.getAbsolutePath() + "/report/");
		destFolder.mkdirs();
	}

	public String getDestinationFolderPath() {
		return destFolder.getAbsolutePath();
	}

	public void transformDataFile() {
		InputStream inputStream = null;
		FileOutputStream outputStream = null;
		try {
			inputStream = this.getClass().getResourceAsStream(inputXslTransformPath);
			Source xslSource = new StreamSource(inputStream);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = null;
			transformer = tf.newTransformer(xslSource);

			File outputFile = new File(destFolder.getAbsolutePath() + outputDataFilePath);
			outputFile.getParentFile().mkdirs();

			outputStream = new FileOutputStream(outputFile);
			Result transformResult = new StreamResult(outputStream);
			transformer.transform((Source) new JDOMSource(inputXmlDocument), transformResult);
		} catch (TransformerConfigurationException e) {
			logger.error("[Transformer Configuration Exception] EXCEPTION ", e);
		} catch (TransformerException e) {
			logger.error("[Transformer Exception] EXCEPTION ", e);
		} catch (FileNotFoundException e) {
			logger.error("[File Not Exception] EXCEPTION ", e);
		} finally {
			closeStream(inputStream);
			closeStream(outputStream);
		}
	}

	private void closeStream(Closeable io) {
		try {
			if (io != null)
				io.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}

	public void exportReportResources() {
		InputStream fileListInput = getClass().getResourceAsStream(REPORT_FILE_LIST);
		BufferedReader br = new BufferedReader(new InputStreamReader(fileListInput));
		String line;
		try {
			while ((line = br.readLine()) != null && line.trim().length() > 0) {
				InputStream input = getClass().getResourceAsStream(line);
				// Copy the file to the root folder of destFolder:
				// bring them up one level => line: /report/file.ext => file.ext
				File dest = new File(destFolder.getAbsoluteFile() + ("/../" + line));
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
