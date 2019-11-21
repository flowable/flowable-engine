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
package org.flowable.ui.common.service.idm;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.flowable.ui.common.model.RemoteGroup;
import org.flowable.ui.common.model.RemoteToken;
import org.flowable.ui.common.model.RemoteUser;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class RemoteIdmServiceImpl implements RemoteIdmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteIdmServiceImpl.class);

    @Autowired
    protected ObjectMapper objectMapper;

    protected String url;
    protected String adminUser;
    protected String adminPassword;

    public RemoteIdmServiceImpl(FlowableCommonAppProperties properties) {
        url = properties.determineIdmAppUrl();
        adminUser = properties.getIdmAdmin().getUser();
        Assert.hasText(adminUser, "Admin user must not be empty");
        adminPassword = properties.getIdmAdmin().getPassword();
        Assert.hasText(adminUser, "Admin user password should not be empty");
    }

    @Override
    public RemoteUser authenticateUser(String username, String password) {
        JsonNode json = callRemoteIdmService(url + "api/idm/users/" + encode(username), username, password);
        if (json != null) {
            return parseUserInfo(json);
        }
        return null;
    }

    @Override
    public RemoteToken getToken(String tokenValue) {
        JsonNode json = callRemoteIdmService(url + "api/idm/tokens/" + encode(tokenValue), adminUser, adminPassword);
        if (json != null) {
            RemoteToken token = new RemoteToken();
            token.setId(json.get("id").asText());
            token.setValue(json.get("value").asText());
            token.setUserId(json.get("userId").asText());
            return token;
        }
        return null;
    }

    @Override
    public RemoteUser getUser(String userId) {
        JsonNode json = callRemoteIdmService(url + "api/idm/users/" + encode(userId), adminUser, adminPassword);
        if (json != null) {
            return parseUserInfo(json);
        }
        return null;
    }

    @Override
    public List<RemoteUser> findUsersByNameFilter(String filter) {
        JsonNode json = callRemoteIdmService(url + "api/idm/users?filter=" + encode(filter), adminUser, adminPassword);
        if (json != null) {
            return parseUsersInfo(json);
        }
        return new ArrayList<>();
    }

    @Override
    public List<RemoteUser> findUsersByGroup(String groupId) {
        JsonNode json = callRemoteIdmService(url + "api/idm/groups/" + encode(groupId) + "/users", adminUser, adminPassword);
        if (json != null) {
            return parseUsersInfo(json);
        }
        return new ArrayList<>();
    }

    @Override
    public RemoteGroup getGroup(String groupId) {
        JsonNode json = callRemoteIdmService(url + "api/idm/groups/" + encode(groupId), adminUser, adminPassword);
        if (json != null) {
            return parseGroupInfo(json);
        }
        return null;
    }

    @Override
    public List<RemoteGroup> findGroupsByNameFilter(String filter) {
        JsonNode json = callRemoteIdmService(url + "api/idm/groups?filter=" + encode(filter), adminUser, adminPassword);
        if (json != null) {
            return parseGroupsInfo(json);
        }
        return new ArrayList<>();
    }

    protected JsonNode callRemoteIdmService(String url, String username, String password) {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(
                Base64.getEncoder().encode((username + ":" + password).getBytes(StandardCharsets.UTF_8))));

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslsf = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);
            clientBuilder.setSSLSocketFactory(sslsf);
        } catch (Exception e) {
            LOGGER.warn("Could not configure SSL for http client", e);
        }

        CloseableHttpClient client = clientBuilder.build();

        try {
            HttpResponse response = client.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return objectMapper.readTree(response.getEntity().getContent());
            }
        } catch (Exception e) {
            LOGGER.warn("Exception while getting token", e);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LOGGER.warn("Exception while closing http client", e);
                }
            }
        }
        return null;
    }

    protected List<RemoteUser> parseUsersInfo(JsonNode json) {
        List<RemoteUser> result = new ArrayList<>();
        if (json != null && json.isArray()) {
            ArrayNode array = (ArrayNode) json;
            for (JsonNode userJson : array) {
                result.add(parseUserInfo(userJson));
            }
        }
        return result;
    }

    protected RemoteUser parseUserInfo(JsonNode json) {
        RemoteUser user = new RemoteUser();
        user.setId(json.get("id").asText());
        user.setFirstName(json.get("firstName").asText());
        user.setLastName(json.get("lastName").asText());
        if (json.has("displayName") && !json.get("displayName").isNull()) {
            user.setDisplayName(json.get("displayName").asText());
        }
        user.setEmail(json.get("email").asText());
        user.setFullName(json.get("fullName").asText());
        if (json.has("tenantId") && !json.get("tenantId").isNull()) {
            user.setTenantId(json.get("tenantId").asText());
        }

        if (json.has("groups")) {
            for (JsonNode groupNode : ((ArrayNode) json.get("groups"))) {
                user.getGroups().add(new RemoteGroup(groupNode.get("id").asText(), groupNode.get("name").asText()));
            }
        }

        if (json.has("privileges")) {
            for (JsonNode privilegeNode : ((ArrayNode) json.get("privileges"))) {
                user.getPrivileges().add(privilegeNode.asText());
            }
        }

        return user;
    }

    protected List<RemoteGroup> parseGroupsInfo(JsonNode json) {
        List<RemoteGroup> result = new ArrayList<>();
        if (json != null && json.isArray()) {
            ArrayNode array = (ArrayNode) json;
            for (JsonNode userJson : array) {
                result.add(parseGroupInfo(userJson));
            }
        }
        return result;
    }

    protected RemoteGroup parseGroupInfo(JsonNode json) {
        RemoteGroup group = new RemoteGroup();
        group.setId(json.get("id").asText());
        group.setName(json.get("name").asText());
        return group;
    }

    protected String encode(String s) {
        if (s == null) {
            return "";
        }

        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            LOGGER.warn("Could not encode url param", e);
            return null;
        }
    }

}
