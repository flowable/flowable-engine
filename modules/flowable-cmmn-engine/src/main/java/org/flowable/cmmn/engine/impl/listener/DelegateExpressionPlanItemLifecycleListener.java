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
package org.flowable.cmmn.engine.impl.listener;

import java.util.List;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.engine.impl.util.DelegateExpressionUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.task.service.delegate.TaskListener;

/**
 * @author Joram Barrez
 */
public class DelegateExpressionPlanItemLifecycleListener implements PlanItemInstanceLifecycleListener {

    protected String sourceState;
    protected String targetState;
    protected Expression expression;
    protected List<FieldExtension> fieldExtensions;

    public DelegateExpressionPlanItemLifecycleListener(String sourceState, String targetState, Expression expression,
        List<FieldExtension> fieldExtensions) {
        this.sourceState = sourceState;
        this.targetState = targetState;
        this.expression = expression;
        this.fieldExtensions = fieldExtensions;
    }

    @Override
    public String getSourceState() {
        return sourceState;
    }

    @Override
    public String getTargetState() {
        return targetState;
    }

    @Override
    public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
        try {
            DelegatePlanItemInstance delegatePlanItemInstance = (DelegatePlanItemInstance) planItemInstance;
            Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, (DelegatePlanItemInstance) planItemInstance, fieldExtensions);

            if (delegate instanceof PlanItemInstanceLifecycleListener) {
                try {
                    PlanItemInstanceLifecycleListener listener = (PlanItemInstanceLifecycleListener) delegate;
                    listener.stateChanged(delegatePlanItemInstance, oldState, newState);
                } catch (Exception e) {
                    throw new FlowableException("Exception while invoking PlanItemInstanceLifecycleListener: " + e.getMessage(), e);
                }
            } else {
                throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did not resolve to an implementation of " + PlanItemInstanceLifecycleListener.class);
            }

        } catch (Exception e) {
            throw new FlowableException(e.getMessage(), e);
        }
    }

    /**
     * returns the expression text for this planItemInstance lifecycle listener.
     */
    public String getExpressionText() {
        return expression.getExpressionText();
    }

}
