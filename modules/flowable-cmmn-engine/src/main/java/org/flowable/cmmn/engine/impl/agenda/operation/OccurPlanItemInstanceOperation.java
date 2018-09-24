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
package org.flowable.cmmn.engine.impl.agenda.operation;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class OccurPlanItemInstanceOperation extends AbstractDeletePlanItemInstanceOperation {

    public OccurPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }
    
    @Override
    protected String getNewState() {
        return PlanItemInstanceState.COMPLETED;
    }
    
    @Override
    protected String getLifeCycleTransition() {
        return PlanItemTransition.OCCUR;
    }
    
    @Override
    protected boolean isEvaluateRepetitionRule() {
        return false;
    }
    
    @Override
    protected void internalExecute() {
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceOccurred(planItemInstanceEntity);
    }
    
}
