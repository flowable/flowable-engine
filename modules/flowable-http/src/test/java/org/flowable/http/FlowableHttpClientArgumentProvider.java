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

import java.time.Duration;
import java.util.stream.Stream;

import org.flowable.http.common.impl.HttpClientConfig;
import org.flowable.http.common.impl.apache.ApacheHttpComponentsFlowableHttpClient;
import org.flowable.http.common.impl.apache.client5.ApacheHttpComponents5FlowableHttpClient;
import org.flowable.http.common.impl.spring.reactive.SpringWebClientFlowableHttpClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;

/**
 * @author Filip Hrisafov
 */
public class FlowableHttpClientArgumentProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
        HttpClientConfig config = createClientConfig();
        return Stream.of(
                Arguments.of(new SpringWebClientFlowableHttpClient(config)),
                Arguments.of(new ApacheHttpComponentsFlowableHttpClient(config)),
                Arguments.of(new ApacheHttpComponents5FlowableHttpClient(config))
        );
    }

    protected HttpClientConfig createClientConfig() {
        HttpClientConfig config = new HttpClientConfig();
        config.setConnectTimeout(Duration.ofSeconds(5));
        config.setSocketTimeout(Duration.ofSeconds(5));
        config.setConnectionRequestTimeout(Duration.ofSeconds(5));
        config.setRequestRetryLimit(5);
        config.setDisableCertVerify(true);

        return config;
    }

}
