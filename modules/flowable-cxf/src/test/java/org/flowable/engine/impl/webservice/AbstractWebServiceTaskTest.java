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
package org.flowable.engine.impl.webservice;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Server;
import org.flowable.engine.impl.test.AbstractFlowableTestCase;
import org.flowable.engine.impl.test.PluggableFlowableExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * An abstract class for unit test of web-service task
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @author Christophe DENEUX
 */
@Tag("webservice")
@Tag("pluggable")
@ExtendWith(MockWebServiceExtension.class)
@ExtendWith(PluggableFlowableExtension.class)
public abstract class AbstractWebServiceTaskTest extends AbstractFlowableTestCase {

    public static final String WEBSERVICE_MOCK_ADDRESS = "http://localhost:63081/webservicemock";

    protected WebServiceMock webServiceMock;

    protected Server server;

    protected ConcurrentMap<QName, URL> originalOverriddenEndpointAddresses;

    @BeforeEach
    protected void setUp(WebServiceMock webServiceMock, Server server) {
        this.webServiceMock = webServiceMock;
        this.server = server;

        originalOverriddenEndpointAddresses = new ConcurrentHashMap<>(processEngineConfiguration.getWsOverridenEndpointAddresses());
    }

    @AfterEach
    void tearDown() {
        processEngineConfiguration.getWsOverridenEndpointAddresses().clear();
        processEngineConfiguration.setWsOverridenEndpointAddresses(originalOverriddenEndpointAddresses);
    }
}
