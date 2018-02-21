package de.jcup.egradle.core.domain;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import de.jcup.egradle.core.TestUtil;
import de.jcup.egradle.core.util.FileSupport;

public class GradleRootProjectTest {
	@Test
	public void rootproject1_has_no_gradle_settings_and_so_identified_as_single_project__as_fallback() throws Exception {
		GradleRootProject root1 = new GradleRootProject(TestUtil.ROOTFOLDER_1);
		assertFalse(root1.isMultiProject());
	}
	@Test
	public void rootproject2_is_identified_as_multiproject() throws Exception {
		GradleRootProject root2 = new GradleRootProject(TestUtil.ROOTFOLDER_2);
		assertTrue(root2.isMultiProject());
	}
	
	@Test
	public void rootproject3_is_identified_as_singleproject()throws Exception {
		GradleRootProject root3 = new GradleRootProject(TestUtil.ROOTFOLDER_3);
		assertFalse(root3.isMultiProject());
	}
	
	@Test
	public void rootproject4_is_identified_as_singleproject_even_when_having_an_include_comment_inside ()throws Exception {
		GradleRootProject root4 = new GradleRootProject(TestUtil.ROOTFOLDER_4);
		assertFalse(root4.isMultiProject());
	}
	
	@Test
	public void add_subproject_to_gradle_multiproject_creates_subfolder() throws Exception{
		/* prepare */
		File rootFolder = TestUtil.createTempTestFolder();
		
		FileSupport fileSupport = FileSupport.DEFAULT;
		fileSupport.createTextFile(rootFolder, "settings.gradle","include 'core'");
		GradleRootProject rootProject = new GradleRootProject(rootFolder);

		/* check precondition fulfilled*/
		assertTrue("precondition failed: not a multiproject?!", rootProject.isMultiProject());
		
		/* execute */
		rootProject.createNewSubProject("mySubProject");
	
		/* test */
		assertTrue(new File(rootFolder,"mySubProject").exists());
		
	}

	@Test
	public void add_subproject_to_gradle_multiproject_creates_include_part() throws Exception{
		/* prepare */
		File rootFolder = TestUtil.createTempTestFolder();
		
		FileSupport fileSupport = FileSupport.DEFAULT;
		fileSupport.createTextFile(rootFolder, "settings.gradle","include 'core'");
		GradleRootProject rootProject = new GradleRootProject(rootFolder);
		
		/* check precondition fulfilled*/
		assertTrue("precondition failed: not a multiproject?!", rootProject.isMultiProject());
		
		/* execute */
		rootProject.createNewSubProject("mySubProject");
		
		/* test */
		String content = fileSupport.readTextFile(new File(rootFolder,"settings.gradle"));
		assertEquals("include 'core', 'mySubProject'", content);
		
	}
	
	@Test
	public void add_subproject_to_gradle_multiproject_creates_include_part_and_parts_around() throws Exception{
		/* prepare */
		File rootFolder = TestUtil.createTempTestFolder();
		
		FileSupport fileSupport = FileSupport.DEFAULT;
		fileSupport.createTextFile(rootFolder, "settings.gradle","/* a comment before */\ninclude 'core'\n/* a comment after */\n");
		GradleRootProject rootProject = new GradleRootProject(rootFolder);
		
		/* check precondition fulfilled*/
		assertTrue("precondition failed: not a multiproject?!", rootProject.isMultiProject());
		
		/* execute */
		rootProject.createNewSubProject("mySubProject");
		
		/* test */
		String content = fileSupport.readTextFile(new File(rootFolder,"settings.gradle"));
		assertEquals("/* a comment before */\ninclude 'core', 'mySubProject'\n/* a comment after */", content);
		
	}
}
