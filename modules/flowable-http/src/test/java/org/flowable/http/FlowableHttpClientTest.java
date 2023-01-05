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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.flowable.http.bpmn.HttpServiceTaskTestServer;
import org.flowable.http.common.api.HttpHeaders;
import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.HttpResponse;
import org.flowable.http.common.api.MultiValuePart;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.core.io.ClassPathResource;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Filip Hrisafov
 */
class FlowableHttpClientTest {

    @BeforeEach
    void setUp() {
        HttpServiceTaskTestServer.setUp();
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void simpleGet(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setUrl("http://localhost:9798/test");
        request.setMethod("GET");
        HttpResponse response = httpClient.prepareRequest(request).call();

        assertThatJson(response.getBody())
                .isEqualTo("{"
                        + "  name: {"
                        + "    firstName: 'John',"
                        + "    lastName: 'Doe'"
                        + "  }"
                        + "}");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHttpHeaders().get("Content-Type"))
                .containsExactly("application/json");
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void getImage(FlowableHttpClient httpClient) throws IOException {
        HttpRequest request = new HttpRequest();
        request.setUrl("http://localhost:9798/resource?resource=org/flowable/http/images/flowable-logo.png");
        request.setMethod("GET");
        HttpResponse response = httpClient.prepareRequest(request).call();

        byte[] imageBytes;
        try (InputStream stream = new ClassPathResource("org/flowable/http/images/flowable-logo.png").getInputStream()) {
            imageBytes = IOUtils.toByteArray(stream);
        }

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBodyBytes()).isEqualTo(imageBytes);
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void getWithAllParameters(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setUrl("http://localhost:9798/api/test?testArg=testValue");
        request.setMethod("GET");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Test", "Test Value");
        request.setHttpHeaders(httpHeaders);
        HttpResponse response = httpClient.prepareRequest(request).call();

        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  url: 'http://localhost:9798/api/test',"
                        + "  args: {"
                        + "    testArg: [ 'testValue' ]"
                        + "  },"
                        + "  headers: {"
                        + "    X-Test: [ 'Test Value' ]"
                        + "  }"
                        + "}");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHttpHeaders().get("Content-Type"))
                .containsExactly("application/json");
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void postWithAllParameters(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setUrl("http://localhost:9798/api/test?testArg=testPostValue");
        request.setMethod("POST");
        request.setBody("{ body: 'kermit' }");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Test", "Test Post Value");
        request.setHttpHeaders(httpHeaders);
        HttpResponse response = httpClient.prepareRequest(request).call();

        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  url: 'http://localhost:9798/api/test',"
                        + "  body: \"{ body: 'kermit' }\","
                        + "  args: {"
                        + "    testArg: [ 'testPostValue' ]"
                        + "  },"
                        + "  headers: {"
                        + "    X-Test: [ 'Test Post Value' ]"
                        + "  }"
                        + "}");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHttpHeaders().get("Content-Type"))
                .containsExactly("application/json");
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void postWithMultiPart(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setUrl("http://localhost:9798/api/test-multi?testArg=testMultiPartValue");
        request.setMethod("POST");
        request.addMultiValuePart(MultiValuePart.fromText("name", "kermit"));
        request.addMultiValuePart(MultiValuePart.fromFile("document", "kermit document".getBytes(StandardCharsets.UTF_8), "kermit.txt"));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Test", "Test MultiPart Value");
        request.setHttpHeaders(httpHeaders);
        HttpResponse response = httpClient.prepareRequest(request).call();

        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  url: 'http://localhost:9798/api/test-multi',"
                        + "  args: {"
                        + "    testArg: [ 'testMultiPartValue' ]"
                        + "  },"
                        + "  headers: {"
                        + "    X-Test: [ 'Test MultiPart Value' ]"
                        + "  },"
                        + "  parts: {"
                        + "    name: ["
                        + "      {"
                        + "        content: 'kermit'"
                        + "      }"
                        + "    ],"
                        + "    document: ["
                        + "      {"
                        + "        content: 'kermit document',"
                        + "        filename: 'kermit.txt'"
                        + "      }"
                        + "    ]"
                        + "  }"
                        + "}");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHttpHeaders().get("Content-Type"))
                .containsExactly("application/json");
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void deleteWithoutBody(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setUrl("http://localhost:9798/api/test");
        request.setMethod("DELETE");
        HttpResponse response = httpClient.prepareRequest(request).call();
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  url: 'http://localhost:9798/api/test',"
                        + "  body: \"\""
                        + "}");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHttpHeaders().get("Content-Type"))
                .containsExactly("application/json");
    }

    @ParameterizedTest
    @ArgumentsSource(FlowableHttpClientArgumentProvider.class)
    void deleteWithBody(FlowableHttpClient httpClient) {
        HttpRequest request = new HttpRequest();
        request.setUrl("http://localhost:9798/api/test");
        request.setMethod("DELETE");
        request.setBody("{ body: 'kermit' }");
        HttpResponse response = httpClient.prepareRequest(request).call();
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  url: 'http://localhost:9798/api/test',"
                        + "  body: \"{ body: 'kermit' }\""
                        + "}");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHttpHeaders().get("Content-Type"))
                .containsExactly("application/json");
    }

}
