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
package org.flowable.engine.impl.cmd;

import org.flowable.batch.api.Batch;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;

public class GetBatchDocumentCmd implements Command<String> {
    
    protected String batchId;
    
    public GetBatchDocumentCmd(String batchId) {
        this.batchId = batchId;
    }
    
    @Override
    public String execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        Batch batch = processEngineConfiguration.getBatchServiceConfiguration().getBatchService().getBatch(batchId);
        if (batch == null) {
            throw new FlowableObjectNotFoundException("No batch found for id " + batchId);
        }
        
        return batch.getBatchDocumentJson(processEngineConfiguration.getEngineCfgKey());
    }
}
