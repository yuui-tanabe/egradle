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
package de.jcup.egradle.integration.test;

import static de.jcup.egradle.integration.HoverDataAssert.assertThat;
import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import de.jcup.egradle.codeassist.RelevantCodeCutter;
import de.jcup.egradle.codeassist.dsl.gradle.GradleFileType;
import de.jcup.egradle.codeassist.dsl.gradle.estimation.GradleLanguageElementEstimater;
import de.jcup.egradle.codeassist.hover.HoverData;
import de.jcup.egradle.codeassist.hover.HoverSupport;
import de.jcup.egradle.core.model.Model;
import de.jcup.egradle.integration.IntegrationTestComponents;

public class HoverIntegrationTest {

	@Rule
	public IntegrationTestComponents components = IntegrationTestComponents.initialize();

	@Test
	public void buildfile_18__ear_plugin_by_convention() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test-19-ear-plugin-by-convention.gradle");
		int offset = text.indexOf("ear {");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForElementType("org.gradle.plugins.ear.Ear");

	}

	@Test
	public void buildfile_17__checkstyle_extension() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test-17-checkstyle-extension.gradle");
		int offset = text.indexOf("checkstyle");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForExtension("checkstyle", "org.gradle.api.plugins.quality.CheckstyleExtension");

	}

	@Test
	@Ignore
	/*
	 * TODO ATR, 12.03.2017: strange: CheckStyleReports, to which is delegated
	 * has a html method but this method is a simple getter without Parameter.
	 * So how does the example - visible in javadoc /egradle sdk works (see
	 * test-15 file) ?
	 */
	public void buildfile_15__reports_delegates_toCheckStyleReports() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile(
				"test-15-checkstyle-task_myCheckStyleTask_type_CheckStyle.gradle");
		int offset = text.indexOf("html");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.plugins.quality.Checkstyle.reports", "groovy.lang.Closure");

	}

	@Test
	public void buildfile_15__my_task_with_type_checkstyle_reports_delegates_toCheckStyleReports() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile(
				"test-15-checkstyle-task_myCheckStyleTask_type_CheckStyle.gradle");
		int offset = text.indexOf("reports");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.plugins.quality.Checkstyle.reports", "groovy.lang.Closure");

	}

	@Test
	public void buildfile_14__tasks_withType_CheckStyle_reports_delegates_toCheckStyleReports() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test-14-checkstyle-tasks.withTypeCheckstyle.gradle");
		int offset = text.indexOf("reports");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.plugins.quality.Checkstyle.reports", "groovy.lang.Closure");

	}

	@Test
	public void buildfile__with_dependencies_in_root__when_cursor_is_at_dependencies_offset_exactly__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test1-dependencies-block-inside-root.gradle");
		int offset = text.indexOf("dependencies");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.Project.dependencies", "groovy.lang.Closure");
	}

	@Test
	public void buildfile__with_dependencies_in_root__when_cursor_is_at_gradleApi_offset_exactly__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test7-dependencies-block-inside-root-with-gradleApi.gradle");
		int offset = text.indexOf("gradleApi");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.artifacts.dsl.DependencyHandler.gradleApi");
	}

	@Test
	public void buildfile__with_dependencies_in_root__when_cursor_is_at_dependencies_offset_negative_one__no_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test1-dependencies-block-inside-root.gradle");
		int offset = text.indexOf("dependencies");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset - 1);

		/* test */
		assertNull(hoverData);

	}

	@Test
	public void buildfile__with_dependencies_in_root__when_cursor_is_at_dependencies_offset_more_than_length_of_dependencies_string__no_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test1-dependencies-block-inside-root.gradle");
		int offset = text.indexOf("dependencies");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset + "dependencies".length() + 1);

		/* test */
		assertNull(hoverData);

	}

	@Test
	public void buildfile__with_dependencies_in_root__when_cursor_is_at_dependencies_offset_plus1__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test1-dependencies-block-inside-root.gradle");
		int offset = text.indexOf("dependencies");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset + 1);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.Project.dependencies", "groovy.lang.Closure");

	}

	@Test
	public void buildfile__with_dependencies_in_root__when_cursor_is_at_dependencies_offset_plus2__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test1-dependencies-block-inside-root.gradle");
		int offset = text.indexOf("dependencies");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset + 2);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.Project.dependencies", "groovy.lang.Closure");

	}

	@Test
	public void buildfile__with_dependencies_in_root__when_cursor_is_at_dependencies_offset_plus_length_of_dependencies__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test1-dependencies-block-inside-root.gradle");
		int offset = text.indexOf("dependencies");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset + "dependencies".length());

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.Project.dependencies", "groovy.lang.Closure");

	}

	@Test
	public void buildfile__with_dependencies_in_subprojects__when_cursor_is_at_dependencies() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test2-dependencies-block-inside-subprojects.gradle");
		int offset = text.indexOf("dependencies");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.Project.dependencies", "groovy.lang.Closure");
	}

	@Test
	public void buildfile__with_dependencies_in_allprojects__when_cursor_is_at_dependencies() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test3-dependencies-block-inside-allprojects.gradle");
		int offset = text.indexOf("dependencies");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		assertThat(hoverData).isForMethod("org.gradle.api.Project.dependencies", "groovy.lang.Closure");
	}

	@Test
	public void buildfile__with_dependencies_in_allprojects__when_cursor_is_at_allprojects() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test3-dependencies-block-inside-allprojects.gradle");
		int offset = text.indexOf("allprojects");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.Project.allprojects","groovy.lang.Closure");
		/* @formatter:on*/

	}

	@Test
	public void buildfile__with_repositories_in_root__when_cursor_is_at_repositories() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test4-repositories-block-inside-root.gradle");
		int offset = text.indexOf("repositories");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.Project.repositories","groovy.lang.Closure");
		/* @formatter:on*/

	}

	@Test
	public void buildfile__with_repositories_in_subprojects__when_cursor_is_at_repositories() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test5-repositories-block-inside-subprojects.gradle");
		int offset = text.indexOf("repositories");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.Project.repositories","groovy.lang.Closure");
		/* @formatter:on*/

	}

	@Test
	public void buildfile__with_repositories_in_root__when_cursor_is_at_mavenLocal() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test4-repositories-block-inside-root.gradle");
		int offset = text.indexOf("mavenLocal");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.artifacts.dsl.RepositoryHandler.mavenLocal");
		/* @formatter:on*/

	}

	@Test
	public void buildfile__with_repositories_in_subprojects__when_cursor_is_at_mavenLocal() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test4-repositories-block-inside-root.gradle");
		int offset = text.indexOf("mavenLocal");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.artifacts.dsl.RepositoryHandler.mavenLocal");
		/* @formatter:on*/

	}

	@Test
	public void buildfile__with_repositories_in_root__when_cursor_is_at_mavenCentral() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test4-repositories-block-inside-root.gradle");
		int offset = text.indexOf("mavenCentral");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.artifacts.dsl.RepositoryHandler.mavenCentral");
		/* @formatter:on*/

	}

	@Test
	public void buildfile__with_repositories_in_subprojects__when_cursor_is_at_mavenCentral() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test5-repositories-block-inside-subprojects.gradle");
		int offset = text.indexOf("mavenCentral");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.artifacts.dsl.RepositoryHandler.mavenCentral");
		/* @formatter:on*/

	}

	@Test
	public void buildfile__with_repositories_in_subprojects__when_cursor_is_at_first_mavenCentral_with_parameters() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile(
				"test6-repositories-block-in-root-with-mavencentral-with-parameters.gradle");
		int offset = text.indexOf("mavenCentral");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.artifacts.dsl.RepositoryHandler.mavenCentral","java.util.Map");
		/* @formatter:on*/

	}

	@Test
	public void buildfile__with_repositories_in_subprojects__when_cursor_is_at_second_mavenCentral_with_parameters() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile(
				"test6-repositories-block-in-root-with-mavencentral-with-parameters.gradle");
		int offset = text.lastIndexOf("mavenCentral");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).isForMethod("org.gradle.api.artifacts.dsl.RepositoryHandler.mavenCentral","java.util.Map");
		/* @formatter:on*/

	}

	@Test
	public void buildfile_10_with_dependencies_in_root__when_cursor_is_at_jar_offset__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test-10-jar-task-configuration-in-root.gradle");
		int offset = text.indexOf("jar");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset + 2);

		/* test */
		assertThat(hoverData).isForExtension("jar", "org.gradle.api.tasks.bundling.Jar");

	}

	@Test
	public void buildfile_10_with_dependencies_in_root__when_cursor_is_at_manifest_offset__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test-10-jar-task-configuration-in-root.gradle");
		int offset = text.indexOf("manifest");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset + 2);

		/* test */
		assertThat(hoverData).isForElementType("org.gradle.api.java.archives.Manifest");

	}

	@Test
	public void buildfile_10_with_dependencies_in_root__when_cursor_is_at_eachFile_offset__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test-10-jar-task-configuration-in-root.gradle");
		int offset = text.indexOf("eachFile");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset + 2);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).
			isForMethod("org.gradle.api.tasks.AbstractCopyTask.eachFile", "groovy.lang.Closure").
			isForElementType("org.gradle.api.file.FileCopyDetails");
		/* @formatter:on*/
	}

	@Test
	public void buildfile_11_with_dependencies_in_root__when_cursor_is_at_eachFile_offset__has_correct_hoverdata() {
		/* prepare */
		String text = loadTextFromIntegrationTestFile("test-11-war-task-configuration-in-root.gradle");
		int offset = text.indexOf("eachFile");

		/* execute */
		HoverData hoverData = calculateHoverData(text, offset + 2);

		/* test */
		/* @formatter:off*/
		assertThat(hoverData).
			isForMethod("org.gradle.api.tasks.AbstractCopyTask.eachFile", "groovy.lang.Closure").
			isForElementType("org.gradle.api.file.FileCopyDetails");
		/* @formatter:on*/
	}

	/* short cut method for calculation */
	private HoverData calculateHoverData(String text, int offset) {
		HoverSupport hoverSupport = components.getHoverSupport();
		RelevantCodeCutter relevantCodeCutter = components.getRelevantCodeCutter();
		Model buildModel = components.buildModel(text);
		GradleLanguageElementEstimater estimator = components.getEstimator();
		return hoverSupport.caclulateHoverData(text, offset, relevantCodeCutter, buildModel,
				GradleFileType.GRADLE_BUILD_SCRIPT, estimator);
	}

	private String loadTextFromIntegrationTestFile(String testFileName) {
		String text = components.loadTestFile("integration/" + testFileName);
		assertNotNull("testcase corrupt", text);
		return text;
	}
}
