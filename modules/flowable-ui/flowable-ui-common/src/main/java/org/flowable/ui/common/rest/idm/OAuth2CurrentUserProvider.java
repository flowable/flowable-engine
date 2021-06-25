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
package org.flowable.ui.common.rest.idm;

import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.security.SecurityScope;
import org.flowable.ui.common.security.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * @author Filip Hrisafov
 */
public class OAuth2CurrentUserProvider implements CurrentUserProvider {

    protected String firstNameKey;
    protected String lastNameKey;
    protected String fullNameKey;
    protected String emailKey;

    @Override
    public UserRepresentation getCurrentUser(Authentication authentication) {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        UserRepresentation userRepresentation;
        if (principal instanceof OidcUser) {
            userRepresentation = getCurrentUser((OidcUser) principal);
        } else {
            userRepresentation = getCurrentUser(principal);
        }

        SecurityScope securityScope = SecurityUtils.getSecurityScope(authentication);
        userRepresentation.setTenantId(securityScope.getTenantId());

        for (String groupId : securityScope.getGroupIds()) {
            GroupRepresentation group = new GroupRepresentation();
            group.setId(groupId);
            userRepresentation.getGroups().add(group);
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            userRepresentation.getPrivileges().add(authority.getAuthority());
        }
        return userRepresentation;
    }

    protected UserRepresentation getCurrentUser(OidcUser user) {
        Map<String, Object> userAttributes = user.getAttributes();

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(user.getName());
        userRepresentation.setFirstName(getAttribute(firstNameKey, userAttributes, user.getGivenName()));
        userRepresentation.setLastName(getAttribute(lastNameKey, userAttributes, user.getFamilyName()));
        String fullName = getAttribute(fullNameKey, userAttributes, user.getFullName());
        if (StringUtils.isBlank(fullName)) {
            StringJoiner joiner = new StringJoiner(" ");
            if (StringUtils.isNotBlank(userRepresentation.getFirstName())) {
                joiner.add(userRepresentation.getFirstName());
            }

            if (StringUtils.isNotBlank(user.getMiddleName())) {
                joiner.add(user.getMiddleName());
            }

            if (StringUtils.isNotBlank(userRepresentation.getLastName())) {
                joiner.add(userRepresentation.getLastName());
            }

            fullName = joiner.toString();
        }

        userRepresentation.setFullName(fullName);
        userRepresentation.setEmail(getAttribute(emailKey, userAttributes, user.getEmail()));

        return userRepresentation;

    }

    protected UserRepresentation getCurrentUser(OAuth2User user) {
        Map<String, Object> userAttributes = user.getAttributes();
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(user.getName());
        userRepresentation.setFirstName(getAttribute(firstNameKey, userAttributes, null));
        userRepresentation.setLastName(getAttribute(lastNameKey, userAttributes, null));
        String fullName = getAttribute(fullNameKey, userAttributes, null);
        if (StringUtils.isBlank(fullName)) {
            StringJoiner joiner = new StringJoiner(" ");
            if (StringUtils.isNotBlank(userRepresentation.getFirstName())) {
                joiner.add(userRepresentation.getFirstName());
            }

            if (StringUtils.isNotBlank(userRepresentation.getLastName())) {
                joiner.add(userRepresentation.getLastName());
            }

            fullName = joiner.toString();
        }

        userRepresentation.setFullName(fullName);
        userRepresentation.setEmail(getAttribute(emailKey, userAttributes, null));

        return userRepresentation;
    }

    protected String getAttribute(String attribute, Map<String, Object> attributes, String defaultAttributeValue) {
        if (StringUtils.isEmpty(attribute)) {
            return defaultAttributeValue;
        }

        Object attributeValue = attributes.get(attribute);

        if (attributeValue != null) {
            return attributeValue.toString();
        }

        return defaultAttributeValue;
    }

    @Override
    public boolean supports(Authentication authentication) {
        return authentication.getPrincipal() instanceof OAuth2User;
    }

    public String getFirstNameKey() {
        return firstNameKey;
    }

    public void setFirstNameKey(String firstNameKey) {
        this.firstNameKey = firstNameKey;
    }

    public String getLastNameKey() {
        return lastNameKey;
    }

    public void setLastNameKey(String lastNameKey) {
        this.lastNameKey = lastNameKey;
    }

    public String getFullNameKey() {
        return fullNameKey;
    }

    public void setFullNameKey(String fullNameKey) {
        this.fullNameKey = fullNameKey;
    }

    public String getEmailKey() {
        return emailKey;
    }

    public void setEmailKey(String emailKey) {
        this.emailKey = emailKey;
    }
}
