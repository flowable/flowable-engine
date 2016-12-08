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
package org.flowable.app.service.idm;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flowable.app.model.common.RemoteGroup;
import org.flowable.app.model.common.RemoteToken;
import org.flowable.app.model.common.RemoteUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class RemoteIdmServiceImpl implements RemoteIdmService {
  
  private static final Logger logger = LoggerFactory.getLogger(RemoteIdmService.class);

  private static final String PROPERTY_URL = "idm.app.url";
  private static final String PROPERTY_ADMIN_USER = "idm.admin.user";
  private static final String PROPERTY_ADMIN_PASSWORD = "idm.admin.password";

  @Autowired
  protected Environment environment;
  
  @Autowired
  protected ObjectMapper objectMapper;

  protected String url;
  protected String adminUser;
  protected String adminPassword;

  @PostConstruct
  protected void init() {
    url = environment.getRequiredProperty(PROPERTY_URL);
    adminUser = environment.getRequiredProperty(PROPERTY_ADMIN_USER);
    adminPassword = environment.getRequiredProperty(PROPERTY_ADMIN_PASSWORD);
  }
  
  public RemoteToken getToken(String tokenValue) {
    JsonNode json = callRemoteIdmService(url + "/api/idm/tokens/" + encode(tokenValue));
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
    JsonNode json = callRemoteIdmService(url + "/api/idm/users/" + encode(userId));
    if (json != null) {
      RemoteUser user = new RemoteUser();
      user.setId(json.get("id").asText());
      user.setFirstName(json.get("firstName").asText());
      user.setLastName(json.get("lastName").asText());
      user.setEmail(json.get("email").asText());
      user.setFullName(json.get("fullName").asText());
      
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
    return null;
  }
  
  protected JsonNode callRemoteIdmService(String url) {
    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(
        Base64.encodeBase64((adminUser + ":" + adminPassword).getBytes(Charset.forName("UTF-8")))));
    
    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    SSLConnectionSocketFactory sslsf = null;
    try {
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      clientBuilder.setSSLSocketFactory(sslsf);
    } catch (Exception e) {
      logger.warn("Could not configure SSL for http client", e);
    }

    CloseableHttpClient client = clientBuilder.build();
    
    try {
      HttpResponse response = client.execute(httpGet);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        return objectMapper.readTree(response.getEntity().getContent());
      }
    } catch (Exception e) {
      logger.warn("Exception while getting token", e);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (IOException e) {
          logger.warn("Exception while closing http client", e);
        }
      }
    }
    return null;
  }
  
  protected String encode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (Exception e) {
      logger.warn("Could not encode url param", e);
      return null;
    }
  }

}
