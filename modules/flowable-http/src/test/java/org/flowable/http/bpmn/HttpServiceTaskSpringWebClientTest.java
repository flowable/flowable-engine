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
package org.flowable.http.bpmn;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import io.netty.handler.timeout.ReadTimeoutException;

/**
 * @author Filip Hrisafov
 */
@ConfigurationResource("flowableSpringWebClient.cfg.xml")
public class HttpServiceTaskSpringWebClientTest extends HttpServiceTaskTest {

    @Test
    @Deployment
    @Override
    // We override because we have a different BPMN XML
    public void testMapException() {
        super.testMapException();
    }

    @Test
    @Deployment(resources = "org/flowable/http/bpmn/HttpServiceTaskTest.testConnectTimeout.bpmn20.xml")
    @Override
    public void testConnectTimeout() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("connectTimeout"))
                .isInstanceOf(WebClientRequestException.class)
                .hasCauseInstanceOf(ReadTimeoutException.class);
    }

    @Disabled("Nothing special to test. It is the same as the testRequestTimeout")
    @Test
    @Deployment(resources = "org/flowable/http/bpmn/HttpServiceTaskTest.testRequestTimeout2.bpmn20.xml")
    @Override
    public void testRequestTimeoutFromProcessModelHasPrecedence() {
        super.testRequestTimeoutFromProcessModelHasPrecedence();
    }

    @Test
    @Deployment(resources = "org/flowable/http/bpmn/HttpServiceTaskTest.testRequestTimeout.bpmn20.xml")
    @Override
    public void testRequestTimeout() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("requestTimeout"))
                .isInstanceOf(WebClientRequestException.class)
                .hasCauseInstanceOf(ReadTimeoutException.class);
    }

    @Override
    protected String get500ResponseReason() {
        return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
    }
}
