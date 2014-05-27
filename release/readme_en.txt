§ Robusta 工具版權為 Taipei Tech, CSIE, Software Systems Lab 所有 §

System Requirements：
	Java SE Development Kit (JDK) 1.6 or newer version.
	Eclipse Classic 3.6 or newer version.

Contents：
	folder「licenses」 with 2 html files
	folder「dropins」 with 1 jar files
	folder「lib」 with 1 jar files
	file「readme_zhTW.txt」and「readme_en.txt」

Install Robusta：
	一、Copy whole「dropins」folder into root folder of eclipse.
	二、重新啟動 eclipse。
	三、在 eclipse 介面上選擇「Project」→「Clean...」→「Clean all projects」。

確認安裝成功：
	在 eclipse 介面上選擇「Help」→「About Eclipse Platform」→「Installation Details」→ 分頁「Plug-ins」，若「Plug-in Name」欄有出現「Robusta」，則代表安裝成功。

使用 RL(Robustness Level) annotation：
	一、將資料夾「lib」內的「ntut.csie.robusta.agile.exception_1.0.0.jar」加入到專案的「lib」資料夾下。
	二、對 eclipse 內的專案點擊右鍵，選擇「Refresh」。
	三、對該 jar 檔點擊右鍵，選擇「Build Path」→「Add to Build Path」。
	之後專案應該就可以正確地辨認 RL annotation。
