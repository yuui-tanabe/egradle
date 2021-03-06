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
package de.jcup.egradle.integration;

import org.apache.commons.lang3.StringUtils;

import de.jcup.egradle.core.TextProvider;
import de.jcup.egradle.core.TextProviderException;

public class IntegrationTestTextProvider implements TextProvider {

	private String text;

	IntegrationTestTextProvider(String text) {
		this.text = text;
	}

	@Override
	public String getText(int offset, int length) throws TextProviderException {
		try {
			return text.substring(offset, offset + length);
		} catch (RuntimeException e) {
			throw new TextProviderException("cannot get text", e);
		}
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public int getLineOffset(int offset) throws TextProviderException {
		/* lines in read textfiles will always contain \n... */
		String[] lines = StringUtils.splitByWholeSeparator(text, "\n");
		if (offset == 0) {
			return 0;
		}
		int linePos = 0;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int ending = linePos + line.length() + 1;// +1 because of \n was
														// removed before
			if (offset >= linePos && offset <= ending) {
				return linePos;
			}
			linePos = ending + 1;
		}
		throw new TextProviderException("Cannot determine line offset for:" + offset);
	}

}
