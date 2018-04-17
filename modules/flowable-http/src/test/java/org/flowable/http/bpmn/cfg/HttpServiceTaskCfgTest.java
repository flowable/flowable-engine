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

import javax.net.ssl.SSLHandshakeException;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.test.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskCfgTest extends HttpServiceTaskCfgTestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServiceTaskCfgTest.class);

    public HttpServiceTaskCfgTest() {
        super("flowable.cfg.xml");
    }

    @Override
    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        LOGGER.info("Set Http client config");
        processEngineConfiguration
                .getHttpClientConfig()
                .setDisableCertVerify(false);
    }

    @Deployment
    public void testHttpsSelfSignedFail() {
        try {
            runtimeService.startProcessInstanceByKey("httpsSelfSignedFail").getId();
            fail("FlowableException expected");
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertTrue(e.getCause() instanceof SSLHandshakeException);
        }
    }
}
