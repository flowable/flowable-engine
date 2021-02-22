/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.flowable.ui.idm.service.keycloak;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.flowable.common.engine.api.FlowableException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Filip Hrisafov
 */
public class KeycloakConfiguration implements InitializingBean {

    protected String server;
    protected String authenticationRealm = "master";
    protected String authenticationUser = "admin";
    protected String authenticationPassword;

    protected String realm;

    protected Duration clockSkew = Duration.ofSeconds(60);
    protected Clock clock = Clock.systemUTC();

    protected RestTemplate restTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(server, "server must be set");
        Assert.hasText(authenticationPassword, "authenticationPassword must be set");
        Assert.hasText(realm, "realm must be set");
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
            restTemplate.getInterceptors().add(new AuthenticationTokenInterceptor());
        }
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        Assert.notNull(clock, "authenticationServer cannot be null");
        if (server.endsWith("/")) {
            this.server = server;
        } else {
            this.server = server + "/";
        }
    }

    public String getAuthenticationRealm() {
        return authenticationRealm;
    }

    public void setAuthenticationRealm(String authenticationRealm) {
        Assert.notNull(clock, "authenticationRealm cannot be null");
        this.authenticationRealm = authenticationRealm;
    }

    public String getAuthenticationUser() {
        return authenticationUser;
    }

    public void setAuthenticationUser(String authenticationUser) {
        Assert.notNull(clock, "authenticationUser cannot be null");
        this.authenticationUser = authenticationUser;
    }

    public String getAuthenticationPassword() {
        return authenticationPassword;
    }

    public void setAuthenticationPassword(String authenticationPassword) {
        Assert.notNull(clock, "authenticationPassword cannot be null");
        this.authenticationPassword = authenticationPassword;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        Assert.notNull(clock, "realm cannot be null");
        this.realm = realm;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        Assert.notNull(clock, "restTemplate cannot be null");
        this.restTemplate = restTemplate;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        Assert.notNull(clockSkew, "clockSkew cannot be null");
        Assert.isTrue(clockSkew.getSeconds() >= 0, "clockSkew must be >= 0");
        this.clockSkew = clockSkew;
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        Assert.notNull(clock, "clock cannot be null");
        this.clock = clock;
    }

    public class AuthenticationTokenInterceptor implements ClientHttpRequestInterceptor {

        protected final RestTemplate tokenRestTemplate;

        protected AccessToken accessToken = new AccessToken(null, KeycloakConfiguration.this.clock.instant().minusSeconds(10));

        public AuthenticationTokenInterceptor() {
            this.tokenRestTemplate = new RestTemplate();
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

            HttpHeaders headers = request.getHeaders();
            if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                headers.setBearerAuth(getAccessTokenValue());
            }

            return execution.execute(request, body);
        }

        public String getAccessTokenValue() {
            String accessTokenValue;
            if (hasTokenExpired()) {
                AccessToken accessToken = fetchAccessToken();
                this.accessToken = accessToken;
                accessTokenValue = accessToken.getValue();
            } else {
                accessTokenValue = this.accessToken.getValue();
            }

            return accessTokenValue;
        }

        public AccessToken fetchAccessToken() {
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> tokenRequestBody = new LinkedMultiValueMap<>();
            tokenRequestBody.add("username", getAuthenticationUser());
            tokenRequestBody.add("password", getAuthenticationPassword());
            tokenRequestBody.add("grant_type", "password");
            tokenRequestBody.add("client_id", "admin-cli");

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenRequestBody, tokenHeaders);

            ResponseEntity<JsonNode> tokenResponse = tokenRestTemplate
                    .postForEntity(getServer() + "auth/realms/{realm}/protocol/openid-connect/token", tokenRequest, JsonNode.class, getAuthenticationRealm());

            HttpStatus statusCode = tokenResponse.getStatusCode();
            if (statusCode.is2xxSuccessful()) {
                JsonNode body = tokenResponse.getBody();
                if (body != null) {
                    String accessToken = body.path("access_token").asText(null);
                    long expiresIn = body.path("expires_in").asLong(0);
                    Instant expiresAt = Instant.now().plusSeconds(expiresIn > 0 ? expiresIn : 1);
                    return new AccessToken(accessToken, expiresAt);
                }
                throw new FlowableException("Could not get access token");
            } else {
                throw new FlowableException("Could not get access token. Status code: " + statusCode + ". Token response: " + tokenResponse.getBody());
            }
        }

        protected boolean hasTokenExpired() {
            return KeycloakConfiguration.this.clock.instant().isAfter(accessToken.getExpiresAt().minus(clockSkew));
        }

    }

    public static class AccessToken {

        protected final String value;
        protected final Instant expiresAt;

        public AccessToken(String value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        public String getValue() {
            return value;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
