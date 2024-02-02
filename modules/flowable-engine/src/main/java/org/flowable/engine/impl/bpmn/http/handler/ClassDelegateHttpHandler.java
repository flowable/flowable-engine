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

package org.flowable.engine.impl.bpmn.http.handler;

import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.impl.bpmn.helper.AbstractClassDelegate;
import org.flowable.engine.impl.bpmn.http.delegate.HttpRequestHandlerInvocation;
import org.flowable.engine.impl.bpmn.http.delegate.HttpResponseHandlerInvocation;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.HttpResponse;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.http.common.api.delegate.HttpRequestHandler;
import org.flowable.http.common.api.delegate.HttpResponseHandler;

/**
 * Helper class for HTTP handlers to allow class delegation.
 *
 * This class will lazily instantiate the referenced classes when needed at runtime.
 *
 * @author Tijs Rademakers
 */
public class ClassDelegateHttpHandler extends AbstractClassDelegate implements HttpRequestHandler, HttpResponseHandler {

    private static final long serialVersionUID = 1L;

    public ClassDelegateHttpHandler(String className, List<FieldDeclaration> fieldDeclarations) {
        super(className, fieldDeclarations);
    }

    public ClassDelegateHttpHandler(Class<?> clazz, List<FieldDeclaration> fieldDeclarations) {
        super(clazz, fieldDeclarations);
    }

    @Override
    public void handleHttpRequest(VariableContainer execution, HttpRequest httpRequest, FlowableHttpClient client) {
        HttpRequestHandler httpRequestHandler = getHttpRequestHandlerInstance();
        CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(new HttpRequestHandlerInvocation(httpRequestHandler, execution, httpRequest, client));
    }

    @Override
    public void handleHttpResponse(VariableContainer execution, HttpResponse httpResponse) {
        HttpResponseHandler httpResponseHandler = getHttpResponseHandlerInstance();
        CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(new HttpResponseHandlerInvocation(httpResponseHandler, execution, httpResponse));
    }

    protected HttpRequestHandler getHttpRequestHandlerInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);
        if (delegateInstance instanceof HttpRequestHandler) {
            return (HttpRequestHandler) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + HttpRequestHandler.class);
        }
    }

    protected HttpResponseHandler getHttpResponseHandlerInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);
        if (delegateInstance instanceof HttpResponseHandler) {
            return (HttpResponseHandler) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + HttpResponseHandler.class);
        }
    }

}
