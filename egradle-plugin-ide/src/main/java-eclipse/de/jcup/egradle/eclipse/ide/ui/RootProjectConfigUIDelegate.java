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
package de.jcup.egradle.eclipse.ide.ui;

import static de.jcup.egradle.eclipse.ide.preferences.EGradleIdePreferenceConstants.*;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;

import de.jcup.egradle.core.config.MutableGradleConfiguration;
import de.jcup.egradle.core.domain.CancelStateProvider;
import de.jcup.egradle.core.process.EGradleShellType;
import de.jcup.egradle.core.process.OutputHandler;
import de.jcup.egradle.eclipse.ide.IDEActivator;
import de.jcup.egradle.eclipse.ide.IDEUtil;
import de.jcup.egradle.eclipse.ide.execution.validation.RootProjectValidationHandler;
import de.jcup.egradle.eclipse.ide.execution.validation.RootProjectValidationObserver;
import de.jcup.egradle.eclipse.ide.execution.validation.RootProjectValidationProgressRunnable;
import de.jcup.egradle.eclipse.preferences.ChangeableComboFieldEditor;
import de.jcup.egradle.eclipse.preferences.EGradleCallType;
import de.jcup.egradle.eclipse.ui.SWTFactory;
import de.jcup.egradle.eclipse.util.ColorManager;
import de.jcup.egradle.eclipse.util.EclipseUtil;

public class RootProjectConfigUIDelegate implements RootProjectValidationObserver, IPropertyChangeListener {

	private ChangeableComboFieldEditor gradleCallTypeRadioButton;
	private ChangeableComboFieldEditor shellFieldEditor;
	private StringFieldEditor gradleCommandFieldEditor;
	private DirectoryFieldEditor gradleInstallBinDirectoryFieldEditor;
	private Group gradleCallGroup;
	private Text validationOutputField;
	private DirectoryFieldEditor rootPathDirectoryEditor;
	private DirectoryFieldEditor defaultJavaHomeDirectoryEditor;
	private Button validationButton;

	private RootProjectValidationHandler validation;
	private RootProjectConfigMode mode;
	private Button restoreMetaDataCheckbox;
	private boolean rootPathMayBeEmpty;

	public RootProjectConfigUIDelegate(RootProjectValidationHandler validation) {
		this(validation, null);
	}

	public RootProjectConfigUIDelegate(RootProjectValidationHandler validation,
			RootProjectConfigMode rootProjectConfigMode) {
		this.validation = validation;
		if (rootProjectConfigMode == null) {
			this.mode = RootProjectConfigMode.IMPORT_PROJECTS;
		} else {
			this.mode = rootProjectConfigMode;
		}
	}

	/**
	 * Creates the configuration UI containing field editors. Field editors are
	 * abstractions of the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and restore itself.
	 * 
	 * @param parent
	 *            parent composite to add to
	 */
	public void createConfigUI(Composite parent) {
		createDefaults(parent);
		createGradleCallTypeGroup(parent);
		createValidationGroup(parent);
	}

	private void createValidationGroup(Composite parent) {
		if (!mode.isValidationGroupNeeded()) {
			return;
		}
		/* ------------------------------------ */
		/* - Check output - */
		/* ------------------------------------ */
		GridData groupLayoutData = new GridData();
		groupLayoutData.horizontalAlignment = GridData.FILL;
		groupLayoutData.verticalAlignment = GridData.FILL;
		groupLayoutData.grabExcessHorizontalSpace = true;
		groupLayoutData.grabExcessVerticalSpace = true;
		groupLayoutData.verticalSpan = 2;
		groupLayoutData.horizontalSpan = 3;

		Group validationGroup = SWTFactory.createGroup(parent, "Validate preferences correct", 1, 10, SWT.FILL);
		validationGroup.setLayoutData(groupLayoutData);

		GridData labelGridData = new GridData();
		labelGridData.horizontalAlignment = GridData.FILL;
		labelGridData.verticalAlignment = GridData.BEGINNING;
		labelGridData.grabExcessHorizontalSpace = false;
		labelGridData.grabExcessVerticalSpace = false;

		GridData gridDataLastColumn = new GridData();
		gridDataLastColumn.horizontalAlignment = GridData.FILL;
		gridDataLastColumn.verticalAlignment = GridData.FILL;
		gridDataLastColumn.grabExcessHorizontalSpace = true;
		gridDataLastColumn.grabExcessVerticalSpace = true;
		gridDataLastColumn.verticalSpan = 2;
		gridDataLastColumn.horizontalSpan = 2;
		gridDataLastColumn.minimumHeight = 50;
		gridDataLastColumn.heightHint = 100;

		validationOutputField = new Text(validationGroup, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);
		validationOutputField.setLayoutData(gridDataLastColumn);

		validationButton = new Button(validationGroup, SWT.NONE);
		validationButton.setText("Start validation");
		validationButton.setImage(getValidationButtonImage());

		validationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (validationRunning == true) {
					return;
				}
				setValid(false);
				handleValidationRunning(true);
				validationOutputField.setText("Start validation...\n");

				OutputHandler validationOutputHandler = new OutputHandler() {

					@Override
					public void output(String line) {
						EclipseUtil.safeAsyncExec(new Runnable() {

							@Override
							public void run() {
								validationOutputField.append(line + "\n");
							}
						});
					}

				};

				MutableGradleConfiguration configuration = new MutableGradleConfiguration();
				configuration.setGradleCommand(getGradleCommand());
				configuration.setGradleBinDirectory(getGradleBinDirectory());
				configuration.setShellCommand(getShellCommand());
				configuration.setWorkingDirectory(getWorkingDirectory());
				configuration.setJavaHome(getGlobalJavaHomePath());

				startValidation(validationOutputHandler, configuration);
			}

			private void startValidation(OutputHandler validationOutputHandler,
					MutableGradleConfiguration configuration) {
				try {
					RootProjectConfigUIDelegate observer = RootProjectConfigUIDelegate.this;
					ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(
							EclipseUtil.getActiveWorkbenchShell());

					RootProjectValidationProgressRunnable runnable = new RootProjectValidationProgressRunnable(
							new CancelStateProvider() {

								@Override
								public boolean isCanceled() {
									return progressMonitorDialog.getProgressMonitor().isCanceled();
								}

							}, configuration, observer, validationOutputHandler);
					/*
					 * use own progress monitor dialog here - progress service
					 * did not work well because this is in model state
					 */
					progressMonitorDialog.run(true, true, runnable);
				} catch (InvocationTargetException | InterruptedException e1) {
					IDEUtil.logError("Was not able to execute validation", e1);
				}
			}
		});
	}

	private void createGradleCallTypeGroup(Composite parent) {
		GridData groupLayoutData = new GridData();
		groupLayoutData.horizontalAlignment = GridData.FILL;
		groupLayoutData.verticalAlignment = GridData.BEGINNING;
		groupLayoutData.grabExcessHorizontalSpace = true;
		groupLayoutData.grabExcessVerticalSpace = false;
		groupLayoutData.verticalSpan = 3;
		groupLayoutData.horizontalSpan = 3;

		GridData labelGridData = new GridData();
		labelGridData.horizontalAlignment = GridData.FILL;
		labelGridData.verticalAlignment = GridData.BEGINNING;
		labelGridData.grabExcessHorizontalSpace = false;
		labelGridData.grabExcessVerticalSpace = false;

		GridData gridDataLastColumn = new GridData();
		gridDataLastColumn.horizontalAlignment = GridData.FILL;
		gridDataLastColumn.verticalAlignment = GridData.FILL;
		gridDataLastColumn.grabExcessHorizontalSpace = true;
		gridDataLastColumn.grabExcessVerticalSpace = false;
		gridDataLastColumn.horizontalSpan = 2;

		GridData labelGridData2 = new GridData();
		labelGridData2.horizontalAlignment = GridData.FILL;
		labelGridData2.verticalAlignment = GridData.BEGINNING;
		labelGridData2.grabExcessHorizontalSpace = false;
		labelGridData2.grabExcessVerticalSpace = false;
		labelGridData2.grabExcessHorizontalSpace = true;

		gradleCallGroup = SWTFactory.createGroup(parent, "Gradle call", 1, 10, SWT.FILL);
		if (debug) {
			gradleCallGroup.setBackground(getColorManager().getColor(new RGB(0, 255, 0)));
		}
		gradleCallGroup.setLayoutData(groupLayoutData);
		/* @formatter:off */
		String[][] entryNamesAndValues = new String[][] {
				new String[] { "Windows - Gradle wrapper in root project",EGradleCallType.WINDOWS_GRADLE_WRAPPER.getId() },
				new String[] { "Windows - Use gradle installation", EGradleCallType.WINDOWS_GRADLE_INSTALLED.getId() },
				new String[] { "Linux/Mac - Gradle wrapper in root project", EGradleCallType.LINUX_GRADLE_WRAPPER.getId() },
				new String[] { "Linux/Mac - Use gradle installation", EGradleCallType.LINUX_GRADLE_INSTALLED.getId() },
				new String[] { "Custom", EGradleCallType.CUSTOM.getId() } };
		/* @formatter:on */
		gradleCallTypeRadioButton = new ChangeableComboFieldEditor(P_GRADLE_CALL_TYPE.getId(), "Call type",
				entryNamesAndValues, gradleCallGroup);
		gradleCallTypeRadioButton.getLabelControl(gradleCallGroup).setLayoutData(labelGridData);
		gradleCallTypeRadioButton.getComboBoxControl(gradleCallGroup).setLayoutData(gridDataLastColumn);
		addField(gradleCallTypeRadioButton);

		if (debug) {
			gradleCallTypeRadioButton.getComboBoxControl(gradleCallGroup)
					.setBackground(getColorManager().getColor(new RGB(0, 0, 255)));
		}
		gradleCallTypeRadioButton.setPropertyChangeListener(this);

		/* @formatter:off */
		String[][] shellTypeComboValues = 
				new String[][] { 
					new String[] { "(no shell used)", EGradleShellType.NONE.getId() },
					new String[] { "bash", EGradleShellType.BASH.getId() },
					new String[] { "sh", EGradleShellType.SH.getId() },
					new String[] { "cmd", EGradleShellType.CMD.getId() } };
		/* @formatter:on */
		shellFieldEditor = new ChangeableComboFieldEditor(P_GRADLE_SHELL.getId(), "Shell", shellTypeComboValues,
				gradleCallGroup);
		shellFieldEditor.getLabelControl(gradleCallGroup).setLayoutData(labelGridData);
		shellFieldEditor.getComboBoxControl(gradleCallGroup).setLayoutData(gridDataLastColumn);
		addField(shellFieldEditor);

		gradleCommandFieldEditor = new StringFieldEditor(P_GRADLE_CALL_COMMAND.getId(), "Gradle call", gradleCallGroup);
		gradleCommandFieldEditor.getLabelControl(gradleCallGroup).setLayoutData(labelGridData);
		gradleCommandFieldEditor.getTextControl(gradleCallGroup).setLayoutData(gridDataLastColumn);
		addField(gradleCommandFieldEditor);

		gradleInstallBinDirectoryFieldEditor = new DirectoryFieldEditor(P_GRADLE_INSTALL_BIN_FOLDER.getId(),
				"Gradle bin folder:", gradleCallGroup);
		gradleInstallBinDirectoryFieldEditor.getLabelControl(gradleCallGroup).setLayoutData(labelGridData);
		gradleInstallBinDirectoryFieldEditor.getTextControl(gradleCallGroup).setLayoutData(labelGridData2);

		addField(gradleInstallBinDirectoryFieldEditor);
	}

	private ColorManager getColorManager() {
		IDEActivator iDEActivator = IDEActivator.getDefault();
		if (iDEActivator == null) {
			return ColorManager.getStandalone();
		}
		return iDEActivator.getColorManager();
	}

	private void createDefaults(Composite parent) {
		GridData groupLayoutData = new GridData();
		groupLayoutData.horizontalAlignment = GridData.FILL;
		groupLayoutData.verticalAlignment = GridData.BEGINNING;
		groupLayoutData.grabExcessHorizontalSpace = true;
		groupLayoutData.grabExcessVerticalSpace = false;
		groupLayoutData.verticalSpan = 2;
		groupLayoutData.horizontalSpan = 3;

		Group defaultGroup = SWTFactory.createGroup(parent, "", 1, 10, SWT.FILL);
		defaultGroup.setLayoutData(groupLayoutData);

		rootPathDirectoryEditor = new DirectoryFieldEditor(P_ROOTPROJECT_PATH.getId(), "&Gradle root project path",
				defaultGroup);
		addField(rootPathDirectoryEditor);
		GridData directoryTextLayout = new GridData(SWT.FILL, SWT.TOP, true, false);

		String rootPathTooltipText = "Default root path. Can be overriden in launch configurations";
		rootPathDirectoryEditor.getLabelControl(defaultGroup).setToolTipText(rootPathTooltipText);

		Text rootPathTextControl = rootPathDirectoryEditor.getTextControl(defaultGroup);
		rootPathTextControl.setToolTipText(rootPathTooltipText);
		rootPathTextControl.setLayoutData(directoryTextLayout);

		rootPathDirectoryEditor.setEmptyStringAllowed(rootPathMayBeEmpty);

		if (!mode.isRootPathDirectoryEditable()) {
			rootPathDirectoryEditor.setEnabled(false, defaultGroup);
		}
		/* java home default */
		defaultJavaHomeDirectoryEditor = new DirectoryFieldEditor(P_JAVA_HOME_PATH.getId(), "&JAVA HOME (optional)",
				defaultGroup);
		defaultJavaHomeDirectoryEditor.setEmptyStringAllowed(true);
		String defaultJavaHomeDirectoryTooltipText = "A default global JAVA_HOME path. Can be overriden in launch configurations";
		defaultJavaHomeDirectoryEditor.getLabelControl(defaultGroup)
				.setToolTipText(defaultJavaHomeDirectoryTooltipText);

		Text javaHomeTextControl = defaultJavaHomeDirectoryEditor.getTextControl(defaultGroup);
		javaHomeTextControl.setToolTipText(defaultJavaHomeDirectoryTooltipText);
		javaHomeTextControl.setLayoutData(directoryTextLayout);

		addField(defaultJavaHomeDirectoryEditor);

		/* restore meta data */
		if (mode.isRestoreMetaDataCheckBoxNeeded()) {
			restoreMetaDataCheckbox = SWTFactory.createCheckButton(defaultGroup,
					"Restore former meta data (e.g. Team provider data)", null, true, SWT.LEFT);
		}
	}

	Image getValidationButtonImage() {
		return IDEUtil.getImage("icons/gradle-og.png");
	}

	private void setValid(boolean valid) {
		validation.onValidationStateChanged(valid);

	}

	private void addField(FieldEditor field) {
		validation.addFieldEditor(field);
	}

	private boolean validationRunning = false;
	boolean debug;

	@Override
	public void handleValidationResult(boolean valid) {
		validation.handleValidationResult(valid);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (validation.isHandlingPropertyChanges()) {
			return;
		}
		if (isGradleCallTypeButton(event)) {
			updateCallTypeFields(event.getNewValue());
		}
	}

	public boolean isGradleCallTypeButton(PropertyChangeEvent event) {
		return gradleCallTypeRadioButton == event.getSource();
	}

	/**
	 * Update call type has changed before - this method updates only dependent
	 * parts!
	 * 
	 * @param value
	 */
	public void updateCallTypeFields(Object value) {
		if (!(value instanceof String)) {
			return;
		}
		String callTypeId = (String) value;
		updateCallTypeGroupEnabledState(callTypeId);

		EGradleCallType callType = EGradleCallType.findById(callTypeId);
		if (callType == null) {
			throw new IllegalStateException("Wrong implemented - not a call type ID:" + callTypeId);
		}
		if (EGradleCallType.CUSTOM.equals(callType)) {
			/*
			 * do nothing, just keep the editor fields from former default
			 * settings - user can change...
			 */
		} else {
			setCallGroupPartsEnabled(false);
			/* set defaults */
			shellFieldEditor.setStringValue(callType.getDefaultShell().getId());
			gradleInstallBinDirectoryFieldEditor.setStringValue(callType.getDefaultGradleBinFolder());
			gradleCommandFieldEditor.setStringValue(callType.getDefaultGradleCommand());
		}

	}

	/**
	 * Set call type by given id
	 * 
	 * @param callTypeId
	 */
	public void updateCallTypeGroupEnabledState(String callTypeId) {
		if (EGradleCallType.CUSTOM.getId().equals(callTypeId)) {
			/* set all editable */
			setCallGroupPartsEnabled(true);
		} else {
			setCallGroupPartsEnabled(false);
			/* set defaults */
		}
	}

	private void setCallGroupPartsEnabled(boolean enabled) {
		shellFieldEditor.setEnabled(enabled, gradleCallGroup);
		gradleCommandFieldEditor.setEnabled(enabled, gradleCallGroup);
		gradleInstallBinDirectoryFieldEditor.setEnabled(enabled, gradleCallGroup);
	}

	public void init(IWorkbench workbench) {

	}

	@Override
	public void handleValidationRunning(boolean running) {
		validationRunning = running;
		validation.handleValidationRunning(running);
	}

	public String getRootPathDirectory() {
		return rootPathDirectoryEditor.getStringValue();
	}

	public void setGradleCallTypeId(String gradleCallTypeID) {
		gradleCallTypeRadioButton.setStringValue(gradleCallTypeID);
		updateCallTypeFields(gradleCallTypeID);
	}

	public void setRootPathMayBeEmpty(boolean rootPathMayBeEmpty) {
		this.rootPathMayBeEmpty = rootPathMayBeEmpty;
	}

	public String getGradleRootPathText() {
		return this.rootPathDirectoryEditor.getStringValue();
	}

	public void setGlobalJavaHomePath(String globalJavaHomePath) {
		this.defaultJavaHomeDirectoryEditor.setStringValue(globalJavaHomePath);
	}

	public void setRootProjectPath(String rootPath) {
		this.rootPathDirectoryEditor.setStringValue(rootPath);
	}

	public String getGlobalJavaHomePath() {
		return defaultJavaHomeDirectoryEditor.getStringValue();
	}

	public boolean isRestoringMetaData() {
		if (restoreMetaDataCheckbox == null) {
			return false;
		}
		return restoreMetaDataCheckbox.getSelection();
	}

	private String getShellId() {
		return shellFieldEditor.getStringValue();
	}

	public EGradleShellType getShellCommand() {
		String shellTypeAsString = getShellId();
		EGradleShellType type = EGradleShellType.findById(shellTypeAsString);
		return type;
	}

	public String getWorkingDirectory() {
		return rootPathDirectoryEditor.getStringValue();
	}

	public String getGradleBinDirectory() {
		return gradleInstallBinDirectoryFieldEditor.getStringValue();
	}

	public String getGradleCommand() {
		return gradleCommandFieldEditor.getStringValue();
	}

	public String getCallTypeId() {
		return gradleCallTypeRadioButton.getStringValue();
	}

	public void setGradleBinInstallFolder(String gradleBinInstallFolder) {
		gradleInstallBinDirectoryFieldEditor.setStringValue(gradleBinInstallFolder);
	}

	public void setGradleCallCommand(String gradleCallCommand) {
		this.gradleCommandFieldEditor.setStringValue(gradleCallCommand);
	}

	public void setShellId(String shellId) {
		this.shellFieldEditor.setStringValue(shellId);
	}

}