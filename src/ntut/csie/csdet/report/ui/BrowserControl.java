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
 * Browser�ʧ@
 * @author Shiau
 */
public class BrowserControl extends LocationAdapter {
	private static Logger logger = LoggerFactory.getLogger(BrowserControl.class);
	//�ӷ�Browser
	Browser browser;

	//�����SourceCode���M�צW��
	String projectName = "";
	//�����SourceCode��Package���|
	String packagePath = "";
	//�����SourceCode�����
	String LineString  = "";

	//Ĳ�o�ʧ@�����}�@����
	public void changed(final LocationEvent event) {
		//�YBrowser��Null�A�h���oBrowser�ӷ�
		if (browser == null)
			browser = (Browser) event.getSource();

		//���}�ܧ�A�ǤJ�T��
		String info = browser.getUrl();

		//URL�̫�@�X��"#"�A��ܭnLink��SourceCode
		if (info.charAt(info.length()- 1) == '#')
		{
			//�ѪRURL�A���o�{���X��ƥ��n��T
			parseUrl(info);

			//���oFile��ProjectName
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (project != null) {

				//���oClass��Path
				IFile javaFile = project.getFile(new Path(packagePath));
				if (javaFile != null) {
					//���}File��Editor
					IEditorPart edit = openEditor(project, javaFile);
					//���o
					Document document = getDocument(javaFile);
					//�ϥտ�ܦ��
					selectLine(edit, LineString, document);
				}
			}
		}
	}

	/**
	 * ���oFile��Document
	 * @param javaFile		���s����.java��
	 * @return				File��Document
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
	 * �}��File��Editor
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
			e.printStackTrace();
		}
		return edit;
	}

	/**
	 * �ϥտ�ܦ��
	 * @param edit
	 * @param LineString
	 * @param document
	 */
	private void selectLine(IEditorPart edit, String LineString, Document document) {
		try {
			IEditorPart sourceEditor = edit;

			if (sourceEditor instanceof ITextEditor) {
				ITextEditor editor = (ITextEditor) sourceEditor;
				
				//���ϥժ���Ƹ��
				IRegion lineInfo = null;
				try {
					//���o��ƪ����
					lineInfo = document.getLineInformation(Integer.valueOf(LineString) - 1);
				} catch (BadLocationException e) {
					logger.error("[BadLocation] EXCEPTION ",e);
				}
				
				//�ϥի��w�����
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			}
		} catch (Exception ex) {
			RLEHTPlugin.logError("�䥦���~�I", ex);
		}
	}

	/**
	 * �ѪRURL�A���o�{���X��ƥ��n��T
	 * @param info
	 */
	private void parseUrl(String info) {
		//URL���}�W��Gfile:///..../#ProjectName/PackagePath.../ClassName.java#���#
		//�h��HTML��m
		info = info.substring(info.indexOf("#") + 2, info.length() - 1);
		//�M�צW�ٵ�����m
		int projectEndLocation = info.indexOf("/");
		//Package���|������m
		int pathEndLocation = info.indexOf("#");
		//��ư_�l��m
		int lineStartLocation = info.lastIndexOf("#") + 1;

		///���o��T///
		projectName = info.substring(0, projectEndLocation);
		packagePath = info.substring(projectEndLocation, pathEndLocation);
		LineString = info.substring(lineStartLocation);
	}
}
