/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
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
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                GitignoreListPanel panel = GitignoreListPanel.getDefault();
                panel.setEnabledOverwrite(isEnabled);
                panel.setEnabledPostscript(isEnabled);

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
                    // # Generated by http://gitignore.io
                    gitignoreContent = gitignoreContent.replace("# Generated by http://gitignore.io\n", ""); // NOI18N
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
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                NotifyDescriptor.Message message = new NotifyDescriptor.Message(error, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(message);
            }
        });
    }
}
