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
package org.activiti.engine.test.bpmn.servicetask;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.activiti.engine.impl.webservice.WebServiceMock;
import org.activiti.engine.impl.webservice.WebServiceMockImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

/**
 * @author Esteban Robles Luna
 */
public abstract class AbstractWebServiceTaskTest extends PluggableFlowableTestCase {

    protected WebServiceMock webServiceMock;
    private Server server;

    @Override
    protected void initializeProcessEngine() {
        super.initializeProcessEngine();

        webServiceMock = new WebServiceMockImpl();
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(WebServiceMock.class);
        svrFactory.setAddress("http://localhost:63081/webservicemock");
        svrFactory.setServiceBean(webServiceMock);
        svrFactory.getInInterceptors().add(new LoggingInInterceptor());
        svrFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        server = svrFactory.create();
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        server.stop();
        server.destroy();
    }

    protected boolean isValidating() {
        return true;
    }
}
