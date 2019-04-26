/*
 * Copyright 2019 Red Hat, Inc, and individual contributors.
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
package org.xstefank.check;

import javax.json.JsonArray;
import javax.json.JsonObject;

import io.xstefank.Check;
import org.xstefank.api.GitHubAPI;
import org.xstefank.model.Utils;
import org.xstefank.model.yaml.RegexDefinition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LatestCommitCheck implements Check {

    static final String DEFAULT_MESSAGE = "Invalid commit message content";

    private Pattern pattern;
    private String message;

    public LatestCommitCheck(RegexDefinition commit) {
        if (commit == null || commit.getPattern() == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        this.pattern = commit.getPattern();
        this.message = (commit.getMessage() != null) ? commit.getMessage() : DEFAULT_MESSAGE;
    }

    @Override
    public String check(JsonObject payload) {
        JsonArray commitsJson = GitHubAPI.getCommitsJSON(payload);
        String commitMessage = commitsJson.getJsonObject(commitsJson.size() - 1)
                .getJsonObject(Utils.COMMIT)
                .getString(Utils.MESSAGE);

        Matcher matcher = pattern.matcher(commitMessage.split(Utils.GITHUB_LINE_SEPARATOR, 2)[0]);

        if (!matcher.matches()) {
            return message;
        }

        return null;
    }
}
