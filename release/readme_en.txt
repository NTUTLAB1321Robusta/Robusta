Copyright of Robusta is retained by Software System Lab of 
Department of Computer Science and Information Engineering at 
Taipei Tech.

System Requirements:
	Java SE Development Kit (JDK) 1.6 or newer.
	Eclipse Classic 3.6 or newer.
	
How to install:
	Option1: (for Eclipse 3.7+)
		1. Find the "Install" icon right by "Robusta."
		2. Drag and drop the icon into a running Eclipse.
		3. Make sure the checkbox right by "Robusta" is checked.
		4. Click "Confirm"
		5. Accept terms of the license agreement.
		6. Click on "Finish" then "OK"
		7. Click on "Yes" to restart eclipse if the system asks.
		8. To check if the installation succeeded, click on "Help" on the Menu bar -> "About Eclipse" ->  "Installation Details" -> "Installed software" tab -> "Robusta Exception Handling" should appear there. 
	
	Option2:
		1. Click on "Help" on the Menu bar -> "Install New Software..."
		2. Click on "Add..." on the top right of the install window.
		3. Enter "Robusta" for Name and "http://pl.csie.ntut.edu.tw/project/Robusta/installation/" for Location, then click on "OK."
		4. Click on the checkbox right by "Robusta" under Name.
		5. Click "Next" -> "Next"
		6. Accept terms of the license agreement.
		7. Click on "Finish" then "OK"
		8. Click on "Yes" to restart eclipse if the system asks.
		9. To check if the installation succeeded, click on "Help" on the Menu bar -> "About Eclipse" ->  "Installation Details" -> "Installed software" tab -> "Robusta Exception Handling" should appear there. 

Use RL(Robustness Level) annotation:
	1. Copy and place "ntut.csie.robusta.agile.exception_1.0.0.jar" from the "lib" folder of this package to the "lib" folder of target project. 
	2. Right click on the target project and choose "Refresh" .
	3. Right click on the "ntut.csie.robusta.agile.exception_1.0.0.jar" that is already in the project's "lib" folder, then choose "Build Path" -> "Add to Build Path" . The project should now be able to recognize RL annotation.
