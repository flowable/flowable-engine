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

import java.util.Map;

import org.flowable.cmmn.engine.CmmnRuntimeService;
import org.flowable.cmmn.engine.impl.ServiceImpl;
import org.flowable.cmmn.engine.impl.cmd.GetVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.GetVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.RemoveVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.SetVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.TerminateCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.TriggerPlanItemInstanceCmd;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.engine.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.runtime.MilestoneInstanceQuery;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceQuery;

/**
 * @author Joram Barrez
 */
public class CmmnRuntimeServiceImpl extends ServiceImpl implements CmmnRuntimeService {

    @Override
    public CaseInstanceBuilder createCaseInstanceBuilder() {
        return new CaseInstanceBuilderImpl(this);
    }

    public CaseInstance startCaseInstance(CaseInstanceBuilder caseInstanceBuilder) {
        return commandExecutor.execute(new StartCaseInstanceCmd(caseInstanceBuilder));
    }

    @Override public void triggerPlanItemInstance(String planItemInstanceId) {
        commandExecutor.execute(new TriggerPlanItemInstanceCmd(planItemInstanceId));
    }

    @Override
    public void terminateCaseInstance(String caseInstanceId) {
        commandExecutor.execute(new TerminateCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public Map<String, Object> getVariables(String caseInstanceId) {
        return commandExecutor.execute(new GetVariablesCmd(caseInstanceId));
    }

    @Override
    public Object getVariable(String caseInstanceId, String variableName) {
        return commandExecutor.execute(new GetVariableCmd(caseInstanceId, variableName));
    }

    @Override
    public void setVariables(String caseInstanceId, Map<String, Object> variables) {
        commandExecutor.execute(new SetVariablesCmd(caseInstanceId, variables));
    }

    @Override
    public void removeVariable(String caseInstanceId, String variableName) {
        commandExecutor.execute(new RemoveVariableCmd(caseInstanceId, variableName));
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
