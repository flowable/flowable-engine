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
package org.flowable.ui.idm.application;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.ui.idm.model.SSOUserInfo;
import org.flowable.ui.idm.rest.app.SSOHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Primary;

@Component
@Primary
@ConditionalOnProperty(
            value="flowable.sso.provider",
            havingValue = "keycloak")
public class KeyCloakSSOHandler implements SSOHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(KeyCloakSSOHandler.class);

    @Value("${flowable.sso.keycloak.url.auth:#{null}}")
    private String ssoExternalUrl;

    @Value("${flowable.sso.keycloak.client-id:#{null}}")
    private String ssoClientId;

    @Value("${flowable.sso.keycloak.client-secret:#{null}}")
    private String ssoClientSecret;

    @Value("${flowable.sso.keycloak.url.token:#{null}}")
    private String ssoTokenUrl;

    @Value("${flowable.sso.keycloak.url.info:#{null}}")
    private String ssoUserInfoUrl;

    @Value("${flowable.sso.keycloak.scope:#{null}}")
    private String ssoScope;

    @Value("${flowable.sso.keycloak.userinfo.tenant:#{null}}")
    private String ssoUserInfoTenant;

    @Value("${flowable.sso.keycloak.userinfo.client_roles:#{null}}")
    private String ssoUserInfoClientRoles;

    @Autowired
    private Environment env;

    private String redirectUri = null;

    private String state = UUID.randomUUID().toString();

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public SSOUserInfo handleSsoReturn(HttpServletRequest request, HttpServletResponse response, MultiValueMap<String, String> body) {
        LOGGER.debug("SSOhandler = {}", KeyCloakSSOHandler.class);

        String stateReturned = request.getParameter("state");

        if( !stateReturned.equals(state)){
            LOGGER.error("Return sso 'state' did not match original state");
            return null;
        }

        String access_token = getAccessToken(request.getParameter("code"));
        return getUserInfo(access_token);
    }

    @Override
    public String getExternalUrl(String redirectUri) {
        this.redirectUri = redirectUri;
        this.state = UUID.randomUUID().toString();
        return ssoExternalUrl
            + "?response_type=code"
            + "&client_id=" + ssoClientId
            + "&scope=" + ssoScope
            + "&state=" + state
            + "&redirect_uri=" + redirectUri
            + "&response_mode=query";
    }

    private String getAccessToken(String code) {
        // request access token
        MultiValueMap<String, String> json = createMap(
            new AbstractMap.SimpleEntry<>("client_id", ssoClientId),
            new AbstractMap.SimpleEntry<>("client_secret", ssoClientSecret),
            new AbstractMap.SimpleEntry<>("code", code),
            new AbstractMap.SimpleEntry<>("grant_type", "authorization_code"),
            new AbstractMap.SimpleEntry<>("redirect_uri", redirectUri)
        );

        RestTemplate template = new RestTemplate();
        String s = template.postForObject(ssoTokenUrl,
            getHttpEntity(json),
            String.class);

        JSONObject response = new JSONObject(s);
        return response.getString("access_token");
    }

    private SSOUserInfo getUserInfo(String accessToken) {
        RestTemplate template = new RestTemplate();
        JSONObject json;

        MultiValueMap<String, String> body = createMap(
            new AbstractMap.SimpleEntry<>("access_token", accessToken)
        );

        String resp = template.postForObject(ssoUserInfoUrl, getHttpEntity(body), String.class);

        json = new JSONObject(resp);

        List<String> privs = new ArrayList<>();
        if(json.has(ssoUserInfoClientRoles)){
            for (Object priv : json.getJSONArray(ssoUserInfoClientRoles)) {
                privs.add(priv.toString());
            }
        }

        String tenant = null;
        if(json.has(ssoUserInfoTenant)){
            tenant = json.getString(ssoUserInfoTenant);
        }

        return new SSOUserInfo(
            json.getString("preferred_username"),
            json.getString("given_name"),
            json.getString("family_name"),
            json.getString("email"),
            privs,
            tenant
        );

    }

    private HttpEntity<MultiValueMap<String, String>> getHttpEntity(MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return new HttpEntity<>(body, headers);
    }

    private MultiValueMap<String, String> createMap(AbstractMap.SimpleEntry<String, String>... values) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>(values.length);

        for (AbstractMap.SimpleEntry<String, String> entry : values) {
            map.add(entry.getKey(), entry.getValue());
        }

        return map;
    }
}