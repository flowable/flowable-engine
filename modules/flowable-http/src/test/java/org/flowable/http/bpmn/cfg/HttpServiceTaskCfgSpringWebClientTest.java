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
package org.flowable.http.bpmn.cfg;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.net.ssl.SSLHandshakeException;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.test.Deployment;
import org.flowable.http.common.impl.spring.reactive.SpringWebClientFlowableHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientRequestException;

/**
 * @author Filip Hrisafov
 */
public class HttpServiceTaskCfgSpringWebClientTest extends HttpServiceTaskCfgTest {

    @Override
    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        super.additionalConfiguration(processEngineConfiguration);
        processEngineConfiguration.getHttpClientConfig().setDefaultParallelInSameTransaction(true);
        processEngineConfiguration.getHttpClientConfig().setHttpClient(new SpringWebClientFlowableHttpClient(processEngineConfiguration.getHttpClientConfig()));
    }

    @Test
    @Deployment(resources = "org/flowable/http/bpmn/cfg/HttpServiceTaskCfgTest.testHttpsSelfSignedFail.bpmn20.xml")
    public void testHttpsSelfSignedFail() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("httpsSelfSignedFail").getId())
                .isInstanceOf(WebClientRequestException.class)
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }
}
