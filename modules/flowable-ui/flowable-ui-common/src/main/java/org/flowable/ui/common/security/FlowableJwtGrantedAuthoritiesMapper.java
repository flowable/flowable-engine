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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.Assert;

/**
 * The Spring Security default implementation prefixes the authorities with {@code SCOPE_} and uses the scopes from OAuth2 in the authorities.
 * However, in order to support authorities from custom attributes with need custom attributes mapper.
 *
 * @author Filip Hrisafov
 * @see FlowableOAuth2GrantedAuthoritiesMapper
 */
public class FlowableJwtGrantedAuthoritiesMapper implements Converter<Jwt, Collection<GrantedAuthority>> {

    protected final String authoritiesAttribute;
    protected final String groupsAttribute;
    protected final Collection<GrantedAuthority> defaultAuthorities;
    protected JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    public FlowableJwtGrantedAuthoritiesMapper(String authoritiesAttribute, String groupsAttribute, Collection<String> defaultAuthorities,
            Collection<String> defaultGroups) {
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
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

        if (StringUtils.isNotBlank(authoritiesAttribute)) {
            Object claim = jwt.getClaim(authoritiesAttribute);
            Collection<String> additionalAuthorities = asStringCollection(claim);
            for (String authority : additionalAuthorities) {
                authorities.add(new SimpleGrantedAuthority(authority));
            }
        }

        if (StringUtils.isNotBlank(groupsAttribute)) {
            Object claim = jwt.getClaim(groupsAttribute);
            Collection<String> groups = asStringCollection(claim);
            for (String group : groups) {
                authorities.add(SecurityUtils.createGroupAuthority(group));
            }

        }

        authorities.addAll(defaultAuthorities);
        return authorities;
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

    public void setDefaultConverter(JwtGrantedAuthoritiesConverter defaultConverter) {
        Assert.notNull(defaultConverter, "defaultConverter must not be null");
        this.defaultConverter = defaultConverter;
    }
}
