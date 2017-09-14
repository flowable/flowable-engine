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

import java.io.Serializable;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class StartCaseInstanceCmd implements Command<CaseInstance>, Serializable {

    protected String caseDefinitionId;
    protected String caseDefinitionKey;
    
    // TODO: add params to cmmnruntimeservice
    protected String businessKey;
    protected String initiator;
    protected String tenantId;

    public StartCaseInstanceCmd(String caseDefinitionId, String caseDefinitionKey) {
        this.caseDefinitionId = caseDefinitionId;
        this.caseDefinitionKey = caseDefinitionKey;
    }

    @Override
    public CaseInstance execute(CommandContext commandContext) {
        if (caseDefinitionId != null) {
            return CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                    .getCaseInstanceHelper().startCaseInstanceById(commandContext, caseDefinitionId);
            
        } else if (caseDefinitionKey != null) {
            return CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                    .getCaseInstanceHelper().startCaseInstanceByKey(commandContext, caseDefinitionKey);
            
        } else {
            throw new FlowableIllegalArgumentException("Cannot start case instance: no casedefinition id nor key provided");
            
        }
    }
   
}
