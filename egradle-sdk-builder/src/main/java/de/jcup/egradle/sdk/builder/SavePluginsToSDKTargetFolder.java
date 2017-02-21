package de.jcup.egradle.sdk.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SavePluginsToSDKTargetFolder implements SDKBuilderAction {

	@Override
	public void execute(SDKBuilderContext context) throws IOException {
		File targetXMLPluginsFile = new File(context.targetPathDirectory, context.gradleOriginPluginsFile.getName());
		try (FileOutputStream outputStream = new FileOutputStream(targetXMLPluginsFile)) {
			context.pluginsExporter.exportPlugins(context.xmlPlugins, outputStream);
		}

	}

}
