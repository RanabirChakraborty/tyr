package org.xstefank.api;

import com.fasterxml.jackson.databind.JsonNode;

import org.jboss.logging.Logger;
import org.xstefank.model.CommitStatus;
import org.xstefank.model.StatusPayload;
import org.xstefank.model.TyrProperties;
import org.xstefank.model.Utils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.xstefank.model.Utils.TOKEN_PROPERTY;

public class GitHubAPI {

    private static final String oauthToken = readToken();
    private static final Logger log = Logger.getLogger(GitHubAPI.class);

    public static void updateCommitStatus(String repository, String sha, CommitStatus status,
                                          String targetUrl, String description, String context) {

        Client resteasyClient = ClientBuilder.newClient();
        URI statusUri = UriBuilder
                .fromUri(Utils.GITHUB_BASE)
                .path("/repos")
                .path("/" + repository)
                .path("/statuses")
                .path("/" + sha)
                .build();

        WebTarget target = resteasyClient.target(statusUri);

        Entity<StatusPayload> json = Entity.json(new StatusPayload(status.toString(),
                targetUrl, description, context));

        log.info(json.toString());

        Response response = target.request()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "token " + oauthToken)
                .post(json);

        log.info("Github status update: " + response.getStatus());
        log.info("Github response: " + response.readEntity(String.class));
        response.close();
    }

    public static JsonNode getJsonWithCommits(JsonNode prPayload) {
        return getJsonFromUri(getCommitsUri(prPayload));
    }

    public static JsonNode getJsonWithPullRequest(JsonNode issuePayload) {
        return getJsonFromUri(getPullRequestUri(issuePayload));
    }

    static JsonNode getJsonFromUri(URI uri) {
        Client restClient = ClientBuilder.newClient();
        WebTarget target = restClient.target(uri);

        Response response = target.request()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "token " + oauthToken)
                .get();

        if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            throw new IllegalArgumentException("Can not get json from URI. Authentication with invalid github token");
        }
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new IllegalArgumentException("Can not get json from URI. Response status " + response.getStatus());
        }

        JsonNode json = response.readEntity(JsonNode.class);
        response.close();

        return json;
    }

    private static URI getCommitsUri(JsonNode prPayload) {
        String url = prPayload.get(Utils.PULL_REQUEST).get(Utils.COMMITS_URL).asText();
        return UriBuilder.fromPath(url)
                .build();
    }

    private static URI getPullRequestUri(JsonNode issuePayload) {
        String url = issuePayload.get(Utils.ISSUE).get(Utils.PULL_REQUEST).get(Utils.URL).asText();
        return UriBuilder.fromPath(url)
                .build();
    }

    private static String readToken() {
        String token = TyrProperties.getProperty(TOKEN_PROPERTY);
        return token == null ? System.getenv(Utils.TOKEN_ENV) : token;
    }
}
