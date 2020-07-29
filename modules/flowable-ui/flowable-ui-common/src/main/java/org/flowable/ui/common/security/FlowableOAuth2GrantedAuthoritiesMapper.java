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
package org.flowable.ui.common.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

/**
 * The Spring Security default implementation prefixes the authorities with {@code SCOPE_} and uses the scopes from OAuth2 in the authorities.
 * However, in order to support authorities from custom attributes with need custom attributes mapper.
 *
 * @author Filip Hrisafov
 * @see FlowableJwtGrantedAuthoritiesMapper
 */
public class FlowableOAuth2GrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

    protected final String authoritiesAttribute;
    protected final String groupsAttribute;
    protected final Collection<GrantedAuthority> defaultAuthorities;

    public FlowableOAuth2GrantedAuthoritiesMapper(String authoritiesAttribute, String groupsAttribute,
            Collection<String> defaultAuthorities, Collection<String> defaultGroups) {
        this.authoritiesAttribute = authoritiesAttribute;
        this.groupsAttribute = groupsAttribute;
        this.defaultAuthorities = new LinkedHashSet<>();
        if (defaultAuthorities != null) {
            for (String defaultAuthority : defaultAuthorities) {
                this.defaultAuthorities.add(new SimpleGrantedAuthority(defaultAuthority));
            }
        }

        if (defaultGroups != null) {
            for (String defaultGroup : defaultGroups) {
                this.defaultAuthorities.add(SecurityUtils.createGroupAuthority(defaultGroup));
            }

        }
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        List<GrantedAuthority> newAuthorities = new ArrayList<>(authorities);

        OAuth2UserAuthority userAuthority = getOAuth2UserAuthority(authorities);

        if (userAuthority instanceof OidcUserAuthority) {

            OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) userAuthority;
            if (StringUtils.isNotBlank(authoritiesAttribute)) {
                Object claim = oidcUserAuthority.getUserInfo().getClaim(authoritiesAttribute);
                Collection<String> extraAuthorities = asStringCollection(claim);
                for (String extraAuthority : extraAuthorities) {
                    newAuthorities.add(new SimpleGrantedAuthority(extraAuthority));
                }
            }

            if (StringUtils.isNotBlank(groupsAttribute)) {
                Object claim = oidcUserAuthority.getUserInfo().getClaim(groupsAttribute);
                Collection<String> groups = asStringCollection(claim);
                for (String group : groups) {
                    newAuthorities.add(SecurityUtils.createGroupAuthority(group));
                }
            }

        } else if (userAuthority != null) {
            if (StringUtils.isNotBlank(authoritiesAttribute)) {
                Object attribute = userAuthority.getAttributes().get(authoritiesAttribute);
                Collection<String> extraAuthorities = asStringCollection(attribute);
                for (String extraAuthority : extraAuthorities) {
                    newAuthorities.add(new SimpleGrantedAuthority(extraAuthority));
                }
            }

            if (StringUtils.isNotBlank(groupsAttribute)) {
                Object attribute = userAuthority.getAttributes().get(groupsAttribute);
                Collection<String> groups = asStringCollection(attribute);
                for (String group : groups) {
                    newAuthorities.add(SecurityUtils.createGroupAuthority(group));
                }
            }
        }

        newAuthorities.addAll(defaultAuthorities);

        return newAuthorities;
    }

    protected Collection<String> asStringCollection(Object value) {
        if (value instanceof Collection) {
            return (Collection<String>) value;
        } else if (value instanceof String) {
            return Arrays.asList(((String) value).split(","));
        } else {
            return Collections.emptyList();
        }
    }

    protected OAuth2UserAuthority getOAuth2UserAuthority(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OAuth2UserAuthority) {
                return (OAuth2UserAuthority) authority;
            }
        }

        return null;
    }
}
