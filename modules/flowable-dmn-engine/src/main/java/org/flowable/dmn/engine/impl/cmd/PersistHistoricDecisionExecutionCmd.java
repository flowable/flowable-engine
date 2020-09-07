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
package org.flowable.dmn.engine.impl.cmd;

import org.apache.commons.lang3.BooleanUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntityManager;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Yvo Swillens
 */
public class PersistHistoricDecisionExecutionCmd implements Command<Void> {

    protected ExecuteDecisionContext executeDecisionContext;

    public PersistHistoricDecisionExecutionCmd(ExecuteDecisionContext executeDecisionContext) {
        this.executeDecisionContext = executeDecisionContext;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        if (executeDecisionContext == null) {
            throw new FlowableIllegalArgumentException("ExecuteDecisionContext is null");
        }

        DmnEngineConfiguration engineConfiguration = CommandContextUtil.getDmnEngineConfiguration();

        if (engineConfiguration.isHistoryEnabled()) {
            HistoricDecisionExecutionEntityManager historicDecisionExecutionEntityManager = engineConfiguration.getHistoricDecisionExecutionEntityManager();
            HistoricDecisionExecutionEntity decisionExecutionEntity = historicDecisionExecutionEntityManager.create();
            decisionExecutionEntity.setDecisionDefinitionId(executeDecisionContext.getDecisionId());
            decisionExecutionEntity.setDeploymentId(executeDecisionContext.getDeploymentId());
            decisionExecutionEntity.setStartTime(executeDecisionContext.getDecisionExecution().getStartTime());
            decisionExecutionEntity.setEndTime(executeDecisionContext.getDecisionExecution().getEndTime());
            decisionExecutionEntity.setInstanceId(executeDecisionContext.getInstanceId());
            decisionExecutionEntity.setExecutionId(executeDecisionContext.getExecutionId());
            decisionExecutionEntity.setActivityId(executeDecisionContext.getActivityId());
            decisionExecutionEntity.setScopeType(executeDecisionContext.getScopeType());
            decisionExecutionEntity.setTenantId(executeDecisionContext.getTenantId());

            Boolean failed = executeDecisionContext.getDecisionExecution().isFailed();
            if (BooleanUtils.isTrue(failed)) {
                decisionExecutionEntity.setFailed(failed.booleanValue());
            }

            ObjectMapper objectMapper = engineConfiguration.getObjectMapper();
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
            }

            try {
                decisionExecutionEntity.setExecutionJson(objectMapper.writeValueAsString(executeDecisionContext.getDecisionExecution()));
            } catch (Exception e) {
                throw new FlowableException("Error writing execution json", e);
            }

            historicDecisionExecutionEntityManager.insert(decisionExecutionEntity);
        }

        return null;
    }
}
