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
package org.flowable.http.common.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.client.ExecutableHttpRequest;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.http.common.impl.apache.ApacheHttpComponentsFlowableHttpClient;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class HttpClientConfigTest {

    @Test
    void determineHttpClientWhenSet() {
        HttpClientConfig config = new HttpClientConfig();

        FlowableHttpClient httpClient = new FlowableHttpClient() {

            @Override
            public ExecutableHttpRequest prepareRequest(HttpRequest request) {
                return null;
            }
        };

        config.setHttpClient(httpClient);

        assertThat(config.determineHttpClient()).isEqualTo(httpClient);
    }

    @Test
    void determineHttpClientWhenNotSet() {
        HttpClientConfig config = new HttpClientConfig();

        assertThat(config.determineHttpClient()).isInstanceOf(ApacheHttpComponentsFlowableHttpClient.class);
    }
}
