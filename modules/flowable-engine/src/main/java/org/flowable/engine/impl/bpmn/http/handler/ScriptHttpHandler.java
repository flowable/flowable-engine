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

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.scripting.AbstractScriptEvaluator;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.HttpResponse;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.http.common.api.delegate.HttpRequestHandler;
import org.flowable.http.common.api.delegate.HttpResponseHandler;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * Scripting capable implementation of HttpRequest and HttpResponse handler.
 */
public class ScriptHttpHandler extends AbstractScriptEvaluator implements HttpRequestHandler, HttpResponseHandler {

    public ScriptHttpHandler(Expression language, Expression script) {
        this.script = script;
        this.language = language;
    }

    @Override
    protected ScriptingEngines getScriptingEngines() {
        return CommandContextUtil.getProcessEngineConfiguration().getScriptingEngines();
    }

    @Override
    public void handleHttpRequest(VariableContainer execution, HttpRequest httpRequest, FlowableHttpClient client) {
        if (execution instanceof VariableScope) {
            ((VariableScope) execution).setTransientVariableLocal("httpRequest", httpRequest);
            evaluateScriptRequest(createScriptRequest(execution));
        } else {
            throw new FlowableIllegalStateException(
                    "The given execution " + execution.getClass().getName() + " is not of type " + VariableScope.class.getName());
        }
    }

    @Override
    public void handleHttpResponse(VariableContainer execution, HttpResponse httpResponse) {
        if (execution instanceof VariableScope) {
            ((VariableScope) execution).setTransientVariableLocal("httpResponse", httpResponse);
            evaluateScriptRequest(createScriptRequest(execution));
        } else {
            throw new FlowableIllegalStateException(
                    "The given execution " + execution.getClass().getName() + " is not of type " + VariableScope.class.getName());
        }
    }
}
