package ntut.csie.csdet.report;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.preference.RobustaSettings;

public class PastReportsHistory {

	public List<File> getFileList(String projectName) {
		List<File> fileList = new ArrayList<File>();
		File directory = new File(RobustaSettings.getRobustaReportFolder(projectName));

		File[] allFolder = directory.listFiles();

		if (allFolder == null)
			return fileList;
		for (File folder : allFolder) {
			if (folder.isDirectory()) {
				if (folder.getName().equals("report"))
					continue;
				File[] files = folder.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".xml");
					}
				});

				for (File file : files) {
					fileList.add(file);
				}
			}
		}
		return fileList;
	}
}
