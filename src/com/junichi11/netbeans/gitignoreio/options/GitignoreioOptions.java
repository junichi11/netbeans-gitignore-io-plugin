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
package com.junichi11.netbeans.gitignoreio.options;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author junichi11
 */
public final class GitignoreioOptions {

    private static final GitignoreioOptions INSTANCE = new GitignoreioOptions();
    private static final String DEFAULT_GITIGNORES = "default.gitignores"; // NOI18N

    private GitignoreioOptions() {
    }

    public static GitignoreioOptions getInstance() {
        return INSTANCE;
    }

    public String getDefaultGitignores() {
        return getPreferences().get(DEFAULT_GITIGNORES, ""); // NOI18N
    }

    public void setDefaultGitignores(String gitignores) {
        getPreferences().put(DEFAULT_GITIGNORES, gitignores);
    }

    private Preferences getPreferences() {
        return NbPreferences.forModule(GitignoreioOptions.class);
    }
}
