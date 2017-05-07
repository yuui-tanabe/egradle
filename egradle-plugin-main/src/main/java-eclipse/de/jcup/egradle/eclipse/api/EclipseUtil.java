/*
 * Copyright 2016 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.egradle.eclipse.api;

import java.io.File;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.debug.ui.IJavaDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.osgi.framework.Bundle;

import de.jcup.egradle.core.api.FileHelper;
import de.jcup.egradle.eclipse.MainActivator;

public class EclipseUtil {

	public static ImageDescriptor createImageDescriptor(String path, String pluginId) {
		Bundle bundle = Platform.getBundle(pluginId);

		URL url = FileLocator.find(bundle, new Path(path), null);

		ImageDescriptor imageDesc = ImageDescriptor.createFromURL(url);
		return imageDesc;
	}

	
	public static IEditorPart getActiveEditor() {
		IWorkbenchPage page = getActivePage();
		IEditorPart activeEditor = page.getActiveEditor();
		return activeEditor;
	}

	/**
	 * Returns active page or <code>null</code>
	 * 
	 * @return active page or <code>null</code>
	 */
	public static IWorkbenchPage getActivePage() {
		if (!PlatformUI.isWorkbenchRunning()) {
			return null;
		}
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		return window.getActivePage();
	}

	/**
	 * Returns active workbench shell - or <code>null</code>
	 * 
	 * @return active workbench shell - or <code>null</code>
	 */
	public static Shell getActiveWorkbenchShell() {
		IWorkbench workbench = getWorkbench();
		if (workbench == null) {
			return null;
		}
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		Shell shell = window.getShell();
		return shell;
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench workbench = getWorkbench();
		if (workbench == null) {
			return null;
		}
		IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();

		if (workbenchWindow != null) {
			return workbenchWindow;
		}
		/* fall back - try to execute in UI */
		WorkbenchWindowRunnable wwr = new WorkbenchWindowRunnable();
		getSafeDisplay().syncExec(wwr);
		return wwr.workbenchWindowFromUI;
	}

	public static IProject[] getAllProjects() {
		IProject[] projects = getWorkspace().getRoot().getProjects();
		return projects;
	}

	public static FileHelper getFileHelper() {
		return FileHelper.DEFAULT;
	}

	/**
	 * Get image by path from image registry. If not already registered a new
	 * image will be created and registered. If not createable a fallback image
	 * is used instead
	 * 
	 * @param path
	 * @param pluginId
	 *            - plugin id to identify which plugin image should be loaded
	 * @return image
	 */
	public static Image getImage(String path, String pluginId) {
		ImageRegistry imageRegistry = getImageRegistry();
		if (imageRegistry == null) {
			return null;
		}
		Image image = imageRegistry.get(path);
		if (image == null) {
			ImageDescriptor imageDesc = createImageDescriptor(path, pluginId);
			image = imageDesc.createImage();
			if (image == null) {
				image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			}
			imageRegistry.put(path, image);
		}
		return image;
	}
	
	
	public static EclipseResourceHelper getResourceHelper() {
		return EclipseResourceHelper.DEFAULT;
	}

	public static Display getSafeDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	/**
	 * Gets the egradle temp folder (user.home/.egradle). If not existing the
	 * folder will be created
	 * 
	 * @return temp folder never <code>null</code> and always existing
	 */
	public static File getTempFolder() {
		return getTempFolder(null);
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public static boolean isWorkspaceAutoBuildEnabled() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IWorkspaceDescription description = workspace.getDescription();
		return description.isAutoBuilding();
	}

	public static void log(IStatus status) {
		MainActivator.getDefault().getLog().log(status);
	}

	public static void log(String message, Throwable t) {

		if (t instanceof CoreException) {
			Throwable cause = getRootCause(t);
			message = resolveMessageIfNotSet(message, cause);
			log(new Status(IStatus.ERROR, getUniqueIdentifier(), IStatus.ERROR, t.getMessage(), cause));
		} else {
			message = resolveMessageIfNotSet(message, t);
			log(new Status(IStatus.ERROR, getUniqueIdentifier(), IJavaDebugUIConstants.INTERNAL_ERROR, message, t));
		}

	}

	public static void log(Throwable t) {
		log(null, t);
	}

	public static void logInfo(String message) {
		log(new Status(IStatus.INFO, MainActivator.PLUGIN_ID, message));
	}

	

	


	public static void logWarning(String message) {
		log(new Status(IStatus.WARNING, MainActivator.PLUGIN_ID, message));
	}

	

	public static void safeAsyncExec(Runnable runnable) {
		getSafeDisplay().asyncExec(runnable);
	}

	public static void setWorkspaceAutoBuild(boolean flag) throws CoreException {
		IWorkspace workspace = getWorkspace();
		IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(flag);
		workspace.setDescription(description);
	}

	/**
	 * Shows console view
	 */
	public static void showConsoleView() {
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			try {
				activePage.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			} catch (PartInitException e) {
				logWarning("Was not able to show console");
			}

		}
	}

	public static void throwCoreException(String message) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, MainActivator.PLUGIN_ID, message));

	}

	public static void throwCoreException(String message, Exception e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, MainActivator.PLUGIN_ID, message, e));

	}

	private static ImageRegistry getImageRegistry() {
		MainActivator mainActivator = MainActivator.getDefault();
		if (mainActivator == null) {
			return null;
		}
		return mainActivator.getImageRegistry();
	}
	
	private static Throwable getRootCause(Throwable t) {
		if (t == null) {
			return null;
		}
		Throwable rootCause = t;
		while (t.getCause() != null) {
			rootCause = t.getCause();
		}
		return rootCause;
	}

	/**
	 * Gets the egradle temp folder (user.home/.egradle/$subfolder). If not
	 * existing the folder will be created
	 * 
	 * @param subFolder
	 *            subfolder inside egradle temporary folder. If
	 *            <code>null</code> the egradle temporary folder will be
	 *            returned
	 * @return temp folder never <code>null</code> and always existing
	 */
	private static File getTempFolder(String subFolder) {
		String userHome = System.getProperty("user.home");

		StringBuilder sb = new StringBuilder();
		sb.append(userHome);
		sb.append("/.egradle");
		if (StringUtils.isNotBlank(subFolder)) {
			sb.append("/");
			sb.append(subFolder);
		}

		String path = sb.toString();

		File egradleTempFolder = new File(path);
		if (!egradleTempFolder.exists()) {
			egradleTempFolder.mkdirs();
			if (!egradleTempFolder.exists()) {
				throw new IllegalStateException("Was not able to create egradle temp folder:" + path);
			}
		}
		return egradleTempFolder;
	}

	private static String getUniqueIdentifier() {
		return "EGradle";
	}

	/**
	 * Returns workbench or <code>null</code>
	 * 
	 * @return workbench or <code>null</code>
	 */
	private static IWorkbench getWorkbench() {
		if (!PlatformUI.isWorkbenchRunning()) {
			return null;
		}
		IWorkbench workbench = PlatformUI.getWorkbench();
		return workbench;
	}

	private static String resolveMessageIfNotSet(String message, Throwable cause) {
		if (message == null) {
			if (cause == null) {
				message = "Unknown";
			} else {
				message = cause.getMessage();
			}
		}
		return message;
	}

	private static class WorkbenchWindowRunnable implements Runnable{
		IWorkbenchWindow workbenchWindowFromUI;
		@Override
		public void run() {
			IWorkbench workbench = getWorkbench();
			if (workbench == null) {
				return;
			}
			workbenchWindowFromUI=workbench.getActiveWorkbenchWindow();
		}
		
	}
}
