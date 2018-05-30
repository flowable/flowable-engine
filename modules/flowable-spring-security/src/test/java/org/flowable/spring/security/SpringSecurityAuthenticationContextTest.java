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
package org.flowable.spring.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Filip Hrisafov
 */
public class SpringSecurityAuthenticationContextTest {

    private SpringSecurityAuthenticationContext underTest = new SpringSecurityAuthenticationContext();

    private Authentication initial;

    @Before
    public void setUp() {
        initial = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @After
    public void tearDown() {
        SecurityContextHolder.getContext().setAuthentication(initial);
    }

    @Test
    public void noSpringSecurityContextShouldReturnNothing() {
        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication())
            .as("Spring security authentication")
            .isNull();

        assertThat(underTest.getAuthenticatedUserId())
            .as("Flowable authenticated userId")
            .isNull();

        assertThat(underTest.getPrincipal())
            .as("Flowable authenticated principal")
            .isNull();
    }

    @Test
    public void shouldUseAuthenticationFromSpringSecurityContext() {
        SecurityContext context = SecurityContextHolder.getContext();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken("test", null);
        context.setAuthentication(authenticationToken);

        assertThat(underTest.getAuthenticatedUserId())
            .as("Flowable authenticated userId")
            .isEqualTo("test");

        assertThat(underTest.getPrincipal())
            .as("Flowable authenticated principal")
            .isSameAs(authenticationToken);
    }

    @Test
    public void shouldCorrectlySetPrincipalInContext() {
        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication())
            .as("Spring security authentication")
            .isNull();

        Principal principal = new TestPrincipal("test");
        underTest.setPrincipal(principal);

        assertThat(underTest.getAuthenticatedUserId())
            .as("Flowable authenticated userId")
            .isEqualTo("test");

        assertThat(underTest.getPrincipal())
            .as("Flowable authenticated principal")
            .isNotNull()
            .isInstanceOfSatisfying(UsernamePasswordAuthenticationToken.class, token -> {
                assertThat(token.getPrincipal()).isSameAs(principal);
            });

        assertThat(context.getAuthentication())
            .as("Spring security authentication")
            .isNotNull()
            .isInstanceOfSatisfying(UsernamePasswordAuthenticationToken.class, token -> {
                assertThat(token.getPrincipal()).isSameAs(principal);
            });
    }

    @Test
    public void shouldCorrectlySetAuthenticationInContext() {
        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication())
            .as("Spring security authentication")
            .isNull();

        Principal principal = new TestingAuthenticationToken("kermit", null);
        underTest.setPrincipal(principal);

        assertThat(underTest.getAuthenticatedUserId())
            .as("Flowable authenticated userId")
            .isEqualTo("kermit");

        assertThat(underTest.getPrincipal())
            .as("Flowable authenticated principal")
            .isSameAs(principal);

        assertThat(context.getAuthentication())
            .as("Spring security authentication")
            .isSameAs(principal);
    }

    private static class TestPrincipal implements Principal {

        private final String name;

        private TestPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

}