/*
 * Copyright 2019 junichi11.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.junichi11.netbeans.gitignoreio.ui.actions;

import com.junichi11.netbeans.gitignoreio.ui.GitignoreListPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.modules.csl.api.UiUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Versioning",
        id = "com.junichi11.netbeans.gitignoreio.ui.actions.GenerateGitIgnoreAction")
@ActionRegistration(
        displayName = "#CTL_GenerateGitIgnoreAction")
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 2550),
    @ActionReference(path = "Projects/Actions", position = 2550)
})
@Messages("CTL_GenerateGitIgnoreAction=Generate gitignore file")
public final class GenerateGitIgnoreAction implements ActionListener {

    private static final String GITIGNORE_NAME = ".gitignore"; // NOI18N
    private static final String UTF8 = "UTF-8"; // NOI18N
    private final Project context;
    private File gitignoreFile;

    public GenerateGitIgnoreAction(Project context) {
        this.context = context;
    }

    @NbBundle.Messages({
        "GenerateGitignoreAction.new.file.error.message=File already exists.",
        "GenerateGitignoreAction.select.gitignore.list.message=Please select item from gitignore list."
    })
    @Override
    public void actionPerformed(ActionEvent ev) {

        FileObject projectDirectory = context.getProjectDirectory();
        gitignoreFile = new File(FileUtil.toFile(projectDirectory), GITIGNORE_NAME);
        final boolean isEnabled = gitignoreFile.exists();
        final String projectDirectoryPath = FileUtil.toFile(projectDirectory).getAbsolutePath();
        SwingUtilities.invokeLater(() -> {
            GitignoreListPanel panel = GitignoreListPanel.getDefault();
            panel.setEnabledOverwrite(isEnabled);
            panel.setEnabledPostscript(isEnabled);
            panel.setFilePath(projectDirectoryPath);

            DialogDescriptor descriptor;
            try {
                descriptor = panel.showDialog();
                if (descriptor.getValue() == DialogDescriptor.OK_OPTION) {
                    // get content
                    String gitignoreContent = panel.getGitignoreContent();
                    if (gitignoreContent == null) {
                        String warning = Bundle.GenerateGitignoreAction_select_gitignore_list_message();
                        showDialog(warning);
                        return;
                    }
                    if (!panel.getFilePath().equals(projectDirectoryPath)) {
                        gitignoreFile = new File(new File(panel.getFilePath()), GITIGNORE_NAME);
                    }

                    // create file
                    if (!createFile() && panel.isNormal()) {
                        // show dialog
                        String error = Bundle.GenerateGitignoreAction_new_file_error_message();
                        showDialog(error);
                    } else {
                        // write
                        writeFile(panel, gitignoreContent);
                    }

                    // open file
                    FileObject gitignore = FileUtil.toFileObject(gitignoreFile);
                    if (gitignore != null) {
                        UiUtils.open(gitignore, 0);
                    }
                }
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                showDialog(ex.getMessage());
            }
        });
    }

    private boolean createFile() throws IOException {
        return gitignoreFile.createNewFile();
    }

    private void writeFile(GitignoreListPanel panel, String gitignoreContent) {
        if (!gitignoreFile.exists()) {
            return;
        }

        PrintWriter pw;
        try {
            if (panel.isNormal() || panel.isOverwrite()) {
                pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(gitignoreFile), UTF8), true); // NOI18N
            } else if (panel.isPostscript()) {
                pw = new PrintWriter(new FileWriter(gitignoreFile, true));
            } else {
                return;
            }
            try {
                if (panel.isNormal() || panel.isOverwrite()) {
                    pw.print(gitignoreContent);
                } else if (panel.isPostscript()) {
                    // # Created by https://www.gitignore.io
                    gitignoreContent = gitignoreContent.replace("# Created by https://www.gitignore.io\n", ""); // NOI18N
                    pw.write(gitignoreContent);
                }
            } finally {
                pw.close();
            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (UnsupportedEncodingException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void showDialog(final String error) {
        SwingUtilities.invokeLater(() -> {
            NotifyDescriptor.Message message = new NotifyDescriptor.Message(error, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(message);
        });
    }
}
