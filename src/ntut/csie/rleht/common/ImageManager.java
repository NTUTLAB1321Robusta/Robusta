package ntut.csie.rleht.common;

import java.io.File;
import java.net.URL;

import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageManager {
	private static Logger logger =LoggerFactory.getLogger(ImageManager.class);
	private static ImageRegistry register = new ImageRegistry();

	private static ImageManager instance = new ImageManager();

	protected ImageManager() {
		init();
	}

	private void init() {
		Bundle bundle = Platform.getBundle(RLEHTPlugin.PLUGIN_ID);
		URL url = bundle.getEntry("icons");
		try {
			url = FileLocator.toFileURL(url);

		}
		catch (Exception ex) {
			logger.error("[init] 取得目錄失敗！ ",ex);
			ErrorLog.getInstance().logWarning("取得目錄失敗！", ex);
		}

		File file = new File(url.getPath());

		File[] images = file.listFiles();
		for (int i = 0; i < images.length; i++) {
			File f = images[i];
			if (!f.isFile()) {
				continue;
			}
			String name = f.getName();
			if (!name.endsWith(".gif")) {
				continue;
			}

			String key = name.substring(0, name.indexOf('.'));

			register.put(key, RLEHTPlugin.getImageDescriptor("icons/" + name));

		}
	}

	public static ImageManager getInstance() {
		return instance;
	}

	public Image get(String key) {
		Image image = register.get(key);
		return (image == null ? ImageDescriptor.getMissingImageDescriptor().createImage() : image);
	}

	public ImageDescriptor getDescriptor(String key) {
		ImageDescriptor imageDesc = register.getDescriptor(key);
		return (imageDesc == null ? ImageDescriptor.getMissingImageDescriptor() : imageDesc);
	}

}
