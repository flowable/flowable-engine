package org.flowable.engine.impl.bpmn.http.handler;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.scripting.AbstractScriptEvaluator;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.impl.bpmn.http.delegate.HttpRequestHandlerInvocation;
import org.flowable.engine.impl.bpmn.listener.ScriptExecutingListener;
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
            validateParametersAndEvaluteScript((VariableScope) execution);
        } else {
            throw new FlowableIllegalStateException(
                    "The given execution " + execution.getClass().getName() + " is not of type " + VariableScope.class.getName());
        }
    }

    @Override
    public void handleHttpResponse(VariableContainer execution, HttpResponse httpResponse) {
        if (execution instanceof VariableScope) {
            ((VariableScope) execution).setTransientVariableLocal("httpResponse", httpResponse);
            validateParametersAndEvaluteScript((VariableScope) execution);
        } else {
            throw new FlowableIllegalStateException(
                    "The given execution " + execution.getClass().getName() + " is not of type " + VariableScope.class.getName());
        }
    }
}
