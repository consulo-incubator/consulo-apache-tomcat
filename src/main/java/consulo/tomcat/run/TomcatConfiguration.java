/*
 * Copyright 2013-2017 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.tomcat.run;

import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import consulo.bundle.SdkUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.packaging.artifacts.ArtifactPointerUtil;
import consulo.util.pointers.NamedPointer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author VISTALL
 * @since 04.11.13.
 */
public class TomcatConfiguration extends LocatableConfigurationBase implements ModuleRunConfiguration
{
	public static final int DEFAULT_JPDA_ADDRESS = 8000;
	public int JPDA_ADDRESS = DEFAULT_JPDA_ADDRESS;
	private NamedPointer<Sdk> mySdkPointer;
	private List<TomcatArtifactDeployItem> myDeploymentItems = new ArrayList<TomcatArtifactDeployItem>();

	public TomcatConfiguration(Project project, ConfigurationFactory factory, String name)
	{
		super(project, factory, name);
	}

	@NotNull
	@Override
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
	{
		SettingsEditorGroup<TomcatConfiguration> c = new SettingsEditorGroup<TomcatConfiguration>();
		c.addEditor("General", new TomcatGeneralSettingsEditor(getProject()));
		c.addEditor("Deployment", new TomcatDeploymentSettingsEditor(getProject()));
		c.addEditor("Log", new LogConfigurationPanel<TomcatConfiguration>());
		return c;
	}

	@Nullable
	@Override
	public LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile predefinedLogFile)
	{
		if(predefinedLogFile.equals(TomcatConfigurationType.TOMCAT_LOCALHOST_LOG))
		{
			Calendar calendar = Calendar.getInstance();

			StringBuilder builder = new StringBuilder();
			builder.append(ContainerPathManager.get().getSystemPath());
			builder.append("/apache-tomcat/");
			builder.append(getName());
			builder.append("_");
			builder.append(getProject().getName());
			builder.append("/logs/localhost.");
			builder.append(calendar.get(Calendar.YEAR));
			builder.append("-");
			builder.append(calendar.get(Calendar.MONTH) + 1);
			builder.append("-");
			builder.append(calendar.get(Calendar.DAY_OF_MONTH));
			builder.append(".log");

			FileUtil.createIfNotExists(new File(builder.toString()));
			return new LogFileOptions("localhost.log", builder.toString(), true, true, false);
		}
		return super.getOptionsForPredefinedLogFile(predefinedLogFile);
	}

	@Nullable
	@Override
	public RunProfileState getState(@NotNull Executor executor, @NotNull final ExecutionEnvironment executionEnvironment) throws ExecutionException
	{
		return new TomcatRunState(executionEnvironment);
	}

	@Nullable
	public Sdk getSdk()
	{
		return mySdkPointer == null ? null : mySdkPointer.get();
	}

	public String getSdkName()
	{
		return mySdkPointer == null ? null : mySdkPointer.getName();
	}

	public void setSdkName(String sdkName)
	{
		mySdkPointer = SdkUtil.createPointer(sdkName);
	}

	@Override
	public void readExternal(Element element) throws InvalidDataException
	{
		super.readExternal(element);

		String sdkName = element.getAttributeValue("sdk-name");
		if(sdkName != null)
		{
			mySdkPointer = SdkUtil.createPointer(sdkName);
		}

		for(Element artifactElement : element.getChildren("artifact"))
		{
			String name = artifactElement.getAttributeValue("name");
			String path = artifactElement.getAttributeValue("path");
			if(name != null)
			{
				myDeploymentItems.add(new TomcatArtifactDeployItem(ArtifactPointerUtil.getPointerManager(getProject()).create(name), StringUtil.notNullize(path)));
			}
		}
	}

	@Override
	public void writeExternal(Element element) throws WriteExternalException
	{
		super.writeExternal(element);

		if(mySdkPointer != null)
		{
			element.setAttribute("sdk-name", mySdkPointer.getName());
		}

		for(TomcatArtifactDeployItem artifact : myDeploymentItems)
		{
			Element artifactElement = new Element("artifact");
			artifactElement.setAttribute("name", artifact.getArtifactPointer().getName());
			artifactElement.setAttribute("path", artifact.getPath());
			element.addContent(artifactElement);
		}
	}

	public List<TomcatArtifactDeployItem> getDeploymentItems()
	{
		return myDeploymentItems;
	}

	@NotNull
	@Override
	public Module[] getModules()
	{
		return ModuleManager.getInstance(getProject()).getModules();
	}
}
