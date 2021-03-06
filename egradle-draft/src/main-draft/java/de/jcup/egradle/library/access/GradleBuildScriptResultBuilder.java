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
 package de.jcup.egradle.library.access;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;

public class GradleBuildScriptResultBuilder {


	public Future<EGradleBuildscriptResult> build(String buildScript, String pathToGradle) {
		CompletableFuture<EGradleBuildscriptResult> future = new CompletableFuture<>();

		new Thread() {
			@Override
			public void run() {
				try {
					GradleBuildScriptResultBuilder.this.start(future, buildScript, pathToGradle);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		return future;
	}

	protected void start(CompletableFuture<EGradleBuildscriptResult> future, String buildScript, String pathToGradle)
			throws EGradleBuildScriptException {
		Exception error = null;
		File dir = new File(pathToGradle, "lib");
		List<URL> list = new ArrayList<>();
		File[] jarFiles = dir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File file) {
				return file != null && file.isFile() && file.getName().endsWith(".jar");
			}
		});
		for (File file : jarFiles) {
			try {
				addFileToList(list, file);
			} catch (MalformedURLException e) {
				throw new EGradleBuildScriptException(
						"Was not able to parse buildscript because of missing library: " + file, e);
			}
		}
		URL[] urls = (URL[]) list.toArray(new URL[list.size()]);
		URLClassLoader child = new URLClassLoader(urls);
		Thread.currentThread().setContextClassLoader(child);

		StringBuilder buildscriptWrapper = createBuildscriptWrapper(buildScript);

		try {
			Class<?> classToLoad;
			classToLoad = Class.forName("de.jcup.egradle.library.access.ASTBuilderCaller", true, child);
			Method method = classToLoad.getDeclaredMethod("build", String.class);
			Object instance = classToLoad.newInstance();

			@SuppressWarnings("unchecked")
			List<ASTNode> result = (List<ASTNode>) method.invoke(instance, buildscriptWrapper.toString());

			completeFutureByResults(future, result);
			if (future.isDone()){
				return;
			}
			error = new EGradleBuildScriptException("Did not found expected buildscript closure!");
		} catch (InvocationTargetException | ClassNotFoundException | NoSuchMethodException | InstantiationException
				| IllegalAccessException e) {
			error=new EGradleBuildScriptException("Reflection problems", e);
		} catch (SecurityException e) {
			error= new EGradleBuildScriptException("Security problems", e);
		}
		if (error!=null){
			future.complete(new EGradleBuildscriptResult(error));
		}

	}

	protected StringBuilder createBuildscriptWrapper(String buildScript) {
		StringBuilder buildscriptWrapper = new StringBuilder();
		buildscriptWrapper.append("import org.gradle.api.internal.project.*\n");
//		buildscriptWrapper.append("org.gradle.api.internal.project.ProjectScript buildscript= {\n");
//		buildscriptWrapper.append("ProjectScript buildscript= {\n");
		buildscriptWrapper.append("DefaultProject p = new DefaultProject(); \n");
		buildscriptWrapper.append("p.buidscript {");
		buildscriptWrapper.append(buildScript);
		buildscriptWrapper.append("\n}");
		return buildscriptWrapper;
	}

	protected void completeFutureByResults(CompletableFuture<EGradleBuildscriptResult> future, List<ASTNode> result) {
		for (ASTNode node : result) {
			BlockStatement bs = (BlockStatement) node;
			for (org.codehaus.groovy.ast.stmt.Statement st : bs.getStatements()) {
				if (st instanceof ReturnStatement) {
					ReturnStatement rs = (ReturnStatement) st;
					Expression expression = rs.getExpression();
					if (expression instanceof DeclarationExpression) {
						DeclarationExpression de = (DeclarationExpression) expression;
						String name = de.getVariableExpression().getName();
						if (! "buildscript".equals(name)){
							continue;
						}
						Expression right = de.getRightExpression();
						if (right instanceof ClosureExpression) {
							ClosureExpression ce = (ClosureExpression) right;
							Statement buildCode = ce.getCode();
							
							future.complete(new EGradleBuildscriptResult(buildCode));
							return;
						}else{
							future.complete(new EGradleBuildscriptResult(new EGradleBuildScriptException("Did not found expected buildscript closure ,but:"+right)));
							return;
						}
					}
				}
			}
		}
	}


	private void addFileToList(List<URL> list, File file) throws MalformedURLException {
		if (!file.exists()) {
			throw new IllegalStateException("file does not exist");
		}
		URL url = file.toURI().toURL();
		System.out.println("add url to classpath:" + url);
		list.add(url);
	}
}
