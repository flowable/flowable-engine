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

import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.DeleteHistoricProcessInstancesCmd;
import org.flowable.engine.impl.cmd.DeleteRelatedDataOfRemovedHistoricProcessInstancesCmd;
import org.flowable.engine.impl.cmd.DeleteTaskAndActivityDataOfRemovedHistoricProcessInstancesCmd;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

public class BpmnHistoryCleanupJobHandler implements JobHandler {

    public static final String TYPE = "bpmn-history-cleanup";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        CommandConfig config = new CommandConfig().transactionRequiresNew();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        
        HistoricProcessInstanceQueryImpl historicProcessInstanceQuery = processEngineConfiguration.getHistoryCleaningManager().createHistoricProcessInstanceCleaningQuery();
        commandExecutor.execute(config, new DeleteHistoricProcessInstancesCmd(historicProcessInstanceQuery));
        commandExecutor.execute(config, new DeleteTaskAndActivityDataOfRemovedHistoricProcessInstancesCmd());
        commandExecutor.execute(config, new DeleteRelatedDataOfRemovedHistoricProcessInstancesCmd());
    }
    
}
