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
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Complements {@link FlowableRestApplicationPreAuthUntrustedProxyTest}: with an allowlist that
 * DOES contain the test client's loopback source, a request carrying a privileged principal
 * header is honoured and succeeds. Together the two tests pin both sides of the trusted-proxy
 * contract — trusted source honoured, untrusted source ignored.
 *
 * @author Arief Hidayat
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "flowable.rest.app.authentication-mode=pre-auth",
        "flowable.rest.app.pre-auth.principal-header=X-Forwarded-User",
        // Both IPv4 and IPv6 loopback, since the test client may connect over either.
        "flowable.rest.app.pre-auth.trusted-proxies=127.0.0.1/32,::1"
    }
)
@AutoConfigureTestRestTemplate
public class FlowableRestApplicationPreAuthTrustedProxyTest {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void principalHeaderFromTrustedSourceIsHonoured() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-User", "rest-admin");
        HttpEntity<?> request = new HttpEntity<>(headers);

        String url = "http://localhost:" + serverPort + "/flowable-rest/service/repository/process-definitions";
        ResponseEntity<String> entity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        assertThat(entity.getStatusCode())
            .as("principal header from a trusted source address")
            .isEqualTo(HttpStatus.OK);
    }
}
