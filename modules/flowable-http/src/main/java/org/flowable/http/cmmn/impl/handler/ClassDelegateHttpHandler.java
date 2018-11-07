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

package org.flowable.http.cmmn.impl.handler;

import org.apache.http.client.HttpClient;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegate;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.http.HttpRequest;
import org.flowable.http.HttpResponse;
import org.flowable.http.delegate.HttpRequestHandler;
import org.flowable.http.delegate.HttpResponseHandler;

import java.util.List;

/**
 * Helper class for HTTP handlers to allow class delegation.
 *
 * This class will lazily instantiate the referenced classes when needed at runtime.
 *
 * @author Tijs Rademakers
 */
public class ClassDelegateHttpHandler extends CmmnClassDelegate implements HttpRequestHandler, HttpResponseHandler {

    private static final long serialVersionUID = 1L;

    public ClassDelegateHttpHandler(String className, List<FieldExtension> fieldExtension) {
        super(className, fieldExtension);
    }

    public ClassDelegateHttpHandler(Class<?> clazz, List<FieldExtension> fieldExtension) {
        super(clazz.getName(), fieldExtension);
    }

    @Override
    public void handleHttpRequest(VariableContainer execution, HttpRequest httpRequest, HttpClient client) {
        HttpRequestHandler httpRequestHandler = getHttpRequestHandlerInstance();
        httpRequestHandler.handleHttpRequest(execution, httpRequest, client);
    }

    @Override
    public void handleHttpResponse(VariableContainer execution, HttpResponse httpResponse) {
        HttpResponseHandler httpResponseHandler = getHttpResponseHandlerInstance();
        httpResponseHandler.handleHttpResponse(execution, httpResponse);
    }

    protected HttpRequestHandler getHttpRequestHandlerInstance() {
        Object delegateInstance = instantiate(className);
        if (delegateInstance instanceof HttpRequestHandler) {
            return (HttpRequestHandler) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + HttpRequestHandler.class);
        }
    }

    protected HttpResponseHandler getHttpResponseHandlerInstance() {
        Object delegateInstance = instantiate(className);
        if (delegateInstance instanceof HttpResponseHandler) {
            return (HttpResponseHandler) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + HttpResponseHandler.class);
        }
    }

}
