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
package org.flowable.cmmn.engine.impl.util;

import java.util.Map;

import org.flowable.cmmn.api.delegate.CmmnFault;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Handles propagation of a {@link CmmnFault} by storing fault information as transient variables
 * on the plan item instance and planning the fail operation.
 *
 * The sentry mechanism then handles the rest automatically: the fail operation fires a
 * {@code PlanItemLifeCycleEvent} with the "fault" transition, which triggers criteria evaluation
 * for any sentries with {@code standardEvent="fault"}.
 *
 * @author Joram Barrez
 */
public class FaultPropagation {

    public static void propagateFault(CmmnFault fault, CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        // Store fault info as transient variables so they're available during sentry evaluation
        planItemInstanceEntity.setTransientVariable("faultCode", fault.getFaultCode());
        if (fault.getMessage() != null && !fault.getMessage().isEmpty()) {
            planItemInstanceEntity.setTransientVariable("faultMessage", fault.getMessage());
        }

        // If additional data was provided, store each entry as a transient variable
        Map<String, Object> additionalData = fault.getAdditionalData();
        if (additionalData != null) {
            for (Map.Entry<String, Object> entry : additionalData.entrySet()) {
                planItemInstanceEntity.setTransientVariable(entry.getKey(), entry.getValue());
            }
        }

        CommandContextUtil.getAgenda(commandContext).planFailPlanItemInstanceOperation(planItemInstanceEntity);
    }
}
