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
import org.flowable.cmmn.engine.impl.util.DelegateExpressionUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.impl.cfg.DelegateExpressionFieldInjectionMode;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.http.HttpRequest;
import org.flowable.http.HttpResponse;
import org.flowable.http.delegate.HttpRequestHandler;
import org.flowable.http.delegate.HttpResponseHandler;

import java.util.List;

/**
 * @author Tijs Rademakers
 */
public class DelegateExpressionHttpHandler implements HttpRequestHandler, HttpResponseHandler {

    private static final long serialVersionUID = 1L;

    protected Expression expression;
    protected final List<FieldExtension> fieldExtensions;

    public DelegateExpressionHttpHandler(Expression expression, List<FieldExtension> fieldDeclarations) {
        this.expression = expression;
        this.fieldExtensions = fieldDeclarations;
    }

    @Override
    public void handleHttpRequest(VariableContainer execution, HttpRequest httpRequest, HttpClient client) {
        Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, execution, fieldExtensions);
        if (delegate instanceof HttpRequestHandler) {
            ((HttpRequestHandler) delegate).handleHttpRequest(execution, httpRequest, client);
        } else {
            throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did not resolve to an implementation of " + HttpRequestHandler.class);
        }
    }

    @Override
    public void handleHttpResponse(VariableContainer execution, HttpResponse httpResponse) {
        Object delegate = resolveDelegateExpression(expression, execution, fieldExtensions);
        if (delegate instanceof HttpResponseHandler) {
            ((HttpResponseHandler) delegate).handleHttpResponse(execution, httpResponse);
        } else {
            throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did not resolve to an implementation of " + HttpResponseHandler.class);
        }
    }

    /**
     * returns the expression text for this execution listener. Comes in handy if you want to check which listeners you already have.
     */
    public String getExpressionText() {
        return expression.getExpressionText();
    }

    public static Object resolveDelegateExpression(Expression expression,
                                                   VariableContainer variableScope, List<FieldExtension> fieldExtensions) {

        // Note: we can't cache the result of the expression, because the
        // execution can change: eg. delegateExpression='${mySpringBeanFactory.randomSpringBean()}'
        Object delegate = expression.getValue(variableScope);

        if (fieldExtensions != null && fieldExtensions.size() > 0) {

            DelegateExpressionFieldInjectionMode injectionMode = CommandContextUtil.getProcessEngineConfiguration().getDelegateExpressionFieldInjectionMode();
            if (injectionMode == DelegateExpressionFieldInjectionMode.COMPATIBILITY) {
                CmmnClassDelegate.applyFieldExtensions(fieldExtensions, delegate, variableScope, true);
            } else if (injectionMode == DelegateExpressionFieldInjectionMode.MIXED) {
                CmmnClassDelegate.applyFieldExtensions(fieldExtensions, delegate, variableScope, false);
            }

        }

        return delegate;
    }

}
