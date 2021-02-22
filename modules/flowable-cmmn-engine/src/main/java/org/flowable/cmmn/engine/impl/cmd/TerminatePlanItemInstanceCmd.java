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
package org.flowable.cmmn.engine.impl.cmd;

import java.util.Map;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormInfo;

/**
 * @author Joram Barrez
 */
public class TerminatePlanItemInstanceCmd extends AbstractNeedsPlanItemInstanceCmd {

    public TerminatePlanItemInstanceCmd(String planItemInstanceId) {
        super(planItemInstanceId);
    }

    public TerminatePlanItemInstanceCmd(String planItemInstanceId, Map<String, Object> variables,
            Map<String, Object> formVariables, String formOutcome, FormInfo formInfo,
            Map<String, Object> localVariables,
            Map<String, Object> transientVariables) {
        
        super(planItemInstanceId, variables, formVariables, formOutcome, formInfo, localVariables, transientVariables);
    }

    @Override
    protected void internalExecute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity, null, null);
    }
    
}
