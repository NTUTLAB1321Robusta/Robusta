package ntut.csie.csdet.report.ui;

import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Browser動作
 * @author Shiau
 */
public class BrowserControl extends LocationAdapter {
	private static Logger logger = LoggerFactory.getLogger(BrowserControl.class);
	//來源Browser
	Browser browser;

	//欲選取SourceCode的專案名稱
	String projectName = "";
	//欲選取SourceCode的Package路徑
	String packagePath = "";
	//欲選取SourceCode的行數
	String LineString  = "";

	//觸發動作為網址一改變
	public void changed(final LocationEvent event) {
		//若Browser為Null，去取得Browser來源
		if (browser == null)
			browser = (Browser) event.getSource();

		//網址變更，傳入訊息
		String info = browser.getUrl();

		//URL最後一碼為"#"，表示要Link到SourceCode
		if (info.charAt(info.length()- 1) == '#')
		{
			//解析URL，取得程式碼行數必要資訊
			parseUrl(info);

			//取得File的ProjectName
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (project != null) {

				//取得Class的Path
				IFile javaFile = project.getFile(new Path(packagePath));
				if (javaFile != null) {
					//打開File的Editor
					IEditorPart edit = openEditor(project, javaFile);
					//取得
					Document document = getDocument(javaFile);
					//反白選擇行數
					selectLine(edit, LineString, document);
				}
			}
		}
	}

	/**
	 * 取得File的Document
	 * @param javaFile		欲連結的.java檔
	 * @return				File的Document
	 */
	private Document getDocument(IFile javaFile) {
		ICompilationUnit icu = (ICompilationUnit)JavaCore.create(javaFile);

		Document document = null;
		try {
			document = new Document(icu.getBuffer().getContents());
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}
		return document;
	}

	/**
	 * 開啟File的Editor
	 * @param project
	 * @param javaFile
	 * @return
	 */
	private IEditorPart openEditor(IProject project, IFile javaFile) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(javaFile.getName());

		IEditorPart edit = null;

		try {
			edit = page.openEditor(new FileEditorInput(javaFile), desc.getId());
		} catch (PartInitException e) {
			logger.error("[PartInitException] EXCEPTION ",e);
		}
		return edit;
	}

	/**
	 * 反白選擇行數
	 * @param edit
	 * @param LineString
	 * @param document
	 */
	private void selectLine(IEditorPart edit, String LineString, Document document) {
		try {
			IEditorPart sourceEditor = edit;

			if (sourceEditor instanceof ITextEditor) {
				ITextEditor editor = (ITextEditor) sourceEditor;
				
				//欲反白的行數資料
				IRegion lineInfo = null;
				try {
					//取得行數的資料
					lineInfo = document.getLineInformation(Integer.valueOf(LineString) - 1);
				} catch (BadLocationException e) {
					logger.error("[BadLocation] EXCEPTION ",e);
				}
				
				//反白指定的行數
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			}
		} catch (Exception ex) {
			RLEHTPlugin.logError("其它錯誤！", ex);
		}
	}

	/**
	 * 解析URL，取得程式碼行數必要資訊
	 * @param info
	 */
	private void parseUrl(String info) {
		//URL網址規格：file:///..../#ProjectName/PackagePath.../ClassName.java#行數#
		//去掉HTML位置
		info = info.substring(info.indexOf("#") + 2, info.length() - 1);
		//專案名稱結尾位置
		int projectEndLocation = info.indexOf("/");
		//Package路徑結尾位置
		int pathEndLocation = info.indexOf("#");
		//行數起始位置
		int lineStartLocation = info.lastIndexOf("#") + 1;

		///取得資訊///
		projectName = info.substring(0, projectEndLocation);
		packagePath = info.substring(projectEndLocation, pathEndLocation);
		LineString = info.substring(lineStartLocation);
	}
}
