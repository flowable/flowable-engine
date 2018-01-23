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
package org.flowable.engine.impl.bpmn.listener;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.api.delegate.event.TransactionFlowableEventListener;
import org.flowable.engine.impl.bpmn.helper.BaseDelegateTransactionEventListener;
import org.flowable.engine.impl.bpmn.helper.DelegateExpressionUtil;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class DelegateExpressionTransactionFlowableEventListener extends BaseDelegateTransactionEventListener {

    protected Expression expression;
    protected String state;
    protected boolean failOnException = false;

    public DelegateExpressionTransactionFlowableEventListener(Expression expression, Class<?> entityClass, String transaction) {
        this.expression = expression;
        this.state = transaction;
        this.entityClass = entityClass;
    }

    @Override
    public String getOnTransaction() {
        return state;
    }

    @Override
    public void setOnTransaction(String state) {
        this.state = state;

    }

    public void onEvent(FlowableEvent event) {
        if (isValidEvent(event)) {
            Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, new NoExecutionVariableScope());
            if (delegate instanceof TransactionFlowableEventListener) {
                // Cache result of isFailOnException() from delegate-instance
                // until next event is received. This prevents us from having to resolve
                // the expression twice when an error occurs.
                failOnException = ((TransactionFlowableEventListener) delegate).isFailOnException();

                // Call the delegate
                ((TransactionFlowableEventListener) delegate).onEvent(event);
            } else {

                // Force failing, since the exception we're about to throw
                // cannot be ignored, because it did not originate from the listener itself
                failOnException = true;
                throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did not resolve to an implementation of " + TransactionFlowableEventListener.class.getName());
            }

        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

}
