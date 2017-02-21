package de.jcup.egradle.sdk.builder;

import java.io.FileOutputStream;
import java.io.IOException;

import de.jcup.egradle.codeassist.dsl.Type;
import de.jcup.egradle.codeassist.dsl.XMLType;

public class SaveTypesToSDKTargetFolder implements SDKBuilderAction {

	@Override
	public void execute(SDKBuilderContext context) throws IOException {
		if (context.allTypes.isEmpty()){
			throw new IllegalArgumentException("all types empty!!??!");
		}
		for (String typeName : context.allTypes.keySet()) {
			Type type=context.originGradleFilesProvider.getType(typeName);
			try(FileOutputStream fos = new FileOutputStream(context.allTypes.get(typeName))){
				context.typeExporter.exportType((XMLType) type, fos);
			}
		}

	}

}
