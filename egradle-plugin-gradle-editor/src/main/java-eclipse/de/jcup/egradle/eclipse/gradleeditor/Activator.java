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
package de.jcup.egradle.eclipse.gradleeditor;

import java.io.File;
import java.io.IOException;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.jcup.egradle.codeassist.CodeCompletionRegistry;
import de.jcup.egradle.codeassist.dsl.ApiMappingImporter;
import de.jcup.egradle.codeassist.dsl.FilesystemFileLoader;
import de.jcup.egradle.codeassist.dsl.XMLPluginsImporter;
import de.jcup.egradle.codeassist.dsl.XMLTypeImporter;
import de.jcup.egradle.codeassist.dsl.gradle.GradleDSLTypeProvider;
import de.jcup.egradle.core.api.ErrorHandler;
import de.jcup.egradle.eclipse.api.ColorManager;
import de.jcup.egradle.eclipse.api.EGradleErrorHandler;
import de.jcup.egradle.eclipse.api.EGradleUtil;
import de.jcup.egradle.eclipse.plugin.sdk.SDK;
import de.jcup.egradle.eclipse.plugin.sdk.SDKManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in COMMAND_ID
	public static final String PLUGIN_ID = "de.jcup.egradle.eclipse.plugin.editor.gradle"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	private ColorManager colorManager;

	private CodeCompletionRegistry codeCompletionRegistry;

	/**
	 * The constructor
	 */
	public Activator() {
		colorManager = new ColorManager();
		codeCompletionRegistry = new CodeCompletionRegistry();
	}

	public ColorManager getColorManager() {
		return colorManager;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		ErrorHandler errorHandler = EGradleErrorHandler.INSTANCE;
		codeCompletionRegistry.setErrorHandler(errorHandler);
		
		SDK sdk = SDKManager.get().getCurrentSDK();
		
		
		/* FIXME ATR, 14.02.2017: 14.02.2017: bundle resolving of folde does not seem to work. maybe its would be better
		 * to do xz-packaging to a file and suport the file from SDK to this location and copy + unpack it to user.home
		 * (when not already installed)
		 *  */
		System.out.println("current sdk:"+sdk);
		if (! sdk.isInstalled()){
			try{
				sdk.install();
			}catch(IOException e){
				EGradleUtil.log("Was not able install SDK:"+sdk.getVersion(),e);
			}
		}
		File dslFolder = sdk.getSDKInstallationFolder();
//		File dslFolder = fetchSDKDSLFolder(errorHandler);
		/*
		 * init code completion parts - when dsl folder not correctly set it
		 * will not work but it is robust will only do nothing
		 */
		XMLTypeImporter typeImporter = new XMLTypeImporter();
		XMLPluginsImporter pluginsImporter = new XMLPluginsImporter();
		ApiMappingImporter apiMappingImporter = new ApiMappingImporter();
		FilesystemFileLoader loader = new FilesystemFileLoader(typeImporter, pluginsImporter, apiMappingImporter);
		/*
		 * FIXME ATR, 19.01.2017: make version changeable... Maybe
		 * codeCompletionRegistry.get(GradleDSLTypeProvider.changeVersion...
		 */
		// loader.setDSLFolder(FileHelper.DEFAULT.getEGradleUserHomeFolder("SDK/gradle/3.0"));
		if (dslFolder != null) {
			loader.setDSLFolder(dslFolder);
		}
		GradleDSLTypeProvider gradleDslProvider = new GradleDSLTypeProvider(loader);
		gradleDslProvider.setErrorHandler(errorHandler);
		/*
		 * install dsl type provider as service, so it must be definitely used
		 * shared...
		 */
		codeCompletionRegistry.registerService(GradleDSLTypeProvider.class, gradleDslProvider);

		/* load project per default so show up time for tooltips faster */
		gradleDslProvider.getType("org.gradle.api.Project");

	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		colorManager.dispose();
		super.stop(context);
	}

	public CodeCompletionRegistry getCodeCompletionRegistry() {
		return codeCompletionRegistry;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
