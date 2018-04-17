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

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cmd.SuspendProcessDefinitionCmd;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.job.api.Job;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 */
public class TimerSuspendProcessDefinitionHandler extends TimerChangeProcessDefinitionSuspensionStateJobHandler {

    public static final String TYPE = "suspend-processdefinition";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(Job job, String configuration, ExecutionEntity execution, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = commandContext.getProcessEngineConfiguration();

        boolean suspendProcessInstances = false;
        try {
            JsonNode configNode = processEngineConfiguration.getObjectMapper().readTree(configuration);
            suspendProcessInstances = getIncludeProcessInstances(configNode);
        } catch (Exception e) {
            throw new FlowableException("Error reading json value " + configuration, e);
        }

        String processDefinitionId = job.getProcessDefinitionId();

        SuspendProcessDefinitionCmd suspendProcessDefinitionCmd = new SuspendProcessDefinitionCmd(processDefinitionId, null, suspendProcessInstances, null, job.getTenantId());
        suspendProcessDefinitionCmd.execute(commandContext);
    }

}
