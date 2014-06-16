Copyright of Robusta is retained by Software System Lab of 
Department of Computer Science and Information Engineering at 
Taipei Tech.

System Requirements:
	Java SE Development Kit (JDK) 1.6 or newer.
	Eclipse Classic 3.6 or newer.

Contents Included:
	|   artifacts.xml
	|   content.xml
	|   readme_en.txt
	|   readme_zhTW.txt
	|
	+---licenses
	|       epl-v10.html
	|       notice.html
	|
	+---features
	|       taipeitech.csie.robusta_1.6.8.jar
	|
	\---plugins
			taipeitech.csie.robusta_1.6.8.jar

How to install:
	1. Click on "Help" -> "Install New Software..." -> "Add" -> "Local" -> Browse to the extracted folder of this plugin (the folder contain content.xml).
	2. Continue to finish the installation wizard until finish.
	3. To check if the installation succedded, click on "Help" -> "About Eclipse" -> "Installation Details" -> "Installed software" tab -> "Robusta Exception Handling" should appear here.

Use RL(Robustness Level) annotation:
	1. Copy and place "ntut.csie.robusta.agile.exception_1.0.0.jar" from the "lib" folder of this package to the "lib" folder of target project. 
	2. Right click on the target project and choose "Refresh" .
	3. Right click on the "ntut.csie.robusta.agile.exception_1.0.0.jar" that is already in the project's "lib" folder, then choose "Build Path" -> "Add to Build Path" . The project should now be able to recognize RL annotation.
