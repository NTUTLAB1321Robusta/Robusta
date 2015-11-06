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
 * Browser control action
 * @author Shiau
 */
public class BrowserControl extends LocationAdapter {
	private static Logger logger = LoggerFactory.getLogger(BrowserControl.class);
	//resource browser
	Browser browser;

	//source code project name
	String projectName = "";
	//source code Package path
	String packagePath = "";
	//source code line number
	String LineString  = "";

	//when URl has changed, this function will be invocated
	public void changed(final LocationEvent event) {
		//if Browser is null，get a new browser resource
		if (browser == null)
			browser = (Browser) event.getSource();

		//get input information when URL change
		String info = browser.getUrl();

		//check for valid URL
		if (info.charAt(info.length()- 1) == '#')
		{
			//parse URL to separate information inside
			parseUrl(info);

			//get project name of file 
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (project != null) {

				//get class path
				IFile javaFile = project.getFile(new Path(packagePath));
				if (javaFile != null) {
					//launch editor to open file 
					IEditorPart edit = openEditor(project, javaFile);
					Document document = getDocument(javaFile);
					//high light selected line number
					selectLine(edit, LineString, document);
				}
			}
		}
	}

	/**
	 * transform java file into document
	 * @param javaFile		target java file
	 * @return				return file document
	 */
	private Document getDocument(IFile javaFile) {
		ICompilationUnit icu = (ICompilationUnit)JavaCore.create(javaFile);

		Document document = null;
		try {
			document = new Document(icu.getBuffer().getContents());
		} catch (JavaModelException e1) {
			throw new RuntimeException("Fail to get document", e1);
		}
		return document;
	}

	/**
	 * the editor to open java file
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
	 * high light selected line number
	 * @param edit
	 * @param LineString
	 * @param document
	 */
	private void selectLine(IEditorPart edit, String LineString, Document document) {
		try {
			IEditorPart sourceEditor = edit;

			if (sourceEditor instanceof ITextEditor) {
				ITextEditor editor = (ITextEditor) sourceEditor;
				
				//the code region which will be high light
				IRegion lineInfo = null;
				lineInfo = getLineInfo(LineString, document, lineInfo);
				
				//high light selected line number
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			}
		} catch (NumberFormatException nfe) {
			RLEHTPlugin.logError("wrong line number！", nfe);
		} catch (Exception ex) {
			RLEHTPlugin.logError("some other problem happen！", ex);
		}
	}

	private IRegion getLineInfo(String LineString, Document document,
			IRegion lineInfo) {
		try {
			lineInfo = document.getLineInformation(Integer.valueOf(LineString) - 1);
		} catch (BadLocationException e) {
			logger.error("[BadLocation] EXCEPTION ",e);
		}
		return lineInfo;
	}

	/**
	 * 解析URL，取得程式碼行數必要資訊
	 * @param info
	 */
	private void parseUrl(String info) {
		//URL formate：file:///..../#ProjectName/PackagePath.../ClassName.java#line number#
		//remove HTML tag
		info = info.substring(info.indexOf("#") + 2, info.length() - 1);
		//get end index of project name 
		int projectEndLocation = info.indexOf("/");
		//get end index of package
		int pathEndLocation = info.indexOf("#");
		//get start index of line number
		int lineStartLocation = info.lastIndexOf("#") + 1;

		projectName = info.substring(0, projectEndLocation);
		packagePath = info.substring(projectEndLocation, pathEndLocation);
		LineString = info.substring(lineStartLocation);
	}
}
