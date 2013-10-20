package ntut.csie.rleht;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class RLEHTPlugin extends AbstractUIPlugin {
	private static Logger logger = LoggerFactory.getLogger(RLEHTPlugin.class);

	// The plug-in ID
	public static final String PLUGIN_ID = "Robusta";

	// The shared instance
	private static RLEHTPlugin plugin;

	/**
	 * The constructor
	 */
	public RLEHTPlugin() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static RLEHTPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static void logDebug(String message) {
		if (getDefault().isDebugging()) {
			logger.debug(message);
			getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, IStatus.OK, message, null));
		}
	}

	public static void logError(String message, Throwable t) {
		logger.error(message, t);
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, t));
	}

}
