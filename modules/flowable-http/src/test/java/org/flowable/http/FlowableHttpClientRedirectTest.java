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
package org.flowable.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.stream.Stream;

import org.flowable.http.bpmn.HttpServiceTaskTestServer;
import org.flowable.http.common.api.HttpHeaders;
import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.HttpResponse;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.http.common.impl.HttpClientConfig;
import org.flowable.http.common.impl.apache.client5.ApacheHttpComponents5FlowableHttpClient;
import org.flowable.http.common.impl.spring.reactive.SpringWebClientFlowableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Tests that the HTTP clients follow redirects driven by {@link HttpRequest#isNoRedirects()} and do not leak
 * credentials to a different host when a redirect crosses hosts. Both hosts are served by the same test server, reached
 * as {@code localhost} vs {@code 127.0.0.1} to simulate a cross-host redirect.
 */
class FlowableHttpClientRedirectTest {

    protected static final String LOCALHOST_BASE = "http://localhost:9798";
    protected static final String LOOPBACK_IP_BASE = "http://127.0.0.1:9798";

    @BeforeEach
    void setUp() {
        HttpServiceTaskTestServer.setUp();
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void followsRedirectByDefault(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setMethod("GET");
        request.setUrl(LOCALHOST_BASE + "/redirect?location=" + LOCALHOST_BASE + "/echo-authorization");

        HttpResponse response = httpClient.prepareRequest(request).call();

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("auth=<none>");
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void doesNotFollowRedirectWhenNoRedirects(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setMethod("GET");
        request.setUrl(LOCALHOST_BASE + "/redirect?location=" + LOCALHOST_BASE + "/echo-authorization");
        request.setNoRedirects(true);

        HttpResponse response = httpClient.prepareRequest(request).call();

        assertThat(response.getStatusCode()).isEqualTo(302);
        assertThat(response.getHttpHeaders().get("Location"))
                .containsExactly(LOCALHOST_BASE + "/echo-authorization");
    }

    @ParameterizedTest
    @ArgumentsSource(RedirectSafeFlowableHttpClientArgumentProvider.class)
    void doesNotForwardCredentialsOnCrossHostRedirect(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setMethod("GET");
        request.setUrl(LOCALHOST_BASE + "/redirect?location=" + LOOPBACK_IP_BASE + "/echo-authorization");
        HttpHeaders secureHeaders = new HttpHeaders();
        secureHeaders.add("Authorization", "Bearer test-token");
        request.setSecureHttpHeaders(secureHeaders);

        HttpResponse response = httpClient.prepareRequest(request).call();

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody())
                .as("Authorization must not be forwarded to a different host on redirect")
                .isEqualTo("auth=<none>");
    }

    @ParameterizedTest
    @ArgumentsSource(RedirectSafeFlowableHttpClientArgumentProvider.class)
    void forwardsCredentialsOnSameHostRedirect(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setMethod("GET");
        request.setUrl(LOCALHOST_BASE + "/redirect?location=" + LOCALHOST_BASE + "/echo-authorization");
        HttpHeaders secureHeaders = new HttpHeaders();
        secureHeaders.add("Authorization", "Bearer test-token");
        request.setSecureHttpHeaders(secureHeaders);

        HttpResponse response = httpClient.prepareRequest(request).call();

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody())
                .as("Authorization may be forwarded when the redirect stays on the same host")
                .isEqualTo("auth=Bearer test-token");
    }

    protected static HttpClientConfig createClientConfig() {
        HttpClientConfig config = new HttpClientConfig();
        config.setConnectTimeout(Duration.ofSeconds(5));
        config.setSocketTimeout(Duration.ofSeconds(5));
        config.setConnectionRequestTimeout(Duration.ofSeconds(5));
        config.setRequestRetryLimit(5);
        config.setDisableCertVerify(true);
        return config;
    }

    /**
     * Only the clients that strip credentials on cross-host redirects: the Apache HttpClient 5 and the Spring WebClient
     * based clients. The legacy Apache HttpClient 4 based client is intentionally not covered.
     */
    public static class RedirectSafeFlowableHttpClientArgumentProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            HttpClientConfig config = createClientConfig();
            return Stream.of(
                    Arguments.of(new SpringWebClientFlowableHttpClient(config)),
                    Arguments.of(new ApacheHttpComponents5FlowableHttpClient(config))
            );
        }
    }
}
