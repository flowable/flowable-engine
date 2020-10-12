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

package org.flowable.cmmn.engine.impl.runtime;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class DefaultCmmnDynamicStateManager extends AbstractCmmnDynamicStateManager implements CmmnDynamicStateManager {
    
    public DefaultCmmnDynamicStateManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public void movePlanItemInstanceState(ChangePlanItemStateBuilderImpl changePlanItemStateBuilder, CommandContext commandContext) {
        String caseInstanceId = changePlanItemStateBuilder.getCaseInstanceId();
        
        if (caseInstanceId == null) {
            throw new FlowableException("Could not resolve case instance id");
        }
        
        CaseInstanceChangeState caseInstanceChangeState = new CaseInstanceChangeState()
            .setCaseInstanceId(caseInstanceId)
            .setActivatePlanItemDefinitions(changePlanItemStateBuilder.getActivatePlanItemDefinitions())
            .setTerminatePlanItemDefinitions(changePlanItemStateBuilder.getTerminatePlanItemDefinitions())
            .setChangePlanItemDefinitionsToAvailable(changePlanItemStateBuilder.getChangeToAvailableStatePlanItemDefinitions())
            .setCaseVariables(changePlanItemStateBuilder.getCaseVariables())
            .setChildInstanceTaskVariables(changePlanItemStateBuilder.getChildInstanceTaskVariables());
        
        doMovePlanItemState(caseInstanceChangeState, commandContext);
    }

    @Override
    protected boolean isDirectPlanItemDefinitionMigration(PlanItemDefinition currentPlanItemDefinition, PlanItemDefinition newPlanItemDefinition) {
        return false;
    }

}
