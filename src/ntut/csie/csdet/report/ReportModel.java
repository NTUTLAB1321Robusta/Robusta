package ntut.csie.csdet.report;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportModel {
	private List<PackageModel> smellList = new ArrayList<PackageModel>();
	//�M�צW��
	private String projectName = "";
	//�x�s���|
	private String projectPath = "";
	//Smell�`��
	private int ignoreTotalSize = 0;
	private int dummyTotalSize = 0;
	private int unMainTotalSize = 0;
	private int nestedTryTotalSize = 0;

	/**
	 * ���o�سy�ɶ�
	 */
	public String getDateTime()
	{
		Locale lo = Locale.TAIWAN;
        Calendar cl= Calendar.getInstance();
        Date d = cl.getTime();
        DateFormat df1 = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,lo);
        
        return df1.format(d).toString();
	}
	
	//�W�[Smell���`��
	public void addIgnoreTotalSize(int ignoreSize) {
		this.ignoreTotalSize += ignoreSize;
	}
	public void addDummyTotalSize(int dummySize) {
		this.dummyTotalSize += dummySize;
	}
	public void addUnMainTotalSize(int unMainSize) {
		this.unMainTotalSize += unMainSize;
	}
	public void addNestedTotalTrySize(int nestedTrySize) {
		this.nestedTryTotalSize += nestedTrySize;
	}
	
	//���oSmell���`��
	public int getIgnoreTotalSize() {
			return ignoreTotalSize;
	}
	public int getDummyTotalSize() {
			return dummyTotalSize;
	}
	public int getUnMainTotalSize() {
			return unMainTotalSize;
	}
	public int getNestedTryTotalSize() {
			return nestedTryTotalSize;
	}
	public int getTotalSmellCount() {
		return getIgnoreTotalSize() + getDummyTotalSize() + getUnMainTotalSize() + getNestedTryTotalSize();
	}

	//�]�w�Ψ��oProject���W��
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getProjectName() {
		return projectName;
	}

	//�]�w�Ψ��oProject�����|
	public String getProjectPath() {
		return projectPath;
	}
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath + "/" + getProjectName() + "_Report";
	}

	public PackageModel getPackage(int i) {
		if (i >= smellList.size())
			return null;
		else
			return smellList.get(i);
	}
	public int getPackagesSize() {
		return smellList.size();
	}

	/**
	 * �[�J�s��Package
	 * @param packageName
	 * @return
	 */
	public PackageModel addSmellList(String packageName) {
//		for (PackageModel pm : smellList) {
//			if (pm.getPackageName().equals(packageName)) {
//				System.out.println("[PackageName]===>"+pm.getPackageName());
//				return pm;
//			}
//		}
		PackageModel newPackageModel = new PackageModel();
		newPackageModel.setPackageName(packageName);
		smellList.add(newPackageModel);
		return newPackageModel;
	}
}
