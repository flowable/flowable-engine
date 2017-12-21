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
package org.flowable.engine.impl.jobexecutor;

import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cmd.SuspendProcessDefinitionCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    public void execute(JobEntity job, String configuration, Object execution, CommandContext commandContext) {
    	JsonObject cfgJson = new JsonParser().parse(configuration).getAsJsonObject();
        String processDefinitionId = job.getProcessDefinitionId();
        boolean suspendProcessInstances = getIncludeProcessInstances(cfgJson);

        SuspendProcessDefinitionCmd suspendProcessDefinitionCmd = new SuspendProcessDefinitionCmd(processDefinitionId, null, suspendProcessInstances, null, job.getTenantId());
        suspendProcessDefinitionCmd.execute(commandContext);
    }

}
