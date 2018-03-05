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
package org.flowable.test.spring.boot;

import flowable.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filip Hrisafov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class RestApiSecurityApplicationTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    public void userDetailsService() {
        assertThat(userDetailsService.loadUserByUsername("jlong"))
            .as("jlong user")
            .isNotNull()
            .satisfies(user -> {
                assertThat(user.getAuthorities())
                    .as("jlong authorities")
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("users");
            });

        assertThat(userDetailsService.loadUserByUsername("jbarrez"))
            .as("jbarrez user")
            .isNotNull()
            .satisfies(user -> {
                assertThat(user.getAuthorities())
                    .as("jbarrez authorities")
                    .hasSize(2)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("users", "admin");
            });
    }
}
