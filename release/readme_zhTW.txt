§ Robusta 工具版權為台北科技大學 資訊工程系 軟體系統實驗室 所有 §

系統需求：
	Java SE Development Kit (JDK) 1.6 或以上版本
	Eclipse Classic 3.6 或以上版本

下載包內容：
	「licenses」 資料夾：2 個 html 檔
	「dropins」 資料夾：1 個 jar 檔
	「lib」 資料夾：1 個 jar 檔
	2 個 README.txt

安裝 Robusta：
	一、儲存專案並關閉 eclipse。
	二、將「dropins」資料夾整個複製到 eclipse 根目錄下。
	三、重新啟動 eclipse。
	四、在 eclipse 工具列上選擇「Project」→「Clean...」→「Clean all projects」。
	五、若要確認是否安裝成功，可以在 eclipse 工具列上選擇「Help」→「About Eclipse Platform」→「Installation Details」→ 分頁「Plug-ins」，在「Plug-in Name」欄應該會找到「Robusta」。

使用 RL(Robustness Level) annotation：
	一、將資料夾「lib」內的「ntut.csie.robusta.agile.exception_1.0.0.jar」加入到專案的「lib」資料夾下。
	二、對 eclipse 內的專案點擊右鍵，選擇「Refresh」。
	三、對該 jar 檔點擊右鍵，選擇「Build Path」→「Add to Build Path」。
	之後專案應該就可以正確地辨認 RL annotation。
