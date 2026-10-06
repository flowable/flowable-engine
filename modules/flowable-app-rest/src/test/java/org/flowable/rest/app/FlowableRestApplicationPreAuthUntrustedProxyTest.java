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
 * Pins the trusted-proxy contract: with an allowlist configured that does NOT contain the test
 * client's source address, a request carrying a valid principal header must still be rejected.
 * This is the case a plain header-present / header-absent suite cannot distinguish — a spoofed
 * header from an untrusted source looks identical to a legitimate one unless the source address
 * is checked.
 *
 * <p>The allowlist is {@code 10.0.0.0/8}; the {@link TestRestTemplate} connects from loopback
 * ({@code 127.0.0.1}), which is outside that range, so the header is ignored and the request is
 * denied.
 *
 * @author Arief Hidayat
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "flowable.rest.app.authentication-mode=pre-auth",
        "flowable.rest.app.pre-auth.principal-header=X-Forwarded-User",
        "flowable.rest.app.pre-auth.trusted-proxies=10.0.0.0/8"
    }
)
@AutoConfigureTestRestTemplate
public class FlowableRestApplicationPreAuthUntrustedProxyTest {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void principalHeaderFromUntrustedSourceIsIgnored() {
        HttpHeaders headers = new HttpHeaders();
        // A valid, privileged user id -- but arriving from a source outside the allowlist.
        headers.set("X-Forwarded-User", "rest-admin");
        HttpEntity<?> request = new HttpEntity<>(headers);

        String url = "http://localhost:" + serverPort + "/flowable-rest/service/repository/process-definitions";
        ResponseEntity<String> entity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        assertThat(entity.getStatusCode())
            .as("spoofed principal header from an untrusted source address")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
