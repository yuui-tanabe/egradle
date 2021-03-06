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
package de.jcup.egradle.eclipse.ide.launch;

import static de.jcup.egradle.eclipse.ide.launch.EGradleLauncherConstants.*;

import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import de.jcup.egradle.eclipse.ide.IDEActivator;
import de.jcup.egradle.eclipse.ide.IDEUtil;

public class EGradleLaunchConfigurationMainTab extends AbstractLaunchConfigurationTab {

	private Text rootProjectPathField;
	private Text projectNameField;
	private Text optionsField;
	private TaskUIPartsDelegate taskUIPartsDelegate;

	public EGradleLaunchConfigurationMainTab() {
		this.taskUIPartsDelegate = createTaskUIPartsDelegate();
	}

	protected TaskUIPartsDelegate createTaskUIPartsDelegate() {
		return new DefaultTaskUIPartsDelegate();
	}

	@Override
	public void createControl(Composite parent) {

		parent.setLayout(new FillLayout());

		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		GridLayout gridLayout = new GridLayout(2, false);
		composite.setLayout(gridLayout);

		GridData labelGridData = new GridData();
		labelGridData.horizontalAlignment = GridData.FILL;
		labelGridData.verticalAlignment = GridData.BEGINNING;
		labelGridData.grabExcessHorizontalSpace = false;
		labelGridData.grabExcessVerticalSpace = false;

		GridData gridDataSingleLine = new GridData();
		gridDataSingleLine.horizontalAlignment = GridData.FILL;
		gridDataSingleLine.verticalAlignment = GridData.BEGINNING;
		gridDataSingleLine.grabExcessHorizontalSpace = true;
		gridDataSingleLine.grabExcessVerticalSpace = false;
		gridDataSingleLine.verticalSpan = 2;
		gridDataSingleLine.horizontalSpan = 2;

		GridData gridDataTwoLines = new GridData();
		gridDataTwoLines.horizontalAlignment = GridData.FILL;
		gridDataTwoLines.verticalAlignment = GridData.BEGINNING;
		gridDataTwoLines.grabExcessHorizontalSpace = true;
		gridDataTwoLines.grabExcessVerticalSpace = false;
		gridDataTwoLines.verticalSpan = 2;
		gridDataTwoLines.horizontalSpan = 2;

		GridData gridDataLastColumn = new GridData();
		gridDataLastColumn.horizontalAlignment = GridData.FILL;
		gridDataLastColumn.verticalAlignment = GridData.FILL;
		gridDataLastColumn.grabExcessHorizontalSpace = true;
		gridDataLastColumn.grabExcessVerticalSpace = false;
		gridDataLastColumn.verticalSpan = 2;
		gridDataLastColumn.horizontalSpan = 2;
		gridDataLastColumn.minimumHeight = 50;
		gridDataLastColumn.heightHint = 100;

		/* ------------------------------------ */
		/* - Root path - */
		/* ------------------------------------ */
		Label rootPathLabel = new Label(composite, SWT.NULL);
		rootPathLabel.setText("Root path:");
		rootPathLabel.setLayoutData(labelGridData);

		rootProjectPathField = new Text(composite, SWT.BORDER);
		rootProjectPathField.setLayoutData(gridDataSingleLine);
		rootProjectPathField.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});
		rootProjectPathField.setToolTipText(
				"When not explicit defined, the root path from current EGradle Root Project is used.\n\nBut if defined you can execute also gradle tasks in other projects!");
		rootProjectPathField
				.setMessage("EGradle Root Project: " + IDEUtil.getRootProject().getFolder().getAbsolutePath());
		/* ------------------------------------ */
		/* - Project - */
		/* ------------------------------------ */
		Label projectLabel = new Label(composite, SWT.NULL);
		projectLabel.setText("Project:");
		projectLabel.setLayoutData(labelGridData);

		projectNameField = new Text(composite, SWT.BORDER);
		projectNameField.setLayoutData(gridDataSingleLine);
		projectNameField.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});
		projectNameField.setToolTipText(
				"Keep empty for root project or enter gradle subproject name here if you want to execute the tasks in a sub project.");
		/* ------------------------------------ */
		/* - Tasks - */
		/* ------------------------------------ */
		taskUIPartsDelegate.addTaskComponents(composite, labelGridData, gridDataTwoLines);
		/* ------------------------------------ */
		/* - Separator - */
		/* ------------------------------------ */
		Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		/* ------------------------------------ */
		/* - Options - */
		/* ------------------------------------ */
		Label optionsLabel = new Label(composite, SWT.NULL);
		optionsLabel.setText("Raw options:");
		optionsLabel.setToolTipText(
				"Here you can define options.\nBut system properties and gradle propject properties can be defined more comfortable at the dedicated tab sheets!");
		optionsLabel.setLayoutData(labelGridData);

		optionsField = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		optionsField.setLayoutData(gridDataLastColumn);
		optionsField.setToolTipText(optionsLabel.getToolTipText());
		optionsField.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});

		Link link = new Link(composite, SWT.NONE);
		String message = "(Valid options for gradle can be found at <a href=\"https://docs.gradle.org/current/userguide/gradle_command_line.html\">Gradle command line userguide</a>)";
		link.setText(message);
		link.setSize(400, 100);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					// Open default external browser
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
				} catch (Exception ex) {
					IDEUtil.logError("Was not able to open url in external browser", ex);
				}
			}
		});
		link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

		composite.pack();

	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			taskUIPartsDelegate.setTaskFieldText(configuration);
			rootProjectPathField.setText(configuration.getAttribute(PROPERTY_ROOT_PROJECT_PATH, ""));
			projectNameField.setText(configuration.getAttribute(PROPERTY_PROJECTNAME, ""));
			optionsField.setText(configuration.getAttribute(PROPERTY_OPTIONS, ""));
		} catch (CoreException e) {
			throw new IllegalStateException("cannot init", e);
		}

	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		IProject project = getProject();
		if (project != null) {
			EGradleLaunchShortCut.setProjectNameIgnoreVirtualRootProjectNames(configuration, project.getName());
		}
	}

	/**
	 * Try to resolve the project
	 * 
	 * @return project or <code>null</code>
	 */
	protected IProject getProject() {
		IWorkbenchWindow window = IDEActivator.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			ISelection selection = page.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				if (!ss.isEmpty()) {
					Object obj = ss.getFirstElement();
					if (obj instanceof IProject) {
						return (IProject) obj;
					}
					if (obj instanceof IResource) {
						IProject pro = ((IResource) obj).getProject();
						return pro;

					}
				}
			}
		}
		return null;
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		taskUIPartsDelegate.applyTasks(configuration);
		configuration.setAttribute(PROPERTY_ROOT_PROJECT_PATH, rootProjectPathField.getText());
		configuration.setAttribute(PROPERTY_PROJECTNAME, projectNameField.getText());
		configuration.setAttribute(PROPERTY_OPTIONS, optionsField.getText());
	}

	protected abstract class TaskUIPartsDelegate {
		protected abstract void setTaskFieldText(ILaunchConfiguration configuration) throws CoreException;

		protected abstract void addTaskComponents(Composite composite, GridData labelGridData,
				GridData gridDataTwoLines);

		protected abstract void applyTasks(ILaunchConfigurationWorkingCopy configuration);
	}

	protected class DefaultTaskUIPartsDelegate extends TaskUIPartsDelegate {

		protected Text tasksField;

		@Override
		protected void setTaskFieldText(ILaunchConfiguration configuration) throws CoreException {
			tasksField.setText(configuration.getAttribute(PROPERTY_TASKS, ""));
		}

		@Override
		protected void addTaskComponents(Composite composite, GridData labelGridData, GridData gridDataTwoLines) {
			Label taskLabel = new Label(composite, SWT.NULL);
			taskLabel.setText("Tasks:");
			taskLabel.setLayoutData(labelGridData);

			tasksField = new Text(composite, SWT.BORDER);
			tasksField.setLayoutData(gridDataTwoLines);
			tasksField.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					setDirty(true);
					updateLaunchConfigurationDialog();
				}
			});
			tasksField.setToolTipText("Enter gradle tasks here. Separate multiple tasks with a single space character");
		}

		@Override
		protected void applyTasks(ILaunchConfigurationWorkingCopy configuration) {
			configuration.setAttribute(PROPERTY_TASKS, tasksField.getText());
		}

	}

	@Override
	public Image getImage() {
		return IDEUtil.getImage("icons/gradle-og.png");
	}

	@Override
	public String getName() {
		return "Gradle";
	}

}
