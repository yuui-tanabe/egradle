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
package de.jcup.egradle.codeassist.dsl;

/**
 * Represents a property or a variable or a field or...
 * 
 * @author Albert Tregnaghi
 *
 */
public interface Property extends LanguageElement, GradleDocumentationInfo, TypeChild, Comparable<Property> {

	/**
	 * Returns type of property itself
	 * 
	 * @return type of property
	 */
	public Type getType();

	public String getTypeAsString();

}
