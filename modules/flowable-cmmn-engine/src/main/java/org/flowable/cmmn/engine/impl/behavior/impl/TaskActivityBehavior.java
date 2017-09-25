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
package org.flowable.cmmn.engine.impl.behavior.impl;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.impl.behavior.CmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.Task;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class TaskActivityBehavior implements CmmnTriggerableActivityBehavior {
    
    protected boolean isBlocking;
    protected String isBlockingExpression;
    
    public TaskActivityBehavior(Task task) {
        this.isBlocking = task.isBlocking();
        this.isBlockingExpression = task.getBlockingExpression();
    }


    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        if (!determineIsBlocking(planItemInstance)) {
            CommandContextUtil.getAgenda().planCompletePlanItem((PlanItemInstanceEntity) planItemInstance);
        }
    }

    protected boolean determineIsBlocking(DelegatePlanItemInstance planItemInstance) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        boolean blocking = isBlocking;
        if (StringUtils.isNotEmpty(isBlockingExpression)) {
            Expression expression = CommandContextUtil.getExpressionManager(commandContext).createExpression(isBlockingExpression);
            blocking = (Boolean) expression.getValue(planItemInstance);
        }
        return blocking;
    }

    @Override
    public void trigger(DelegatePlanItemInstance planItemInstance) {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            throw new FlowableException("Can only trigger a plan item that is in the ACTIVE state");
        }
        CommandContextUtil.getAgenda().planCompletePlanItem((PlanItemInstanceEntity) planItemInstance);
    }

}
