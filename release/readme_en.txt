Copyright of Robusta is retained by Software System Lab of 
Department of Computer Science and Information Engineering at 
Taipei Tech.

System Requirements：
	Java SE Development Kit (JDK) 1.6 or newer.
	Eclipse Classic 3.6 or newer.

Contents Included：
	Two HTML files under the 「licenses」 folder,
	One jar file under the 「dropins」 folder,
	One jar file under the 「lib」 folder, and
	Two README.txt.

How to install：
	1. Save any unsaved work and close eclipse.
	2. Copy and place the whole 「dropins」 folder into eclipse 「root」 folder.
	3. Restart eclipse.
	4. Click on「Project」→「Clean...」→「Clean all projects」on the menu bar.
	5. Click on 「Help」→「About Eclipse」→「Installation Details」→「Plug-ins」, if 「Robusta」appears under「Plug-in」, the installation succeeded.

Use RL(Robustness Level) annotation：
	1. Copy and place 「ntut.csie.robusta.agile.exception_1.0.0.jar」 from the 「lib」 folder of this package to the 「lib」 folder of target project. 
	2. Right click on the target project and choose 「Refresh」.
	3. Right click on the 「ntut.csie.robusta.agile.exception_1.0.0.jar」 that is already in the project’s 「lib」 folder, then choose「Build Path」→「Add to Build Path」. The project should now be able to recognize RL annotation.

