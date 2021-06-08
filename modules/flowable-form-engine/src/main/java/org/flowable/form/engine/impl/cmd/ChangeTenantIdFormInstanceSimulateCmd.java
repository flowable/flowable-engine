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

package org.flowable.form.engine.impl.cmd;


import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.Key.FormInstances;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeTenantIdFormInstanceSimulateCmd implements Command<ChangeTenantIdResult> {

        private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTenantIdFormInstanceSimulateCmd.class);

        private final String sourceTenantId;
        private final String targetTenantId;
        private final boolean onlyInstancesFromDefaultTenantDefinitions;

        public ChangeTenantIdFormInstanceSimulateCmd(String sourceTenantId, String targetTenantId,
                        boolean onlyInstancesFromDefaultTenantDefinitions) {
                this.sourceTenantId = sourceTenantId;
                this.targetTenantId = targetTenantId;
                this.onlyInstancesFromDefaultTenantDefinitions = onlyInstancesFromDefaultTenantDefinitions;
        }

        @Override
        public ChangeTenantIdResult execute(CommandContext commandContext) {
                LOGGER.debug("Simulating Form Instance migration from '{}' to '{}'{}.", sourceTenantId, targetTenantId,
                                onlyInstancesFromDefaultTenantDefinitions
                                                ? " but only for instances from the default tenant definitions"
                                                : "");
                FormEngineConfiguration formEngineConfiguration = CommandContextUtil.getFormEngineConfiguration(commandContext);
                long changeTenantIdBpmnFormInstances = formEngineConfiguration.getFormInstanceEntityManager()
                                                .countChangeTenantIdFormInstances(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.BPMN);
                long changeTenantIdCmmnFormInstances = formEngineConfiguration.getFormInstanceEntityManager()
                                                .countChangeTenantIdFormInstances(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.CMMN);
                return ChangeTenantIdResult.builder()
                                .addResult(FormInstances,changeTenantIdBpmnFormInstances + changeTenantIdCmmnFormInstances)
                                .build();
        }

}