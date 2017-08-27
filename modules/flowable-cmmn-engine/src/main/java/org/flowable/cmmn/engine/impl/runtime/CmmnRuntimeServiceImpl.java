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

import org.flowable.cmmn.engine.CmmnRuntimeService;
import org.flowable.cmmn.engine.impl.ServiceImpl;
import org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.TerminateCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.TriggerPlanItemInstanceCmd;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.runtime.MilestoneInstanceQuery;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceQuery;

/**
 * @author Joram Barrez
 */
public class CmmnRuntimeServiceImpl extends ServiceImpl implements CmmnRuntimeService {
    
    @Override
    public CaseInstance startCaseInstanceById(String caseDefinitionId) {
        return commandExecutor.execute(new StartCaseInstanceCmd(caseDefinitionId, null));
    }
    
    @Override
    public CaseInstance startCaseInstanceByKey(String caseDefinitionKey) {
        return commandExecutor.execute(new StartCaseInstanceCmd(null, caseDefinitionKey));
    }
    
    @Override
    public void triggerPlanItemInstance(String planItemInstanceId) {
        commandExecutor.execute(new TriggerPlanItemInstanceCmd(planItemInstanceId));
    }
    
    @Override
    public void terminateCaseInstance(String caseInstanceId) {
        commandExecutor.execute(new TerminateCaseInstanceCmd(caseInstanceId));
    }
    
    @Override
    public CaseInstanceQuery createCaseInstanceQuery() {
        return cmmnEngineConfiguration.getCaseInstanceEntityManager().createCaseInstanceQuery();
    }
    
    @Override
    public PlanItemInstanceQuery createPlanItemQuery() {
        return cmmnEngineConfiguration.getPlanItemInstanceEntityManager().createPlanItemInstanceQuery();
    }
    
    @Override
    public MilestoneInstanceQuery createMilestoneInstanceQuery() {
        return cmmnEngineConfiguration.getMilestoneInstanceEntityManager().createMilestoneInstanceQuery();
    }
    
}
