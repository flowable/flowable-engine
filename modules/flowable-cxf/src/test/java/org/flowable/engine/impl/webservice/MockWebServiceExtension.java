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

import static org.flowable.engine.impl.webservice.AbstractWebServiceTaskTest.WEBSERVICE_MOCK_ADDRESS;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Extension is needed, as we need to have the we service run before we start deploying to the flowable engine.
 *
 * @author Filip Hrisafov
 */
public class MockWebServiceExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(MockWebServiceExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        getMockWebServiceContext(context).server.start();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        getMockWebServiceContext(context).stopIfStarted();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return WebServiceMock.class.equals(parameterType) || Server.class.equals(parameterType);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        if (WebServiceMock.class.equals(parameterType)) {
            return getMockWebServiceContext(extensionContext).webServiceMock;
        } else if (Server.class.equals(parameterType)) {
            return getMockWebServiceContext(extensionContext).server;
        }
        throw new ParameterResolutionException("Cannot resolve parameter for parameter context: " + parameterContext);
    }

    private static MockWebServiceContext getMockWebServiceContext(ExtensionContext context) {
        return getStore(context).getOrComputeIfAbsent(context.getUniqueId(), key -> create(), MockWebServiceContext.class);

    }

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

    private static class MockWebServiceContext {

        protected final WebServiceMock webServiceMock;
        protected final Server server;

        private MockWebServiceContext(WebServiceMock webServiceMock, Server server) {
            this.webServiceMock = webServiceMock;
            this.server = server;
        }

        private void stopIfStarted() {
            if (server.isStarted()) {
                server.stop();
                server.destroy();
            }
        }
    }

    private static MockWebServiceContext create() {
        WebServiceMock webServiceMock = new WebServiceMockImpl();
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(WebServiceMock.class);
        svrFactory.setAddress(WEBSERVICE_MOCK_ADDRESS);
        svrFactory.setServiceBean(webServiceMock);
        svrFactory.getInInterceptors().add(new LoggingInInterceptor());
        svrFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        Server server = svrFactory.create();
        return new MockWebServiceContext(webServiceMock, server);
    }
}
