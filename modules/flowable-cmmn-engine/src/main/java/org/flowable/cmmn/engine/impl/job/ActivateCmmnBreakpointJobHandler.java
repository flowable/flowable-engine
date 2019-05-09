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

package org.flowable.cmmn.engine.impl.job;

import static org.flowable.cmmn.engine.impl.agenda.operation.DebugStartPlanItemInstanceOperation.START_PLAN_ITEM_INSTANCE_OPERATION;

import java.io.IOException;

import org.flowable.cmmn.engine.impl.agenda.operation.StartPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Continue in the broken cmmn execution
 *
 * @author martin.grofcik
 */
public class ActivateCmmnBreakpointJobHandler implements JobHandler {

    public static final String CMMN_BREAKPOINT = "cmmn-breakpoint";

    @Override
    public String getType() {
        return CMMN_BREAKPOINT;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ObjectMapper objectMapper = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getObjectMapper();
        try {
            JsonNode configurationObject = objectMapper.readTree(configuration);
            String operation = null;
            if (configurationObject.has("operation")) {
                operation = configurationObject.get("operation").textValue();
            } else {
                throw new FlowableException("operation is mandatory for cmmn-breakpoint");
            }
            switch (operation) {
                case START_PLAN_ITEM_INSTANCE_OPERATION:
                    if (variableScope instanceof PlanItemInstanceEntity) {
                        PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) variableScope;
                        String entryCriterionId = configurationObject.get("entryCriterionId").asText();
                        CommandContextUtil.getAgenda(commandContext)
                            .planOperation(new StartPlanItemInstanceOperation(commandContext, planItemInstanceEntity, entryCriterionId));
                    } else {
                        throw new FlowableException(
                            "Invalid usage of " + CMMN_BREAKPOINT + " job handler, variable scope is of type " + variableScope.getClass());
                    }
                    break;
                default:
                    throw new FlowableException("Unsupported operation '" + operation + "'");
            }
        } catch (IOException e) {
            throw new FlowableException("Configuration "+ configuration +" is not valid.", e);
        }
    }
}
