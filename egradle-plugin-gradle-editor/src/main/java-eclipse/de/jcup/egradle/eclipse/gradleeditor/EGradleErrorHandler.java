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

import de.jcup.egradle.core.util.ErrorHandler;

public class EGradleErrorHandler implements ErrorHandler {

	public static EGradleErrorHandler INSTANCE = new EGradleErrorHandler();

	EGradleErrorHandler() {

	}

	@Override
	public void handleError(String message, Throwable t) {
		EditorUtil.INSTANCE.logError(message, t);
	}

	@Override
	public void handleError(String message) {
		EditorUtil.INSTANCE.logError(message, null);
	}

}
