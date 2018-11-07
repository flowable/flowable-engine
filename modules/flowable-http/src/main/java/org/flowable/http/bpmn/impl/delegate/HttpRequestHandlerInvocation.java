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
package org.flowable.http.bpmn.impl.delegate;

import org.apache.http.client.HttpClient;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.impl.delegate.invocation.DelegateInvocation;
import org.flowable.http.HttpRequest;
import org.flowable.http.delegate.HttpRequestHandler;

/**
 * Class handling invocations of {@link HttpRequestHandler HttpRequestHandlers}
 *
 * @author Tijs Rademakers
 */
public class HttpRequestHandlerInvocation extends DelegateInvocation {

    protected final HttpRequestHandler httpRequestHandlerInstance;
    protected final VariableContainer delegateExecution;
    protected final HttpRequest httpRequest;
    protected final HttpClient client;

    public HttpRequestHandlerInvocation(HttpRequestHandler httpRequestHandlerInstance, VariableContainer delegateExecution,
                    HttpRequest httpRequest, HttpClient client) {

        this.httpRequestHandlerInstance = httpRequestHandlerInstance;
        this.delegateExecution = delegateExecution;
        this.httpRequest = httpRequest;
        this.client = client;
    }

    @Override
    protected void invoke() {
        httpRequestHandlerInstance.handleHttpRequest(delegateExecution, httpRequest, client);
    }

    @Override
    public Object getTarget() {
        return httpRequestHandlerInstance;
    }

}
