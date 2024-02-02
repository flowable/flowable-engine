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
package org.flowable.engine.impl.bpmn.helper;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;

/**
 * An {@link FlowableEventListener} implementation which resolves an expression to a delegate {@link FlowableEventListener} instance and uses this for event notification. <br>
 * <br>
 * In case an entityClass was passed in the constructor, only events that are {@link FlowableEntityEvent}'s that target an entity of the given type, are dispatched to the delegate.
 * 
 * @author Frederik Heremans
 */
public class DelegateExpressionFlowableEventListener extends BaseDelegateEventListener {

    protected Expression expression;
    protected boolean failOnException;

    public DelegateExpressionFlowableEventListener(Expression expression, Class<?> entityClass) {
        this.expression = expression;
        setEntityClass(entityClass);
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (isValidEvent(event)) {
            Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, new NoExecutionVariableScope());
            if (delegate instanceof FlowableEventListener) {
                // Cache result of isFailOnException() from delegate-instance
                // until next event is received. This prevents us from having to resolve
                // the expression twice when an error occurs.
                failOnException = ((FlowableEventListener) delegate).isFailOnException();

                // Call the delegate
                ((FlowableEventListener) delegate).onEvent(event);
            } else {

                // Force failing, since the exception we're about to throw
                // cannot be ignored, because it did not originate from the listener itself
                failOnException = true;
                throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did not resolve to an implementation of " + FlowableEventListener.class.getName());
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        return failOnException;
    }

}
