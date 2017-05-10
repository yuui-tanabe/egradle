package de.jcup.egradle.eclipse.ide;

import static de.jcup.egradle.eclipse.util.EclipseUtil.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.GlobalBuildAction;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.progress.IProgressConstants2;

import de.jcup.egradle.core.Constants;
import de.jcup.egradle.core.domain.GradleRootProject;
import de.jcup.egradle.core.process.OutputHandler;
import de.jcup.egradle.core.process.RememberLastLinesOutputHandler;
import de.jcup.egradle.core.validation.GradleOutputValidator;
import de.jcup.egradle.core.validation.ValidationResult;
import de.jcup.egradle.core.virtualroot.VirtualProjectCreator;
import de.jcup.egradle.core.virtualroot.VirtualRootProjectException;
import de.jcup.egradle.eclipse.ide.console.EGradleSystemConsole;
import de.jcup.egradle.eclipse.ide.console.EGradleSystemConsoleFactory;
import de.jcup.egradle.eclipse.ide.console.EGradleSystemConsoleProcessOutputHandler;
import de.jcup.egradle.eclipse.ide.decorators.EGradleProjectDecorator;
import de.jcup.egradle.eclipse.ide.filehandling.AutomaticalDeriveBuildFoldersHandler;
import de.jcup.egradle.eclipse.ide.preferences.EGradleIdePreferences;
import de.jcup.egradle.eclipse.ide.virtualroot.EclipseVirtualProjectPartCreator;
import de.jcup.egradle.eclipse.ide.virtualroot.VirtualRootProjectNature;
import de.jcup.egradle.eclipse.ui.UnpersistedMarkerHelper;
import de.jcup.egradle.eclipse.util.EclipseUtil;

public class IdeUtil {

	private static final String MESSAGE_MISSING_ROOTPROJECT = "No root project path set. Please setup in preferences!";

	private static final IProgressMonitor NULL_PROGESS = new NullProgressMonitor();

	private static VirtualProjectCreator virtualProjectCreator = new VirtualProjectCreator();

	private static OutputHandler systemConsoleOutputHandler;
	private static UnpersistedMarkerHelper buildScriptProblemMarkerHelper = new UnpersistedMarkerHelper(
			"de.jcup.egradle.script.problem");

	public static OutputHandler getSystemConsoleOutputHandler() {
		if (systemConsoleOutputHandler == null) {
			systemConsoleOutputHandler = new EGradleSystemConsoleProcessOutputHandler();
		}
		return systemConsoleOutputHandler;
	}

	public static RememberLastLinesOutputHandler createOutputHandlerForValidationErrorsOnConsole() {
		int max;
		if (getPreferences().isOutputValidationEnabled()) {
			max = Constants.VALIDATION_OUTPUT_SHRINK_LIMIT;
		} else {
			max = 0;
		}
		return new RememberLastLinesOutputHandler(max);
	}

	/**
	 * Get image by path from image registry. If not already registered a new
	 * image will be created and registered. If not createable a fallback image
	 * is used instead
	 * 
	 * @param path
	 * @return image
	 */
	public static Image getImage(String path) {
		return EclipseUtil.getImage(path, IDEActivator.PLUGIN_ID);
	}

	/**
	 * Open system console
	 * 
	 * @param ensureNoScrollLock
	 *            - if <code>true</code> scroll lock will be disabled
	 */
	public static void openSystemConsole(boolean ensureNoScrollLock) {
		EclipseUtil.safeAsyncExec(new Runnable() {

			@Override
			public void run() {
				IConsole eGradleSystemConsole = EGradleSystemConsoleFactory.INSTANCE.getConsole();
				IWorkbenchPage page = EclipseUtil.getActivePage();
				String id = IConsoleConstants.ID_CONSOLE_VIEW;
				IConsoleView view;
				try {
					view = (IConsoleView) page.showView(id);
					view.display(eGradleSystemConsole);

					if (ensureNoScrollLock) {
						view.setScrollLock(false);
					}

				} catch (PartInitException e) {
					logError("Was not able to show system console", e);
				}
			}

		});

	}

	/**
	 * Does output on {@link EGradleSystemConsole} instance - asynchronous
	 * inside SWT thread
	 * 
	 * @param message
	 */
	public static void outputToSystemConsole(String message) {
		EclipseUtil.getSafeDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				getSystemConsoleOutputHandler().output(message);

			}

		});
	}

	public static void refreshAllProjectDecorations() {
		getSafeDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				if (workbench == null) {
					return;
				}
				IDecoratorManager manager = workbench.getDecoratorManager();

				EGradleProjectDecorator decorator = (EGradleProjectDecorator) manager
						.getBaseLabelProvider("de.jcup.egradle.eclipse.ide.decorators.EGradleProjectDecorator");
				IProject[] projects = getAllProjects();
				/* test if virtual root project is visible */
				for (IProject project : projects) {
					String name = project.getName();
					if (Constants.VIRTUAL_ROOTPROJECT_NAME.equals(name)) {
						/* ok found - so recreate ... */
						try {
							createOrRecreateVirtualRootProject();
						} catch (VirtualRootProjectException e) {
							logError("Cannot (re)create virtual root project",e);
						}
						break;
					}
				}
				if (decorator != null) { // decorator is enabled

					LabelProviderChangedEvent event = new LabelProviderChangedEvent(decorator, projects);
					decorator.fireLabelProviderChanged(event);
				}
			}

		});

	}

	/**
	 * Does a refresh to projects. If enabled build folders are automatically
	 * derived
	 * 
	 * @param monitor
	 */
	public static void refreshAllProjects(IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = NULL_PROGESS;
		}
		AutomaticalDeriveBuildFoldersHandler automaticalDeriveBuildFoldersHandler = new AutomaticalDeriveBuildFoldersHandler();
		outputToSystemConsole("start refreshing all projects");
		IProject[] projects = getAllProjects();
		for (IProject project : projects) {
			try {
				if (monitor.isCanceled()) {
					break;
				}
				String text = "refreshing project " + project.getName();
				monitor.subTask(text);
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

				automaticalDeriveBuildFoldersHandler.deriveBuildFolders(project, monitor);

			} catch (CoreException e) {
				logError("Was not able to refresh all projects", e);
				outputToSystemConsole(Constants.CONSOLE_FAILED + " to refresh project " + project.getName());
			}
		}
		outputToSystemConsole(Constants.CONSOLE_OK);

	}

	public static void cleanAllProjects(boolean buildAfterClean, IWorkbenchWindow window, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = NULL_PROGESS;
		}
		outputToSystemConsole("start cleaning all projects inside eclipse");
		if (monitor.isCanceled()) {
			return;
		}
		// see org.eclipse.ui.internal.ide.dialogs.CleanDialog#buttonPressed
		WorkspaceJob cleanJob = new WorkspaceJob("Clean all projects") {
			@Override
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD.equals(family);
			}

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				doCleanAll(monitor);
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				if (buildAfterClean) {
					if (window == null) {
						logWarning("Not able to do global build because no active workbench window found!");
						;
					} else {
						GlobalBuildAction build = new GlobalBuildAction(window,
								IncrementalProjectBuilder.INCREMENTAL_BUILD);
						build.doBuild();
					}
				}
				return Status.OK_STATUS;
			}
		};

		cleanJob.setRule(getWorkspace().getRuleFactory().buildRule());
		cleanJob.setUser(true);
		cleanJob.setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);
		cleanJob.schedule();

		outputToSystemConsole(Constants.CONSOLE_OK);

	}

	/**
	 * Performs the actual clean operation.
	 * 
	 * @param cleanAll
	 *            if <code>true</true> clean all projects
	 * @param monitor
	 *            The monitor that the build will report to
	 * @throws CoreException
	 *             thrown if there is a problem from the core builder.
	 */
	protected static void doCleanAll(IProgressMonitor monitor) throws CoreException {
		getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
	}

	public static void removeAllValidationErrorsOfConsoleOutput() {
		try {
			buildScriptProblemMarkerHelper.removeAllRegisteredMarkers();
		} catch (CoreException e) {
			logError("Was not able to remove all valdiation errors of console output", e);
		}
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

	/**
	 * Returns preferences for IDE
	 * 
	 * @return preferences
	 */
	public static EGradleIdePreferences getPreferences() {
		return EGradleIdePreferences.getInstance();
	}

	/**
	 * Set new root project folder by given file
	 * 
	 * @param folder
	 * @throws CoreException
	 * @throws IllegalArgumentException
	 *             when folder is not a directory or is <code>null</code>
	 */
	public static void setNewRootProjectFolder(File folder) throws CoreException {
		if (folder == null) {
			throwCoreException("new root folder may not be null!");
		}
		if (!folder.isDirectory()) {
			throwCoreException("new root folder must be a directory, but is not :\n" + folder.getAbsolutePath());
		}
		IdeUtil.getPreferences().setRootProjectPath(folder.getAbsolutePath());
		boolean virtualRootExistedBefore = EclipseVirtualProjectPartCreator.deleteVirtualRootProjectFull(NULL_PROGESS);
		refreshAllProjectDecorations();
		try {
			if (virtualRootExistedBefore) {
				createOrRecreateVirtualRootProject();
			}
		} catch (VirtualRootProjectException e) {
			throwCoreException("Cannot create virtual root project!", e);
		}
	}

	public static boolean existsValidationErrors() {
		/* Not very smart integrated, because static but it works... */
		return buildScriptProblemMarkerHelper.hasRegisteredMarkers();
	}

	/**
	 * If given list of console output contains error messages error markers for
	 * files will be created
	 * 
	 * @param consoleOutput
	 */
	public static void showValidationErrorsOfConsoleOutput(List<String> consoleOutput) {
		boolean validationEnabled = getPreferences().isOutputValidationEnabled();
		if (!validationEnabled) {
			return;
		}
		GradleOutputValidator validator = new GradleOutputValidator();
		ValidationResult result = validator.validate(consoleOutput);
		if (result.hasProblem()) {
			try {
				IResource resource = null;

				String scriptPath = result.getScriptPath();
				File rootFolder = getRootProjectFolderWithoutErrorHandling();
				if (rootFolder == null) {
					/*
					 * this problem should not occure, because other gradle
					 * actions does check this normally before. as a fallback
					 * simply do nothing
					 */
					logInfo("Was not able to validate, because no root folder set!");
					return;
				}
				String rootFolderPath = rootFolder.getAbsolutePath();
				File file = new File(scriptPath);
				if (!file.exists()) {
					resource = getWorkspace().getRoot();
					buildScriptProblemMarkerHelper.createErrorMarker(resource,
							"Build file which prodocues error does not exist:" + file.getAbsolutePath(), 0);
					return;
				}
				IWorkspace workspace = getWorkspace();
				resource = workspace.getRoot().getFileForLocation(Path.fromOSString(scriptPath));
				if (resource == null) {
					if (scriptPath.startsWith(rootFolderPath)) {
						scriptPath = scriptPath.substring(rootFolderPath.length());
					}
					IProject virtualRootProject = workspace.getRoot().getProject(Constants.VIRTUAL_ROOTPROJECT_NAME);
					if (virtualRootProject.exists()) {
						resource = virtualRootProject.getFile(scriptPath);
					}
				}

				if (resource == null) {
					// fall back to workspace root - so at least we can create
					// an
					// error marker...
					resource = getWorkspace().getRoot();
				}
				buildScriptProblemMarkerHelper.createErrorMarker(resource, result.getErrorMessage(), result.getLine());

			} catch (Exception e) {
				logError("Was not able to show validation errors", e);
			}
		}
		return;
	}

	/**
	 * Returns gradle root project. if no root project can be resolved an error
	 * dialog appears and shows information
	 * 
	 * @return root project or <code>null</code>
	 */
	public static GradleRootProject getRootProject() {
		return getRootProject(true);
	}

	/**
	 * Returns gradle root project or null
	 * 
	 * @param showErrorDialog
	 *            - if <code>true</code> an error dialog is shown when root
	 *            project is {@link Null}. if <code>false</code> no error dialog
	 *            is shown
	 * @return root project or <code>null</code>
	 */
	public static GradleRootProject getRootProject(boolean showErrorDialog) {
		String path = getPreferences().getRootProjectPath();
		if (StringUtils.isEmpty(path)) {
			if (showErrorDialog) {
				getDialogSupport().showError(MESSAGE_MISSING_ROOTPROJECT);
			}
			return null;
		}
		GradleRootProject rootProject;
		try {
			rootProject = new GradleRootProject(new File(path));
		} catch (IOException e1) {
			if (showErrorDialog) {
				getDialogSupport().showError(e1.getMessage());
			}
			return null;
		}
		return rootProject;
	}

	/**
	 * Get the root project folder. If not resolvable an error dialog is shown
	 * to user and a {@link IOException} is thrown
	 * 
	 * @return root project folder never <code>null</code>
	 * @throws IOException
	 *             - if root folder would be <code>null</code>
	 */
	public static File getRootProjectFolder() throws IOException {
		GradleRootProject rootProject = getRootProject();
		if (rootProject == null) {
			throw new IOException("No gradle root project available");
		}
		return rootProject.getFolder();
	}

	/**
	 * Returns root project folder or <code>null</code>. No error dialogs or
	 * exceptions are thrown
	 * 
	 * @return root project folder or <code>null</code>
	 */
	public static File getRootProjectFolderWithoutErrorHandling() {
		GradleRootProject rootProject = getRootProject(false);
		if (rootProject == null) {
			return null;
		}
		return rootProject.getFolder();
	}

	/**
	 * If a virtual root project exists, it will be returned, otherwise
	 * <code>null</code>
	 * 
	 * @return vr project or <code>null</code>
	 */
	public static IProject getVirtualRootProject() {
		IProject[] projects = getAllProjects();
		for (IProject project : projects) {
			if (hasVirtualRootProjectNature(project)) {
				return project;
			}
		}
		return null;
	}

	/**
	 * Returns true when given project has virtual root project nature
	 * 
	 * @param project
	 * @return <code>true</code> when given project has the virtual root project
	 *         nature
	 */
	public static boolean hasVirtualRootProjectNature(IProject project) {
		if (project == null) {
			return false;
		}
		boolean virtualProjectNatureFound;
		try {
			virtualProjectNatureFound = project.hasNature(VirtualRootProjectNature.NATURE_ID);
			return virtualProjectNatureFound;
		} catch (CoreException e) {
			/* ignore ... project not found anymore */
			return false;
		}
	}

	/**
	 * Returns true when given project is configured as root project
	 * 
	 * @param project
	 * @return <code>true</code> when project location is same as root project
	 */
	public static boolean isRootProject(IProject project) {
		if (project == null) {
			return false;
		}
		File rootFolder = getRootProjectFolderWithoutErrorHandling();
		if (rootFolder == null) {
			return false;
		}
		try {
			File projectLocation = getResourceHelper().toFile(project.getLocation());
			return rootFolder.equals(projectLocation);
		} catch (CoreException e) {
			/* ignore ... project not found anymore */
			return false;
		}
	}

	/**
	 * Calculates if given project is a sub project for current root. If no root
	 * project is setup, this method will always return false.
	 * 
	 * @param p
	 * @return <code>true</code> when project is sub project of current root
	 *         project
	 * @throws CoreException
	 */
	public static boolean isSubprojectOfCurrentRootProject(IProject p) throws CoreException {
		if (p == null) {
			return false;
		}
		if (!p.exists()) {
			return false;
		}
		File rootFolder = getRootProjectFolderWithoutErrorHandling();
		if (rootFolder == null) {
			return false;
		}

		IPath path = p.getLocation();
		File parentFolder = getResourceHelper().toFile(path);
		if (parentFolder == null) {
			return false;
		}
		if (!parentFolder.exists()) {
			return false;
		}
		if (!rootFolder.equals(parentFolder)) {
			parentFolder = parentFolder.getParentFile();
		}
		if (!rootFolder.equals(parentFolder)) {
			return false;
		}
		return true;
	}

	public static EGradleMessageDialogSupport getDialogSupport() {
		return EGradleMessageDialogSupport.INSTANCE;
	}

	/**
	 * Creates or recreates virtual project - this is done asynchronous. If
	 * there exists already a virtual root project it will be deleted full
	 * before the asynchronous creation process starts!
	 * 
	 * @throws VirtualRootProjectException
	 */
	public static void createOrRecreateVirtualRootProject() throws VirtualRootProjectException {
		GradleRootProject rootProject = getRootProject();
		if (rootProject == null) {
			return;
		}

		try {
			EclipseVirtualProjectPartCreator.deleteVirtualRootProjectFull(NULL_PROGESS);
		} catch (CoreException e1) {
			throw new VirtualRootProjectException("Was not able to delete former virtual root project", e1);
		}

		Job job = new Job("Virtual root project") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				EclipseVirtualProjectPartCreator partCreator = new EclipseVirtualProjectPartCreator(rootProject,
						monitor);
				try {
					virtualProjectCreator.createOrUpdate(rootProject, partCreator);
					return Status.OK_STATUS;
				} catch (VirtualRootProjectException e) {
					getDialogSupport().showError(e.getMessage());
					logError("Was not able to update virtual root project", e);
					return Status.CANCEL_STATUS;
				}
			}
		};
		job.schedule(1000L); // 1 second delay to give IDE the chance to delete
								// old parts

	}

	public static void logInfo(String info) {
		getLog().log(new Status(IStatus.INFO, IDEActivator.PLUGIN_ID, info));
	}

	public static void logWarning(String warning) {
		getLog().log(new Status(IStatus.WARNING, IDEActivator.PLUGIN_ID, warning));
	}

	public static void logError(String error, Throwable t) {
		getLog().log(new Status(IStatus.ERROR, IDEActivator.PLUGIN_ID, error, t));
	}

	private static ILog getLog() {
		ILog log = IDEActivator.getDefault().getLog();
		return log;
	}


}
