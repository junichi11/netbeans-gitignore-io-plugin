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
package com.junichi11.netbeans.gitignoreio.ui;

import com.junichi11.netbeans.gitignoreio.options.GitignoreioOptions;
import java.awt.Dialog;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author junichi11
 */
public class GitignoreListPanel extends JPanel {

    private volatile boolean isConnectedNetwork = true;
    private volatile boolean initilized = false;

    private static final String GITIGNORE_API = "https://www.gitignore.io/api/"; // NOI18N
    private static final String GITIGNORE_API_LIST = GITIGNORE_API + "list"; // NOI18N
    private static final String FORMAT_LINES = "lines"; // NOI18N
    private static final String FORMAT_JSON = "json"; // NOI18N
    private static final String UTF8 = "UTF-8"; // NOI18N
    private static final String GITIGNORE_LAST_FOLDER_SUFFIX = ".gitignore"; // NOI18N
    private static List<String> GITIGNORES;
    private static final GitignoreListPanel INSTANCE = new GitignoreListPanel();
    private static final long serialVersionUID = -5226048221599145625L;
    private static final Logger LOGGER = Logger.getLogger(GitignoreListPanel.class.getName());
    private static final RequestProcessor RP = new RequestProcessor(GitignoreListPanel.class);

    /**
     * Creates new form GitignoreListPanel
     */
    public GitignoreListPanel() {
        initComponents();
    }

    public static GitignoreListPanel getDefault() {
        if (!INSTANCE.initilized) {
            INSTANCE.init();
            INSTANCE.initilized = true;
        }

        // retry
        if (!INSTANCE.isConnectedNetwork) {
            INSTANCE.init();
        }
        return INSTANCE;
    }

    private void init() {
        setMessage(""); // NOI18N
        DefaultListModel<String> model = new DefaultListModel<>();
        availableList.setModel(model);
        addGitignores(""); // NOI18N
        normalRadioButton.setSelected(true);

        // add listener
        filterTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                processUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                processUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                processUpdate();
            }

            private void processUpdate() {
                String text = filterTextField.getText();
                addGitignores(text);
            }
        });
    }

    private void addGitignores(String filter) {
        if (filter == null) {
            return;
        }
        DefaultListModel<String> model = (DefaultListModel<String>) availableList.getModel();
        model.clear();

        filter = filter.replaceAll("\\s+", " "); // NOI18N
        String[] filters = filter.split(" "); // NOI18N
        List<String> availableGitignores = getAvailableGitignores();
        if (availableGitignores != null) {
            availableGitignores.forEach((gitignore) -> {
                for (String f : filters) {
                    if (gitignore.contains(f)) {
                        model.addElement(gitignore);
                    }
                }
            });
            availableList.setModel(model);
        }
    }

    public String getGitignoreContent() throws MalformedURLException, IOException {
        String gitignores = getGitignores();
        String url = GITIGNORE_API + gitignores;
        HttpsURLConnection connection = openUrlConnection(url);
        return getContent(connection, UTF8);
    }

    private HttpsURLConnection openUrlConnection(String url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET"); // NOI18N
        connection.setRequestProperty("User-Agent", "NetBeans Plugin");
        connection.setReadTimeout(2000);
        connection.setConnectTimeout(2000);
        return connection;
    }

    public String getGitignores() {
        return gitignoresTextField.getText().trim();
    }

    public void setGitignores(String ignores) {
        gitignoresTextField.setText(ignores);
    }

    public boolean isNormal() {
        return normalRadioButton.isSelected();
    }

    public boolean isOverwrite() {
        return overwriteRadioButton.isSelected();
    }

    public boolean isPostscript() {
        return postscriptRadioButton.isSelected();
    }

    public void setEnabledOverwrite(boolean isEnabled) {
        overwriteRadioButton.setEnabled(isEnabled);
        if (!isEnabled && isOverwrite()) {
            normalRadioButton.setSelected(true);
        }
    }

    public void setEnabledPostscript(boolean isEnabled) {
        postscriptRadioButton.setEnabled(isEnabled);
        if (!isEnabled && isPostscript()) {
            normalRadioButton.setSelected(true);
        }
    }

    public String getFilePath() {
        return filePathTextField.getText();
    }

    public void setFilePath(String filePath) {
        filePathTextField.setText(filePath);
    }

    private void setMessage(String message) {
        messageLabel.setText(message);
    }

    @NbBundle.Messages({
        "GitignoreListPanel.dialog.title=gitignore.io available list",
        "GitignoreListPanel.network.error=You have to connect to the Internet."
    })
    public DialogDescriptor showDialog() throws IOException {
        if (!isConnectedNetwork) {
            throw new IOException(Bundle.GitignoreListPanel_network_error());
        }
        DialogDescriptor descriptor = new DialogDescriptor(this, Bundle.GitignoreListPanel_dialog_title());
        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.pack();
        dialog.setVisible(true);
        return descriptor;
    }

    @NbBundle.Messages({
        "GitignoreListPanel.message.getting=Getting the available list...",
    })
    private synchronized List<String> getAvailableGitignores() {
        if (GITIGNORES == null) {
            setMessage(Bundle.GitignoreListPanel_message_getting());
            RP.post(() -> {
                GITIGNORES = new ArrayList<>();
                String gitignoreList = getAvailableGitignoresText();
                if (gitignoreList != null) {
                    String[] gitignores = splitGitignores(gitignoreList);
                    Arrays.sort(gitignores);
                    GITIGNORES.addAll(Arrays.asList(gitignores));
                    SwingUtilities.invokeLater(() -> {
                        addGitignores(""); // NOI18N
                        setMessage(""); // NOI18N
                    });
                }
            });
        }
        return GITIGNORES;
    }

    @NbBundle.Messages({
        "GitignoreListPanel.message.connection.error=Connection error",
    })
    private String getAvailableGitignoresText() {
        String list = null;
        try {
            // #5 now use the "lines" format option
            HttpsURLConnection openConnection = openUrlConnection(getApiListURL(FORMAT_LINES));
            list = getContent(openConnection, UTF8);
            isConnectedNetwork = true;
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
            isConnectedNetwork = false;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
            setMessage(Bundle.GitignoreListPanel_message_connection_error());
            isConnectedNetwork = false;
        }
        return list;
    }

    private String getApiListURL(String format) {
        String formatParam = String.format("format=%s", format); // NOI18N
        StringBuilder sb = new StringBuilder();
        sb.append(GITIGNORE_API_LIST);
        sb.append("?"); // NOI18N
        sb.append(formatParam);
        return sb.toString();
    }

    private String getContent(URLConnection connection, String charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedInputStream inuptStream = new BufferedInputStream(connection.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(inuptStream, charset)); // NOI18N
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n"); // NOI18N
        }

        return sb.toString();
    }

    private String[] splitGitignores(String gitignores) {
        // #5 new use the "lines" format option
        return gitignores.split("\n"); // NOI18N
    }

    private GitignoreioOptions getOptions() {
        return GitignoreioOptions.getInstance();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        writeOptionButtonGroup = new javax.swing.ButtonGroup();
        availableListLabel = new javax.swing.JLabel();
        availableListScrollPane = new javax.swing.JScrollPane();
        availableList = new javax.swing.JList<>();
        gitignoresTextField = new javax.swing.JTextField();
        saveAsDefaultButton = new javax.swing.JButton();
        loadDefaultButton = new javax.swing.JButton();
        resetButton = new javax.swing.JButton();
        normalRadioButton = new javax.swing.JRadioButton();
        overwriteRadioButton = new javax.swing.JRadioButton();
        postscriptRadioButton = new javax.swing.JRadioButton();
        filterTextField = new javax.swing.JTextField();
        filterLabel = new javax.swing.JLabel();
        browseButton = new javax.swing.JButton();
        filePathTextField = new javax.swing.JTextField();
        messageLabel = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(availableListLabel, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.availableListLabel.text")); // NOI18N

        availableList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        availableList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                availableListMouseReleased(evt);
            }
        });
        availableList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                availableListValueChanged(evt);
            }
        });
        availableListScrollPane.setViewportView(availableList);

        gitignoresTextField.setText(org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.gitignoresTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(saveAsDefaultButton, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.saveAsDefaultButton.text")); // NOI18N
        saveAsDefaultButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsDefaultButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(loadDefaultButton, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.loadDefaultButton.text")); // NOI18N
        loadDefaultButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadDefaultButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(resetButton, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.resetButton.text")); // NOI18N
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        writeOptionButtonGroup.add(normalRadioButton);
        org.openide.awt.Mnemonics.setLocalizedText(normalRadioButton, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.normalRadioButton.text")); // NOI18N

        writeOptionButtonGroup.add(overwriteRadioButton);
        org.openide.awt.Mnemonics.setLocalizedText(overwriteRadioButton, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.overwriteRadioButton.text")); // NOI18N

        writeOptionButtonGroup.add(postscriptRadioButton);
        org.openide.awt.Mnemonics.setLocalizedText(postscriptRadioButton, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.postscriptRadioButton.text")); // NOI18N

        filterTextField.setText(org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.filterTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(filterLabel, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.filterLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        filePathTextField.setEditable(false);
        filePathTextField.setText(org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.filePathTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(messageLabel, org.openide.util.NbBundle.getMessage(GitignoreListPanel.class, "GitignoreListPanel.messageLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(messageLabel)
                    .addComponent(availableListLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(resetButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loadDefaultButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveAsDefaultButton)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(normalRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(overwriteRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(postscriptRadioButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filePathTextField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseButton)
                .addGap(12, 12, 12))
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(filterLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterTextField)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(gitignoresTextField)
                .addGap(12, 12, 12))
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(availableListScrollPane)
                .addGap(12, 12, 12))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(availableListLabel)
                    .addComponent(saveAsDefaultButton)
                    .addComponent(loadDefaultButton)
                    .addComponent(resetButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(messageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(availableListScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(filterLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gitignoresTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(normalRadioButton)
                    .addComponent(overwriteRadioButton)
                    .addComponent(postscriptRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(browseButton)
                    .addComponent(filePathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void availableListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_availableListValueChanged
//        Object[] gitignores = availableList.getSelectedValues();
//        StringBuilder sb = new StringBuilder();
//        for (Object gitignore : gitignores) {
//            sb.append((String) gitignore).append(","); // NOI18N
//        }
//        int length = sb.length();
//        if (length > 0) {
//            sb.deleteCharAt(length - 1);
//        }
//        gitignoresTextField.setText(sb.toString());
    }//GEN-LAST:event_availableListValueChanged

    private void loadDefaultButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadDefaultButtonActionPerformed
        gitignoresTextField.setText(getOptions().getDefaultGitignores());
    }//GEN-LAST:event_loadDefaultButtonActionPerformed

    private void saveAsDefaultButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsDefaultButtonActionPerformed
        getOptions().setDefaultGitignores(getGitignores());
    }//GEN-LAST:event_saveAsDefaultButtonActionPerformed

    private void availableListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_availableListMouseReleased
        Object source = evt.getSource();
        JList<?> list;
        if (source instanceof JList) {
            list = (JList<?>) source;
        } else {
            return;
        }
        String selectedValue = (String) list.getSelectedValue();
        String gitignores = getGitignores();
        String[] items = gitignores.split(","); // NOI18N
        if (Arrays.asList(items).contains(selectedValue)) {
            return;
        }

        // add value
        if (gitignores.isEmpty()) {
            setGitignores(selectedValue);
        } else {
            setGitignores(gitignores + "," + selectedValue); // NOI18N
        }
    }//GEN-LAST:event_availableListMouseReleased

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        setGitignores(""); // NOI18N
    }//GEN-LAST:event_resetButtonActionPerformed

    @NbBundle.Messages({
        "GitignoreListPanel.browseButton.title=Select a directory"
    })
    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        File file = new FileChooserBuilder(GitignoreListPanel.class.getName() + GITIGNORE_LAST_FOLDER_SUFFIX)
                .setDirectoriesOnly(true)
                .setTitle(Bundle.GitignoreListPanel_browseButton_title())
                .showOpenDialog();
        if (file != null && file.isDirectory()) {
            filePathTextField.setText(file.getAbsolutePath());
            FileObject targetDirectory = FileUtil.toFileObject(file);
            FileObject gitignoreFile = targetDirectory.getFileObject(".gitignore"); // NOI18N
            boolean exists = gitignoreFile != null;
            setEnabledOverwrite(exists);
            setEnabledPostscript(exists);
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<String> availableList;
    private javax.swing.JLabel availableListLabel;
    private javax.swing.JScrollPane availableListScrollPane;
    private javax.swing.JButton browseButton;
    private javax.swing.JTextField filePathTextField;
    private javax.swing.JLabel filterLabel;
    private javax.swing.JTextField filterTextField;
    private javax.swing.JTextField gitignoresTextField;
    private javax.swing.JButton loadDefaultButton;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JRadioButton normalRadioButton;
    private javax.swing.JRadioButton overwriteRadioButton;
    private javax.swing.JRadioButton postscriptRadioButton;
    private javax.swing.JButton resetButton;
    private javax.swing.JButton saveAsDefaultButton;
    private javax.swing.ButtonGroup writeOptionButtonGroup;
    // End of variables declaration//GEN-END:variables
}
