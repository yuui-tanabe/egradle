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
 package de.jcup.egradle.eclipse.gradleeditor.document;

public enum GradleDocumentIdentifiers implements GradleDocumentIdentifier {
	
	JAVA_KEYWORD,
	
	JAVA_LITERAL,
	
	GROOVY_KEYWORD ,
	
	ANNOTATION,
	
	COMMENT,
	
	GROOVY_DOC,
	
	STRING,
	
	GSTRING,
	
	GRADLE_KEYWORD,
	
	GRADLE_APPLY_KEYWORD,
	
	GRADLE_TASK_KEYWORD,
	
	GRADLE_VARIABLE,
	;


	@Override
	public String getId() {
		return name();
	}
	public static String[] allIdsToStringArray(){
		return allIdsToStringArray(null);
	}
	public static String[] allIdsToStringArray(String additionalDefaultId){
		GradleDocumentIdentifiers[] values = values();
		int size = values.length;
		if (additionalDefaultId!=null){
			size+=1;
		}
		String[] data = new String[size];
		int pos=0;
		if (additionalDefaultId!=null){
			data[pos++]=additionalDefaultId;
		}
		for (GradleDocumentIdentifiers d: values){
			data[pos++]=d.getId();
		}
		return data;
	}

}
