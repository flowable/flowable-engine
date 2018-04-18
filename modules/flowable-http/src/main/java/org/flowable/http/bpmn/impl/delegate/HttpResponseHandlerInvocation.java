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

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.impl.delegate.invocation.DelegateInvocation;
import org.flowable.http.HttpResponse;
import org.flowable.http.delegate.HttpResponseHandler;

/**
 * Class handling invocations of {@link HttpResponseHandler HttpResponseHandlers}
 *
 * @author Tijs Rademakers
 */
public class HttpResponseHandlerInvocation extends DelegateInvocation {

    protected final HttpResponseHandler httpResponseHandlerInstance;
    protected final VariableContainer delegateExecution;
    protected final HttpResponse httpResponse;

    public HttpResponseHandlerInvocation(HttpResponseHandler httpResponseHandlerInstance, VariableContainer delegateExecution,
                    HttpResponse httpResponse) {

        this.httpResponseHandlerInstance = httpResponseHandlerInstance;
        this.delegateExecution = delegateExecution;
        this.httpResponse = httpResponse;
    }

    @Override
    protected void invoke() {
        httpResponseHandlerInstance.handleHttpResponse(delegateExecution, httpResponse);
    }

    @Override
    public Object getTarget() {
        return httpResponseHandlerInstance;
    }

}
