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
package org.flowable.rest.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-ldap")
public class FlowableRestApplicationLdapAuthenticationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void nonAuthenticatedUserShouldNotBeAbleToAccessActuator() {
        ResponseEntity<String> entity = restTemplate.getForEntity("/actuator", String.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void authenticateOverLdap() {
        ResponseEntity<String> entity = restTemplate
                .withBasicAuth("admin", "pass")
                .getForEntity("/actuator", String.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void authenticateWithWrongPasswordOverLdap() {
        ResponseEntity<String> entity = restTemplate
                .withBasicAuth("admin", "wrong")
                .getForEntity("/actuator", String.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void authenticateWithNonExistingUserOverLdap() {
        ResponseEntity<String> entity = restTemplate
                .withBasicAuth("non-existing", "wrong")
                .getForEntity("/actuator", String.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
