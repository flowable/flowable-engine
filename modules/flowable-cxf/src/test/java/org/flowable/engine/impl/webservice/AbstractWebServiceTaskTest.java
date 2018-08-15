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

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

/**
 * An abstract class for unit test of web-service task
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @author Christophe DENEUX
 */
@Tag("webservice")
public abstract class AbstractWebServiceTaskTest extends PluggableFlowableTestCase {

    public static final String WEBSERVICE_MOCK_ADDRESS = "http://localhost:63081/webservicemock";

    protected WebServiceMock webServiceMock;

    protected Server server;

    @BeforeEach
    protected void setUp() {

        webServiceMock = new WebServiceMockImpl();
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(WebServiceMock.class);
        svrFactory.setAddress(WEBSERVICE_MOCK_ADDRESS);
        svrFactory.setServiceBean(webServiceMock);
        svrFactory.getInInterceptors().add(new LoggingInInterceptor());
        svrFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        server = svrFactory.create();
        server.start();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        server.stop();
    }

}
