§ Robusta 工具版權為台北科技大學 資訊工程系 軟體系統實驗室 所有 §

系統需求：
	Java SE Development Kit (JDK) 1.6 或以上版本
	Eclipse Classic 3.6 或以上版本

下載包內容：
	「licenses」 資料夾：2 個 html 檔
	「features」 資料夾：1 個 jar 檔
	「plugins」 資料夾：1 個 jar 檔
	artifacts.xml
	content.xml
	readme_en.txt
	readme_zhTW.txt
	
安裝 Robusta：
	一、在上方清單列選擇「Help」 -> 「Install New Software...」。
	二、選擇在 Install 視窗右上的「Add...」。
	三、在 Name 欄輸入"Robusta"，在 Location 欄輸入"http://pl.csie.ntut.edu.tw/project/Robusta/installation/"。
	四、在視窗中的清單勾選「Robusta」。
	五、選擇「Next」-> 「Next」-> 「I accept the terms of the license agreement」-> 「Finish」-> 「OK」。
	六、選擇「Yes」如果系統要求重新啟動 eclipse。
	七、若要確認是否安裝成功，可以在 eclipse 工具列上選擇「Help」→「About Eclipse Platform」→「Installation Details」→ 分頁「Installed Software」，在「Name」欄會出現「Robusta Exception Handling」。

使用 RL(Robustness Level) annotation：
	一、將資料夾「lib」內的「ntut.csie.robusta.agile.exception_1.0.0.jar」加入到專案的「lib」資料夾下。
	二、對 eclipse 內的專案點擊右鍵，選擇「Refresh」。
	三、對該 jar 檔點擊右鍵，選擇「Build Path」→「Add to Build Path」。
	之後專案應該就可以正確地辨認 RL annotation。
