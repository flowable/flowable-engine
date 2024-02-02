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
package org.activiti.engine.impl.jobexecutor;

import java.util.List;

import org.activiti.engine.impl.bpmn.helper.ErrorPropagation;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntityManager;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.job.api.Job;


/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerTaskCompleteJobHandler implements JobHandler {

    public static final String TYPE = "external-worker-complete";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(Job job, String configuration, ExecutionEntity execution, CommandContext commandContext) {
        VariableInstanceEntityManager variableInstanceEntityManager = commandContext.getVariableInstanceEntityManager();
        List<VariableInstanceEntity> jobVariables = variableInstanceEntityManager.findVariableInstancesBySubScopeIdAndScopeType(execution.getId(), ScopeTypes.BPMN_EXTERNAL_WORKER);
        for (VariableInstanceEntity jobVariable : jobVariables) {
            execution.setVariable(jobVariable.getName(), jobVariable.getValue());
            variableInstanceEntityManager.delete(jobVariable);
        }

        if (configuration != null && configuration.startsWith("error:")) {
            String errorCode;
            if (configuration.length() > 6) {
                errorCode = configuration.substring(6);
            } else {
                errorCode = null;
            }
            ErrorPropagation.propagateError(errorCode, execution);
            
        } else {
            execution.signal(null, null);
        }
    }

}
