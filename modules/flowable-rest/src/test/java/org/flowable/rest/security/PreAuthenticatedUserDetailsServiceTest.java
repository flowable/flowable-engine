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
package org.flowable.rest.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.PrivilegeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * @author Arief Hidayat
 */
@ExtendWith(MockitoExtension.class)
class PreAuthenticatedUserDetailsServiceTest {

    @Mock
    protected IdmIdentityService idmIdentityService;

    @Mock
    protected PrivilegeQuery privilegeQuery;

    @Mock
    protected Privilege accessRestApi;

    @Mock
    protected Privilege accessAdmin;

    protected PreAuthenticatedUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new PreAuthenticatedUserDetailsService(idmIdentityService);
    }

    @Test
    void withPrivilegeVerificationGrantsTheUsersActualPrivileges() {
        service.setVerifyRestApiPrivilege(true);
        lenient().when(accessRestApi.getName()).thenReturn(SecurityConstants.PRIVILEGE_ACCESS_REST_API);
        lenient().when(accessAdmin.getName()).thenReturn(SecurityConstants.ACCESS_ADMIN);
        when(idmIdentityService.createPrivilegeQuery()).thenReturn(privilegeQuery);
        when(privilegeQuery.userId("alice")).thenReturn(privilegeQuery);
        when(privilegeQuery.list()).thenReturn(List.of(accessRestApi, accessAdmin));

        UserDetails details = service.loadUserDetails(new PreAuthenticatedAuthenticationToken("alice", "n/a"));

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder(SecurityConstants.PRIVILEGE_ACCESS_REST_API, SecurityConstants.ACCESS_ADMIN);
    }

    @Test
    void withoutPrivilegeVerificationGrantsAccessRestApiUnconditionally() {
        service.setVerifyRestApiPrivilege(false);

        UserDetails details = service.loadUserDetails(new PreAuthenticatedAuthenticationToken("bob", "n/a"));

        assertThat(details.getUsername()).isEqualTo("bob");
        assertThat(details.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly(SecurityConstants.PRIVILEGE_ACCESS_REST_API);
    }

    @Test
    void neverConsultsIdmWhenPrivilegeVerificationIsOff() {
        service.setVerifyRestApiPrivilege(false);

        service.loadUserDetails(new PreAuthenticatedAuthenticationToken("carol", "n/a"));

        // No password is checked and, with verification off, no IDM lookup happens either.
        org.mockito.Mockito.verifyNoInteractions(idmIdentityService);
    }
}
