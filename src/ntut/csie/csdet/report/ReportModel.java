package ntut.csie.csdet.report;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Report���������
 * @author Shiau
 */
public class ReportModel {
	//Smell��T
	private List<PackageModel> smellList = new ArrayList<PackageModel>();
	
	//Filter�����O�_��������
	private boolean derectAllproject;
	
	private Date buildTime;
	
	//Filter����
	private List<String> filterRuleList = new ArrayList<String>();

	//�M�צW��
	private String projectName = "";

	//�x�s���|
	private String projectPath = "";

	//Smell�`��
	private int ignoreTotalSize = 0;
	private int dummyTotalSize = 0;
	private int unMainTotalSize = 0;
	private int nestedTryTotalSize = 0;
	private int carelessCleanUpSize = 0;
	private int overLoggingSize = 0;

	//���ocode counter
	private int tryCounter = 0;
	private int catchCounter = 0;
	private int finallyCounter = 0;

	/**
	 * �]�w�B���o�سy�ɶ�
	 */
	public void setBuildTime() {
		Calendar calendar= Calendar.getInstance();
		buildTime = calendar.getTime();
	}
	public String getBuildTime() {
		//�]�w�榡 ��ܬ��
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.LONG);

        return df.format(buildTime);
	}

	///�W�[Smell���`��///
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
	public void addOverLoggingSize(int overLoggingSize) {
		this.overLoggingSize += overLoggingSize;
	}
	public void addCarelessCleanUpSize(int carelessCleanUpSize) {
		this.carelessCleanUpSize += carelessCleanUpSize;
	}
	
	///���oSmell���`��///
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
	public int getOverLoggingTotalSize() {
		return overLoggingSize;
	}
	public int getCarelessCleanUpTotalSize() {
		return carelessCleanUpSize;
	}
	public int getTotalSmellCount() {
		return getIgnoreTotalSize() + getDummyTotalSize() + getUnMainTotalSize() + getNestedTryTotalSize()
				+ getCarelessCleanUpTotalSize() + getOverLoggingTotalSize();
	}

	///�]�w�Ψ��oProject���W��///
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getProjectName() {
		return projectName;
	}

	///�]�w�Ψ��oProject�����|///
	public String getProjectPath() {
		return projectPath;
	}
	public void setProjectPath(String workspacePath) {
		this.projectPath = workspacePath + "/" + getProjectName() + "_Report";

		File metadataPath = new File(projectPath);

		//�Y�S�����|�N�إ߸��|
		if(!metadataPath.exists())
			metadataPath.mkdir();

		File htmlPath = new File(projectPath + "/" + buildTime.getTime());
		htmlPath.mkdir();
	}
	/**
	 * ���oFile�������m(���L�[�ɶ��Ϲj)
	 * @param fileName	(File���W��)
	 * @param isAddTime (�O�_���[�ɶ�)
	 * @return
	 */
	public String getFilePath(String fileName, boolean isAddTime) {
		if (isAddTime)
			return (projectPath + "/" + buildTime.getTime() + "/" + buildTime.getTime() + "_" + fileName);
		else
			return (projectPath + "/" + fileName);
	}

	/**
	 * ���oPackageModel
	 * @param i PackageIndex
	 * @return
	 */
	public PackageModel getPackage(int i) {
		if (i >= smellList.size())
			return null;
		else
			return smellList.get(i);
	}
	/**
	 * ���oPackage�`��
	 * @return
	 */
	public int getPackagesSize() {
		return smellList.size();
	}

	/**
	 * �[�J�s��Package
	 * @param packageName
	 * @return
	 */
	public PackageModel addSmellList(String packageName) {
		PackageModel newPackageModel = new PackageModel();
		//�]�mPackage�W��
		newPackageModel.setPackageName(packageName);
		smellList.add(newPackageModel);

		return newPackageModel;
	}

	///�s��Filter����O�_��������///
	public boolean isDerectAllproject() {
		return derectAllproject;
	}
	public void setDerectAllproject(boolean derectAllproject) {
		this.derectAllproject = derectAllproject;
	}
	
	///�s��Filter����///
	public List<String> getFilterList() {
		return filterRuleList;
	}
	public void setFilterList(List<String> ruleList) {
		for (String temp: ruleList) {
			temp = temp.replace("EH_STAR", "*");
			temp = temp.replace("EH_LEFT", "");
			temp = temp.replace("EH_RIGHT", "");

			filterRuleList.add(temp);
		}
	}
	
	///���o���������///
	public int getTotalLine() {
		int total = 0;
		for (PackageModel pm : smellList)
			total += pm.getTotalLine();
		return total;
	}
	
	///�s��Code����T///
	public int getTryCounter() {
		return tryCounter;
	}
	public void addTryCounter(int tryCounter) {
		this.tryCounter += tryCounter;
	}
	public int getCatchCounter() {
		return catchCounter;
	}
	public void addCatchCounter(int catchCounter) {
		this.catchCounter += catchCounter;
	}
	public int getFinallyCounter() {
		return finallyCounter;
	}	
	public void addFinallyCounter(int finallyCounter) {
		this.finallyCounter += finallyCounter;
	}
}
