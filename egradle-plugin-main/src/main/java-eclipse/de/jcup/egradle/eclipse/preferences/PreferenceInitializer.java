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
package de.jcup.egradle.eclipse.preferences;

import static de.jcup.egradle.eclipse.preferences.EGradlePreferences.PREFERENCES;
import static de.jcup.egradle.eclipse.preferences.PreferenceConstants.P_GRADLE_CALL_COMMAND;
import static de.jcup.egradle.eclipse.preferences.PreferenceConstants.P_GRADLE_CALL_TYPE;
import static de.jcup.egradle.eclipse.preferences.PreferenceConstants.P_GRADLE_INSTALL_BIN_FOLDER;
import static de.jcup.egradle.eclipse.preferences.PreferenceConstants.P_GRADLE_SHELL;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		IPreferenceStore store = PREFERENCES.getPreferenceStore();
		EGradleCallType defaultCallType = null; 
		if (SystemUtils.IS_OS_WINDOWS){
			defaultCallType = EGradleCallType.WINDOWS_GRADLE_WRAPPER;	
		}else{
			defaultCallType = EGradleCallType.LINUX_GRADLE_WRAPPER;			
		}
		store.setDefault(P_GRADLE_CALL_TYPE.getId(),defaultCallType.getId());
		store.setDefault(P_GRADLE_SHELL.getId(), defaultCallType.getDefaultShell().getId());
		store.setDefault(P_GRADLE_INSTALL_BIN_FOLDER.getId(), defaultCallType.getDefaultGradleBinFolder());
		store.setDefault(P_GRADLE_CALL_COMMAND.getId(), defaultCallType.getDefaultGradleCommand());
	}

}
