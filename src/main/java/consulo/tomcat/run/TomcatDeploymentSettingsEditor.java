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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.impl.ui.ChooseArtifactsDialog;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import consulo.javaee.artifact.ExplodedWarArtifactType;
import consulo.packaging.artifacts.ArtifactPointerUtil;

/**
 * @author VISTALL
 * @since 04.11.13.
 */
public class TomcatDeploymentSettingsEditor extends SettingsEditor<TomcatConfiguration>
{
	private JPanel myRoot;

	private JPanel myDeploymentPane;
	private Project myProject;

	private List<TomcatArtifactDeployItem> myItems;
	private ListTableModel<TomcatArtifactDeployItem> myModel;

	public TomcatDeploymentSettingsEditor(Project project)
	{
		myProject = project;
	}

	@Override
	protected void resetEditorFrom(TomcatConfiguration tomcatConfiguration)
	{
		myItems.clear();
		myItems.addAll(tomcatConfiguration.getDeploymentItems());
		myModel.fireTableDataChanged();
	}

	@Override
	protected void applyEditorTo(TomcatConfiguration tomcatConfiguration) throws ConfigurationException
	{
		List<TomcatArtifactDeployItem> deploymentItems = tomcatConfiguration.getDeploymentItems();
		deploymentItems.clear();
		deploymentItems.addAll(myItems);
	}

	@NotNull
	@Override
	protected JComponent createEditor()
	{
		return myRoot;
	}

	private void createUIComponents()
	{
		ProjectSdksModel model = new ProjectSdksModel();
		if(!model.isInitialized())
		{
			model.reset(myProject);
		}
		myItems = new ArrayList<TomcatArtifactDeployItem>();
		myModel = new ListTableModel<TomcatArtifactDeployItem>(new ColumnInfo[]{
				new ColumnInfo<TomcatArtifactDeployItem, TomcatArtifactDeployItem>("Artifact")
				{
					@Nullable
					@Override
					public TableCellRenderer getRenderer(TomcatArtifactDeployItem tomcatArtifactDeployItem)
					{
						return new ColoredTableCellRenderer()
						{
							@Override
							protected void customizeCellRenderer(JTable jTable, Object o, boolean b, boolean b2, int i, int i2)
							{
								TomcatArtifactDeployItem artifactDeployItem = (TomcatArtifactDeployItem) o;
								Artifact artifact = artifactDeployItem.getArtifactPointer().get();
								if(artifact != null)
								{
									append(artifact.getName());
									setIcon(artifact.getArtifactType().getIcon());
								}
								else
								{
									append(artifactDeployItem.getArtifactPointer().getName(), SimpleTextAttributes.ERROR_ATTRIBUTES);
									setIcon(AllIcons.Toolbar.Unknown);
								}
							}
						};
					}

					@Nullable
					@Override
					public TomcatArtifactDeployItem valueOf(TomcatArtifactDeployItem o)
					{
						return o;
					}
				},
				new ColumnInfo<TomcatArtifactDeployItem, String>("Path")
				{
					@Nullable
					@Override
					public TableCellRenderer getRenderer(TomcatArtifactDeployItem tomcatArtifactDeployItem)
					{
						return new ColoredTableCellRenderer()
						{
							@Override
							protected void customizeCellRenderer(JTable jTable, Object o, boolean b, boolean b2, int i, int i2)
							{
								append((String) o);
							}
						};
					}

					@Override
					public boolean isCellEditable(TomcatArtifactDeployItem tomcatArtifactDeployItem)
					{
						return true;
					}

					@Override
					public void setValue(TomcatArtifactDeployItem tomcatArtifactDeployItem, String value)
					{
						tomcatArtifactDeployItem.setPath(value);
					}

					@Nullable
					@Override
					public String valueOf(TomcatArtifactDeployItem o)
					{
						return o.getPath();
					}
				}
		}, myItems, 0);

		JBTable table = new JBTable(myModel)
		{
			@Override
			public TableCellRenderer getCellRenderer(final int row, final int column)
			{
				final ColumnInfo columnInfo = ((ListTableModel) getModel()).getColumnInfos()[column];
				assert columnInfo != null;
				//noinspection unchecked
				return columnInfo.getRenderer(((ListTableModel) getModel()).getItem(row));
			}
		};

		ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(table);
		toolbarDecorator.setAddAction(new AnActionButtonRunnable()
		{
			@Override
			public void run(AnActionButton anActionButton)
			{
				Artifact[] artifacts = ArtifactManager.getInstance(myProject).getArtifacts();

				List<Artifact> listArtifacts = new ArrayList<Artifact>(artifacts.length);
				loop: for(Artifact artifact : artifacts)
				{
					if(artifact.getArtifactType() != ExplodedWarArtifactType.getInstance())
					{
						continue;
					}

					for(TomcatArtifactDeployItem item : myItems)
					{
						Artifact tempArtifact = item.getArtifactPointer().get();
						if(tempArtifact.equals(artifact))
						{
							continue loop;
						}
					}

					listArtifacts.add(artifact);
				}
				ChooseArtifactsDialog dialog = new ChooseArtifactsDialog(myProject, listArtifacts, "Choose Artifact", null);
				dialog.show();

				if(dialog.isOK())
				{
					for(Artifact artifact : dialog.getChosenElements())
					{
						myModel.addRow(new TomcatArtifactDeployItem(ArtifactPointerUtil.getPointerManager(myProject).create(artifact), artifact.getName() + "/"));
					}
				}
			}
		});

		toolbarDecorator.disableUpDownActions();

		myDeploymentPane = toolbarDecorator.createPanel();
	}
}
